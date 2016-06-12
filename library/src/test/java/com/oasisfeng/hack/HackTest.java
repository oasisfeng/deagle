package com.oasisfeng.hack;

import junit.framework.TestCase;

import java.io.IOException;

/**
 * Test cases for {@link Hack}
 *
 * Created by Oasis on 2015/8/24.
 */
public class HackTest extends TestCase {

	public void testBasicMethod() {
		final Hack.HackedMethod0<Integer, Simple, Hack.Unchecked, Hack.Unchecked, Hack.Unchecked> foo
				= Hack.into(Simple.class).method("foo").returning(int.class).withoutParams();
		assertNotNull(foo);
		assertEquals(7, (int) foo.invoke().on(new Simple()));

		final Hack.HackedMethod2<Void, Void, Hack.Unchecked, Hack.Unchecked, Hack.Unchecked, Integer, String> bar
				= Hack.into(Simple.class).staticMethod("bar").withParams(int.class, String.class);
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
		final Hack.HackedMethod0<?, Simple, Hack.Unchecked, Hack.Unchecked, Hack.Unchecked> foo
				= Hack.into(Simple.class).method("foo").returning(Hack.ANY_TYPE).withoutParams();
		assertNotNull(foo);
		assertEquals(7, foo.invoke().on(new Simple()));
	}

	public void testMethodFallback() {
		final Hack.HackedMethod1<Integer, Simple, Hack.Unchecked, Hack.Unchecked, Hack.Unchecked, Void> foo
				= Hack.into(Simple.class).method("foo").returning(int.class).fallbackReturning(-1).withParam(Void.class);
		assertEquals(-1, (int) foo.invoke().on(new Simple()));
	}

	public void testBasicField() {
		final Simple simple = new Simple();
		final Hack.HackedField<Simple, Integer> field = Hack.into(Simple.class).field("mIntField");
		field.set(simple, 3);
		assertEquals(3, (int) field.get(simple));

		assertFail(null, field.ofType(String.class));
		assertFail(null, Hack.into(Simple.class).staticField("mIntField"));
	}

	public void testFieldFallback() {
		final Hack.HackedTargetField<Integer> field = Hack.into(Simple.class).staticField("mIntField").ofType(int.class).fallbackTo(-1);
		assertEquals(-1, (int) field.get());
		field.set(0);
		assertEquals(-1, (int) field.get());
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
