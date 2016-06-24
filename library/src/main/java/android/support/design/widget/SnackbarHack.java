package android.support.design.widget;

import android.widget.TextView;

/**
 * A tiny hack to get the message view from SnackBar.
 *
 * Created by Oasis on 2015/9/30.
 */
public class SnackbarHack {

	public static TextView getMessageView(final Snackbar snackbar) {
		return ((Snackbar.SnackbarLayout) snackbar.getView()).getMessageView();
	}
}
