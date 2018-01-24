package com.oasisfeng.android.content.pm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.support.annotation.RequiresApi;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;

/**
 * Backward-compatibility helper for {@link LauncherApps}.
 *
 * Created by Oasis on 2018/1/18.
 */
@RequiresApi(LOLLIPOP) public class LauncherAppsCompat {

	public LauncherAppsCompat(final Context context) {
		mLauncherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
	}

	@RequiresApi(N) @SuppressLint("NewApi")
	public ApplicationInfo getApplicationInfo(final String pkg, final int flags, final UserHandle user) throws PackageManager.NameNotFoundException {
		final ApplicationInfo info = mLauncherApps.getApplicationInfo(pkg, flags, user);
		if (SDK_INT < O && info == null)	// On Android 7.x, LauncherApps.getApplicationInfo() does not throw but return null for package not found.
			throw new PackageManager.NameNotFoundException("Package " + pkg + " not found for user " + user.hashCode());
		return info;
	}

	private final LauncherApps mLauncherApps;
}
