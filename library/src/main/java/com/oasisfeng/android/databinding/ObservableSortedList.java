package com.oasisfeng.android.databinding;

import android.databinding.ListChangeRegistry;
import android.databinding.ObservableList;
import android.support.annotation.Nullable;
import android.support.v7.util.SortedList;

import java.util.AbstractList;

/**
 * Observable wrapper of {@link SortedList}, which implements {@link ObservableList}.
 *
 * Created by Oasis on 2015/7/20.
 */
public class ObservableSortedList<T extends ObservableSortedList.Sortable<T>> extends AbstractList<T> implements ObservableList<T> {

	public interface Sortable<T> extends Comparable<T> {
		/** @see android.support.v7.util.SortedList.Callback#areItemsTheSame(Object, Object) */
		boolean isSameAs(T another);
		/** @see android.support.v7.util.SortedList.Callback#areContentsTheSame(Object, Object) */
		boolean isContentSameAs(T another);
	}

	public ObservableSortedList(final Class<T> clazz) {
		mList = new SortedList<>(clazz, new CallbackWrapper());
	}

	/** @see SortedList#beginBatchedUpdates() */
	public void beginBatchedUpdates() { mList.beginBatchedUpdates(); }
	/** @see SortedList#endBatchedUpdates() */
	public void endBatchedUpdates() { mList.endBatchedUpdates(); }

	public void updateItemAt(final int index, final T item) {
		mList.updateItemAt(index, item);
	}

	@Override public boolean add(final T item) {
		mList.add(item);
		return true;	// Even if item is the same, it is still replaced in the list.
	}

	@Override public T set(final int location, final T object) {
		final T old = mList.get(location);
		mList.updateItemAt(location, cast(object));
		return old;
	}

	@Override public int indexOf(final Object object) {
		try {
			return mList.indexOf(cast(object));
		} catch (final ClassCastException ignored) {
			return -1;
		}
	}

	@Override public boolean remove(final Object object) {
		try {
			return mList.remove(cast(object));
		} catch (final ClassCastException ignored) {
			return false;
		}
	}


	@SuppressWarnings("unchecked") private T cast(final Object object) { return (T) object; }

	@Override public boolean contains(final Object object) { return indexOf(object) != SortedList.INVALID_POSITION; }
	@Override public T get(final int location) { return mList.get(location); }
	@Override public int size() { return mList.size(); }
	@Override public void clear() { mList.clear(); }
	@Override public T remove(final int location) { return mList.removeItemAt(location); }

	/* ObservableList */

	@Override public void addOnListChangedCallback(final OnListChangedCallback<? extends ObservableList<T>> callback) {
		if (mListeners == null) this.mListeners = new ListChangeRegistry();
		mListeners.add(callback);
	}

	@Override public void removeOnListChangedCallback(final OnListChangedCallback<? extends ObservableList<T>> callback) {
		if (mListeners == null) return;
		mListeners.remove(callback);
	}

	private final SortedList<T> mList;
	private transient @Nullable ListChangeRegistry mListeners = new ListChangeRegistry();

	public class CallbackWrapper extends SortedList.Callback<T> {

		@Override public final void onInserted(final int position, final int count) {
			if (mListeners != null) mListeners.notifyInserted(ObservableSortedList.this, position, count);
		}

		@Override public final void onRemoved(final int position, final int count) {
			if (mListeners != null) mListeners.notifyRemoved(ObservableSortedList.this, position, count);
		}

		@Override public final void onMoved(final int fromPosition, final int toPosition) {
			if (mListeners != null) mListeners.notifyMoved(ObservableSortedList.this, fromPosition, toPosition, 1);
		}

		@Override public final void onChanged(final int position, final int count) {
			if (mListeners != null) mListeners.notifyChanged(ObservableSortedList.this, position, count);
		}

		@Override public int compare(final T o1, final T o2) {
			return o1.compareTo(o2);
		}

		@Override public boolean areContentsTheSame(final T oldItem, final T newItem) {
			return oldItem.isContentSameAs(newItem);
		}

		@Override public boolean areItemsTheSame(final T item1, final T item2) {
			return item1.isSameAs(item2);
		}
	}
}
