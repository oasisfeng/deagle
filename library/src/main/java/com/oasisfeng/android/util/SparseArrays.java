package com.oasisfeng.android.util;

import android.os.Build;
import android.util.LongSparseArray;
import android.util.SparseArray;

import java.util.Iterator;

import androidx.annotation.RequiresApi;

/**
 * Utilities for {@link android.util.SparseIntArray}
 *
 * Created by Oasis on 2017/1/7.
 */
public class SparseArrays {

	public static <T> Iterable<T> iterate(final SparseArray<T> array) {
		return new Iterable<T>() { @Override public Iterator<T> iterator() {
			return new Iterator<T>() {
				@Override public boolean hasNext() { return i < array.size(); }
				@Override public T next() { return array.valueAt(i ++); }
				int i = 0;
			};
		}};
	}

	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
	public static <T> Iterable<T> iterate(final LongSparseArray<T> array) {
		return new Iterable<T>() { @Override public Iterator<T> iterator() {
			return new Iterator<T>() {
				@Override public boolean hasNext() { return i < array.size(); }
				@Override public T next() { return array.valueAt(i ++); }
				int i = 0;
			};
		}};
	}
}
