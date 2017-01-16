package com.oasisfeng.android.databinding.recyclerview;

import android.databinding.BindingAdapter;
import android.databinding.ObservableList;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;

/**
 * Binding adapters for RecyclerView.
 *
 * Created by Oasis on 2016/2/14.
 */
@SuppressWarnings("unused")
public class RecyclerViewBindingAdapter {

	@BindingAdapter({"items", "item_binder", "item_layout_selector"})
	public static <T> void setItemsAndBinder(final RecyclerView recycler_view, final ObservableList<T> items, final ItemBinder<T> binder, final LayoutSelector<T> layout_selector) {
		recycler_view.setAdapter(new BindingRecyclerViewAdapter<>(items, binder, layout_selector));
	}

	@BindingAdapter({"items", "item_binder", "item_layout"})
	public static <T> void setItemsAndBinder(final RecyclerView recycler_view, final ObservableList<T> items, final ItemBinder<T> binder, final @LayoutRes int item_layout) {
		recycler_view.setAdapter(new BindingRecyclerViewAdapter<>(items, binder, new LayoutSelector<T>() {
			@Override public int getLayoutRes(final T model) { return item_layout; }
		}));
	}
}
