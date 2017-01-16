package com.oasisfeng.android.databinding.recyclerview;

import android.databinding.ViewDataBinding;

/**
 * The binder interface for items in {@link android.support.v7.widget.RecyclerView}.
 *
 * Created by Oasis on 2016/2/14.
 */
public interface ItemBinder<T> {
	void onBind(ViewDataBinding container, T model, ViewDataBinding item);
}
