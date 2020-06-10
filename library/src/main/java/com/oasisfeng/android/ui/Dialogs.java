package com.oasisfeng.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;

import javax.annotation.ParametersAreNonnullByDefault;

import static android.content.res.Configuration.UI_MODE_NIGHT_MASK;
import static android.content.res.Configuration.UI_MODE_NIGHT_YES;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/** @author Oasis */
@ParametersAreNonnullByDefault
@RequiresApi(LOLLIPOP) public class Dialogs {

	/** Create an non-cancellable alert dialog builder. */
	public static @CheckResult Builder buildAlert(final Activity activity, final @StringRes int title, final @StringRes int message) {
		return buildAlert(activity, title != 0 ? activity.getText(title) : null, message != 0 ? activity.getText(message) : null);
	}
	public static @CheckResult Builder buildAlert(final Activity activity, final @Nullable CharSequence title, final @Nullable CharSequence message) {
		final Builder builder = new Builder(activity);
		if (title != null) builder.setTitle(title);
		if (message != null) builder.setMessage(message);
		return builder;
	}

	public static @CheckResult Builder buildList(final Activity activity, final @StringRes int title, final CharSequence[] items,
												 final DialogInterface.OnClickListener listener) {
		return buildList(activity, title != 0 ? activity.getText(title) : null, items, listener);
	}
	public static @CheckResult Builder buildList(final Activity activity, final @Nullable CharSequence title, final CharSequence[] items,
												 final DialogInterface.OnClickListener listener) {
		final Builder builder = new Builder(activity);
		if (title != null) builder.setTitle(title);
		builder.setItems(items, listener);
		return builder;
	}

	public static @CheckResult Builder buildCheckList(final Activity activity, final @Nullable CharSequence title, final CharSequence[] items,
													  final @Nullable boolean[] checkedItems, final DialogInterface.OnMultiChoiceClickListener listener) {
		final Builder builder = new Builder(activity);
		if (title != null) builder.setTitle(title);
		builder.setMultiChoiceItems(items, checkedItems, listener);
		return builder;
	}

	private static int getDayNightThemeForAlertDialog(final Context context) {
		final boolean night = (context.getResources().getConfiguration().uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES;
		return night ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert;
	}

	/** Provide shortcuts for simpler building */
	public static class Builder extends AlertDialog.Builder {

		public @CheckResult Builder withOkButton(final @Nullable Runnable task) {
			setPositiveButton(android.R.string.ok, task == null ? null : (d, w) -> task.run());
			return this;
		}
		public @CheckResult Builder withCancelButton() { setNegativeButton(android.R.string.cancel, null); return this; }
		Builder(final Context context) { super(context, getDayNightThemeForAlertDialog(context)); }
	}

	public static @CheckResult FluentProgressDialog buildProgress(final Activity activity, final @StringRes int message) {
		return buildProgress(activity, activity.getText(message));
	}

	public static @CheckResult FluentProgressDialog buildProgress(final Activity activity, final CharSequence message) {
		final FluentProgressDialog dialog = new FluentProgressDialog(activity);
		dialog.setMessage(message);
		return dialog;
	}

	public static class FluentProgressDialog extends ProgressDialog {

		FluentProgressDialog(final Context context) { super(context, getDayNightThemeForAlertDialog(context)); }
		public FluentProgressDialog indeterminate() { setIndeterminate(true); return this; }
		public FluentProgressDialog nonCancelable() { setCancelable(false); return this; }
		public FluentProgressDialog onCancel(final OnCancelListener listener) { setOnCancelListener(listener); return this; }
		public FluentProgressDialog start() { super.show(); return this; }
	}
}
