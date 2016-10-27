package com.oasisfeng.android.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.M;

/** @author Oasis */
public class Apps {

    public static Apps of(final Context context) {
        return new Apps(context);
    }

    /** Check whether specified app is installed on the device, even if not installed in current user (Android 4.2+). */
    public @CheckResult boolean isInstalledOnDevice(final String pkg) {
        try { //noinspection WrongConstant,deprecation
            mContext.getPackageManager().getApplicationInfo(pkg, PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (final NameNotFoundException e) {
            return false;
        }
    }

    /** Check whether specified app is installed in current user, even if hidden by system (Android 5+). */
    public @CheckResult boolean isInstalledInCurrentUser(final String pkg) {
        try { @SuppressWarnings("deprecation")
            final ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(pkg, PackageManager.GET_UNINSTALLED_PACKAGES);
            //noinspection SimplifiableIfStatement
            if (SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) return true;
            return (info.flags & ApplicationInfo.FLAG_INSTALLED) != 0;
        } catch (final NameNotFoundException e) {
            return false;
        }
    }

    /** Use {@link #isInstalledInCurrentUser(String)} or {@link #isInstalledOnDevice(String)} instead */
    @Deprecated public @CheckResult boolean isInstalled(final String pkg) {
        return isInstalledInCurrentUser(pkg);
    }

    public static boolean isPrivileged(final ApplicationInfo app) {
        if (SDK_INT >= M) {
            if (ApplicationInfo_privateFlags == NO_SUCH_FIELD) return isSystem(app);    // Fallback
            try {
                if (ApplicationInfo_privateFlags == null) {
                    ApplicationInfo_privateFlags = ApplicationInfo.class.getField("privateFlags");
                    if (ApplicationInfo_privateFlags.getType() != int.class) throw new NoSuchFieldException();
                }
                return ((int) ApplicationInfo_privateFlags.get(app) & PRIVATE_FLAG_PRIVILEGED) != 0;
            } catch (final NoSuchFieldException | IllegalAccessException | MultiCatchROECompat e) {
                ApplicationInfo_privateFlags = NO_SUCH_FIELD;
                Log.e(TAG, "Incompatible ROM: No public integer field - ApplicationInfo.privateFlags");
                return isSystem(app);
            }
        }
        return SDK_INT >= KITKAT && (app.flags & FLAG_PRIVILEGED) != 0;
    }
    private static final int FLAG_PRIVILEGED = 1<<30;
    private static final int PRIVATE_FLAG_PRIVILEGED = 1<<3;        // ApplicationInfo.PRIVATE_FLAG_PRIVILEGED

    private static boolean isSystem(final ApplicationInfo app) {
        return (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    private static Field ApplicationInfo_privateFlags;
    private static final Field NO_SUCH_FIELD;
    static {
        try {
            NO_SUCH_FIELD = Apps.class.getDeclaredField("NO_SUCH_FIELD");
        } catch (final NoSuchFieldException e) { throw new LinkageError(); }
    }

    public @CheckResult boolean isEnabled(final String pkg) throws NameNotFoundException {
        final ApplicationInfo app_info = mContext.getPackageManager().getApplicationInfo(pkg, 0);
        return app_info.enabled;
    }

    /** Check whether specified app is installed in current user, enabled and not hidden */
    public @CheckResult boolean isAvailable(final String pkg) {
        try {
            return isEnabled(pkg);
        } catch (final NameNotFoundException e) {
            return false;
        }
    }

    public @CheckResult boolean isInstalledBy(final String... installer_pkgs) {
        try {
            return Arrays.asList(installer_pkgs).contains(mContext.getPackageManager().getInstallerPackageName(mContext.getPackageName()));
        } catch(final IllegalArgumentException e) {
            return false;       // Should never happen
        }
    }

    public @CheckResult CharSequence getAppName(final String pkg) {
        try { //noinspection WrongConstant,deprecation
        	final ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(pkg, PackageManager.GET_UNINSTALLED_PACKAGES);
            return getAppName(info);
        } catch (final NameNotFoundException e) {
        	return "<?>";
        }
    }

    public @CheckResult CharSequence getAppName(final ApplicationInfo app_info) {
        try {
        	return app_info.loadLabel(mContext.getPackageManager());
        } catch (final RuntimeException e) {	// Including Resources.NotFoundException
        	return app_info.packageName;
        }
    }

    public @CheckResult String getAppNames(final Collection<String> pkgs, final String separator) {
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
    private static final String TAG = "Apps";
}
