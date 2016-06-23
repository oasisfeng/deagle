package com.oasisfeng.android.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.util.Collection;
import java.util.Collections;

/**
 * Non-UI fragment to load and cache label text and icon of applications.
 *
 * Created by Oasis on 2016/2/24.
 */
public class AppLabelCache {

	private static final boolean DEBUG = true;

	private static class Entry {
		final String pkg;
		CharSequence label;
		Drawable raw_icon;
		Drawable icon;
		int flags;

		public Entry(final String pkg) { this.pkg = pkg; }
	}

	public interface LabelLoadCallback {
		/** Must be thread-safe */ boolean isCancelled(String pkg);
		@UiThread void onTextLoaded(String pkg, CharSequence text, final int flags);
		@UiThread void onIconLoaded(String pkg, Drawable icon);
		@WorkerThread void onError(String pkg, Throwable error);
	}

	@UiThread public void loadLabelTextOnly(final Collection<String> pkgs, final LabelLoadCallback callback) {
		if (DEBUG) Log.d(TAG, "Start batch text loading: " + pkgs.size());
		load(pkgs, false, callback);
	}

	/** Load text and icon */
	@UiThread public void loadLabel(final String pkg, final LabelLoadCallback callback) {
		if (DEBUG) Log.d(TAG, "Start single full loading: " + pkg);
		load(Collections.singleton(pkg), true, callback);
	}

	@UiThread private void load(final Collection<String> pkgs, final boolean load_icon, final LabelLoadCallback callback) {
		if (mContext == null) throw new IllegalStateException("Not attached");
		final PackageManager pm = mContext.getPackageManager();

		// Early callback if cached
		final Entry[] pending = new Entry[pkgs.size()]; int i = 0;
		for (final String pkg : pkgs) {
			Entry entry = getCache(pkg);
			if (entry != null) {
				if (entry.label != null) {
					callback.onTextLoaded(pkg, entry.label, entry.flags);
					if (entry.icon != null) {
						callback.onIconLoaded(pkg, entry.icon);
						if (DEBUG) Log.v(TAG, "Cache hit (text + icon): " + pkg);
						continue;		// Both text and icon are cached, no need to load
					} else if (DEBUG) Log.v(TAG, "Cache hit (text): " + pkg);
				} else if (DEBUG) Log.d(TAG, "Empty cache: " + pkg);
			} else {
				if (DEBUG) Log.v(TAG, "Cache missed: " + pkg);
				putCache(pkg, entry = new Entry(pkg));
			}
			pending[i ++] = entry;
		}

		if (i > 0) new AsyncTask<Entry, Entry, Integer>() {

			@Override @WorkerThread protected Integer doInBackground(final Entry... pending) {
				if (pending.length > 0) Log.d(TAG, "App label batch loading started.");
				for (final Entry entry : pending) {
					if (entry == null) break;		// No more
					final String pkg = entry.pkg;
					if (entry.label != null && entry.icon != null) {
						Log.w(TAG, "Loaded already: " + pkg);
						continue;	// Already loaded elsewhere
					}
					// Skip the heavy work if the TextView was already re-used.
					if (callback.isCancelled(pkg)) {
						if (DEBUG) Log.d(TAG, "Skip loading (cancelled): " + pkg);
						continue;
					}

					final ApplicationInfo info;
					try { //noinspection WrongConstant
						info = pm.getApplicationInfo(pkg, PackageManager.GET_UNINSTALLED_PACKAGES);
					} catch (final PackageManager.NameNotFoundException e) {
						callback.onError(pkg, e);
						continue;
					}

					entry.flags = info.flags;
					if (entry.label == null) try {
						if (DEBUG) Log.d(TAG, "Load text: " + pkg);
						entry.label = info.loadLabel(pm);
						publishProgress(entry);
					} catch (final RuntimeException e) {
						callback.onError(pkg, e);
					}

					if (load_icon && entry.icon == null && entry.raw_icon == null) try {
						if (DEBUG) Log.d(TAG, "Load icon: " + pkg);
						// Avoid PackageItemInfo.loadIcon() for unbadged icon.
						Drawable dr = null;
						if (info.packageName != null)
							dr = pm.getDrawable(info.packageName, info.icon, info);
						if (dr == null)
							dr = pm.getDefaultActivityIcon();
						entry.raw_icon = dr;
						publishProgress(entry);
					} catch (final RuntimeException e) {
						callback.onError(pkg, e);
					}
				}
				return pending.length;
			}

			@Override @UiThread protected void onProgressUpdate(final Entry... entries) {
				final Entry entry = entries[0];
				final String pkg = entry.pkg;

				final Drawable raw_icon = entry.raw_icon;
				if (raw_icon != null) {
					entry.raw_icon = null;
					if (DEBUG) Log.d(TAG, "Resize icon: " + pkg);
					entry.icon = mIconResizer.createIconThumbnail(raw_icon);
				}

				if (callback.isCancelled(pkg)) {    // The TextView was already re-used, discard this update.
					//noinspection ConstantConditions
					if (DEBUG) Log.d(TAG, "Discard loaded label for " + pkg);
					return;
				}

				if (DEBUG) Log.d(TAG, "Update text: " + pkg);
				callback.onTextLoaded(pkg, entry.label, entry.flags);
				if (entry.icon != null) {
					if (DEBUG) Log.d(TAG, "Update icon: " + pkg);
					callback.onIconLoaded(pkg, entry.icon);
				}
			}

			@Override protected void onPostExecute(final Integer count) {
				if (count > 0) Log.d(TAG, "App label batch loading finished.");
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pending);	// To improve efficiency and avoid blocking other AsyncTasks
	}

	@UiThread public static AppLabelCache load(final Activity activity) {
		final FragmentManager fm = activity.getFragmentManager();
		final Fragment fragment = fm.findFragmentByTag(TAG);
		if (fragment instanceof AppLabelCache.CacheFragment)
			return ((AppLabelCache.CacheFragment) fragment).mCache;
		if (fragment != null) Log.w(TAG, "Fragment type mismatch: " + fragment.getClass());

		final AppLabelCache.CacheFragment new_fragment = new AppLabelCache.CacheFragment();
		fm.beginTransaction().add(new_fragment, TAG).commitAllowingStateLoss();
		fm.executePendingTransactions();
		return new_fragment.mCache;
	}

	@UiThread private Entry getCache(final String pkg) { return mLruCache.get(pkg); }
	@UiThread private Entry putCache(final String pkg, final Entry entry) { return mLruCache.put(pkg, entry); }
	@UiThread private Entry removeCache(final String pkg) { return mLruCache.remove(pkg); }
	@UiThread private void trimCacheToSize(final int max_size) { mLruCache.trimToSize(max_size); }

	private AppLabelCache() {}

	private final LruCache<String, Entry> mLruCache = new LruCache<>(KMaxCacheCapacity);
	private Activity mContext;

	private static final int KMaxCacheCapacity = 500;
	private static final int KMinCacheSizeToTrim = 12;
	private static final String TAG = "AppLabelCache";

	public static class CacheFragment extends Fragment {

		@Override public void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}

		@SuppressWarnings("deprecation") @Override public void onAttach(final Activity activity) {
			super.onAttach(activity);
			mCache.mContext = activity;
			final IntentFilter pkg_filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
			pkg_filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
			pkg_filter.addDataScheme("package");
			activity.registerReceiver(mPackageChangeReceiver, pkg_filter);
			activity.registerReceiver(mPackageChangeReceiver, new IntentFilter(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE));
		}

		@Override public void onDetach() {	// onDestroy() will not be called since setRetainInstance(true)
			getActivity().unregisterReceiver(mPackageChangeReceiver);
			super.onDetach();
		}

		@Override public void onTrimMemory(final int level) {
			super.onTrimMemory(level);
			if (level < TRIM_MEMORY_RUNNING_LOW) return;
			mCache.trimCacheToSize(KMinCacheSizeToTrim);
		}

		private final BroadcastReceiver mPackageChangeReceiver = new BroadcastReceiver() { @Override public void onReceive(final Context context, final Intent intent) {
			final Uri data = intent.getData();
			if (data != null) {
				final String pkg = data.getSchemeSpecificPart();
				mCache.removeCache(pkg);
			} else {
				final String[] pkgs = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
				if (pkgs != null) for (final String pkg : pkgs)
					mCache.removeCache(pkg);
			}
		}};

		private final AppLabelCache mCache = new AppLabelCache();
	}

	private final IconResizer mIconResizer = new IconResizer();
}
