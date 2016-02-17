package com.oasisfeng.android.databinding.recyclerview;

/**
 * Item binder for items with same layout.
 *
 * Created by Oasis on 2016/2/14.
 */
public class MonoItemBinder<T> implements ItemBinder<T> {

	public MonoItemBinder(final int binding_variable, final int layout_id) {
		this.mBindingVariable = binding_variable;
		this.mLayoutId = layout_id;
	}

	public int getLayoutRes(final T model) {
		return mLayoutId;
	}

	public int getBindingVariable(final T model) {
		return mBindingVariable;
	}

	protected final int mBindingVariable;
	protected final int mLayoutId;
}
