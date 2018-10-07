/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oasisfeng.java.util;

import java.io.Serializable;

import java9.util.Objects;
import java9.util.function.Function;
import java9.util.function.ToDoubleFunction;
import java9.util.function.ToIntFunction;
import java9.util.function.ToLongFunction;

/**
 * A comparison function, which imposes a <i>total ordering</i> on some
 * collection of objects.  Comparators can be passed to a sort method (such
 * as {@link java.util.Collections#sort(java.util.List,java.util.Comparator) Collections.sort} or {@link
 * java.util.Arrays#sort(Object[],java.util.Comparator) Arrays.sort}) to allow precise control
 * over the sort order.  Comparators can also be used to control the order of
 * certain data structures (such as {@link java.util.SortedSet sorted sets} or {@link
 * java.util.SortedMap sorted maps}), or to provide an ordering for collections of
 * objects that don't have a {@link Comparable natural ordering}.<p>
 *
 * The ordering imposed by a comparator <tt>c</tt> on a set of elements
 * <tt>S</tt> is said to be <i>consistent with equals</i> if and only if
 * <tt>c.compare(e1, e2)==0</tt> has the same boolean value as
 * <tt>e1.equals(e2)</tt> for every <tt>e1</tt> and <tt>e2</tt> in
 * <tt>S</tt>.<p>
 *
 * Caution should be exercised when using a comparator capable of imposing an
 * ordering inconsistent with equals to order a sorted set (or sorted map).
 * Suppose a sorted set (or sorted map) with an explicit comparator <tt>c</tt>
 * is used with elements (or keys) drawn from a set <tt>S</tt>.  If the
 * ordering imposed by <tt>c</tt> on <tt>S</tt> is inconsistent with equals,
 * the sorted set (or sorted map) will behave "strangely."  In particular the
 * sorted set (or sorted map) will violate the general contract for set (or
 * map), which is defined in terms of <tt>equals</tt>.<p>
 *
 * For example, suppose one adds two elements {@code a} and {@code b} such that
 * {@code (a.equals(b) && c.compare(a, b) != 0)}
 * to an empty {@code TreeSet} with comparator {@code c}.
 * The second {@code add} operation will return
 * true (and the size of the tree set will increase) because {@code a} and
 * {@code b} are not equivalent from the tree set's perspective, even though
 * this is contrary to the specification of the
 * {@link java.util.Set#add Set.add} method.<p>
 *
 * Note: It is generally a good idea for comparators to also implement
 * <tt>java.io.Serializable</tt>, as they may be used as ordering methods in
 * serializable data structures (like {@link java.util.TreeSet}, {@link java.util.TreeMap}).  In
 * order for the data structure to serialize successfully, the comparator (if
 * provided) must implement <tt>Serializable</tt>.<p>
 *
 * For the mathematically inclined, the <i>relation</i> that defines the
 * <i>imposed ordering</i> that a given comparator <tt>c</tt> imposes on a
 * given set of objects <tt>S</tt> is:<pre>
 *       {(x, y) such that c.compare(x, y) &lt;= 0}.
 * </pre> The <i>quotient</i> for this total order is:<pre>
 *       {(x, y) such that c.compare(x, y) == 0}.
 * </pre>
 *
 * It follows immediately from the contract for <tt>compare</tt> that the
 * quotient is an <i>equivalence relation</i> on <tt>S</tt>, and that the
 * imposed ordering is a <i>total order</i> on <tt>S</tt>.  When we say that
 * the ordering imposed by <tt>c</tt> on <tt>S</tt> is <i>consistent with
 * equals</i>, we mean that the quotient for the ordering is the equivalence
 * relation defined by the objects' {@link Object#equals(Object)
 * equals(Object)} method(s):<pre>
 *     {(x, y) such that x.equals(y)}. </pre>
 *
 * <p>Unlike {@code Comparable}, a comparator may optionally permit
 * comparison of null arguments, while maintaining the requirements for
 * an equivalence relation.
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <T> the type of objects that may be compared by this comparator
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see Comparable
 * @see java.io.Serializable
 * @since 1.2
 */
@FunctionalInterface
public interface Comparator<T> extends java.util.Comparator<T> {

	/**
	 * Returns a comparator that imposes the reverse ordering of this
	 * comparator.
	 *
	 * @return a comparator that imposes the reverse ordering of this
	 *         comparator.
	 * @since 1.8
	 */
	default Comparator<T> reversed() {
		return Collections.reverseOrder(this);
	}

	/**
	 * Returns a lexicographic-order comparator with another comparator.
	 * If this {@code Comparator} considers two elements equal, i.e.
	 * {@code compare(a, b) == 0}, {@code other} is used to determine the order.
	 *
	 * <p>The returned comparator is serializable if the specified comparator
	 * is also serializable.
	 *
	 * @apiNote
	 * For example, to sort a collection of {@code String} based on the length
	 * and then case-insensitive natural ordering, the comparator can be
	 * composed using following code,
	 *
	 * <pre>{@code
	 *     Comparator<String> cmp = Comparator.comparingInt(String::length)
	 *             .thenComparing(String.CASE_INSENSITIVE_ORDER);
	 * }</pre>
	 *
	 * @param  other the other comparator to be used when this comparator
	 *         compares two objects that are equal.
	 * @return a lexicographic-order comparator composed of this and then the
	 *         other comparator
	 * @throws NullPointerException if the argument is null.
	 * @since 1.8
	 */
	default Comparator<T> thenCompare(Comparator<? super T> other) {
		Objects.requireNonNull(other);
		return (Comparator<T> & Serializable) (c1, c2) -> {
			int res = compare(c1, c2);
			return (res != 0) ? res : other.compare(c1, c2);
		};
	}

	/**
	 * Returns a lexicographic-order comparator with a function that
	 * extracts a key to be compared with the given {@code Comparator}.
	 *
	 * @implSpec This default implementation behaves as if {@code
	 *           thenComparing(comparing(keyExtractor, cmp))}.
	 *
	 * @param  <U>  the type of the sort key
	 * @param  keyExtractor the function used to extract the sort key
	 * @param  keyComparator the {@code Comparator} used to compare the sort key
	 * @return a lexicographic-order comparator composed of this comparator
	 *         and then comparing on the key extracted by the keyExtractor function
	 * @throws NullPointerException if either argument is null.
	 * @see Comparators#comparing(Function, Comparator)
	 * @see #thenCompare(Comparator)
	 * @since 1.8
	 */
	default <U> Comparator<T> thenCompare(
			Function<? super T, ? extends U> keyExtractor,
			Comparator<? super U> keyComparator)
	{
		return thenCompare(Comparators.comparing(keyExtractor, keyComparator));
	}

	/**
	 * Returns a lexicographic-order comparator with a function that
	 * extracts a {@code Comparable} sort key.
	 *
	 * @implSpec This default implementation behaves as if {@code
	 *           thenComparing(comparing(keyExtractor))}.
	 *
	 * @param  <U>  the type of the {@link Comparable} sort key
	 * @param  keyExtractor the function used to extract the {@link
	 *         Comparable} sort key
	 * @return a lexicographic-order comparator composed of this and then the
	 *         {@link Comparable} sort key.
	 * @throws NullPointerException if the argument is null.
	 * @see Comparators#comparing(Function)
	 * @see #thenCompare(Comparator)
	 * @since 1.8
	 */
	default <U extends Comparable<? super U>> Comparator<T> thenCompare(
			Function<? super T, ? extends U> keyExtractor)
	{
		return thenCompare(Comparators.comparing(keyExtractor));
	}

	/**
	 * Returns a lexicographic-order comparator with a function that
	 * extracts a {@code int} sort key.
	 *
	 * @implSpec This default implementation behaves as if {@code
	 *           thenComparing(comparingInt(keyExtractor))}.
	 *
	 * @param  keyExtractor the function used to extract the integer sort key
	 * @return a lexicographic-order comparator composed of this and then the
	 *         {@code int} sort key
	 * @throws NullPointerException if the argument is null.
	 * @see Comparators#comparingInt(ToIntFunction)
	 * @see #thenCompare(Comparator)
	 * @since 1.8
	 */
	default Comparator<T> thenCompareInt(ToIntFunction<? super T> keyExtractor) {
		return thenCompare(Comparators.comparingInt(keyExtractor));
	}

	/**
	 * Returns a lexicographic-order comparator with a function that
	 * extracts a {@code long} sort key.
	 *
	 * @implSpec This default implementation behaves as if {@code
	 *           thenComparing(comparingLong(keyExtractor))}.
	 *
	 * @param  keyExtractor the function used to extract the long sort key
	 * @return a lexicographic-order comparator composed of this and then the
	 *         {@code long} sort key
	 * @throws NullPointerException if the argument is null.
	 * @see Comparators#comparingLong(ToLongFunction)
	 * @see #thenCompare(Comparator)
	 * @since 1.8
	 */
	default Comparator<T> thenComparingLong(ToLongFunction<? super T> keyExtractor) {
		return thenCompare(Comparators.comparingLong(keyExtractor));
	}

	/**
	 * Returns a lexicographic-order comparator with a function that
	 * extracts a {@code double} sort key.
	 *
	 * @implSpec This default implementation behaves as if {@code
	 *           thenComparing(comparingDouble(keyExtractor))}.
	 *
	 * @param  keyExtractor the function used to extract the double sort key
	 * @return a lexicographic-order comparator composed of this and then the
	 *         {@code double} sort key
	 * @throws NullPointerException if the argument is null.
	 * @see Comparators#comparingDouble(ToDoubleFunction)
	 * @see #thenCompare(Comparator)
	 * @since 1.8
	 */
	default Comparator<T> thenComparingDouble(ToDoubleFunction<? super T> keyExtractor) {
		return thenCompare(Comparators.comparingDouble(keyExtractor));
	}
}
