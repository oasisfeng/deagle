package com.oasisfeng.android.databinding.adapters;

import android.databinding.BindingAdapter;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.SwipeDismissBehavior;
import android.view.View;

/**
 * Binding adapter to attach {@link SwipeDismissBehavior} to any view inside {@link CoordinatorLayout}.
 *
 * Created by Oasis on 2017/9/9.
 */
public class SwipeDismissBehaviorBindingAdapter {

	@BindingAdapter(value = {"swipeDismissible", "dragDismissDistance", "onDismiss"}, requireAll = false)
	public static void setSwipeDismissBehavior(final View view, final boolean dismissible, final @Nullable Float drag_dismiss_distance,
											   final Runnable dismiss_callback) {
		final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
		final CoordinatorLayout.Behavior behavior = params.getBehavior();
		if (! dismissible) {
			if (behavior instanceof SwipeDismissBehavior) params.setBehavior(null);
			return;
		}
		final SwipeDismissBehavior<?> swipe_behavior = behavior instanceof SwipeDismissBehavior ? (SwipeDismissBehavior) behavior
				: new SwipeDismissBehavior<>();
		swipe_behavior.setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY);
		if (drag_dismiss_distance != null) swipe_behavior.setDragDismissDistance(drag_dismiss_distance);
		swipe_behavior.setListener(new SwipeDismissBehavior.OnDismissListener() {

			@Override public void onDismiss(final View view) {
				view.setVisibility(View.GONE);
				view.setAlpha(1);		// Reset the alpha after fade-out for possible later reuse of the card view.
				if (dismiss_callback != null) dismiss_callback.run();
			}

			@Override public void onDragStateChanged(final int state) {}
		});
		params.setBehavior(swipe_behavior);
	}
}
