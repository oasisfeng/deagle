package com.oasisfeng.android.databinding;

import java.util.AbstractList;
import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ListChangeRegistry;
import androidx.databinding.ObservableList;
import androidx.recyclerview.widget.SortedList;

/**
 * Observable wrapper of {@link SortedList}, which implements {@link ObservableList}.
 *
 * Created by Oasis on 2015/7/20.
 */
public class ObservableSortedList<T extends ObservableSortedList.Sortable<? super T>> extends AbstractList<T> implements ObservableList<T> {

	public interface Sortable<T> extends Comparable<T> {
		/** @see androidx.recyclerview.widget.SortedList.Callback#areItemsTheSame(Object, Object) */
		boolean isSameAs(T another);
		/** @see androidx.recyclerview.widget.SortedList.Callback#areContentsTheSame(Object, Object) */
		boolean isContentSameAs(T another);
	}

	public ObservableSortedList(final Class<T> clazz) {
		mList = new SortedList<>(clazz, new CallbackWrapper());
	}

	/** @see SortedList#beginBatchedUpdates() */
	public void beginBatchedUpdates() { mList.beginBatchedUpdates(); }
	/** @see SortedList#endBatchedUpdates() */
	public void endBatchedUpdates() { mList.endBatchedUpdates(); }

	/** @see SortedList#updateItemAt(int, Object) */
	public void updateItemAt(final int index, final T item) {
		mList.updateItemAt(index, item);
	}

	/** @see SortedList#add(Object) */
	@Override public boolean add(final T item) {
		mList.add(item);
		return true;	// Even if item is the same, it is still replaced in the list.
	}

	/** @see SortedList#addAll(Collection) */
	@Override public boolean addAll(final @NonNull Collection<? extends T> c) {
		@SuppressWarnings("unchecked") final Collection<T> casted = (Collection<T>) c;
		mList.addAll(casted);
		return true;
	}

	/** @see SortedList#updateItemAt(int, Object) */
	@Override public T set(final int location, final T object) {
		final T old = mList.get(location);
		mList.updateItemAt(location, cast(object));
		return old;
	}

	/** @see SortedList#indexOf(Object) */
	@Override public int indexOf(final Object object) {
		try {
			return mList.indexOf(cast(object));
		} catch (final ClassCastException ignored) {
			return -1;
		}
	}

	/** @see SortedList#remove(Object) */
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

	private class CallbackWrapper extends SortedList.Callback<T> {

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
