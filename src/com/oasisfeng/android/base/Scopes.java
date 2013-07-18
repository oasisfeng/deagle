package com.oasisfeng.android.base;

import static android.content.Context.MODE_PRIVATE;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;

import com.oasisfeng.android.base.Scopes.Scope;
import com.oasisfeng.android.base.Scopes.ScopeRunnable;

/** @author Oasis */
public class Scopes {

    public interface Scope {

        public void ifNotYet(String tag, ScopeRunnable runnable);
        public boolean firstTime(String tag);
    }

    public interface ScopeRunnable {
        boolean run();
    }

    public static Scope app(final Context context) { return new AppScope(context); }
    public static Scope version(final Context context) { return new VersionScope(context); }
    public static Scope process() { return ProcessScope.mSingleton; }

    private Scopes() {}
}

class ProcessScope implements Scope {

    @Override public void ifNotYet(final String tag, final ScopeRunnable runnable) {
        if (mSeen.contains(tag))
            if (runnable.run())
                mSeen.add(tag);
    }

    @Override public boolean firstTime(final String tag) {
        return mSeen.add(tag);
    }

    private final Set<String> mSeen = new HashSet<String>();
    static final Scope mSingleton = new ProcessScope();
}

class VersionScope extends SharedPrefsBasedScopeImpl {

    private static final String KPrefsKeyVersionCode = "version-code";

    VersionScope(final Context context) {
        super(resetIfVersionChanges(context, context.getSharedPreferences("version.scope", MODE_PRIVATE)));
    }

    private static SharedPreferences resetIfVersionChanges(final Context context, final SharedPreferences prefs) {
        final int version = Versions.code(context);
        if (version != prefs.getInt(KPrefsKeyVersionCode, 0))
            prefs.edit().clear().putInt(KPrefsKeyVersionCode, version).commit();
        return prefs;
    }
}

class AppScope extends SharedPrefsBasedScopeImpl {

    AppScope(final Context context) { super(context.getSharedPreferences("app.scope", MODE_PRIVATE)); }
}

class SharedPrefsBasedScopeImpl implements Scope {

    private static final String KPrefsKeyNotYet = "not-yet-";
    private static final String KPrefsKeyFirstTime = "first-time-";

    @Override public void ifNotYet(final String tag, final ScopeRunnable runnable) {
        final String key = KPrefsKeyNotYet + tag;
        if (! mPrefs.getBoolean(key, true)) return;
        if (runnable.run())
            mPrefs.edit().putBoolean(key, false).commit();
    }

    @Override public boolean firstTime(final String tag) {
        final String key = KPrefsKeyFirstTime + tag;
        if (! mPrefs.getBoolean(key, true)) return false;
        mPrefs.edit().putBoolean(key, false).commit();
        return true;
    }

    protected SharedPrefsBasedScopeImpl(final SharedPreferences prefs) {
        mPrefs = prefs;
    }

    private final SharedPreferences mPrefs;
}