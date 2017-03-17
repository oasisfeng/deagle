package com.oasisfeng.android.content.pm;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

/**
 * Permission-related helpers
 *
 * Created by Oasis on 2016/9/27.
 */
public class Permissions {

	public static boolean has(final Context context, final String permission) {
		return context.checkPermission(permission, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
	}
}
