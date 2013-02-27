package com.oasisfeng.pattern.update;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.util.Log;

/**
 * Device experience friendly package update observer, eliminated the legacy broadcast receiver if running on Android 3.1+
 *
 * <p>First, define two classes extending this abstract class, as following:
 * <pre>
   class PackageUpdateObserver extends AbstractPackageUpdateObserver {

       {@literal @}Override protected void onPackageUpdated(final Context context) {
           // Anything to be done after package update
       }
   }

   class PackageUpdateObserverLegacy extends PackageUpdateObserver {
   }</pre>

 * Then, declare two broadcast receivers in your AndroidManifest.xml
 * <pre>
   &lt;receiver android:name=".PackageUpdateObserverLegacy"&gt;
       &lt;intent-filter>
           &lt;action android:name="android.intent.action.PACKAGE_REPLACED" />
           &lt;data android:scheme="package" />
       &lt;/intent-filter>
   &lt;/receiver>

   &lt;receiver android:name=".PackageUpdateObserver"&gt;
       &lt;intent-filter&gt;
           &lt;action android:name="android.intent.action.MY_PACKAGE_REPLACED" /&gt;
       &lt;/intent-filter&gt;
   &lt;/receiver&gt;</pre>

 * @author Oasis
 */
public abstract class AbstractPackageUpdateObserver extends BroadcastReceiver {

    abstract protected void onPackageUpdated(final Context context);

    @Override public void onReceive(final Context context, final Intent intent) {
        if (Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
            // Disable legacy receiver on Android 3.1+ for better device experience.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                for (final ResolveInfo receiver : context.getPackageManager().queryBroadcastReceivers(intent, 0)) {
                    Class<?> clazz; try { clazz = Class.forName(receiver.activityInfo.name);
                    } catch (final ClassNotFoundException e) {
                        Log.e(TAG, "Cannot find receiver: " + receiver.activityInfo.name);
                        continue;
                    }
                    if (! AbstractPackageUpdateObserver.class.isAssignableFrom(clazz)) continue;  // Skip other receivers.
                    final ComponentName component = new ComponentName(context, clazz);
                    context.getPackageManager().setComponentEnabledSetting(component,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
                return;     // No need to proceed since Intent.ACTION_MY_PACKAGE_REPLACED will also be triggered.
            }
            // Ignore packages other than myself
            if (! context.getPackageName().equals(intent.getData().getSchemeSpecificPart())) return;
        } else if (! Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;

        Log.i(TAG, "Package updated");
        onPackageUpdated(context);
    }

    private static final String TAG = "Installer";
}
