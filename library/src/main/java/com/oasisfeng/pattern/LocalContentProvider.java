package com.oasisfeng.pattern;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Local Content Provider Pattern:
 * Self-contained component across the app, as singleton instance for each process.
 *
 * Created by Oasis on 2016/8/20.
 */
@ParametersAreNonnullByDefault
public class LocalContentProvider extends ContentProvider {

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

	protected @NonNull ProviderInfo queryProviderInfo() {
		final ProviderInfo provider = queryProviderInfo(context(), getClass());
		if (provider == null) throw new IllegalStateException(getClass().getCanonicalName() + " is not properly configured.");
		return provider;
	}

	public static @Nullable <T extends ContentProvider> ProviderInfo queryProviderInfo(final Context context, final Class<T> clazz) {
		try {
			return context.getPackageManager().getProviderInfo(new ComponentName(context, clazz), PackageManager.GET_DISABLED_COMPONENTS);
		} catch (final PackageManager.NameNotFoundException e) { return null; }
	}

	/** Helper method to eliminate the nullness check. Be ware, never call it in the constructor! */
	protected Context context() { return getContext(); }

	/** Do nothing in onCreate() to eliminate overhead in application start-up. "Lazy Loading" pattern is suggested for construction work. */
	@Override public boolean onCreate() { return false; }

	/* The following methods are not relevant for local content provider. */
	@Nullable @Override public String getType(final @NonNull Uri uri) { return null; }
	@Override public @Nullable Uri insert(final @NonNull Uri uri, final @Nullable ContentValues contentValues) { return null; }
	@Override public int delete(final @NonNull Uri uri, final @Nullable String s, final @Nullable String[] strings) { return 0; }
	@Override public int update(final @NonNull Uri uri, final @Nullable ContentValues contentValues, final @Nullable String s,
								final @Nullable String[] strings) { return 0; }
	@Nullable @Override public Cursor query(final @NonNull Uri uri, final @Nullable String[] projection, final @Nullable String selection,
											final @Nullable String[] selection_args, final @Nullable String sort) { return null; }

	private static LocalContentProvider sSingleton;		// Short path for intra-classloader access
}
