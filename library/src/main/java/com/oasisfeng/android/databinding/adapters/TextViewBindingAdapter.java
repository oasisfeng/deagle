package com.oasisfeng.android.databinding.adapters;

import android.databinding.BindingAdapter;
import android.support.annotation.StringRes;
import android.widget.TextView;

/**
 * Extra bindings for {@link android.widget.TextView}
 *
 * Created by Oasis on 2016/8/23.
 */
@SuppressWarnings("unused")
public class TextViewBindingAdapter {

	@BindingAdapter("text")
	public static void setText(final TextView view, final @StringRes int text_res) {
		view.setText(text_res > 0 ? view.getContext().getText(text_res) : null);
	}
}
