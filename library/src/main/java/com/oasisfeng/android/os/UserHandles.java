package com.oasisfeng.android.os;

import android.os.Parcel;
import android.os.Process;
import android.os.UserHandle;
import android.util.Pair;

import com.oasisfeng.android.annotation.AppIdInt;
import com.oasisfeng.android.annotation.UserIdInt;

import androidx.annotation.VisibleForTesting;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;

/**
 * Created by Oasis on 2018-9-6.
 */
public class UserHandles {

	private static final UserHandle MY_USER_HANDLE = Process.myUserHandle();
	@VisibleForTesting static Pair<Integer, UserHandle> sCache = null;	// Must before SYSTEM

	/**
	 * Enable multi-user related side effects. Set this to false if
	 * there are problems with single user use-cases.
	 */
	private static final boolean MU_ENABLED = true;

	/**
	 * Range of uids allocated for a user.
	 */
	private static final int PER_USER_RANGE = 100000;

	/** A user id constant to indicate the "system" user of the device */
	public static final @UserIdInt int USER_SYSTEM = 0;

	/** A user handle to indicate the "system" user of the device */
	public static final UserHandle SYSTEM = from(USER_SYSTEM);

	public static UserHandle getUserHandleForUid(final int uid) {
		return SDK_INT >= N ? UserHandle.getUserHandleForUid(uid) : of(getUserId(uid));
	}

	public static UserHandle of(final @UserIdInt int user_id) {
		if (user_id == USER_SYSTEM) return SYSTEM;
		final Pair<Integer, UserHandle> cache = sCache;
		if (cache != null && cache.first == user_id) return cache.second;
		final UserHandle user = from(user_id);
		sCache = new Pair<>(user_id, user);
		return user;
	}

	private static UserHandle from(final @UserIdInt int user_id) {
		if (MY_USER_HANDLE.hashCode() == user_id) return MY_USER_HANDLE;
		final Parcel parcel = Parcel.obtain();
		try {
			final int begin = parcel.dataPosition();
			parcel.writeInt(user_id);
			parcel.setDataPosition(begin);
			return UserHandle.CREATOR.createFromParcel(parcel);
		} finally {
			parcel.recycle();
		}
	}

	/**
	 * Returns the user id for a given uid.
	 */
	public static @UserIdInt int getUserId(final int uid) {
		if (MU_ENABLED) {
			return uid / PER_USER_RANGE;
		} else {
			return USER_SYSTEM;
		}
	}

	/**
	 * Returns the app id (or base uid) for a given uid, stripping out the user id from it.
	 */
	public static @AppIdInt int getAppId(final int uid) {
		return uid % PER_USER_RANGE;
	}
}
