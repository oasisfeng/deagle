package com.oasisfeng.android.base;

import static android.content.Context.MODE_PRIVATE;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.oasisfeng.android.base.Scopes.Scope;

/** @author Oasis */
public class Scopes {

    public static final String KPrefsNameAppScope = "app.scope";
    public static final String KPrefsNameVersionScope = "version.scope";

    public interface Scope {

        public boolean isMarked(String tag);
        public boolean mark(String tag);
        public boolean unmark(String tag);
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

	private static final int KSessionTimeout = 30 * 60 * 1000;		// TODO: Configurable

	public SessionScope(final Activity activity) {
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

	@Override public boolean isMarked(final String tag) {
        return mSeen.contains(tag);
    }

    @Override public boolean mark(final String tag) {
        return mSeen.add(tag);
    }

    @Override public boolean unmark(final String tag) {
        return mSeen.remove(tag);
    }

    protected final Set<String> mSeen = new HashSet<String>();
}

class VersionScope extends SharedPrefsBasedScopeImpl {

	private static final String KPrefsKeyVersionCode = "version-code";

    VersionScope(final Context context) {
        super(resetIfVersionChanges(context, context.getSharedPreferences(Scopes.KPrefsNameVersionScope, MODE_PRIVATE)));
    }

    private static SharedPreferences resetIfVersionChanges(final Context context, final SharedPreferences prefs) {
        final int version = Versions.code(context);
        if (version != prefs.getInt(KPrefsKeyVersionCode, 0))
            prefs.edit().clear().putInt(KPrefsKeyVersionCode, version).apply();
        return prefs;
    }
}

class AppScope extends SharedPrefsBasedScopeImpl {

	AppScope(final Context context) { super(context.getSharedPreferences(Scopes.KPrefsNameAppScope, MODE_PRIVATE)); }
}

class SharedPrefsBasedScopeImpl implements Scope {

    private static final String KPrefsKeyPrefix = "first-time-";        // Old name, for backward-compatibility

    @Override public boolean isMarked(final String tag) {
        final String key = KPrefsKeyPrefix + tag;
        return ! mPrefs.getBoolean(key, true);
    }

    @Override public boolean mark(final String tag) {
        final String key = KPrefsKeyPrefix + tag;
        if (! mPrefs.getBoolean(key, true)) return false;
        mPrefs.edit().putBoolean(key, false).apply();
        return true;
    }

    @Override public boolean unmark(final String tag) {
        final String key = KPrefsKeyPrefix + tag;
        if (mPrefs.getBoolean(key, true)) return false;
        mPrefs.edit().putBoolean(key, true).apply();
        return true;
    }

    protected SharedPrefsBasedScopeImpl(final SharedPreferences prefs) {
        mPrefs = prefs;
    }

    private final SharedPreferences mPrefs;
}