package com.oasisfeng.android.content.pm;

import static android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS;
import static android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;

import com.oasisfeng.android.os.UserHandles;

/**
 * Created by Oasis on 2019-2-12.
 */
public class PackageManagerCompat {

	@SuppressLint("NewApi") public int getPackageUid(final String pkg, final int user) throws PackageManager.NameNotFoundException {
		if (SDK_INT >= N) {
			final int uid = mPackageManager.getPackageUid(pkg, MATCH_UNINSTALLED_PACKAGES | MATCH_DISABLED_COMPONENTS);
			return user == UserHandles.MY_USER_ID ? uid : UserHandles.getUid(user, UserHandles.getAppId(uid));
		}
		// API 18 - 23: public int getPackageUid(String packageName, int userHandle)
		return mPackageManager.getPackageUid(pkg, user);
	}

	public int getPackageUid(final String pkg) throws PackageManager.NameNotFoundException {
		return getPackageUid(pkg, UserHandles.MY_USER_ID);
	}

	public PackageManagerCompat(final Context context) {
		mPackageManager = context.getPackageManager();
	}

	private final PackageManager mPackageManager;
}
