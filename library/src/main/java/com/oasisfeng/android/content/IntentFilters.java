package com.oasisfeng.android.content;

import android.content.IntentFilter;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Helpers for building {@link IntentFilter}.
 *
 * Created by Oasis on 2015/12/29.
 */
public class IntentFilters {

	public static class FluentIntentFilter extends IntentFilter {

		public FluentIntentFilter withAction(final String action) {
			super.addAction(action);
			return this;
		}

		public FluentIntentFilter withActions(final String... actions) {
			for (final String action : actions) super.addAction(action);
			return this;
		}

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

	public static FluentIntentFilter forActions(final String... actions) {
		return new FluentIntentFilter().withActions(actions);
	}
}
