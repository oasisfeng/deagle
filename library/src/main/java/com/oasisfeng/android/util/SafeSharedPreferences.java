package com.oasisfeng.android.util;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;

import com.oasisfeng.deagle.BuildConfig;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * <ul>
 *   <li>Guard against ClassCastException in getters of SharedPreferences, return default value if type mismatched.</li>
 *   <li>Prevent anonymous class from being used in {@link #registerOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener)
 *   registerOnSharedPreferenceChangeListener()}</li>
 *   <li>Prevent the string set return by getStringSet() from modification.</li>
 * </ul>
 *
 * @author Oasis
 */
public class SafeSharedPreferences implements SharedPreferences {

	public static SharedPreferences wrap(final SharedPreferences prefs) {
		if (prefs instanceof SafeSharedPreferences) return prefs;
        return new SafeSharedPreferences(prefs);
    }

    private SafeSharedPreferences(final SharedPreferences aDelegate) {
    	mDelegate = aDelegate;
    }

	@Override public void registerOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
		if (BuildConfig.DEBUG && listener.getClass().isAnonymousClass())
			throw new Error("Never use anonymous inner class for listener, since it is weakly-referenced by SharedPreferences instance.");
		mDelegate.registerOnSharedPreferenceChangeListener(listener);
	}

	@Override public void unregisterOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
		mDelegate.unregisterOnSharedPreferenceChangeListener(listener);
	}

	@Override public String getString(final String key, final String defValue) {
		try {
			return mDelegate.getString(key, defValue);
		} catch (final ClassCastException e) {
			return defValue;
		}
	}

	@Override public int getInt(final String key, final int defValue) {
		try {
			return mDelegate.getInt(key, defValue);
		} catch (final ClassCastException e) {
			return defValue;
		}
	}

	@Override public long getLong(final String key, final long defValue) {
		try {
			return mDelegate.getLong(key, defValue);
		} catch (final ClassCastException e) {
			return defValue;
		}
	}

	@Override public float getFloat(final String key, final float defValue) {
		try {
			return mDelegate.getFloat(key, defValue);
		} catch (final ClassCastException e) {
			return defValue;
		}
	}

	@Override public boolean getBoolean(final String key, final boolean defValue) {
		try {
			return mDelegate.getBoolean(key, defValue);
		} catch (final ClassCastException e) {
			return defValue;
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override public Set<String> getStringSet(final String key, final Set<String> defValues) {
		try {
			return Collections.unmodifiableSet(mDelegate.getStringSet(key, defValues));		// To enforce the immutability
		} catch (final ClassCastException e) {
			return defValues;
		}
	}

	@Override public Map<String, ?> getAll() { return mDelegate.getAll(); }
	@Override public boolean contains(final String key) { return mDelegate.contains(key); }
	@Override public Editor edit() { return mDelegate.edit(); }

	private final SharedPreferences mDelegate;
}
