package com.oasisfeng.java.util;

import java.io.Serializable;

/**
 * Created by Oasis on 2018-7-4.
 */
class Collections {

	/**
	 * Returns a comparator that imposes the reverse of the <em>natural
	 * ordering</em> on a collection of objects that implement the
	 * {@code Comparable} interface.  (The natural ordering is the ordering
	 * imposed by the objects' own {@code compareTo} method.)  This enables a
	 * simple idiom for sorting (or maintaining) collections (or arrays) of
	 * objects that implement the {@code Comparable} interface in
	 * reverse-natural-order.  For example, suppose {@code a} is an array of
	 * strings. Then: <pre>
	 *          Arrays.sort(a, Collections.reverseOrder());
	 * </pre> sorts the array in reverse-lexicographic (alphabetical) order.<p>
	 *
	 * The returned comparator is serializable.
	 *
	 * @param  <T> the class of the objects compared by the comparator
	 * @return A comparator that imposes the reverse of the <i>natural
	 *         ordering</i> on a collection of objects that implement
	 *         the <tt>Comparable</tt> interface.
	 * @see Comparable
	 */
	@SuppressWarnings("unchecked")
	public static <T> Comparator<T> reverseOrder() {
		return (Comparator<T>) ReverseComparator.REVERSE_ORDER;
	}

	/**
	 * @serial include
	 */
	private static class ReverseComparator
			implements Comparator<Comparable<Object>>, Serializable {

		private static final long serialVersionUID = 7207038068494060240L;

		static final ReverseComparator REVERSE_ORDER
				= new ReverseComparator();

		public int compare(Comparable<Object> c1, Comparable<Object> c2) {
			return c2.compareTo(c1);
		}

		private Object readResolve() { return java.util.Collections.reverseOrder(); }

		@Override
		public Comparator<Comparable<Object>> reversed() {
			return Comparators.naturalOrder();
		}
	}

	/**
	 * Returns a comparator that imposes the reverse ordering of the specified
	 * comparator.
	 *
	 * <p>The returned comparator is serializable (assuming the specified
	 * comparator is also serializable or {@code null}).
	 *
	 * @param <T> the class of the objects compared by the comparator
	 * @param cmp a comparator who's ordering is to be reversed by the returned
	 * comparator or {@code null}
	 * @return A comparator that imposes the reverse ordering of the
	 *         specified comparator.
	 * @since 1.5
	 */
	static <T> Comparator<T> reverseOrder(Comparator<T> cmp) {
		if (cmp instanceof ReverseComparator2)
			return ((ReverseComparator2<T>)cmp).cmp;

		return new ReverseComparator2<>(cmp);
	}

	/**
	 * @serial include
	 */
	private static class ReverseComparator2<T> implements Comparator<T>,
			Serializable
	{
		private static final long serialVersionUID = 4374092139857L;

		/**
		 * The comparator specified in the static factory.  This will never
		 * be null, as the static factory returns a ReverseComparator
		 * instance if its argument is null.
		 *
		 * @serial
		 */
		final Comparator<T> cmp;

		ReverseComparator2(Comparator<T> cmp) {
			assert cmp != null;
			this.cmp = cmp;
		}

		public int compare(T t1, T t2) {
			return cmp.compare(t2, t1);
		}

		public boolean equals(Object o) {
			return (o == this) ||
					(o instanceof ReverseComparator2 &&
							cmp.equals(((ReverseComparator2)o).cmp));
		}

		public int hashCode() {
			return cmp.hashCode() ^ Integer.MIN_VALUE;
		}

		@Override
		public Comparator<T> reversed() {
			return cmp;
		}
	}
}
