package com.oasisfeng.android.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import java.util.Arrays;
import java.util.Collection;

/** @author Oasis */
public class Apps {

    public static Apps of(final Context context) {
        return new Apps(context);
    }

    public boolean isInstalled(final String pkg) {
        try {   // Despite stated in JavaDocs, this API actually never throws exceptions and returns empty array for non-existent package.
            final int[] gids = mContext.getPackageManager().getPackageGids(pkg);
            return gids == null || gids.length > 0;     // Null is the normal case for installed app
        } catch (final NameNotFoundException e) {       // Still catch the checked exception in case the implementation of this API
            return false;                               //   get corrected to properly reflect the JavaDocs.
        }
    }

    public boolean isEnabled(final String pkg) throws NameNotFoundException {
        final ApplicationInfo app_info = mContext.getPackageManager().getApplicationInfo(pkg, 0);
        return app_info.enabled;
    }

    public boolean isAvailable(final String pkg) {
        try {
            return isEnabled(pkg);
        } catch (final NameNotFoundException e) {
            return false;
        }
    }

    public boolean isInstalledBy(final String... installer_pkgs) {
        try {
            return Arrays.asList(installer_pkgs).contains(mContext.getPackageManager().getInstallerPackageName(mContext.getPackageName()));
        } catch(final IllegalArgumentException e) {
            return false;       // Should never happen
        }
    }

    public CharSequence getAppName(final String pkg) {
        try {
        	final ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(pkg, 0);
            return getAppName(info);
        } catch (final NameNotFoundException e) {
        	return "<?>";
        }
    }

    public CharSequence getAppName(final ApplicationInfo app_info) {
        try {
        	return app_info.loadLabel(mContext.getPackageManager());
        } catch (final RuntimeException e) {	// Including Resources.NotFoundException
        	return app_info.packageName;
        }
    }

    public String getAppNames(final Collection<String> pkgs, final String separator) {
        final StringBuilder app_names = new StringBuilder();
        final PackageManager pm = mContext.getPackageManager();
        for (final String pkg : pkgs) {
        	app_names.append(separator);
            final ApplicationInfo info;
            try { //noinspection WrongConstant,deprecation
                info = pm.getApplicationInfo(pkg, PackageManager.GET_UNINSTALLED_PACKAGES);
            } catch (final NameNotFoundException e) {
            	app_names.append("<?>");
            	continue;
            }
            try {
            	app_names.append(info.loadLabel(pm));
            } catch (final RuntimeException e) {	// Including Resources.NotFoundException
            	app_names.append(info.packageName);
            }
        }
        return app_names.substring(separator.length());
    }

    private Apps(final Context context) {
        mContext = context;
    }

    private final Context mContext;
}
