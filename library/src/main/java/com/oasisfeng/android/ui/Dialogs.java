package com.oasisfeng.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

/** @author Oasis */
@RequiresApi(LOLLIPOP) public class Dialogs {

	/** Create an non-cancellable alert dialog builder. */
	public static @CheckResult Builder buildAlert(final Activity activity, final @StringRes int title, final @StringRes int message) {
		return buildAlert(activity, title != 0 ? activity.getText(title) : null, message != 0 ? activity.getText(message) : null);
	}

	public static @CheckResult Builder buildAlert(final Activity activity, final @Nullable CharSequence title, final @Nullable CharSequence message) {
		final Builder builder = new Builder(activity);
		builder.setCancelable(true);
		if (title != null) builder.setTitle(title);
		if (message != null) builder.setMessage(message);
		return builder;
	}

	/** Provide shortcuts for simpler building */
	public static class Builder extends AlertDialog.Builder {

		public @CheckResult Builder withOkButton(final @Nullable Runnable task) {
			setPositiveButton(android.R.string.ok, task == null ? null : (d, w) -> task.run());
			return this;
		}
		public @CheckResult Builder withCancelButton() { setNegativeButton(android.R.string.cancel, null); return this; }
		Builder(final Context context) { super(context, android.R.style.Theme_Material_Light_Dialog_Alert); }
	}

	public static @CheckResult FluentProgressDialog buildProgress(final Activity activity, final CharSequence message) {
		final FluentProgressDialog dialog = new FluentProgressDialog(activity);
		dialog.setMessage(message);
		return dialog;
	}

	public static class FluentProgressDialog extends ProgressDialog {

		FluentProgressDialog(final Context context) { super(context, android.R.style.Theme_Material_Light_Dialog_Alert); }
		public FluentProgressDialog indeterminate() { setIndeterminate(true); return this; }
		public FluentProgressDialog nonCancelable() { setCancelable(false); return this; }
		public FluentProgressDialog start() { super.show(); return this; }
	}
}
