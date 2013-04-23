package com.oasisfeng.android.base;

import static android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
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
        if (sVersionCode == 0) loadVersionInfo(context);
        return sVersionCode;
    }

    public static String name(final Context context) {
        if (sVersionName == null) loadVersionInfo(context);
        return sVersionName;
    }

    @SuppressLint("DefaultLocale")
    public static boolean isVersionOf(final Context context, final String tag) {
        return name(context).toLowerCase().contains(tag);
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
