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
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.util.Pair;

/**
 * Non-UI fragment to load and cache label text and icon of applications.
 *
 * Created by Oasis on 2016/2/24.
 */
public class AppLabelCache {

	private static final boolean DEBUG = true;

	public interface LabelLoadCallback {
		/** Must be thread-safe */ boolean isCancelled(String pkg);
		@UiThread void onTextLoaded(CharSequence text);
		@UiThread void onIconLoaded(Drawable icon);
		@WorkerThread void onError(Throwable error);
	}

	@UiThread public void loadLabelTextOnly(final String pkg, final LabelLoadCallback callback) {
		load(pkg, false, callback);
	}

	@UiThread public void loadLabel(final String pkg, final LabelLoadCallback callback) {
		load(pkg, true, callback);
	}

	@UiThread private void load(final String pkg, final boolean load_icon, final LabelLoadCallback callback) {
		if (mContext == null) throw new IllegalStateException("Not attached");
		final PackageManager pm = mContext.getPackageManager();
		final Pair<CharSequence, Drawable> cached = mLruCache.get(pkg);
		final CharSequence cached_text;
		if (cached != null) {
			cached_text = cached.first;
			callback.onTextLoaded(cached_text);
			if (cached.second != null) {
				callback.onIconLoaded(cached.second);
				return;		// Both text and icon are cached
			}	// Fall-through to load icon.
		} else cached_text = null;

		new AsyncTask<CharSequence/* cached text */, CharSequence/* loaded text */, Drawable/* loaded icon */>() {

			@Override protected Drawable doInBackground(final CharSequence... params) {
				mLabelText = params[0];
				//noinspection WrongThread, skip the heavy work if the TextView was already re-used.
				if (callback.isCancelled(pkg)) {    //noinspection ConstantConditions
					if (DEBUG) Log.d(TAG, "Skip loading task for " + pkg);
					return null;
				}

				final ApplicationInfo info;
				try {
					info = pm.getApplicationInfo(pkg, 0);
				} catch (final PackageManager.NameNotFoundException e) {
					cancel(false);
					callback.onError(e);
					return null;
				}
				if (mLabelText == null) publishProgress(mLabelText = info.loadLabel(pm));
				return load_icon ? mIconResizer.createIconThumbnail(info.loadIcon(pm)) : null;
			}

			@Override protected void onProgressUpdate(final CharSequence... label) {
				mLruCache.put(pkg, new Pair<>(mLabelText, (Drawable) null));	// Partially cache text first
				if (callback.isCancelled(pkg)) {    // The TextView was already re-used, discard this update.
					cancel(false);
					//noinspection ConstantConditions
					if (DEBUG) Log.d(TAG, "Discard loaded label for " + pkg);
					return;
				}
				callback.onTextLoaded(this.mLabelText = label[0]);
			}

			@Override protected void onPostExecute(final @Nullable Drawable icon) {
				if (icon == null) return;
				callback.onIconLoaded(icon);
				mLruCache.put(pkg, new Pair<>(mLabelText, icon));
			}

			private CharSequence mLabelText;
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, cached_text);
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

	private AppLabelCache() {}

	private final LruCache<String, Pair<CharSequence, Drawable>> mLruCache = new LruCache<>(KMaxCacheCapacity);
	private Activity mContext;

	private static final int KMaxCacheCapacity = 100;
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
			mCache.mLruCache.trimToSize(KMinCacheSizeToTrim);
		}

		private final BroadcastReceiver mPackageChangeReceiver = new BroadcastReceiver() { @Override public void onReceive(final Context context, final Intent intent) {
			final Uri data = intent.getData();
			if (data != null) {
				final String pkg = data.getSchemeSpecificPart();
				mCache.mLruCache.remove(pkg);
			} else {
				final String[] pkgs = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
				if (pkgs != null) for (final String pkg : pkgs)
					mCache.mLruCache.remove(pkg);
			}
		}};

		private final AppLabelCache mCache = new AppLabelCache();
	}

	private final IconResizer mIconResizer = new IconResizer();
}
