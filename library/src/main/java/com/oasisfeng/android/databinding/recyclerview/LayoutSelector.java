package com.oasisfeng.android.databinding.recyclerview;

import androidx.annotation.LayoutRes;

/**
 * The layout selector interface for items in {@link androidx.recyclerview.widget.RecyclerView RecyclerView}.
 *
 * Created by Oasis on 2017/1/16.
 */
public interface LayoutSelector<T> {
	@LayoutRes int getLayoutRes(T model);
}
