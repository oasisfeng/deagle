package com.oasisfeng.android.base;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

/** @author Oasis */
public class Versions {

    public static int code(final Context context) {
        if (sVersionCode == 0) loadVersionInfo(context);
        return sVersionCode;
    }

    public static String name(final Context context) {
        if (sVersionName == null) loadVersionInfo(context);
        return sVersionName;
    }

    public static long lastUpdateTime(final Context context) {
        if (sLastUpdateTime == 0) loadVersionInfo(context);
        return sLastUpdateTime;
    }

    private static void loadVersionInfo(final Context context) {
        try {
            final PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            sVersionCode = info.versionCode;
            sVersionName = info.versionName;
            sLastUpdateTime = info.lastUpdateTime;
        } catch (final NameNotFoundException e) { /* Should never happen */ }
    }

    /** @param v version code as of {@link android.os.Build.VERSION#SDK_INT} */
    public static String getAndroidVersionNumber(final int v) {
        switch (v) {
        case 32: return "12L";
        case 31: return "12";
        case 30: return "11";
        case 29: return "10";
        case 28: return "9";
        case 27: return "8.1";
        case 26: return "8.0";
        case 25: return "7.1";
        case 24: return "7.0";
        case 23: return "6";
        case 22: return "5.1";
        case 21: return "5.0";
        case 20:
        case 19: return "4.4";
        case 18: return "4.3";
        case 17: return "4.2";
        case 16: return "4.1";
        case 15:
        case 14: return "4.0";
        case 13: return "3.2";
        case 12: return "3.1";
        case 11: return "3.0";
        default:
            if (v > 32) return String.valueOf(v - 20);     // Android 13+
            if (v > 4) return "2.x";
            return "1.x";
        }
    }

    private static int sVersionCode;
    private static String sVersionName;
    private static long sLastUpdateTime;
}
