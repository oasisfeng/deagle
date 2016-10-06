package com.oasisfeng.android.os;

import android.os.Bundle;
import android.os.Parcelable;

import com.oasisfeng.android.util.Consumer;

/**
 * Bundle-related helpers
 *
 * Created by Oasis on 2016/10/4.
 */
public class Bundles {

	@SafeVarargs public static Bundle build(final Consumer<Bundle>... actions) {
		final Bundle bundle = new Bundle(actions.length);
		for (final Consumer<Bundle> action : actions)
			action.accept(bundle);
		return bundle;
	}

	/** Make a Bundle for a single key/value pair. */
	public static Bundle forPair(final String key, final String value) {
		final Bundle b = new Bundle(1);
		b.putString(key, value);
		return b;
	}

	public static Bundle forPair(final String key, final Parcelable value) {
		final Bundle b = new Bundle(1);
		b.putParcelable(key, value);
		return b;
	}

	public static Bundle of(final Parcelable value) { return forPair(null, value); }

	private Bundles() {}
}
