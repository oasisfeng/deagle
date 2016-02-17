package com.oasisfeng.android.databinding.recyclerview;

import android.databinding.BindingAdapter;
import android.support.v7.widget.RecyclerView;

import com.oasisfeng.android.databinding.ObservableSortedList;

import java.util.Collection;

/**
 * Binding adapters for RecyclerView.
 *
 * Created by Oasis on 2016/2/14.
 */
@SuppressWarnings("unused")
public class RecyclerViewBindings {

	@BindingAdapter("items") public static <T extends ObservableSortedList.Sortable<T>>
	void setItems(final RecyclerView recycler_view, final Collection<T> items) {
		final BindingRecyclerViewAdapter<T> adapter = new BindingRecyclerViewAdapter<>(items);
		recycler_view.setAdapter(adapter);
	}

	@BindingAdapter("itemBinder") public static <T extends ObservableSortedList.Sortable<T>>
	void setItemBinder(final RecyclerView recycler_view, final ItemBinder<T> binder) {
		@SuppressWarnings("unchecked") final BindingRecyclerViewAdapter<T> adapter = (BindingRecyclerViewAdapter<T>) recycler_view.getAdapter();
		adapter.setItemBinder(binder);
	}
}
