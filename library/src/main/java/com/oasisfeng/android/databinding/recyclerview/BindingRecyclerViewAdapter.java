package com.oasisfeng.android.databinding.recyclerview;

import android.databinding.DataBindingUtil;
import android.databinding.ObservableList;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.oasisfeng.android.databinding.ObservableSortedList;

import java.util.Collection;

/**
 * Adapter of RecycleView for data-binding
 *
 * Created by Oasis on 2016/2/14.
 */
public class BindingRecyclerViewAdapter<T extends ObservableSortedList.Sortable<T>> extends RecyclerView.Adapter<BindingRecyclerViewAdapter.ViewHolder> {

	public BindingRecyclerViewAdapter(final Collection<T> items) {
		@SuppressWarnings("unchecked") final Class<T> clazz = (Class<T>) items.iterator().next().getClass();
		this.mItems = new ObservableSortedList<>(clazz);
		this.mItems.addOnListChangedCallback(new OnListChangedCallback<ObservableSortedList<T>>(this));
		this.mItems.addAll(items);
	}

	public void setItemBinder(final ItemBinder<T> itemBinder) {
		this.mItemBinder = itemBinder;
	}

	@Override public ViewHolder onCreateViewHolder(final ViewGroup view_group, final int layout_id) {
		if (mInflater == null) mInflater = LayoutInflater.from(view_group.getContext());
		final ViewDataBinding binding = DataBindingUtil.inflate(mInflater, layout_id, view_group, false);
		return new ViewHolder(binding);
	}

	@Override public void onBindViewHolder(final ViewHolder holder, final int position) {
		final T item = mItems.get(position);
		holder.binding.setVariable(mItemBinder.getBindingVariable(item), item);
		holder.binding.executePendingBindings();
	}

	@Override public int getItemViewType(final int position) {
		return mItemBinder.getLayoutRes(mItems.get(position));
	}

	@Override public int getItemCount() {
		return mItems == null ? 0 : mItems.size();
	}

	private final ObservableSortedList<T> mItems;
	private ItemBinder<T> mItemBinder;
	private transient LayoutInflater mInflater;

	public static class ViewHolder extends RecyclerView.ViewHolder {

		ViewHolder(final ViewDataBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}

		final ViewDataBinding binding;
	}

	private static class OnListChangedCallback<T extends ObservableList> extends ObservableList.OnListChangedCallback<T> {

		@Override public void onChanged(final ObservableList sender) {
			mAdapter.notifyDataSetChanged();
		}

		@Override public void onItemRangeChanged(final ObservableList sender, final int positionStart, final int itemCount) {
			mAdapter.notifyItemRangeChanged(positionStart, itemCount);
		}

		@Override public void onItemRangeInserted(final ObservableList sender, final int positionStart, final int itemCount) {
			mAdapter.notifyItemRangeInserted(positionStart, itemCount);
		}

		@Override public void onItemRangeMoved(final ObservableList sender, final int fromPosition, final int toPosition, final int itemCount) {
			mAdapter.notifyItemMoved(fromPosition, toPosition);
		}

		@Override public void onItemRangeRemoved(final ObservableList sender, final int positionStart, final int itemCount) {
			mAdapter.notifyItemRangeRemoved(positionStart, itemCount);
		}

		OnListChangedCallback(final BindingRecyclerViewAdapter<?> adapter) {
			mAdapter = adapter;
		}

		private final BindingRecyclerViewAdapter<?> mAdapter;
	}
}
