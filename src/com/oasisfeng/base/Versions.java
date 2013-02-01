package com.oasisfeng.base;

import static android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;

/** @author Oasis */
public class Versions {

    public static final boolean DEBUG;
    static {
        boolean debug = com.oasisfeng.deagle.BuildConfig.DEBUG;
        if (! debug) DEBUG = debug;
        else try {
            // To workaround the unreliable "BuildConfig.DEBUG".
            //   See http://code.google.com/p/android/issues/detail?id=27940
            final ApplicationInfo app_info = Applications.CURRENT.getApplicationInfo();
            debug = (app_info.flags & FLAG_DEBUGGABLE) != 0;
        } catch (final Exception e) {}      // Including NPE
        finally { DEBUG = debug; }
    }

    public static int code(final Context context) {
        if (sVersionCode == 0) try {
            sVersionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (final NameNotFoundException e) { /* Should never happen */ return 0; }
        return sVersionCode;
    }
    private static int sVersionCode;
}
