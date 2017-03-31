package com.oasisfeng.android.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;

import com.oasisfeng.android.google.GooglePlayStore;
import com.oasisfeng.android.i18n.Locales;

import java.util.Locale;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A helper class to simply build an "About" dialog.
 *
 * <p>Declare the activity in AndroidManifest.xml:
 * <pre>
       &lt;activity android:name="com.oasisfeng.ui.About"
           android:theme="@android:style/Theme.Holo.DialogWhenLarge.NoActionBar" /&gt;
   </pre>
   Show the dialog in code: <pre>AboutActivity.show(context, R.xml.about);</pre>

 * @author Oasis
 */
@ParametersAreNonnullByDefault
public class AboutActivity extends PreferenceActivity {

    private static final String EXTRA_XML_RESOURCE_ID = "xml";

    /** Override to provide your own fragment implementation */
    @SuppressWarnings("MethodMayBeStatic") protected Fragment createAboutFragment() { return new AboutFragment(); }

    public static void show(final Context context, final int xml_res) {
        show(context, xml_res, AboutActivity.class);
    }

    /** Show about activity of your own implementation */
    public static void show(final Context context, final int xml_res, final Class<? extends Activity> activity) {
        context.startActivity(new Intent(context, activity).putExtra(EXTRA_XML_RESOURCE_ID, xml_res));
    }

    @Override protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, createAboutFragment()).commit();
    }

    public static class AboutFragment extends PreferenceFragment {

        @Override public void onCreate(final @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(getActivity().getIntent().getIntExtra(EXTRA_XML_RESOURCE_ID, 0));
            final Preference version_pref = getPreferenceScreen().findPreference("version");
            if (version_pref != null) try {
                final PackageInfo package_info = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                version_pref.setTitle(version_pref.getTitle() + " " + package_info.versionName);
            } catch (final NameNotFoundException ignored) {}
        }

        @Override public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen, final Preference preference) {
            if ("translation".equals(preference.getKey())) {
                final Locale default_locale = Locale.getDefault();
                final Locale locale = Locales.getFrom(getActivity());
                if (Locales.switchTo(getActivity(), locale == null || default_locale.equals(locale) ? new Locale("en") : default_locale))
                	getActivity().recreate();
                return true;
            } else if (preference.getIntent() != null)
                GooglePlayStore.updatePreferenceIntent(getActivity(), preference);
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }
}
