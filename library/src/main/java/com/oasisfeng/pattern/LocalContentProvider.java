package com.oasisfeng.pattern;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.support.annotation.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Local Content Provider Pattern:
 * Self-contained component across the app, as singleton instance for each process.
 *
 * Created by Oasis on 2016/8/20.
 */
@ParametersAreNonnullByDefault
public class LocalContentProvider extends PseudoContentProvider {

	protected static <T extends LocalContentProvider> T getInstance(final Context context, final Class<T> clazz) {
		@SuppressWarnings("unchecked") final T singleton = (T) sSingleton;
		if (singleton != null) return singleton;
		final ProviderInfo provider = queryProviderInfo(context, clazz);
		if (provider == null) throw new IllegalStateException(clazz + " not declared in AndroidManifest.xml");
		final T instance = getInstance(context, provider.authority);
		sSingleton = instance;
		return instance;
	}

	protected static <T extends LocalContentProvider> T getInstance(final Context context, final String authority) {
		final ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(authority);
		if (client == null) throw new IllegalStateException("No active provider associated with authority: " + authority);
		try {
			final ContentProvider provider = client.getLocalContentProvider();
			if (provider == null)
				throw new IllegalStateException("android:multiprocess=\"true\" is required for this provider.");
			if (! (provider instanceof LocalContentProvider))
				throw new IllegalArgumentException("Not a LocalContentProvider associated with authority: " + authority);
			@SuppressWarnings("unchecked") final T casted = (T) provider;
			return casted;
		} finally { //noinspection deprecation
			client.release();
		}
	}

	public static @Nullable <T extends ContentProvider> ProviderInfo queryProviderInfo(final Context context, final Class<T> clazz) {
		try {
			return context.getPackageManager().getProviderInfo(new ComponentName(context, clazz), PackageManager.GET_DISABLED_COMPONENTS);
		} catch (final PackageManager.NameNotFoundException e) { return null; }
	}

	/** Do nothing in onCreate() to eliminate overhead in application start-up. "Lazy Loading" pattern is suggested for construction work. */
	@Override public boolean onCreate() { return true; }

	private static LocalContentProvider sSingleton;		// Short path for intra-classloader access
}
