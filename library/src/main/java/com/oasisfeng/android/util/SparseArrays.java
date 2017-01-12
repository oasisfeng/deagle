package com.oasisfeng.android.util;

import android.util.SparseArray;

import com.google.common.collect.AbstractIterator;

import java.util.Iterator;

/**
 * Utilities for {@link android.util.SparseIntArray}
 *
 * Created by Oasis on 2017/1/7.
 */
public class SparseArrays {

	public static <T> Iterable<T> iterate(final SparseArray<T> array) {
		final int size = array.size();
		return new Iterable<T>() { @Override public Iterator<T> iterator() {
			return new AbstractIterator<T>() {
				@Override protected T computeNext() { return i < size ? array.valueAt(i ++) : endOfData(); }
				int i = 0;
			};
		}};
	}
}
