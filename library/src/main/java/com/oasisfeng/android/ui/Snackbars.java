package com.oasisfeng.android.ui;

import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.customtabs.CustomTabsSession;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

/** @author Oasis */
public class Snackbars {

	private static final int DEFAULT_DURATION = 10_000;
	private static final int KMaxLines = 3;

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
		return new Additional() { @Override public void invoke(final Snackbar snackbar) {
			snackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
		}};
	}

	@CheckResult public static Additional withAction(final int label_res, final View.OnClickListener listener) {
		return new Additional() { @Override public void invoke(final Snackbar snackbar) {
			snackbar.setAction(label_res, listener);
		}};
	}

	@CheckResult public static Additional withAction(final CharSequence label, final View.OnClickListener listener) {
		return new Additional() { @Override public void invoke(final Snackbar snackbar) {
			snackbar.setAction(label, listener);
		}};
	}

	@CheckResult public static Additional withLink(final int text_res, final Uri uri) {
		return new Additional() { @Override public void invoke(final Snackbar snackbar) {
			final CharSequence text = snackbar.getView().getContext().getText(text_res);
			withLink(text, uri).invoke(snackbar);
		}};
	}

	@CheckResult public static Additional withLink(final CharSequence text, final Uri uri) {
		return new Additional() { @Override public void invoke(final Snackbar snackbar) {
			snackbar.setAction(text, new View.OnClickListener() {
				@Override public void onClick(final View v) {
					WebContent.view(v.getContext(), uri);
				}
			});    // Default action before pre-cache
			snackbar.getView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

				@Override public void onViewAttachedToWindow(final View snackbar_view) {
					mConnection = WebContent.preload(snackbar_view.getContext(), uri, new WebContent.OnSessionReadyListener() { @Override public void onSessionReady(final CustomTabsSession session) {
						snackbar.setAction(text, new View.OnClickListener() {
							@Override public void onClick(final View v) {
								WebContent.view(v.getContext(), uri, session, null);
							}
						});
					}});
				}

				@Override public void onViewDetachedFromWindow(final View v) {
					if (mConnection != null) v.getContext().unbindService(mConnection);
				}

				private ServiceConnection mConnection;
			});}
		};
	}

	private static Snackbar tweak(final Snackbar snackbar) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) snackbar.getView().setZ(999);
		final TextView msg_view = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		msg_view.setMaxLines(KMaxLines);	// Extend max lines
		msg_view.setTextColor(0xffffffff);	// Workaround the light theme conflict
		return snackbar;
	}

	public interface Additional {
		void invoke(Snackbar instance);
	}
}
