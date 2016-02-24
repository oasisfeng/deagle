package com.oasisfeng.android.databinding.adapters;

import android.app.Activity;
import android.content.Context;
import android.databinding.BindingAdapter;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import com.oasisfeng.android.app.Activities;
import com.oasisfeng.android.ui.AppLabelCache;
import com.oasisfeng.android.ui.IconResizer;

/**
 * Binding adapter for asynchronous loading application label and icon
 * Icon is set into the starting side compound drawable of the given TextView.
 *
 * Created by Oasis on 2016/2/18.
 */
@SuppressWarnings("unused")
public class AppLabelBindingAdapter {

	private static final boolean DEBUG = true;

	@BindingAdapter("app_label") public static void loadAppLabel(final TextView label_view, final String pkg) {
		final Context context = label_view.getContext();
		final Activity activity = Activities.findActivityFrom(context);
		if (! (context instanceof Activity)) throw new IllegalStateException("No activity found in the context of " + label_view + ": " + context);
		// Set initial values before application label and icon are loaded (or fail to load)
		label_view.setText(pkg);
		label_view.setTag(pkg);		// Store in tag as an indicator for re-use check.
		// TODO: Test performance to decide whether to cache this default icon
		final Drawable default_icon = context.getPackageManager().getDefaultActivityIcon();
		label_view.setCompoundDrawablesWithIntrinsicBounds(default_icon, null, null, null);

		final AppLabelCache cache = AppLabelCache.load(activity);
		cache.loadLabel(pkg, new AppLabelCache.LabelLoadCallback() {

			@Override public boolean isCancelled(final String pkg) {
				return ! pkg.equals(label_view.getTag());
			}

			@Override public void onTextLoaded(final CharSequence text) {
				label_view.setText(text);
			}

			@Override public void onIconLoaded(final Drawable icon) {
				label_view.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
			}

			@Override public void onError(final Throwable error) {
				// TODO
			}
		});
	}

	private static final IconResizer sIconResizer = new IconResizer();

	private static final String TAG = "AppLabelBind";
}
