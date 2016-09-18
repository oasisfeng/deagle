package com.oasisfeng.android.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.support.annotation.Nullable;
import android.util.Log;

import com.oasisfeng.deagle.BuildConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A base class for AIDL service to simplify the boilerplate code, supporting both local and remote clients simultaneously.
 *
 * <p>Remember to declare your service in AndroidManifest.xml as:
 * <b>(be sure to set android:exported="false" if you have no intention to expose this service)</b>
 * <pre>
 *   &lt;service android:name=".DemoService" android:exported="false"&gt;
 *     &lt;intent-filter&gt;
 *       &lt;action android:name="com.taobao.android.service.IDemoService" /&gt;
 *     &lt;/intent-filter&gt;
 *   &lt;/service&gt;
 * </pre>
 *
 * <pre>
 * public class DemoService extends AidlService&lt;IDemoService.Stub&gt; {
 *
 *   public class Impl extends IDemoService.Stub implements Closeable {
 *
 *     protected String mName;
 *
 *     &#64;Override public String helloWorld(final String name) {
 *       mName = name;
 *       return "Hello " + name;
 *     }
 *
 *     public void close() {
 *       // Optional destruction code here, as of onDestroy() in service.
 *       // No need to implement Closeable (or AutoCloseable) if nothing to do in destruction.
 *     }
 *   }
 *
 *   protected IDemoService.Stub createBinder() {
 *       return new Impl();
 *   }
 * }
 * </pre>
 *
 * @author Oasis
 */
public abstract class AidlService<Stub extends Binder & IInterface> extends Service {

	public AidlService() {
		super.onCreate();
		final Type[] types = getActualTypeArguments(getClass());
		final Type type = types[0];
		if (! (type instanceof Class) || ! Binder.class.isAssignableFrom((Class) type))
			throw new IllegalArgumentException(type + " is not an AIDL stub");
		//noinspection unchecked
		mInterface = (Class<? extends IInterface>) ((Class) type).getInterfaces()[0];

		if (BuildConfig.DEBUG)
			for (Class<?> clazz = getClass(); clazz != AidlService.class; clazz = clazz.getSuperclass())
				for (final Field field : clazz.getDeclaredFields())
					if (! Modifier.isStatic(field.getModifiers()))
						throw new IllegalStateException("AidlService-derived class must be stateless. " +
								"Unlike Android service, it is not singleton. " +
								"Consider moving fields to the binder class.");
	}

	/**
	 * Put initialization code in {@link #createBinder()} instead of this method.
	 * Only the binder, not the service instance is singleton.
	 */
	@Override public final void onCreate() { super.onCreate(); }

	@Override public final IBinder onBind(final Intent intent) {
		Log.d(toString(), "onBind");
		mBinder = LocalAidlServices.bind(this, mInterface, intent);
		return mBinder != null ? mBinder.asBinder() : null;
	}

	@Nullable Stub onBindFromLocal() {
		final Stub stub = createBinder();
		mBinder = stub;
		return stub;
	}

	/**
	 * Create the binder instance. This method will be called only once
	 * until the returned stub is no longer used (and thus closed).
	 */
	protected abstract @Nullable Stub createBinder();

	void closeBinder() {
		if (mBinder == null) throw new IllegalStateException("binder is null");
		synchronized (this) {
			if (mBinder instanceof AutoCloseable) close((AutoCloseable) mBinder);
			mBinder = null;
		}
	}

	@SuppressLint("NewApi")	// AutoCloseable is hidden but accessible
	private void close(final AutoCloseable closeable) {
		try {
			Log.d(toString(), "close");
			closeable.close();
		} catch (final Exception e) {
			Log.w(toString(), "Error closing " + closeable, e);
		}
	}

	/** Called by AMS after all remote clients disconnect, while local bindings could be still in use. */
	@Override public final boolean onUnbind(final Intent intent) {
		if (mBinder != null) LocalAidlServices.unbind(this, mBinder);
		return false;
	}

	/**
	 * Put destruction code in {@link AutoCloseable#close()} of the stub instead of this method.
	 * Only the binder, not the service instance is singleton.
	 */
	@Override public final void onDestroy() { super.onDestroy(); }

	private static Type[] getActualTypeArguments(Class<?> derivation) {
		while (derivation != null) {
			final Type type = derivation.getGenericSuperclass();
			if (type instanceof ParameterizedType) {
				final ParameterizedType ptype = (ParameterizedType) type;
				if (AidlService.class.equals(ptype.getRawType()))
					return ptype.getActualTypeArguments();
			}
			derivation = derivation.getSuperclass();
		}
		throw new IllegalArgumentException();
	}

	@Override public final int onStartCommand(final Intent intent, final int flags, final int startId) {
		Log.w(toString(), "Start operation is not allowed for AIDL service.");
		stopSelf(startId);
		return START_NOT_STICKY;
	}

	@Override public String toString() {
		if (mName != null) return mName;
		final String name = getSimpleName();
		return mName = name.substring(name.indexOf('$') + 1);
	}

	private String getSimpleName() {
		final String name = getClass().getName();
		if (name.endsWith("$Service")) return name.substring(name.lastIndexOf('.') + 1, name.length() - 8/* "$Service".length */);
		return name.substring(name.lastIndexOf('.') + 1);
	}

	private final Class<? extends IInterface> mInterface;
	private String mName;
	private IInterface mBinder;
}
