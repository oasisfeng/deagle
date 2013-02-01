package com.oasisfeng.base;

import android.app.Application;
import android.util.Log;

/** @author Oasis */
class Applications {

    static final Application CURRENT;
    static {
        Application app = null;
        try { app = (Application) Class.forName("android.app.AppGlobals").getMethod("getInitialApplication").invoke(null); }
        catch (final Exception e) { Log.wtf("Applications", "Failed to get current application.", e); }
        finally { CURRENT = app; }
    }
}
