package com.oasisfeng.pattern;

import android.annotation.SuppressLint;
import android.content.Context;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Special content provider to provide a global static {@link Context}.
 * This provider must be declared with "android:multiprocess" set to "true".
 *
 * Created by Oasis on 2017/3/24.
 */
@ParametersAreNonnullByDefault
public class GlobalContextProvider extends LocalContentProvider {

	public static Context get() {
		if (sContext == null) throw new IllegalStateException(GlobalContextProvider.class.getSimpleName() + " is not correctly configured");
		return sContext;
	}

	@Override public boolean onCreate() {
		if (sContext != null) return false;		// Prevent tampering
		sContext = context();
		return false;
	}

	@SuppressLint("StaticFieldLeak") private static Context sContext;
}
