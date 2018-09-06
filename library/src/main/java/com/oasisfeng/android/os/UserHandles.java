package com.oasisfeng.android.os;

import android.os.Parcel;
import android.os.UserHandle;
import android.util.Pair;

import com.oasisfeng.android.annotation.UserIdInt;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;

/**
 * Created by Oasis on 2018-9-6.
 */
public class UserHandles {

	private static Pair<Integer, UserHandle> sCache = null;	// Must before SYSTEM

	/**
	 * Enable multi-user related side effects. Set this to false if
	 * there are problems with single user use-cases.
	 */
	private static final boolean MU_ENABLED = true;

	/**
	 * Range of uids allocated for a user.
	 */
	public static final int PER_USER_RANGE = 100000;

	/** A user id constant to indicate the "system" user of the device */
	public static final @UserIdInt int USER_SYSTEM = 0;

	/** A user handle to indicate the "system" user of the device */
	public static final UserHandle SYSTEM = of(USER_SYSTEM);

	public static UserHandle getUserHandleForUid(final int uid) {
		return SDK_INT >= N ? UserHandle.getUserHandleForUid(uid) : of(getUserId(uid));
	}

	public static UserHandle of(final @UserIdInt int userId) {
		if (userId == USER_SYSTEM) return SYSTEM;
		final Pair<Integer, UserHandle> cache = sCache;
		if (cache != null && cache.first == userId) return cache.second;

		final Parcel parcel = Parcel.obtain();
		try {
			final int begin = parcel.dataPosition();
			parcel.writeInt(userId);
			parcel.setDataPosition(begin);
			final UserHandle user = UserHandle.CREATOR.createFromParcel(parcel);
			sCache = new Pair<>(userId, user);
			return user;
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
}
