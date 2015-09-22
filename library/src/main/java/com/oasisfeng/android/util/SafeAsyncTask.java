package com.oasisfeng.android.util;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/** @author Oasis */
public abstract class SafeAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    /** This method should be called in application initialization, to make sure
	 *  AsyncTask's internal handler is created on the main thread. */
	public static void init() { /* Do nothing besides static initialization. */ }

	public SafeAsyncTask(final Activity activity) {
		mActivityRef = new WeakReference<>(activity);
	}

    protected void onProgressUpdate(final Activity activity, final Progress... values) {}
    protected void onPostExecute(final Activity activity, final Result result) {}
    protected void onCancelled(final Activity activity, final Result result) {}

    public static boolean setDefaultExecutor(final ThreadPoolExecutor executor) {
        try {
            final Method setter = AsyncTask.class.getMethod("setDefaultExecutor", Executor.class);
            setter.setAccessible(true);
            setter.invoke(null, executor);
            return true;
        } catch (final Exception e) {
            try {
                final Field field = AsyncTask.class.getDeclaredField("sDefaultExecutor"); // Honeycomb and above
                field.setAccessible(true);
                field.set(null, executor);
                return true;
            } catch (final Exception ex) {
                try {
                    final Field field = AsyncTask.class.getDeclaredField("sExecutor");    // Old versions
                    field.setAccessible(true);
                    field.set(null, executor);      // Only accept ThreadPoolExecutor
                    return true;
                } catch (final Exception exc) {
                    Log.d(TAG, "Failed to install as default executor of AsyncTask.");
                    return false;
                }
            }
        }
    }

    /** No more {@link AsyncTask#onCancelled()}, always use {@link #onCancelled(Activity, Result)} instead */
    @Override protected final void onCancelled() {}

	@SafeVarargs @Override protected final void onProgressUpdate(final Progress... values) {
        final Activity activity = getActivityIfAvailable();
        if (activity == null) return;
        onProgressUpdate(activity, values);
	}

    @Override protected final void onPostExecute(final Result result) {
	    final Activity activity = getActivityIfAvailable();
	    if (activity == null) return;
		onPostExecute(activity, result);
	}

    @Override protected final void onCancelled(final Result result) {
        final Activity activity = getActivityIfAvailable();
        if (activity == null) return;
        onCancelled(activity, result);
    }

    /** Check activity status to avoid crash due to referencing activity which is no longer available. */
    private Activity getActivityIfAvailable() {
        final Activity activity = mActivityRef.get();
	    if (activity == null || activity.isFinishing()
	            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
	        mActivityRef.clear();      // Avoid duplicate check
	        return null;
	    }
        return activity;
    }

	private final WeakReference<Activity> mActivityRef;

    private static final String TAG = SafeAsyncTask.class.getSimpleName();
}
