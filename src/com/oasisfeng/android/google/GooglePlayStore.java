package com.oasisfeng.android.google;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.preference.Preference;

/** @author Oasis */
public class GooglePlayStore {

    private static final String PLAY_PACKAGE_NAME = "com.android.vending";
    private static final String PLAY_URL_PREFIX = "https://play.google.com/store/apps/details?id=";

    public static boolean isAvailable(final Context context) {
        try {
            final ApplicationInfo play_app = context.getPackageManager().getApplicationInfo(PLAY_PACKAGE_NAME, 0);
            return play_app.enabled;
        } catch (final NameNotFoundException e) {
            return false;
        }

    }

    public static void showApp(final Context context, final String pkg) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_URL_PREFIX + pkg));
        updatePlayUrlIntent(context, intent);
        try { context.startActivity(intent);
        } catch(final ActivityNotFoundException e) { /* In case of Google Play malfunction */ }
    }

    public static boolean isInstalledByGooglePlay(final Context context) {
        final String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
        return PLAY_PACKAGE_NAME.equals(installer);
    }

    public static void updatePreferenceIntent(final Context context, final Preference preference) {
        final Intent intent = preference.getIntent();
        updatePlayUrlIntent(context, intent);
    }

    /** Modify intent to launch Google Play Store directly if possible (without opener-app selector) */
    private static void updatePlayUrlIntent(final Context context, final Intent intent) {
        if (intent == null || intent.getPackage() != null) return;      // Skip intent with explicit target package
        final Uri uri = intent.getData();
        if (uri == null) return;
        intent.setPackage(PLAY_PACKAGE_NAME);
        if (intent.resolveActivity(context.getPackageManager()) != null) return;
        intent.setPackage(null);
    }
}
