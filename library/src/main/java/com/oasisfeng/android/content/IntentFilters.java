package com.oasisfeng.android.content;

import android.content.IntentFilter;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PatternMatcher;

import java.util.Collection;
import java.util.Iterator;

/**
 * Helpers for building {@link IntentFilter}.
 *
 * Created by Oasis on 2015/12/29.
 */
public class IntentFilters {

	public static class FluentIntentFilter extends IntentFilter {

		public FluentIntentFilter withAction(final String action) { addAction(action); return this; }
		public FluentIntentFilter withActions(final String... actions) { for (final String action : actions) addAction(action); return this; }
		public FluentIntentFilter withDataScheme(final String scheme) { addDataScheme(scheme); return this; }
		public FluentIntentFilter withDataSchemes(final String... schemes) { for (final String scheme : schemes) addDataScheme(scheme); return this; }
		public FluentIntentFilter withData(final String scheme, final String ssp, final int type) {
			addDataScheme(scheme); addDataSchemeSpecificPart(ssp, type); return this;
		}
		public FluentIntentFilter withDataType(final String type) throws MalformedMimeTypeException { addDataType(type); return this; }
		public FluentIntentFilter withCategory(final String category) { addCategory(category); return this; }
		public FluentIntentFilter withCategories(final String... categories) { for (final String category : categories) addCategory(category); return this; }
		public FluentIntentFilter inPriority(final int priority) { setPriority(priority); return this; }

		public FluentIntentFilter(final IntentFilter filter) { super(filter); }
		public FluentIntentFilter() {}

		public static final Parcelable.Creator<FluentIntentFilter> CREATOR = new Parcelable.Creator<FluentIntentFilter>() {
			public FluentIntentFilter createFromParcel(final Parcel source) {
				return new FluentIntentFilter(IntentFilter.CREATOR.createFromParcel(source));
			}
			public FluentIntentFilter[] newArray(final int size) { return new FluentIntentFilter[size]; }
		};
	}

	public static FluentIntentFilter build() { return new FluentIntentFilter(); }

	public static FluentIntentFilter forAction(final String action) {
		return new FluentIntentFilter().withAction(action);
	}

	public static FluentIntentFilter forActions(final String... actions) {
		return new FluentIntentFilter().withActions(actions);
	}

	public static boolean equalIgnoringOrder(final Collection<IntentFilter> ca, final Collection<IntentFilter> cb) {
		if (ca.size() != cb.size()) return false;
		NEXT: for (final IntentFilter a : ca) {
			for (final IntentFilter b : cb)
				if (equal(a, b)) continue NEXT;
			return false;
		}
		return true;
	}

	public static boolean equal(final Iterable<IntentFilter> ia, final Iterable<IntentFilter> ib) {
		final Iterator<IntentFilter> a = ia.iterator(), b = ib.iterator();
		while (a.hasNext()) {
			if (! b.hasNext()) return false;
			if (! equal(a.next(), b.next())) return false;
		}
		return ! b.hasNext();
	}

	public static boolean equal(final IntentFilter a, final IntentFilter b) {
		return equal(a.actionsIterator(), b.actionsIterator())
				&& equal(a.categoriesIterator(), b.categoriesIterator())
				&& equal(a.typesIterator(), b.typesIterator())
				&& equal(a.schemesIterator(), b.schemesIterator())
				&& equal_ssp(a, b)
				&& equal_authorities(a.authoritiesIterator(), b.authoritiesIterator())
				&& equal_patterns(a.pathsIterator(), b.pathsIterator());
	}

	private static boolean equal(final Iterator<String> a, final Iterator<String> b) {
		return a == b || (a != null && b != null && elementsEqual(a, b));
	}

	private static boolean equal_ssp(final IntentFilter a, final IntentFilter b) { //noinspection SimplifiableIfStatement
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return true;
		return equal_patterns(a.schemeSpecificPartsIterator(), b.schemeSpecificPartsIterator());
	}

	private static boolean equal_patterns(final Iterator<PatternMatcher> a, final Iterator<PatternMatcher> b) {
		if (a == b) return true;
		if (a == null || b == null) return false;
		while (a.hasNext()) {
			if (! b.hasNext()) return false;
			final PatternMatcher o1 = a.next();
			final PatternMatcher o2 = b.next();
			if (! equal(o1, o2)) return false;
		}
		return ! b.hasNext();
	}

	private static boolean equal_authorities(final Iterator<IntentFilter.AuthorityEntry> a, final Iterator<IntentFilter.AuthorityEntry> b) {
		if (a == b) return true;
		if (a == null || b == null) return false;
		while (a.hasNext()) {
			if (! b.hasNext()) return false;
			final IntentFilter.AuthorityEntry o1 = a.next();
			final IntentFilter.AuthorityEntry o2 = b.next();
			if (! equal(o1, o2)) return false;
		}
		return ! b.hasNext();
	}

	private static boolean equal(final PatternMatcher a, final PatternMatcher b) {
		return a == b || (a != null && b != null && a.getType() == b.getType() && equal(a.getPath(), b.getPath()));
	}

	private static boolean equal(final IntentFilter.AuthorityEntry a, final IntentFilter.AuthorityEntry b) {
		return a == b || (a != null && b != null && equal(a.getHost(), b.getHost()) && a.getPort() == b.getPort());
	}

	private static boolean elementsEqual(final Iterator<?> a, final Iterator<?> b) {
		while (a.hasNext()) {
			if (! b.hasNext()) return false;
			if (! equal(a.next(), b.next())) return false;
		}
		return ! b.hasNext();
	}

	private static boolean equal(final Object a, final Object b) { return a == b || (a != null && a.equals(b)); }
}
