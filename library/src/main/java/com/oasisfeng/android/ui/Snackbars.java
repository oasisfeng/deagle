package com.oasisfeng.android.ui;

import android.app.Activity;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.oasisfeng.deagle.R;

import androidx.annotation.CheckResult;

/** @author Oasis */
public class Snackbars {

	private static final int DEFAULT_DURATION = 10_000;
	private static final int KMaxLines = 3;

	/** Try to find {@link androidx.coordinatorlayout.widget.CoordinatorLayout CoordinatorLayout} with ID {@link R.id#coordinator coordinator} */
	@CheckResult public static Snackbar make(final Activity activity, final int text_res, final Additional... additional) {
		View coordinator = activity.findViewById(R.id.coordinator);
		if (coordinator == null) coordinator = activity.findViewById(android.R.id.content);
		return make(coordinator, text_res, additional);
	}

	@CheckResult public static Snackbar make(final View coordinator, final int text_res, final Additional... additional) {
		final Snackbar snackbar = Snackbar.make(coordinator, text_res, DEFAULT_DURATION);
		for (final Additional add : additional) if (add != null) add.invoke(snackbar);
		return tweak(snackbar);
	}

	@CheckResult public static Snackbar make(final View coordinator, final CharSequence text, final Additional... additional) {
		final Snackbar snackbar = Snackbar.make(coordinator, text, DEFAULT_DURATION);
		for (final Additional add : additional) if (add != null) add.invoke(snackbar);
		return tweak(snackbar);
	}

	@CheckResult public static Additional lastsForever() {
		return snackbar -> snackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
	}

	@CheckResult public static Additional whenSwiped(final Runnable callback) {
		return snackbar -> snackbar.addCallback(new Snackbar.Callback() { @Override public void onDismissed(final Snackbar snackbar, final int event) {
			if (event == DISMISS_EVENT_SWIPE) callback.run();
		}});
	}

	@CheckResult public static Additional withAction(final int label_res, final View.OnClickListener listener) {
		return snackbar -> snackbar.setAction(label_res, listener);
	}

	@CheckResult public static Additional withAction(final CharSequence label, final View.OnClickListener listener) {
		return snackbar -> snackbar.setAction(label, listener);
	}

	@CheckResult public static Additional withLink(final int text_res, final Uri uri) {
		return snackbar -> withLink(snackbar.getView().getContext().getText(text_res), uri).invoke(snackbar);
	}

	@CheckResult public static Additional withLink(final CharSequence text, final Uri uri) {
		return snackbar -> {
			snackbar.setAction(text, v -> WebContent.view(v.getContext(), uri));    // Default action before pre-cache
			snackbar.getView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

				@Override public void onViewAttachedToWindow(final View snackbar_view) {
					mConnection = WebContent.preload(snackbar_view.getContext(), uri,
							session -> snackbar.setAction(text, v -> WebContent.view(v.getContext(), uri, session, null)));
				}

				@Override public void onViewDetachedFromWindow(final View v) {
					if (mConnection != null) v.getContext().unbindService(mConnection);
				}

				private ServiceConnection mConnection;
			});};
	}

	private static Snackbar tweak(final Snackbar snackbar) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) snackbar.getView().setZ(999);
		final TextView msg_view = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
		msg_view.setMaxLines(KMaxLines);	// Extend max lines
		msg_view.setTextColor(0xffffffff);	// Workaround the light theme conflict
		return snackbar;
	}

	public interface Additional {
		void invoke(Snackbar instance);
	}
}
