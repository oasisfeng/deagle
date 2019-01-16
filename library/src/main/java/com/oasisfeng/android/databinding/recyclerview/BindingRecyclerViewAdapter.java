package com.oasisfeng.android.databinding.recyclerview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableList;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adapter of RecycleView for data-binding
 *
 * Created by Oasis on 2016/2/14.
 */
public class BindingRecyclerViewAdapter<T> extends RecyclerView.Adapter<BindingRecyclerViewAdapter.ViewHolder> {

	BindingRecyclerViewAdapter(final ObservableList<T> items, final ItemBinder<T> binder, final LayoutSelector<T> layout_selector) {
		mItems = items;
		mItemBinder = binder;
		mLayoutSelector = layout_selector;
		items.addOnListChangedCallback(new OnListChangedCallback<ObservableList<T>>(this));
		notifyDataSetChanged();
	}

	@Override public ViewHolder onCreateViewHolder(final ViewGroup view_group, final int layout_id) {
		if (mInflater == null) mInflater = LayoutInflater.from(view_group.getContext());
		final ViewDataBinding binding = DataBindingUtil.inflate(mInflater, layout_id, view_group, false);
		return new ViewHolder(binding);
	}

	@Override public void onBindViewHolder(final ViewHolder holder, final int position) {
		final T item = mItems.get(position);
		mItemBinder.onBind(mBinding, item, holder.binding);
		holder.binding.setLifecycleOwner(mLifecycleOwner);
		holder.binding.executePendingBindings();
	}

	@Override public void onAttachedToRecyclerView(final RecyclerView view) {
		mBinding = DataBindingUtil.findBinding(view);
		final Context context = view.getContext();
		if (context instanceof LifecycleOwner) mLifecycleOwner = (LifecycleOwner) context;
	}

	@Override public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
		mLifecycleOwner = null;
		mBinding = null;
	}

	@Override public int getItemViewType(final int position) {
		return mLayoutSelector.getLayoutRes(mItems.get(position));
	}

	@Override public int getItemCount() {
		return mItems == null ? 0 : mItems.size();
	}

	private ViewDataBinding mBinding;
	private final ObservableList<T> mItems;
	private final ItemBinder<T> mItemBinder;
	private final LayoutSelector<T> mLayoutSelector;
	private transient LayoutInflater mInflater;
	private LifecycleOwner mLifecycleOwner;

	public static class ViewHolder extends RecyclerView.ViewHolder {

		ViewHolder(final ViewDataBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}

		public final ViewDataBinding binding;
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
