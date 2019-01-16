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

import android.os.SystemClock;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import androidx.annotation.VisibleForTesting;

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

	/**
	 * Returns a supplier that caches the instance supplied by the delegate and removes the cached
	 * value after the specified time has passed. Subsequent calls to {@code get()} return the cached
	 * value if the expiration time has not passed. After the expiration time, a new value is
	 * retrieved, cached, and returned. See:
	 * <a href="http://en.wikipedia.org/wiki/Memoization">memoization</a>
	 *
	 * <p>The returned supplier is thread-safe. The supplier's serialized form does not contain the
	 * cached value, which will be recalculated when {@code
	 * get()} is called on the reserialized instance.
	 *
	 * @param duration the length of time after a value is created that it should stop being returned
	 *     by subsequent {@code get()} calls
	 * @param unit the unit that {@code duration} is expressed in
	 * @throws IllegalArgumentException if {@code duration} is not positive
	 * @since 2.0
	 */
	public static <T> Supplier<T> memoizeWithExpiration(final Supplier<T> delegate, final long duration, final TimeUnit unit) {
		return new ExpiringMemoizingSupplier<T>(delegate, duration, unit);
	}

	@VisibleForTesting
	static class ExpiringMemoizingSupplier<T> implements Supplier<T>, Serializable {
		final Supplier<T> delegate;
		final long durationMillis;
		transient volatile T value;
		// The special value 0 means "not yet initialized".
		transient volatile long expirationMillis;

		ExpiringMemoizingSupplier(final Supplier<T> delegate, final long duration, final TimeUnit unit) {
			if (delegate == null) throw new NullPointerException();
			this.delegate = delegate;
			this.durationMillis = unit.toMillis(duration);
			if (duration <= 0) throw new IllegalArgumentException();
		}

		@Override
		public T get() {
			// Another variant of Double Checked Locking.
			//
			// We use two volatile reads. We could reduce this to one by
			// putting our fields into a holder class, but (at least on x86)
			// the extra memory consumption and indirection are more
			// expensive than the extra volatile reads.
			long millis = expirationMillis;
			final long now = SystemClock.uptimeMillis();
			if (millis == 0 || now - millis >= 0) {
				synchronized (this) {
					if (millis == expirationMillis) { // recheck for lost race
						final T t = delegate.get();
						value = t;
						millis = now + durationMillis;
						// In the very unlikely event that millis is 0, set it to 1;
						// no one will notice 1 ns of tardiness.
						expirationMillis = (millis == 0) ? 1 : millis;
						return t;
					}
				}
			}
			return value;
		}

		@Override public String toString() {
			return "Suppliers.memoizeWithExpiration(" + delegate + ", " + durationMillis + "ms)";
		}

		private static final long serialVersionUID = 0;
	}
}
