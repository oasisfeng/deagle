package com.oasisfeng.pattern;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Local Content Provider Pattern:
 * Self-contained component across the app, as singleton instance for each process.
 *
 * Created by Oasis on 2016/8/20.
 */
public class LocalContentProvider extends ContentProvider {

	protected static <T extends LocalContentProvider> T getInstance(final Context context, final String authority) {
		final ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(authority);
		if (client == null) throw new IllegalStateException("No provider associated with authority: " + authority);
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

	protected static <T extends LocalContentProvider> ProviderInfo queryProviderInfo(final Context context, final Class<T> clazz) {
		final PackageInfo pkg_info; try {
			pkg_info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PROVIDERS);
		} catch (final PackageManager.NameNotFoundException e) { return null; }		// Should never happen;
		if (pkg_info.providers == null) return null;
		final String name = clazz.getName();
		for (final ProviderInfo provider : pkg_info.providers)
			if (name.equals(provider.name)) return provider;
		return null;
	}

	/** Helper method to eliminate the nullness check. Be ware, never call it in the constructor! */
	protected Context context() { return getContext(); }

	/** Do nothing in onCreate() to eliminate overhead in application start-up. "Lazy Loading" pattern is suggested for construction work. */
	@Override public boolean onCreate() { return true; }

	/* The following methods are not relevant for local content provider. */
	@Nullable @Override public String getType(final @NonNull Uri uri) { return null; }
	@Override public @Nullable Uri insert(final @NonNull Uri uri, final ContentValues contentValues) { return null; }
	@Override public int delete(final @NonNull Uri uri, final String s, final String[] strings) { return 0; }
	@Override public int update(final @NonNull Uri uri, final ContentValues contentValues, final String s, final String[] strings) { return 0; }
	@Nullable @Override public Cursor query(final @NonNull Uri uri, final String[] projection, final String selection,
											final String[] selection_args, final String sort) { return null; }
}
