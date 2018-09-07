package com.oasisfeng.android.os;

import android.os.UserHandle;
import android.util.Pair;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class UserHandlesTest {

	@Test public void testConstants() {
		assertEquals(0, UserHandles.SYSTEM.hashCode());
		assertEquals(0, UserHandles.USER_SYSTEM);
	}

	@Test public void testOf() {
		assertEquals(0, UserHandles.of(0).hashCode());
		assertEquals(1, UserHandles.of(1).hashCode());
		assertEquals(7, UserHandles.of(7).hashCode());
		assertEquals(-1, UserHandles.of(-1).hashCode());
	}

	@Test public void testGetUserHandleForUid() {
		assertEquals(0, UserHandles.getUserHandleForUid(17).hashCode());
		assertEquals(1, UserHandles.getUserHandleForUid(100029).hashCode());
		assertEquals(7, UserHandles.getUserHandleForUid(700031).hashCode());
	}

	@Test public void testCache() {
		UserHandles.sCache = null;
		UserHandles.of(0);
		assertNull(UserHandles.sCache);		// Cache should not be used for user 0.

		UserHandles.of(10);
		final Pair<Integer, UserHandle> cache = UserHandles.sCache;
		assertEquals(10, cache.first.intValue());		// Cache should not be used for user 0.
		final UserHandle user10 = cache.second;
		assertEquals(10, user10.hashCode());

		UserHandles.of(10);
		assertEquals(cache, UserHandles.sCache);				// Cache should not be updated if hit.

		UserHandles.of(11);
		assertNotEquals(cache, UserHandles.sCache);				// Cache should be updated if missed.
		assertEquals(11, UserHandles.sCache.first.intValue());
	}
}
