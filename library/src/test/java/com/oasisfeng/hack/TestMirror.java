package com.oasisfeng.hack;

import com.oasisfeng.hack.Hack.Fallback;

import java.util.List;

import androidx.annotation.Nullable;

/**
 * Created by Oasis on 2019-10-14.
 */
interface TestMirror extends Hack.Mirror<TestClass> {

	interface InnerClass extends Hack.Mirror {
		String getPackageName();
		int getUid();
		List<OpEntry> getOps();
	}

	interface OpEntry extends Hack.Mirror {
		int getOp();
		long getMode();
	}

	int basicMethod(final int a, final String b);

	void missingMethod();
	int missingMethod(final int a);
	String missingMethod(final String a);
	@Fallback(Fallback.TRUE) boolean missingMethodWithFallback(final boolean a);
	@Fallback(-1) int missingMethodWithFallback(final int a);
	@Fallback(-1) long missingMethodWithFallback(final long a);
	@Fallback(-1) double missingMethodWithFallback(final double a);

	List<InnerClass> complexMethod(int a, String b, @Nullable int[] c);
	void setMode(int code, int uid, String packageName, int mode);

	/** Retrieve the default mode for the operation. */
	static int opToDefaultMode(final int op) {
		return Hack.mirrorStaticMethod(TestMirror.class, "opToDefaultMode", -1, op);
	}
}
