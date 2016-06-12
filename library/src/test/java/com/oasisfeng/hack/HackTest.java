package com.oasisfeng.hack;

import com.oasisfeng.hack.Hack.Unchecked;

import junit.framework.TestCase;

import java.io.IOException;

/**
 * Test cases for {@link Hack}
 *
 * Created by Oasis on 2015/8/24.
 */
public class HackTest extends TestCase {

	public void testBasicMethod() {
		final Hack.HackedMethod0<Integer, Simple, Unchecked, Unchecked, Unchecked> foo
				= Hack.into(Simple.class).method("foo").returning(int.class).withoutParams();
		assertNotNull(foo);
		assertEquals(7, (int) foo.invoke().on(new Simple()));

		final Hack.HackedMethod3<Void, Void, Unchecked, Unchecked, Unchecked, Integer, String, Simple> bar
				= Hack.into(Simple.class).staticMethod("bar").withParams(int.class, String.class, Simple.class);
		assertNotNull(bar);
		bar.invoke();

		assertFail(NoSuchMethodException.class, Hack.into(Simple.class).method("notExist").withoutParams());
		assertFail(NoSuchMethodException.class, Hack.into(Simple.class).method("foo").withParam(int.class));
		assertFail(null, Hack.into(Simple.class).staticMethod("foo").withoutParams());
		assertFail(null, Hack.into(Simple.class).method("foo").returning(Void.class).withoutParams());
		assertFail(null, Hack.into(Simple.class).method("foo").throwing(RuntimeException.class).withoutParams());
		assertFail(null, Hack.into(Simple.class).method("foo").throwing(IOException.class, UnsupportedOperationException.class).withoutParams());
	}

	public void testMethodReturningAnyType() {
		final Hack.HackedMethod0<?, Simple, Unchecked, Unchecked, Unchecked> foo
				= Hack.into(Simple.class).method("foo").returning(Hack.ANY_TYPE).withoutParams();
		assertNotNull(foo);
		assertEquals(7, foo.invoke().on(new Simple()));
	}

	public void testMethodFallback() {
		final Hack.HackedMethod1<Integer, Simple, Unchecked, Unchecked, Unchecked, Void> foo
				= Hack.into(Simple.class).method("foo").returning(int.class).fallbackReturning(-1).withParam(Void.class);
		assertEquals(-1, (int) foo.invoke().on(new Simple()));
	}

	public void testBasicField() {
		final Simple simple = new Simple();
		final Hack.HackedField<Simple, Integer> field = Hack.into(Simple.class).field("mIntField").ofType(int.class);
		assertNotNull(field);
		field.set(simple, 3);
		assertEquals(3, (int) field.get(simple));

		assertFail(null, Hack.into(Simple.class).staticField("mIntField").ofType(Hack.ANY_TYPE));
	}

	public void testFieldFallback() {
		final Hack.HackedTargetField<Integer> field = Hack.into(Simple.class).staticField("mIntField").fallbackTo(-1);
		assertEquals(-1, (int) field.get());
		field.set(0);
		assertEquals(-1, (int) field.get());
	}

	public void testClassNotFound() {
		assertFail(ClassNotFoundException.class, Hack.into("NoSuchClass").method("nonSense").withoutParams());
		assertFail(ClassNotFoundException.class, Hack.into(Simple.class).field("mIntField").ofType("NoSuchType"));
		assertFail(ClassNotFoundException.class, Hack.into(Simple.class).staticField("mStaticField").ofType("NoSuchType"));
	}

	@Override protected void setUp() throws Exception {
		super.setUp();
		Hack.setAssertionFailureHandler(new Hack.AssertionFailureHandler() { @Override public void onAssertionFailure(final Hack.AssertionException failure) {
			mFailure = failure;
		}});
		mFailure = new Hack.AssertionException(new IOException());
		assertFail(IOException.class, null);
		mFailure = null;
	}

	private void assertFail(Class<? extends Throwable> failure, final Object hack) {
		assertNull(hack);
		assertNotNull(mFailure);
		if (failure != null) {
			assertNotNull(mFailure.getCause());
			assertEquals(failure, mFailure.getCause().getClass());
		}
		mFailure = null;
	}

	private Hack.AssertionException mFailure;

	@SuppressWarnings("unused") private static class Simple {

		int foo() { return 7; }
		private static void bar(int type, String name, Simple simple) throws IOException {}
		int mIntField;
	}
}
