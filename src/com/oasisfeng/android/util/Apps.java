package com.oasisfeng.android.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;

/** @author Oasis */
public class Apps {

    public static Apps of(final Context context) {
        return new Apps(context);
    }

    public boolean isEnabled(final String pkg) throws NameNotFoundException {
        final ApplicationInfo app_info = mContext.getPackageManager().getApplicationInfo(pkg, 0);
        return app_info.enabled;
    }

    public boolean isAvailable(final String pkg) {
        try {
            final ApplicationInfo app_info = mContext.getPackageManager().getApplicationInfo(pkg, 0);
            return app_info.enabled;
        } catch (final NameNotFoundException e) {
            return false;
        }
    }

    public boolean isInstalledBy(final String installer_pkg) {
        try {
            return installer_pkg.equals(mContext.getPackageManager().getInstallerPackageName(mContext.getPackageName()));
        } catch(final IllegalArgumentException e) {
            return false;       // Should never happen
        }
    }

    private Apps(final Context context) {
        mContext = context;
    }

    private final Context mContext;
}
