package com.oasisfeng.hack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Created by Oasis on 2019-10-14.
 */
class HackMirrorTest {

    @Test fun testBasicMirror() {
        val target = TestClass()
        val mirror = Hack.into(target).with(TestMirror::class.java)
        assertEquals(1, mirror.basicMethod(0, ""))
        assertNotNull(mirror.complexMethod(0, "", null))

        mirror.missingMethod()      // Assert no exception
        assertEquals(0, mirror.missingMethod(0))
        assertEquals(null, mirror.missingMethod(""))
        assertEquals(true, mirror.missingMethodWithFallback(false))
        assertEquals(-1, mirror.missingMethodWithFallback(0))
        assertEquals(-1L, mirror.missingMethodWithFallback(0L))
        assertEquals(-1.0, mirror.missingMethodWithFallback(Double.MAX_VALUE), 0.0)
    }
}