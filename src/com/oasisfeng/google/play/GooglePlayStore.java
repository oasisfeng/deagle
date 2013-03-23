package com.oasisfeng.google.play;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.preference.Preference;

/** @author Oasis */
public class GooglePlayStore {

    private static final String PLAY_SCHEME = "market";
    private static final String PLAY_PACKAGE_NAME = "com.android.vending";
    private static final String PLAY_URL_PREFIX = "https://play.google.com/store/apps/details?id=";
    private static final String PLAY_HOST = "play.google.com";

    public static void showApp(final Context context, final String pkg) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_URL_PREFIX + pkg));
        updatePlayUrlIntent(context.getPackageManager(), intent);
        try { context.startActivity(intent);
        } catch(final ActivityNotFoundException e) { /* In case of Google Play malfunction */ }
    }

    public static boolean isInstalledByGooglePlay(final Context context) {
        final String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
        return PLAY_PACKAGE_NAME.equals(installer);
    }

    public static void updatePreferenceIntent(final Context context, final Preference preference) {
        final Intent intent = preference.getIntent();
        if (intent == null) return;
        final Uri uri = intent.getData();
        if (uri == null || PLAY_SCHEME.equals(uri.getScheme()) || ! PLAY_HOST.equals(uri.getHost())) return;
        updatePlayUrlIntent(context.getPackageManager(), intent);
    }

    private static void updatePlayUrlIntent(final PackageManager pm, final Intent intent) {
        try {
            final PackageInfo play_pkg = pm.getPackageInfo(PLAY_PACKAGE_NAME, 0);
            if (play_pkg.applicationInfo.enabled) intent.setPackage(PLAY_PACKAGE_NAME);
        } catch (final NameNotFoundException e) { /* Fall-through */ }
    }
}
