/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.oasisfeng.android.util;

import android.support.annotation.VisibleForTesting;

import java.io.Serializable;

/**
 * Useful suppliers.
 *
 * <p>All methods return serializable suppliers as long as they're given serializable parameters.
 *
 * @author Laurence Gonsalves
 * @author Harry Heymann
 */
public class Suppliers {

	public static <T> Supplier<T> memoize(final Supplier<T> delegate) {
		if (delegate instanceof NonSerializableMemoizingSupplier || delegate instanceof MemoizingSupplier) return delegate;
		return delegate instanceof Serializable ? new MemoizingSupplier<>(delegate) : new NonSerializableMemoizingSupplier<>(delegate);
	}

	@VisibleForTesting static class MemoizingSupplier<T> implements Supplier<T>, Serializable {
		final Supplier<T> delegate;
		transient volatile boolean initialized;
		// "value" does not need to be volatile; visibility piggy-backs
		// on volatile read of "initialized".
		transient T value;

		MemoizingSupplier(final Supplier<T> delegate) {
			if (delegate == null) throw new NullPointerException();
			this.delegate = delegate;
		}

		@Override
		public T get() {
			// A 2-field variant of Double Checked Locking.
			if (!initialized) {
				synchronized (this) {
					if (!initialized) {
						final T t = delegate.get();
						value = t;
						initialized = true;
						return t;
					}
				}
			}
			return value;
		}

		@Override
		public String toString() {
			return "Suppliers.memoize(" + delegate + ")";
		}

		private static final long serialVersionUID = 0;
	}

	@VisibleForTesting static class NonSerializableMemoizingSupplier<T> implements Supplier<T> {
		volatile Supplier<T> delegate;
		volatile boolean initialized;
		// "value" does not need to be volatile; visibility piggy-backs
		// on volatile read of "initialized".
		T value;

		NonSerializableMemoizingSupplier(final Supplier<T> delegate) {
			if (delegate == null) throw new NullPointerException();
			this.delegate = delegate;
		}

		@Override public T get() {
			// A 2-field variant of Double Checked Locking.
			if (!initialized) {
				synchronized (this) {
					if (!initialized) {
						final T t = delegate.get();
						value = t;
						initialized = true;
						// Release the delegate to GC.
						delegate = null;
						return t;
					}
				}
			}
			return value;
		}

		@Override public String toString() {
			return "Suppliers.memoize(" + delegate + ")";
		}
	}
}
