package com.oasisfeng.android.util;

import java.util.Arrays;
import java.util.Collection;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
        final PackageManager pm = mContext.getPackageManager();
        ApplicationInfo info;
        try {
        	info = pm.getApplicationInfo(pkg, 0);
        } catch (final NameNotFoundException e) {
        	return "<?>";
        }
        try {
        	return info.loadLabel(pm);
        } catch (final RuntimeException e) {	// Including Resources.NotFoundException
        	return info.packageName;
        }
    }

    public String getAppNames(final Collection<String> pkgs, final String separator) {
        final StringBuilder app_names = new StringBuilder();
        final PackageManager pm = mContext.getPackageManager();
        for (final String pkg : pkgs) {
        	app_names.append(separator);
            ApplicationInfo info;
            try {
            	info = pm.getApplicationInfo(pkg, 0);
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
