package com.oasisfeng.variants;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static android.content.Intent.CATEGORY_DEFAULT;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * MIUI related functions
 *
 * Created by Oasis on 2015/9/29.
 */
public class Miui {

	public static boolean isMiuiRom() {
		if (sMiuiVerName == null) sMiuiVerName = getMiuiVerName();
		return ! sMiuiVerName.isEmpty();
	}

	private static String getMiuiVerName() {
		final Properties prop = new Properties();
		final File file = new File(Environment.getRootDirectory(), "build.prop");
		try {
			prop.load(new FileInputStream(file));
		} catch (final IOException e) {
			try {	// Try again once more
				prop.load(new FileInputStream(file));
			} catch (final IOException ex) {
				return "";
			}
		}
		final String ver_name = prop.getProperty("ro.miui.ui.version.name");
		return ver_name == null ? "" : ver_name;
	}
	private static String sMiuiVerName;

	public static boolean startRootSettings(final Context context) {
		try {
			context.startActivity(new Intent("miui.intent.action.ROOT_MANAGER").addCategory(CATEGORY_DEFAULT).addFlags(FLAG_ACTIVITY_NEW_TASK));
			return true;
		} catch (final ActivityNotFoundException e) {
			return false;
		}
	}

	public static boolean startAutoStartSettings(final Context context) {
		try {
			context.startActivity(new Intent("miui.intent.action.OP_AUTO_START").addCategory(CATEGORY_DEFAULT).addFlags(FLAG_ACTIVITY_NEW_TASK));
			return true;
		} catch (final ActivityNotFoundException e) {
			return false;
		}
	}

	public static Intent buildAppPermissionSettingsIntent(final String pkg) {
		return new Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", pkg);
	}
}
