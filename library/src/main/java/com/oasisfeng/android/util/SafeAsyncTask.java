package com.oasisfeng.android.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import androidx.fragment.app.FragmentActivity;

/**
 * Protect against NPE when calling {@link Fragment#getActivity()} in {@link AsyncTask}.
 *
 * Note: Avoiding memory leak is not the purpose of this class. Please consider using lambda (with RetroLambda)
 *
 * @author Oasis
 */
public abstract class SafeAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

	@SuppressLint("StaticFieldLeak")	// AsyncTask here is actually "static", but the two callbacks are probably not.
	public static <T> void execute(final Activity activity, final Function<Activity, T> background, final Consumer<T> finish) {
		final WeakReference<Activity> reference = new WeakReference<>(activity);
		new AsyncTask<Void, Void, T>() {
			@Override protected T doInBackground(final Void... voids) {
				final Activity activity = ifActive(reference.get());
				return activity != null ? background.apply(activity) : null;
			}

			@Override protected void onPostExecute(final T result) {
				finish.accept(result);
			}
		}.execute();
	}
	public static void execute(final Activity activity, final Consumer<Activity> runnable) {
		final WeakReference<Activity> reference = new WeakReference<>(activity);
		AsyncTask.execute(new SafeTask<>(() -> ifActive(reference.get()), runnable));
	}

	public static void execute(final Fragment fragment, final Consumer<Activity> runnable) {
		final WeakReference<Fragment> fragment_reference = new WeakReference<>(fragment);
		AsyncTask.execute(new SafeTask<>(() -> {
			final Fragment f = fragment_reference.get();
			return f == null ? null : f.getActivity();
		}, runnable));
	}

	public static void execute(final androidx.fragment.app.Fragment fragment, final Consumer<FragmentActivity> runnable) {
		final WeakReference<androidx.fragment.app.Fragment> fragment_reference = new WeakReference<>(fragment);
		AsyncTask.execute(new SafeTask<>(() -> {
			final androidx.fragment.app.Fragment f = fragment_reference.get();
			return f == null ? null : f.getActivity();
		}, runnable));
	}

	private interface Supplier<T> {
		T get();
	}

	static class SafeTask<T> implements java.lang.Runnable {

		SafeTask(final Supplier<T> supplier, final Consumer<T> runnable) {
			mSupplier = supplier;
			mRunnable = runnable;
		}

		@Override public void run() {
			final T object = mSupplier.get();
			if (object == null) return;
			mRunnable.accept(object);
		}

		private final Supplier<T> mSupplier;
		private final Consumer<T> mRunnable;
	}

	public SafeAsyncTask(final Activity activity) {
		mActivityRef = new WeakReference<>(activity);
	}

	@SafeVarargs protected final void onProgressUpdate(final Activity activity, final Progress... values) {}
    protected void onPostExecute(final Activity activity, final Result result) {}
    protected void onCancelled(final Activity activity, final Result result) {}

    public static boolean setDefaultExecutor(final ThreadPoolExecutor executor) {
        try { @SuppressWarnings("JavaReflectionMemberAccess")
			final Method setter = AsyncTask.class.getMethod("setDefaultExecutor", Executor.class);
            setter.setAccessible(true);
            setter.invoke(null, executor);
            return true;
        } catch (final Exception e) {
            try { @SuppressWarnings("JavaReflectionMemberAccess")
                final Field field = AsyncTask.class.getDeclaredField("sDefaultExecutor"); // Honeycomb and above
                field.setAccessible(true);
                field.set(null, executor);
                return true;
            } catch (final Exception ex) {
                try { @SuppressWarnings("JavaReflectionMemberAccess")
                    final Field field = AsyncTask.class.getDeclaredField("sExecutor");    // Old versions
                    field.setAccessible(true);
                    field.set(null, executor);      // Only accept ThreadPoolExecutor
                    return true;
                } catch (final Exception exc) {
                    Log.w(TAG, "Failed to install as default executor of AsyncTask.");
                    return false;
                }
            }
        }
    }

    /** No more {@link AsyncTask#onCancelled()}, always use {@link #onCancelled(Activity, Result)} instead */
    @Override protected final void onCancelled() {}

	@SafeVarargs @Override protected final void onProgressUpdate(final Progress... values) {
        final Activity activity = ifActive(mActivityRef.get());
        if (activity == null) return;
        onProgressUpdate(activity, values);
	}

    @Override protected final void onPostExecute(final Result result) {
	    final Activity activity = ifActive(mActivityRef.get());
	    if (activity == null) return;
		onPostExecute(activity, result);
	}

    @Override protected final void onCancelled(final Result result) {
        final Activity activity = ifActive(mActivityRef.get());
        if (activity == null) return;
        onCancelled(activity, result);
    }

    /** Check activity status to avoid crash due to referencing activity which is no longer available. */
    private static Activity ifActive(final Activity activity) {
	    return activity == null || activity.isFinishing() || activity.isDestroyed() ? null : activity;
    }

	private final WeakReference<Activity> mActivityRef;

    private static final String TAG = SafeAsyncTask.class.getSimpleName();
}
