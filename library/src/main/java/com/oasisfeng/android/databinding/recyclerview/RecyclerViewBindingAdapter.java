package com.oasisfeng.android.databinding.recyclerview;

import android.databinding.BindingAdapter;
import android.databinding.ObservableList;
import android.support.v7.widget.RecyclerView;

import com.oasisfeng.android.databinding.ObservableSortedList;

/**
 * Binding adapters for RecyclerView.
 *
 * Created by Oasis on 2016/2/14.
 */
@SuppressWarnings("unused")
public class RecyclerViewBindingAdapter {

	@BindingAdapter({"items", "item_binder"}) public static <T extends ObservableSortedList.Sortable<T>>
	void setItemsAndBinder(final RecyclerView recycler_view, final ObservableList<T> items, final ItemBinder<T> binder) {
		final BindingRecyclerViewAdapter<T> adapter = new BindingRecyclerViewAdapter<>(items, binder);
		recycler_view.setAdapter(adapter);
	}

	interface OnItemClickListener<T extends ObservableSortedList.Sortable<T>> {
		void onClick(T data);
	}
}
