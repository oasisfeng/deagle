package com.oasisfeng.android.ui.support;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.CheckResult;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

/**
 * Utilities for {@link AlertDialog} (the support library version)
 *
 * Created by Oasis on 2016/12/8.
 */
public class Dialogs {

	/** Create an non-cancellable alert dialog builder. */
	public static @CheckResult Dialogs.Builder buildAlert(final Activity context, final @StringRes int title, final @StringRes int message) {
		return buildAlert(context, title != 0 ? context.getText(title) : null, message != 0 ? context.getText(message) : null);
	}

	public static @CheckResult Dialogs.Builder buildAlert(final Activity context, final CharSequence title, final CharSequence message) {
		final Dialogs.Builder builder = new Dialogs.Builder(context);
		builder.setCancelable(true);
		if (title != null) builder.setTitle(title);
		if (message != null) builder.setMessage(message);
		return builder;
	}

	/** Provide shortcuts for simpler building */
	public static class Builder extends AlertDialog.Builder {

		public @CheckResult Builder withOkButton(final Runnable task) {
			setPositiveButton(android.R.string.ok, task == null ? null : new DialogInterface.OnClickListener() { @Override public void onClick(final DialogInterface dialog, final int which) {
				task.run();
			}});
			return this;
		}

		public @CheckResult Builder withCancelButton() {
			setNegativeButton(android.R.string.cancel, null);
			return this;
		}

		public Builder(final Context context) { super(context); }
	}
}
