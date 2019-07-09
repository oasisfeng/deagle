package com.oasisfeng.android.databinding.recyclerview;

import androidx.databinding.ViewDataBinding;

/**
 * The binder interface for items in {@link androidx.recyclerview.widget.RecyclerView RecyclerView}.
 *
 * Created by Oasis on 2016/2/14.
 */
public interface ItemBinder<T> {
	void onBind(ViewDataBinding container, T model, ViewDataBinding item);
}
