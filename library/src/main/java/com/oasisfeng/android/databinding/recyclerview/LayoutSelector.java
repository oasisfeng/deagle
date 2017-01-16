package com.oasisfeng.android.databinding.recyclerview;

import android.support.annotation.LayoutRes;

/**
 * The layout selector interface for items in {@link android.support.v7.widget.RecyclerView}.
 *
 * Created by Oasis on 2017/1/16.
 */
public interface LayoutSelector<T> {
	@LayoutRes int getLayoutRes(T model);
}
