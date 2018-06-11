package com.oasisfeng.android.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.util.Log;
import android.util.TypedValue;

import com.oasisfeng.deagle.R;

/**
 * Helper class for viewing web content with Chrome Custom Tabs support.
 *
 * Created by Oasis on 2015/10/4.
 */
public class WebContent {

	private static final String KChromePackageName = "com.android.chrome";

	/** Caller must unbind the returned ServiceConnection when leaving the scope. */
	public static @CheckResult @Nullable ServiceConnection preload(final Context context, final Uri uri, final @Nullable OnSessionReadyListener listener) {
		final CustomTabsServiceConnection connection;
		if (! CustomTabsClient.bindCustomTabsService(context, KChromePackageName, connection = new CustomTabsServiceConnection() {

			@Override public void onCustomTabsServiceConnected(final ComponentName componentName, final CustomTabsClient client) {
				Log.d(TAG, "Warming up Chrome custom tabs");
				if (client.warmup(0)) {
					final CustomTabsSession session = client.newSession(null);
					if (session != null) {
						session.mayLaunchUrl(uri, null, null);
						if (listener != null) listener.onSessionReady(session);
					}
				}
			}

			@Override public void onServiceDisconnected(final ComponentName name) {}
		})) return null;
		return connection;
	}

	public static boolean view(final Context context, final String url) {
		Uri uri = Uri.parse(url);
		if (uri.isRelative()) uri = Uri.parse("http://" + url);		// Append http if no scheme
		return view(context, uri);
	}

	public static boolean view(final Context context, final Uri uri) {
		return view(context, uri, null, null);
	}

	public static boolean view(final Context context, final Uri uri, final @Nullable CustomTabsSession session, final @Nullable Bundle activity_options) {
		if (! uri.isAbsolute() || ! uri.isHierarchical()) throw new IllegalArgumentException("Invalid URL: " + uri);
		final Intent intent;
		final Activity activity = findActivity(context);
		if (activity != null) {	// Chrome custom tabs are only supported in
			final TypedValue typed_value = new TypedValue();
			activity.getTheme().resolveAttribute(R.attr.colorPrimary, typed_value, true);
			intent = new CustomTabsIntent.Builder(session).setToolbarColor(typed_value.data).setShowTitle(true).build().intent;
		} else intent = new Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setData(uri);
		if (intent.getPackage() == null) intent.setSelector(sBrowserSelector);

		try {
			startActivity(context, intent, activity_options);
			return true;
		} catch (final ActivityNotFoundException ignored) {}

		if (intent.getSelector() != null) {
			intent.setSelector(null);		// Remove browser selector and try again
			try {
				startActivity(context, intent, activity_options);
				Log.d(TAG, "Browser is launched successfully without browser selector.");
				return true;
			} catch (final ActivityNotFoundException ignored) {}
		}

		if ("https".equalsIgnoreCase(uri.getScheme())) {	// A few browser apps lack the intent-filter for https.
			intent.setData(Uri.fromParts("http", uri.getSchemeSpecificPart(), uri.getFragment()));	// Fall-back to http
			try {
				startActivity(context, intent, activity_options);
				Log.d(TAG, "Browser is launched successfully after falling back to HTTP URL.");
				return true;
			} catch (final ActivityNotFoundException exc) {
				Log.w(TAG, "Failed to launch browser for URL: " + uri);
			}
		}
		return false;
	}

	private static void startActivity(final Context context, final Intent intent, final @Nullable Bundle activity_options) {
		context.startActivity(intent, activity_options);
	}

	private static Activity findActivity(Context context) {
		while (context instanceof ContextWrapper) {
			if (context instanceof Activity) return (Activity) context;
			context = ((ContextWrapper) context).getBaseContext();
		}
		return null;
	}

	private static final Intent sBrowserSelector = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER);
	private static final String TAG = "WebContent";

	public interface OnSessionReadyListener {

		/** This method may never be called if Chrome Custom Tabs are not supported on device, or session is failed to create. */
		void onSessionReady(CustomTabsSession session);
	}
}
