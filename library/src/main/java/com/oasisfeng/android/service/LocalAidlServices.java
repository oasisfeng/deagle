package com.oasisfeng.android.service;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Debug;
import android.os.IBinder;
import android.os.IInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.oasisfeng.android.util.MultiCatchROECompat;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Optimized implementation for local-only service.
 *
 * No more AMS IPC involved, no more asynchronous pains.
 *
 * @author Oasis
 */
class LocalAidlServices {

	/**
	 * Two incoming paths:
	 *   Local-binding: bindWith() -> AidlService.onCreate(), onBind() -> bindWith()
	 *   Sys-binding: ActivityThread.handleCreateService() -> AidlService.onCreate(), onBind() -> bindWith()
	 *
	 * @param intent must be explicit (with component pre-resolved)
	 */
	static @Nullable <I extends IInterface> I bind(final Context context, @NonNull final Class<I> service_interface, final Intent intent) {
		if (intent.getComponent() == null)
			throw new IllegalArgumentException("Intent must be explicit (with component set)");
		//noinspection SynchronizationOnLocalVariableOrMethodParameter,
		synchronized (service_interface) {	// Use interface class as a sparse lock to avoid locking sServices during service initialization.
			return bindLocked(context, service_interface, intent);
		}
	}

	private static @Nullable <I extends IInterface> I bindLocked(final Context context, @NonNull final Class<I> service_interface, final Intent intent) {
		final IBinder binder;
		ServiceRecord record = sServices.get(service_interface);
		if (record == null) {
			final IBinder sys_binder = sDummyReceiver.peekService(context, intent);
			if (sys_binder != null) {	// The service is already bound by system, initiate a new binding via AMS.
				record = new ServiceRecord(service_interface, null, sys_binder);
				if (! context.bindService(intent, record, Context.BIND_AUTO_CREATE)) {
					Log.e(TAG, "Failed to bind service with " + intent);
					return null;
				}
			} else {	// Create the service instance locally
				final Service service = createService(context, intent.getComponent().getClassName());
				if (service == null) return null;
				binder = bindService(service, intent);
				if (binder == null) {
					destroyService(service);
					return null;
				}
				record = new ServiceRecord(service_interface, service, binder);
			}

			sServices.put(service_interface, record);
		}

		// Wrap with dynamic proxy, which is later used to differentiate instances in unbind().
		final ProxyHandler handler = new ProxyHandler(service_interface, record.binder);
		final Class[] interfaces = collectInterfacesToProxy(record.binder, service_interface);
		@SuppressWarnings("unchecked") final I instance = (I) Proxy.newProxyInstance(context.getClassLoader(), interfaces, handler);
		record.instances.add(instance);
		return instance;
	}

	private static Class[] collectInterfacesToProxy(final Object impl, final Class<?> service_interface) {
		final Class<?>[] direct_interfaces = impl.getClass().getInterfaces();
		if (direct_interfaces.length == 0) return new Class[] { service_interface };
		for (int i = 0; i < direct_interfaces.length; i ++) {
			final Class<?> direct_interface = direct_interfaces[i];
			if (direct_interface == Closeable.class || direct_interface == AutoCloseable.class) {	// Exclude
				direct_interfaces[i] = service_interface;
				return direct_interfaces;
			}
		}
		final Class<?>[] merged = Arrays.copyOf(direct_interfaces, direct_interfaces.length + 1);
		merged[direct_interfaces.length] = service_interface;
		return merged;
	}

	static <I extends IInterface> boolean unbind(final Context context, final I instance) {
		if (instance == null) throw new IllegalArgumentException("instance is null");
		final InvocationHandler invocation_handler;
		final Class<? extends IInterface> proxy_class = instance.getClass();
		if (! Proxy.isProxyClass(proxy_class)
				|| ! ((invocation_handler = Proxy.getInvocationHandler(instance)) instanceof ProxyHandler))
			throw new IllegalArgumentException("Not a service instance: " + instance);
		final ProxyHandler handler = (ProxyHandler) invocation_handler;

		handler.binder = null;		// Invalidate the proxy
		return unbind(context, handler.itf, instance);
	}

	private static <I extends IInterface> boolean unbind(final Context context, final Class<? extends IInterface> service_interface, final I instance) {
		final ServiceRecord record = sServices.get(service_interface);
		if (record == null) throw new IllegalArgumentException("No service bound for " + service_interface.getName());

		final Iterator iterator = record.instances.iterator();
		// List.remove() is not working on list of dynamic proxies, since equals() method is forwarded.
		while (iterator.hasNext()) if (iterator.next() == instance) {
			iterator.remove();

			if (record.instances.isEmpty()) {
				sServices.remove(service_interface);
				// TODO: Defer the unbinding and destroying
				if (record.service == null)
					try {
						context.unbindService(record);
					} catch (final RuntimeException e) {
						Log.d(TAG, "Ignore failure in service unbinding: " + e);
					}
				else {
					unbindService(record.service, makeIntent(record.service, service_interface));
					destroyService(record.service);
				}
				return true;
			} else return false;
		}
		throw new IllegalArgumentException("Instance not found in service connections of " + service_interface + ": "
				+ instance.getClass().getName() + "@" + System.identityHashCode(instance));
	}

	private static class ProxyHandler implements InvocationHandler {

		private ProxyHandler(final Class<? extends IInterface> service_interface, final IBinder binder) {
			this.binder = binder;
			this.itf = service_interface;
		}

		@Override public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
			if (binder == null) throw new IllegalStateException("Service is already unbound");
			try {
				return method.invoke(binder, args);
			} catch (final InvocationTargetException e) {
				throw e.getTargetException();
			}
		}


		private IBinder binder;
		private final Class<? extends IInterface> itf;
	}

	private static Intent makeIntent(final Context context, final Class<? extends IInterface> service_interface) {
		return new Intent(service_interface.getName()).setPackage(context.getPackageName());
	}

	private static void unbindService(final Service service, final Intent intent) {
		if (service instanceof AidlService)
			((AidlService) service).closeBinder();
		else try {
			final boolean rebind = service.onUnbind(intent);	// TODO: Support rebind if true is returned.
			if (rebind) throw new UnsupportedOperationException("Sorry, onRebind() is not yet supported.");
		} catch (final RuntimeException e) {
			Log.e(TAG, "Error unbinding " + service, e);
		}
	}

	static void destroyService(final Service service) {
		unregisterComponentCallbacks(service.getApplication(), service);
		try {
			final long start = Debug.threadCpuTimeNanos();
			service.onDestroy();
			logExcessiveElapse(start, 5, service, ".onDestroy()");
		} catch (final RuntimeException e) {
			Log.e(TAG, "Error destroying " + service, e);
		}
	}

	static @Nullable Service createService(final Context context, final String classname) {
		// Load class
		final Class<?> clazz;
		try {
			clazz = context.getClassLoader().loadClass(classname);
		} catch (final ClassNotFoundException e) {
			Log.w(TAG, e.toString());
			return null;
		}
		if (! Service.class.isAssignableFrom(clazz)) {
			Log.w(TAG, "Not service class: " + clazz);
			return null;
		}
		@SuppressWarnings("unchecked") final Class<? extends Service> service_class = (Class<? extends Service>) clazz;

		// Instantiate
		final Service service; final String class_name;
		try {
			final long start = Debug.threadCpuTimeNanos();
			service = service_class.newInstance();
			logExcessiveElapse(start, 2, class_name = service_class.getName(), "()");
		} catch (final InstantiationException e) {
			Log.e(TAG, "Failed to instantiate " + classname, e);
			return null;
		} catch (final IllegalAccessException e) {
			Log.e(TAG, "Constructor of " + classname + " is inaccessible", e);
			return null;
		} catch (final RuntimeException e) {
			Log.e(TAG, "Error instantiating " + classname, e);
			return null;
		}

		// Attach
		final Application application = getApplication(context);
		attach(context, service_class, service, application);

		// Create
		try {
			final long start = Debug.threadCpuTimeNanos();
			service.onCreate();
			logExcessiveElapse(start, 5, class_name, ".onCreate()");
		} catch (final RuntimeException e) {
			Log.e(TAG, service + ".onCreate()", e);
		}

		// Hookup lifecycle callbacks
		registerComponentCallbacks(service.getApplication(), service);

		return service;
	}

	private static @Nullable IBinder bindService(final Service service, final Intent intent) {
		// Bind
		IBinder binder = null;
		try {
			final long start = Debug.threadCpuTimeNanos();
			binder = service instanceof AidlService ? ((AidlService) service).onBindFromLocal() : service.onBind(intent);
			logExcessiveElapse(start, 2, service, ".onBind()");
		} catch (final RuntimeException e) {
			Log.e(TAG, service + ".onBind()", e);
		}
		if (binder == null) {	// Error running onBind() or null is returned by onBind().
			destroyService(service);
			try {
				final long start = Debug.threadCpuTimeNanos();
				service.onDestroy();
				logExcessiveElapse(start, 5, service, ".onDestroy()");
			} catch (final RuntimeException e) {
				Log.e(TAG, service + ".onDestroy()", e);
			}
		}
		return binder;
	}

	private static void logExcessiveElapse(final long start_thread_cpu_nanos, final long tolerable_duration_ms, final Object procedure, final String postfix) {
		final long duration_ms = (Debug.threadCpuTimeNanos() - start_thread_cpu_nanos) / 1_000_000;
		if (duration_ms <= tolerable_duration_ms) return;
		Log.w(TAG, procedure.toString() + (postfix != null ? postfix : "") + " consumed " + duration_ms + "ms (thread CPU time)");
	}

	@TargetApi(VERSION_CODES.ICE_CREAM_SANDWICH)
	private static void registerComponentCallbacks(final Application app, final ComponentCallbacks callbacks) {
		if (VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH) return;
		app.registerComponentCallbacks(callbacks);
	}

	@TargetApi(VERSION_CODES.ICE_CREAM_SANDWICH)
	private static void unregisterComponentCallbacks(final Application app, final ComponentCallbacks callbacks) {
		if (app == null || VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH) return;
		app.unregisterComponentCallbacks(callbacks);
	}

	private static void attach(final Context context, final Class<? extends Service> service_class,
			final Service service, final Application application) {
		if (Service_attach == null) return;
		try {
			Service_attach.invoke(service, context, null, service_class.getName(), null, application, null);
		} catch (final IllegalAccessException e) {
			Log.e(TAG, "Unexpected exception when attaching service.", e);
		} catch (final InvocationTargetException e) {
			throw new RuntimeException(e.getTargetException());
		}
	}

	private static Application getApplication(final Context context) {
		if (context instanceof Activity) return ((Activity) context).getApplication();
		if (context instanceof Service) return ((Service) context).getApplication();
		final Context app_context = context.getApplicationContext();
		if (app_context instanceof Application) return (Application) app_context;
		Log.w(TAG, "Cannot discover application from context " + context);
		return null;
	}

	private static final Map<Class<? extends IInterface>, ServiceRecord> sServices = Collections.synchronizedMap(new HashMap<Class<? extends IInterface>, ServiceRecord>());
	private static final BroadcastReceiver sDummyReceiver = new BroadcastReceiver() { @Override public void onReceive(final Context context, final Intent intent) {}};
	private static final String TAG = "LocalSvc";

	// Method signature: (useless parameters for AIDL service - thread, token, activityManager)
	//   public final void attach(Context context, ActivityThread thread, String className, IBinder token, Application application, Object activityManager)
	private static final Method Service_attach;
	static {
		Method method = null;
		try {
			final Class<?> ActivityThread = Class.forName("android.app.ActivityThread");
			method = Service.class.getDeclaredMethod("attach", Context.class, ActivityThread, String.class,
					IBinder.class, Application.class, Object.class);
			method.setAccessible(true);
		} catch (final ClassNotFoundException | NoSuchMethodException | MultiCatchROECompat e) {
			Log.e(TAG, "Incompatible ROM", e);
		}
		Service_attach = method;
	}

	private static class ServiceRecord implements ServiceConnection {

		final Class<? extends IInterface> itf;
		final @Nullable Service service;
		final IBinder binder;
		final List<IInterface> instances = new ArrayList<>();

		@Override public void onServiceConnected(final ComponentName name, final IBinder binder) {
			if (binder != this.binder) Log.e(TAG, "Inconsistent binder: " + binder + " != " + this.binder);
		}

		@Override public void onServiceDisconnected(final ComponentName name) {
			// TODO
		}

		ServiceRecord(final Class<? extends IInterface> service_interface, final @Nullable Service service, final IBinder binder) {
			itf = service_interface;
			this.service = service;
			this.binder = binder;
		}
	}
}
