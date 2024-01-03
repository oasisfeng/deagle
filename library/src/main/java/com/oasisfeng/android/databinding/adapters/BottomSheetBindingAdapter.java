package com.oasisfeng.android.databinding.adapters;

import android.os.Handler;
import android.view.View;

import androidx.databinding.BindingAdapter;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback;

/**
 * Binding adapter for {@link BottomSheetBehavior}
 *
 * Created by Oasis on 2016/2/27.
 */
@SuppressWarnings("unused")
public class BottomSheetBindingAdapter {

	/** Keep collapsed or expanded state as is, or switch to collapsed state. */
	public static final int STATE_COLLAPSED_OR_EXPANDED = 99;

	@BindingAdapter("behavior_state")
	public static void setBottomSheetState(final View view, final @BottomSheetBehavior.State int state) {
		final BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(view);
		try {
			if (state == STATE_COLLAPSED_OR_EXPANDED) {
				final int lastState = behavior.getState();
				if (lastState != BottomSheetBehavior.STATE_COLLAPSED && lastState != BottomSheetBehavior.STATE_EXPANDED)
					behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
			} else behavior.setState(state);
		} catch (final RuntimeException e) {
			new Handler().post(() -> {
				try { behavior.setState(state); } catch (final RuntimeException ignored) {}
			});
		}
	}

	@BindingAdapter("behavior_bottomSheetCallback")
	public static void bindBottomSheetCallback(final View view, final BottomSheetCallback callback) {
		final BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(view);
		behavior.setBottomSheetCallback(callback);
	}
}