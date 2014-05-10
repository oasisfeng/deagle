package com.oasisfeng.android.util;

import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * <ul>
 *   <li>Guard against ClassCastException in getters of SharedPreferences, return default value if type mismatched.</li>
 *   <li>Prevent anonymous class from being used in {@link #registerOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener)
 *   registerOnSharedPreferenceChangeListener()}</li>
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
		if (listener.getClass().isAnonymousClass())
			throw new Error("Never use anonymous class for listener, since it is weakly-referenced in Android SDK.");
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
		} catch (final ClassCastException e) {
			return defValue;
		}
		return mDelegate.getInt(key, defValue);
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
			return mDelegate.getStringSet(key, defValues);
		} catch (final ClassCastException e) {
			return defValues;
		}
	}

	@Override public Map<String, ?> getAll() { return mDelegate.getAll(); }
	@Override public boolean contains(final String key) { return mDelegate.contains(key); }
	@Override public Editor edit() { return mDelegate.edit(); }

	private final SharedPreferences mDelegate;
}
