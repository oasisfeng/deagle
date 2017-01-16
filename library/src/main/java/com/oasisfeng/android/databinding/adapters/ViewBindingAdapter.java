package com.oasisfeng.android.databinding.adapters;

import android.databinding.BindingAdapter;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

/**
 * Complementary binding adapter for {@link View}
 *
 * Created by Oasis on 2016/4/25.
 */
@SuppressWarnings("unused")
public class ViewBindingAdapter {

	@BindingAdapter("visible")
	public static void setVisible(final View view, final boolean visible) {
		view.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
	}

	@BindingAdapter("shown")
	public static void setShown(final View view, final boolean shown) {
		view.setVisibility(shown ? View.VISIBLE : View.GONE);
	}

	@BindingAdapter(requireAll = false, value = {"android:layout_marginTop", "android:layout_marginBottom",
			"android:layout_marginStart", "android:layout_marginEnd", "android:layout_marginLeft", "android:layout_marginRight"})
	public static void setLayoutWidth(final View view, final Float top, final Float bottom, final Float start, final Float end, final Float left, final Float right) {
		final ViewGroup.LayoutParams layout_params = view.getLayoutParams();
		if (! (layout_params instanceof ViewGroup.MarginLayoutParams)) return;
		final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) layout_params;
		//noinspection ResourceType
		params.setMargins(v(left, params.leftMargin), v(top, params.topMargin), v(right, params.rightMargin), v(bottom, params.bottomMargin));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			if (start != null) params.setMarginStart(start.intValue());
			if (end != null) params.setMarginEnd(end.intValue());
		}
		view.setLayoutParams(params);
	}

	private static int v(final Float value, final int default_value) { return value != null ? value.intValue() : default_value; }
}
