package com.oasisfeng.android.databinding.recyclerview;

/**
 * The binder interface for items in {@link android.support.v7.widget.RecyclerView}.
 *
 * Created by Oasis on 2016/2/14.
 */
public interface ItemBinder<T> {
	int getLayoutRes(T model);
	int getBindingVariable(T model);
}
