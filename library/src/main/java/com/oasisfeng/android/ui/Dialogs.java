package com.oasisfeng.android.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

/** @author Oasis */
public class Dialogs {

	/** Create an non-cancellable alert dialog builder. */
	public static Builder buildAlert(final Context context, final int title, final int message) {
		return buildAlert(context, title != 0 ? context.getText(title) : null, message != 0 ? context.getText(message) : null);
	}

	public static Builder buildAlert(final Context context, final CharSequence title, final CharSequence message) {
		final Builder builder = new Builder(context);
		builder.setCancelable(false);
		if (title != null) builder.setTitle(title);
		if (message != null) builder.setMessage(message);
		return builder;
	}

	/** Provide shortcuts for simpler building */
	public static class Builder extends AlertDialog.Builder {

		public Builder withOkButton(final Runnable task) {
			setPositiveButton(android.R.string.ok, task == null ? null : new DialogInterface.OnClickListener() { @Override public void onClick(final DialogInterface dialog, final int which) {
				task.run();
			}});
			return this;
		}

		public Builder withCancelButton() {
			setNegativeButton(android.R.string.cancel, null);
			return this;
		}

		public Builder(final Context context) { super(context); }
	}
}
