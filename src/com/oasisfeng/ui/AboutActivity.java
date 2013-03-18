package com.oasisfeng.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

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

    @Override protected void onCreate(final Bundle savedInstanceState) {
        final Intent intent = getIntent();
        intent.putExtra(EXTRA_SHOW_FRAGMENT, AboutFragment.class.getName()).putExtra(EXTRA_NO_HEADERS, true);
        setIntent(intent);      // We just prepare the intent, onCreate() of PreferenceActivity takes care of the rest.
        super.onCreate(savedInstanceState);

    }

    public static class AboutFragment extends PreferenceFragment {

        @Override public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromIntent(getActivity().getIntent());
        }
    }
}
