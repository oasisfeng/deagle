package com.oasisfeng.android.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;

/** @author Oasis */
public class Apps {

    public static boolean isEnabled(final Context context, final String pkg) throws NameNotFoundException {
        final ApplicationInfo app_info = context.getPackageManager().getApplicationInfo(pkg, 0);
        return app_info.enabled;
    }

    public static boolean isAvailable(final Context context, final String pkg) {
        try {
            final ApplicationInfo app_info = context.getPackageManager().getApplicationInfo(pkg, 0);
            return app_info.enabled;
        } catch (final NameNotFoundException e) {
            return false;
        }
    }
}
