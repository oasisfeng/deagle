package com.oasisfeng.android.databinding.adapters;

import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

/**
 * Binding adapter for click listeners on compound drawables of {@link TextView}.
 *
 * Created by Oasis on 2016/4/21.
 */
@SuppressWarnings("unused")
public class TextViewDrawableClickBindingAdapter {

	@BindingAdapter(value = {"onDrawableStartClick", "onDrawableEndClick", "onDrawableLeftClick", "onDrawableRightClick"}, requireAll = false)
	public static void setOnDrawableClickListener(final TextView view, final OnClickListener start_listener,
			final OnClickListener end_listener, final OnClickListener left_listener, final OnClickListener right_listener) {
		view.setOnTouchListener(new View.OnTouchListener() { @Override public boolean onTouch(final View v, final MotionEvent event) {
			final boolean fire = event.getAction() == MotionEvent.ACTION_UP;	// Non-up action should still be consumed to avoid unexpected asymmetric behaviors
			final TextView view = (TextView) v;
			final Drawable drawable_left = view.getCompoundDrawables()[0], drawable_right = view.getCompoundDrawables()[2];
			final float x = event.getX(0);
			boolean consumed = false;
			if (drawable_left != null && x <= view.getCompoundPaddingLeft() - view.getCompoundDrawablePadding() / 2) {
				if ((consumed = (left_listener != null)) && fire) left_listener.onClick(v);
				if (VERSION.SDK_INT >= JELLY_BEAN_MR1) {
					if (v.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
						if ((consumed = (start_listener != null)) && fire) start_listener.onClick(v);
					} else if ((consumed = (end_listener != null)) && fire) end_listener.onClick(v);
				}
			} else if (drawable_right != null && x >= view.getWidth() - view.getCompoundPaddingRight() + view.getCompoundDrawablePadding() / 2) {
				if ((consumed = (right_listener != null)) && fire) right_listener.onClick(v);
				if (VERSION.SDK_INT >= JELLY_BEAN_MR1) {
					if (v.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
						if ((consumed = (end_listener != null)) && fire) end_listener.onClick(v);
					} else if ((consumed = (start_listener != null)) && fire) start_listener.onClick(v);
				}
			}
			return consumed;
		}});
	}
}
