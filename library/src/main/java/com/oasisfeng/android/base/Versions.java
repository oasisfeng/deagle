package com.oasisfeng.android.base;

import static android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

/** @author Oasis */
public class Versions {

    public static final boolean DEBUG;
    static {
        boolean debug = false;
        try {
            // To workaround the unreliable "BuildConfig.DEBUG".
            //   See http://code.google.com/p/android/issues/detail?id=27940
            final ApplicationInfo app_info = Applications.CURRENT.getApplicationInfo();
            debug = (app_info.flags & FLAG_DEBUGGABLE) != 0;
        } catch (final Exception ignored) {}      // Including NPE
        DEBUG = debug;
    }

    public static int code(final Context context) {
        if (sVersionCode == 0) loadVersionInfo(context);
        return sVersionCode;
    }

    public static String name(final Context context) {
        if (sVersionName == null) loadVersionInfo(context);
        return sVersionName;
    }

    private static void loadVersionInfo(final Context context) {
        try {
            final PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            sVersionCode = info.versionCode;
            sVersionName = info.versionName;
        } catch (final NameNotFoundException e) { /* Should never happen */ }
    }

    private static int sVersionCode;
    private static String sVersionName;
}
