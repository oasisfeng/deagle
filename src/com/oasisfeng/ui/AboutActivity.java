package com.oasisfeng.ui;

import java.util.Locale;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.oasisfeng.i18n.Locales;

/**
 * A helper class to simply build an "About" dialog.
 *
 * <p>Declare the activity in AndroidManifest.xml:
 * <pre>
       &lt;activity android:name="com.oasisfeng.ui.About"
           android:theme="@android:style/Theme.Holo.DialogWhenLarge.NoActionBar"&gt;
           &lt;meta-data android:name="android.preference" android:resource="@xml/about"/&gt;
       &lt;/activity&gt;
   </pre>
   Show the dialog in code: <pre>AboutActivity.show(context);</pre>

 * @author Oasis
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)       // TODO: Support 2.x
public class AboutActivity extends PreferenceActivity {

    public static void show(final Context context) {
        context.startActivity(new Intent(context, AboutActivity.class));
    }

    /** Show with custom AboutFragment */
    public static void show(final Context context, final AboutFragment fragment) {
        context.startActivity(new Intent(context, AboutActivity.class).putExtra(EXTRA_SHOW_FRAGMENT, fragment.getClass().getName()));
    }

    @Override protected void onCreate(final Bundle savedInstanceState) {
        final Intent intent = getIntent().putExtra(EXTRA_NO_HEADERS, true);
        if (! intent.hasExtra(EXTRA_SHOW_FRAGMENT)) intent.putExtra(EXTRA_SHOW_FRAGMENT, AboutFragment.class.getName());
        setIntent(intent);      // We just prepare the intent, onCreate() of PreferenceActivity takes care of the rest.
        super.onCreate(savedInstanceState);

    }

    public static class AboutFragment extends PreferenceFragment {

        @Override public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromIntent(getActivity().getIntent());
            final Preference version_pref = getPreferenceScreen().findPreference("version");
            if (version_pref != null) try {
                final PackageInfo package_info = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                version_pref.setSummary(package_info.versionName);
            } catch (final NameNotFoundException e) {}
        }

        @Override public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen, final Preference preference) {
            if ("translation".equals(preference.getKey())) {
                final Locale default_locale = Locale.getDefault();
                final Locale locale = Locales.getFrom(getActivity());
                Locales.switchTo(getActivity(), locale == null || default_locale.equals(locale) ? new Locale("en") : default_locale);
                getActivity().recreate();
                return true;
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }
}
