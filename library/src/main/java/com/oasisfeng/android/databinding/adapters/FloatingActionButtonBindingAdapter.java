package com.oasisfeng.android.databinding.adapters;

import android.content.res.ColorStateList;
import android.databinding.BindingAdapter;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;

/**
 * Binding adapter for {@link FloatingActionButton}
 *
 * Created by Oasis on 2016/4/17.
 */
@SuppressWarnings("unused")
public class FloatingActionButtonBindingAdapter {

	@BindingAdapter("android:src") public static void setImageRes(final FloatingActionButton fab, final @DrawableRes int drawable) {
		fab.setImageResource(drawable);
	}

	@BindingAdapter("backgroundTint") public static void setBackgroundTint(final FloatingActionButton fab, final @ColorRes int bg_color) {
		fab.setBackgroundTintList(bg_color != 0 ? ColorStateList.valueOf(ContextCompat.getColor(fab.getContext(), bg_color)) : null);
	}
}
