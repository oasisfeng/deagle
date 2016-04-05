package com.oasisfeng.android.databinding.adapters;

import android.databinding.BindingAdapter;
import android.os.Handler;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback;
import android.view.View;

/**
 * Binding adapter for {@link BottomSheetBehavior}
 *
 * Created by Oasis on 2016/2/27.
 */
@SuppressWarnings("unused")
public class BottomSheetBindingAdapter {

	@BindingAdapter("behavior_state")
	public static void setBottomSheetState(final View view, final @BottomSheetBehavior.State int state) {
		final BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(view);
		if (behavior == null) return;
		try {
			behavior.setState(state);
		} catch (final RuntimeException e) {
			new Handler().post(new Runnable() { @Override public void run() {
				try {
					behavior.setState(state);
				} catch (final RuntimeException ignored) {}
			}});
		}
	}

	@BindingAdapter("behavior_bottomSheetCallback")
	public static void bindBottomSheetStateChange(final View view, final BottomSheetCallback callback) {
		final BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(view);
		if (behavior == null) throw new IllegalArgumentException(view + " has no BottomSheetBehavior");
		behavior.setBottomSheetCallback(callback);
	}
}