package com.oasisfeng.android.base;

import android.app.Application;
import android.util.Log;

/** @author Oasis */
public class Applications {

    private static final String TAG = "Applications";

    public static Application getCurrent() {
        return CURRENT;
    }

    static final Application CURRENT;
    static {
        Application app = null;
        try {
            app = (Application) Class.forName("android.app.AppGlobals").getMethod("getInitialApplication").invoke(null);
            if (app == null) throw new IllegalStateException("Static initialization of Applications must be on main thread.");
        } catch (final Exception e) {
            // Alternative path
            Log.e(TAG, "Failed to get current application from AppGlobals.", e);
            try {
                app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
            } catch (final Exception ex) {
                Log.e(TAG, "Failed to get current application from ActivityThread.", e);
            }
        } finally { CURRENT = app; }
    }
}
