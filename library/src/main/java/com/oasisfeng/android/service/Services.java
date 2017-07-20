package com.oasisfeng.android.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Process;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplify the AIDL service usage
 *
 * Created by Oasis on 2015/5/19.
 */
public class Services {

	public interface ServiceReadyThrows<I extends IInterface, E extends Exception> {
		void onServiceReady(I service) throws E;
	}

	public interface AidlStub<I extends IInterface> {
		I asInterface(IBinder obj);
	}

	/** Bind to the specified service for one-time procedure, then unbind the service. */
	public static <I extends IInterface, E extends Exception> boolean use(final Context context, final Class<I> itf, final AidlStub<I> stub, final ServiceReadyThrows<I, E> procedure) {
		return bind(context, itf, new ServiceConnection() {
			@Override public void onServiceConnected(final ComponentName name, final IBinder binder) {
				final I service = stub.asInterface(binder);
				try {
					procedure.onServiceReady(service);
				} catch (final Exception e) {
					Log.e(TAG, "Error in " + procedure, e);
				} finally {
					try { context.unbindService(this); } catch (final RuntimeException ignored) {}
				}
			}

			@Override public void onServiceDisconnected(final ComponentName name) {}
			@Override public String toString() { return "ServiceConnection{" + procedure.toString() + "}"; }	// About the source of service invocation
		});
	}

	public static boolean bind(final Context context, final Class<? extends IInterface> service_interface, final ServiceConnection conn) {
		final Intent service_intent = buildServiceIntent(context, service_interface);
		if (service_intent == null) {
			Log.w(TAG, "No matched service for " + service_interface.getName());
			return false;
		}
		return context.bindService(service_intent, conn, Context.BIND_AUTO_CREATE);
	}

	public static @CheckResult @Nullable <I extends IInterface> I bindLocal(final Context context, final Class<I> service_interface) {
		if (! sRegistry.isEmpty()) {
			@SuppressWarnings("unchecked") final I instance = (I) sRegistry.get(service_interface);
			if (instance != null) return instance;
		}
		return LocalAidlServices.bind(context, service_interface, buildServiceIntent(context, service_interface));
	}

	public static @Nullable Service createLocal(final Context context, final String classname) {
		return LocalAidlServices.createService(context, classname);
	}

	public static void destroyLocal(final Service service) {
		LocalAidlServices.destroyService(service);
	}

	public static <I extends IInterface> void unbindLocal(final Context context, final I instance) {
		if (sRegistry.containsValue(instance)) return;		// TODO: Reference counting registered services
		LocalAidlServices.unbind(context, instance);
	}

	public static <I extends IInterface> void register(final Class<I> service_interface, final I instance) {
		final IInterface existent = sRegistry.get(service_interface);
		if (existent != null) {
			if (existent == instance) return;
			throw new IllegalStateException(service_interface + " was already registered with " + existent.getClass());
		}
		sRegistry.put(service_interface, instance);
	}

	public static <I extends IInterface> boolean unregister(final Class<I> service_interface, final I instance) {
		final IInterface existent = sRegistry.get(service_interface);
		if (existent == null) return false;
		if (instance != existent) throw new IllegalStateException(service_interface + " was registered with " + existent + ", but being unregistered with " + instance);
		sRegistry.remove(service_interface);
		return true;
	}

	public static <I extends IInterface> I peekService(final Context context, final Class<I> service_interface) {
		final IBinder binder = peekService(context, buildServiceIntent(context, service_interface));
		if (binder == null) return null;
		try {	//noinspection unchecked
			return (I) service_interface.getMethod("asInterface", IBinder.class).invoke(binder);
		} catch (final Exception e) {
			Log.e(TAG, "Error calling " + service_interface.getCanonicalName() + ".asInterface() on " + binder, e);
			return null;
		}
	}

	public static IBinder peekService(final Context context, final Intent intent) {
		return sDummyReceiver.peekService(context, intent);
	}

	private static @Nullable Intent buildServiceIntent(final Context context, final Class<?> service_interface) {
		final String name = service_interface.getName(); final Intent intent = new Intent(name);
		final PackageManager pm = context.getPackageManager(); final int uid = Process.myUid();

		final ComponentName component = resolveServiceIntent(context, intent, new Comparator<ResolveInfo>() { @Override public int compare(final ResolveInfo left, final ResolveInfo right) {
			final ApplicationInfo app_left = left.serviceInfo.applicationInfo, app_right = right.serviceInfo.applicationInfo;
			// 1 - UID
			int rank_left = app_left.uid != uid ? 1 : 0;
			int rank_right = app_right.uid != uid ? 1 : 0;
			if (rank_left != rank_right) return rank_left - rank_right;
			// 2 - Priority
			final int priority_diff = left.priority - right.priority;
			if (priority_diff != 0) return (- priority_diff);        // Higher priority wins
			// 3 - Package update time
			if (! app_left.packageName.equals(app_right.packageName)) {
				final PackageInfo pkg_left, pkg_right;
				try {
					pkg_left = pm.getPackageInfo(app_left.packageName, 0);
				} catch (final PackageManager.NameNotFoundException e) {
					return 1;
				}        // right wins
				try {
					pkg_right = pm.getPackageInfo(app_right.packageName, 0);
				} catch (final PackageManager.NameNotFoundException e) {
					return - 1;
				}        // left wins
				if (pkg_left.lastUpdateTime != pkg_right.lastUpdateTime)
					return (int) - (pkg_left.lastUpdateTime - pkg_right.lastUpdateTime);    // Newer wins
			}
			// 4 - Package name
			final String pkg = context.getPackageName();
			rank_left = pkg.equals(app_left.packageName) ? 0 : 1;
			rank_right = pkg.equals(app_right.packageName) ? 0 : 1;
			if (rank_left != rank_right) return rank_left - rank_right;
			return 0;
		}});
		if (component == null) return null;
		intent.setComponent(component);
		return intent;
	}

	/** @param comparator comparator for resolving candidates, smaller wins */
	private static @Nullable ComponentName resolveServiceIntent(final Context context, final Intent intent, final Comparator<ResolveInfo> comparator) {
		final List<ResolveInfo> matches = context.getPackageManager().queryIntentServices(intent, 0);
		if (matches == null || matches.isEmpty()) return null;

		ResolveInfo best_match = matches.get(0);
		final int my_uid = Process.myUid();
		for (final ResolveInfo match : matches) {
			if (! match.serviceInfo.exported && match.serviceInfo.applicationInfo.uid != my_uid) continue;
			if (best_match != match && comparator.compare(match, best_match) < 0)
				best_match = match;
		}

		final ComponentName component = new ComponentName(best_match.serviceInfo.packageName, best_match.serviceInfo.name);
		if (matches.size() > 1) Log.w(TAG, "Final match for " + intent + " among " + matches.size() + ": " + component.flattenToShortString());

		return component;
	}

	private static final Map<Class, IInterface> sRegistry = new IdentityHashMap<>();
	private static final BroadcastReceiver sDummyReceiver = new BroadcastReceiver() { @Override public void onReceive(final Context context, final Intent intent) {}};
	private static final String TAG = "Services";
}
