package com.oasisfeng.android.content;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;

/**
 * Created by Oasis on 2018-6-12.
 */
public class Contexts {

	/**
	 * startForegroundService() was introduced in O, just call startService() before O.
	 *
	 * @param context Context to start Service from.
	 * @param intent The description of the Service to start.
	 *
	 * @see Context#startForegroundService(Intent)
	 */
	public static ComponentName startForegroundService(final Context context, final Intent intent) {
		return SDK_INT >= O ? context.startForegroundService(intent) : context.startService(intent);
	}
}
