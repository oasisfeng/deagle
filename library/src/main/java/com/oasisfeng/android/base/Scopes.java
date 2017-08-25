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

	/* These preferences file should be included in backup configuration of your app */
    public static final String KPrefsNameAppScope = "app.scope";
    public static final String KPrefsNameVersionScope = "version.scope";

    public interface Scope {

        boolean isMarked(@NonNull String tag);
        /** @return whether it is NOT YET marked before */
        boolean mark(@NonNull String tag);
		void markOnly(@NonNull String tag);
		/** @return whether it is marked before */
        boolean unmark(@NonNull String tag);
    }

	/** Throughout the whole lifecycle of this installed app, until uninstalled. (can also be extended to re-installation if configured with backup) */
    public static Scope app(final Context context) { return new AppInstallationScope(context); }
	/** Throughout the current version (code) of this installed app, until the version changes. */
    public static Scope version(final Context context) { return new PackageVersionScope(context); }
	/** Throughout the current update of this installed app, until being updated by in-place re-installation. */
	public static Scope update(final Context context) { return new PackageUpdateScope(context); }
	/** Throughout the current running process of this installed app, until being terminated. */
    public static Scope process() { return ProcessScope.mSingleton; }
	/** Throughout the time-limited session within current running process of this installed app, until session time-out. */
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

	@Override public void markOnly(@NonNull final String tag) {
		mSeen.add(tag);
	}

	@Override public boolean unmark(@NonNull final String tag) {
        return mSeen.remove(tag);
    }

    final Set<String> mSeen = new HashSet<>();
}

class PackageUpdateScope extends SharedPrefsBasedScopeImpl {

	private static final String KPrefsKeyLastUpdateTime = "update-time";

	PackageUpdateScope(final Context context) {
		super(resetIfLastUpdateTimeChanged(context, CrossProcessSharedPreferences.get(context, "update.scope")));
	}

	private static SharedPreferences resetIfLastUpdateTimeChanged(final Context context, final SharedPreferences prefs) {
		final long last_update_time = Versions.lastUpdateTime(context);
		if (last_update_time != prefs.getLong(KPrefsKeyLastUpdateTime, 0))
			prefs.edit().clear().putLong(KPrefsKeyLastUpdateTime, last_update_time).apply();
		return prefs;
	}
}

class PackageVersionScope extends SharedPrefsBasedScopeImpl {

	private static final String KPrefsKeyVersionCode = "version-code";

    PackageVersionScope(final Context context) {
        super(resetIfVersionChanges(context, CrossProcessSharedPreferences.get(context, Scopes.KPrefsNameVersionScope)));
    }

    private static SharedPreferences resetIfVersionChanges(final Context context, final SharedPreferences prefs) {
        final int version = Versions.code(context);
        if (version != prefs.getInt(KPrefsKeyVersionCode, 0))
            prefs.edit().clear().putInt(KPrefsKeyVersionCode, version).apply();
        return prefs;
    }
}

class AppInstallationScope extends SharedPrefsBasedScopeImpl {

	AppInstallationScope(final Context context) { super(CrossProcessSharedPreferences.get(context, Scopes.KPrefsNameAppScope)); }
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

	@Override public void markOnly(@NonNull final String tag) {
		mPrefs.edit().putBoolean(KPrefsKeyPrefix + tag, true).apply();
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