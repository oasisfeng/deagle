package com.oasisfeng.android.base;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.oasisfeng.android.base.Scopes.Scope;
import com.oasisfeng.android.content.CrossProcessSharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** @author Oasis */
public class Scopes {

    public static final String KPrefsNameAppScope = "app.scope";
    public static final String KPrefsNameVersionScope = "version.scope";

    public interface Scope {

        boolean isMarked(@NonNull String tag);
        boolean mark(@NonNull String tag);
        boolean unmark(@NonNull String tag);
    }

    public static Scope app(final Context context) { return new AppScope(context); }
    public static Scope version(final Context context) { return new VersionScope(context); }
    public static Scope process() { return ProcessScope.mSingleton; }
    public static Scope session(final Activity activity) {
    	if (SessionScope.mSingleton == null) SessionScope.mSingleton = new SessionScope(activity);
    	return SessionScope.mSingleton;
    }

    private Scopes() {}
}

class SessionScope extends MemoryBasedScopeImpl {

	private static final int KSessionTimeout = 5 * 60 * 1000;		// TODO: Configurable

	SessionScope(final Activity activity) {
		activity.getApplication().registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

			@Override public void onActivityResumed(final Activity a) {
				if (System.currentTimeMillis() >= mTimeLastSession + KSessionTimeout)
					mSeen.clear();
			}

			@Override public void onActivityPaused(final Activity a) {
				mTimeLastSession = System.currentTimeMillis();
			}

			@Override public void onActivityStopped(final Activity a) {}
			@Override public void onActivityStarted(final Activity a) {}
			@Override public void onActivitySaveInstanceState(final Activity a, final Bundle s) {}
			@Override public void onActivityCreated(final Activity a, final Bundle s) {}
			@Override public void onActivityDestroyed(final Activity a) {}
		});
	}

	private long mTimeLastSession = 0;

	static SessionScope mSingleton;
}

class ProcessScope extends MemoryBasedScopeImpl {

	static final Scope mSingleton = new ProcessScope();
}

class MemoryBasedScopeImpl implements Scope {

	@Override public boolean isMarked(@NonNull final String tag) {
        return mSeen.contains(tag);
    }

    @Override public boolean mark(@NonNull final String tag) {
        return mSeen.add(tag);
    }

    @Override public boolean unmark(@NonNull final String tag) {
        return mSeen.remove(tag);
    }

    final Set<String> mSeen = new HashSet<>();
}

class VersionScope extends SharedPrefsBasedScopeImpl {

	private static final String KPrefsKeyVersionCode = "version-code";

    VersionScope(final Context context) {
        super(resetIfVersionChanges(context, CrossProcessSharedPreferences.get(context, Scopes.KPrefsNameVersionScope)));
    }

    private static SharedPreferences resetIfVersionChanges(final Context context, final SharedPreferences prefs) {
        final int version = Versions.code(context);
        if (version != prefs.getInt(KPrefsKeyVersionCode, 0))
            prefs.edit().clear().putInt(KPrefsKeyVersionCode, version).apply();
        return prefs;
    }
}

class AppScope extends SharedPrefsBasedScopeImpl {

	AppScope(final Context context) { super(CrossProcessSharedPreferences.get(context, Scopes.KPrefsNameVersionScope)); }
}

class SharedPrefsBasedScopeImpl implements Scope {

	private static final String KPrefsKeyPrefix = "mark-";
	private static final String KPrefsKeyPrefixLegacy = "first-time-";	// Old name, for backward-compatibility

    @Override public boolean isMarked(@NonNull final String tag) {
        return mPrefs.getBoolean(KPrefsKeyPrefix + tag, false);
    }

    @Override public boolean mark(@NonNull final String tag) {
        final String key = KPrefsKeyPrefix + tag;
		if (mPrefs.getBoolean(key, false)) return false;
        mPrefs.edit().putBoolean(key, true).apply();
        return true;
    }

    @Override public boolean unmark(@NonNull final String tag) {
        final String key = KPrefsKeyPrefix + tag;
        if (! mPrefs.getBoolean(key, false)) return false;
        mPrefs.edit().putBoolean(key, false).apply();
        return true;
    }

    SharedPrefsBasedScopeImpl(final SharedPreferences prefs) {
        mPrefs = prefs;
		// Migrate legacy entries
		SharedPreferences.Editor editor = null;
		for (final String key : prefs.getAll().keySet()) {
			if (! key.startsWith(KPrefsKeyPrefixLegacy)) continue;
			if (editor == null) editor = prefs.edit();
			try {
				editor.putBoolean(KPrefsKeyPrefix + key.substring(KPrefsKeyPrefixLegacy.length()), ! prefs.getBoolean(key, true));
			} catch (final ClassCastException ignored) {}
			editor.remove(key);
		}
		if (editor != null) editor.apply();
	}

    private final SharedPreferences mPrefs;
}