package com.oasisfeng.android.content.pm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import com.oasisfeng.android.os.UserHandles;

import static android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS;
import static android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;

/**
 * Created by Oasis on 2019-2-12.
 */
public class PackageManagerCompat {

	@SuppressLint("NewApi") public int getPackageUid(final String pkg) throws PackageManager.NameNotFoundException {
		if (SDK_INT >= N) return mPackageManager.getPackageUid(pkg, MATCH_UNINSTALLED_PACKAGES | MATCH_DISABLED_COMPONENTS);
		// API 18 - 23: public int getPackageUid(String packageName, int userHandle)
		return mPackageManager.getPackageUid(pkg, UserHandles.getIdentifier(Process.myUserHandle()));
	}

	public PackageManagerCompat(final Context context) {
		mPackageManager = context.getPackageManager();
	}

	private final PackageManager mPackageManager;
}
