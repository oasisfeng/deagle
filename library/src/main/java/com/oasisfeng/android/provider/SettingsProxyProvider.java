package com.oasisfeng.android.provider;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.MessageQueue;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static android.Manifest.permission.WRITE_SECURE_SETTINGS;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by Oasis on 2018/2/21.
 */
public abstract class SettingsProxyProvider extends ContentProvider {

	protected abstract boolean shouldAllow();

	@Nullable @Override public Cursor query(@NonNull final Uri uri, @Nullable final String[] projection, @Nullable final String selection, @Nullable final String[] selectionArgs, @Nullable final String sortOrder) {
		return context().getContentResolver().query(translate(uri), projection, selection, selectionArgs, sortOrder);
	}

	@Nullable @Override public Uri insert(@NonNull final Uri uri, @Nullable final ContentValues values) {
		return context().getContentResolver().insert(translate(uri), values);
	}

	@Override public int delete(@NonNull final Uri uri, @Nullable final String selection, @Nullable final String[] selectionArgs) {
		return context().getContentResolver().delete(translate(uri), selection, selectionArgs);
	}

	@Override public int update(@NonNull final Uri uri, @Nullable final ContentValues values, @Nullable final String selection, @Nullable final String[] selectionArgs) {
		return context().getContentResolver().update(translate(uri), values, selection, selectionArgs);
	}

	@Nullable @Override public String getType(@NonNull final Uri uri) {
		return context().getContentResolver().getType(translate(uri));
	}

	private Uri translate(final @NonNull Uri uri) {
		if (! shouldAllow()) throw new SecurityException("Not allowed");
		return uri.buildUpon().authority(Settings.AUTHORITY).build();
	}

	@SuppressLint("NewApi")/* getQueue() is hidden but accessible before M */
	@Override public boolean onCreate() {
		new Handler().getLooper().getQueue().addIdleHandler(new MessageQueue.IdleHandler() { @Override public boolean queueIdle() {
			final int result = SettingsProxyProvider.this.context().checkPermission(WRITE_SECURE_SETTINGS, Process.myPid(), Process.myUid());
			context().getPackageManager().setComponentEnabledSetting(new ComponentName(context(), SettingsProxyProvider.this.getClass()),
					result == PERMISSION_GRANTED ? COMPONENT_ENABLED_STATE_ENABLED : COMPONENT_ENABLED_STATE_DEFAULT, DONT_KILL_APP);
			return false;
		}});
		return true;
	}

	@SuppressWarnings("ConstantConditions") protected @NonNull Context context() { return getContext(); }
}
