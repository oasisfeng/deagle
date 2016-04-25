package com.oasisfeng.android.databinding.adapters;

import android.databinding.BindingAdapter;
import android.view.View;

/**
 * Binding adapter for {@link View}
 *
 * Created by Oasis on 2016/4/25.
 */
@SuppressWarnings("unused")
public class ViewBindingAdapter {

	@BindingAdapter("android:visible")
	public static void setVisibility(final View view, final boolean visible) {
		view.setVisibility(visible ? View.VISIBLE : View.GONE);
	}
}
