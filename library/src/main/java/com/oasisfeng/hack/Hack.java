package com.oasisfeng.hack;

import android.support.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** @author Oasis */
public class Hack {

	/** All hacks should be declared in a centralized point extending this class, typically as static
	 * method, and call it in your application initialization stage to verify all the hack
	 * assertions by catching exception thrown:
	 * <pre>
	 * class MyHacks extends HackDeclaration {
	 *
	 *     static HackedField<Object, PackageManager> ContextImpl_mPackageManager;
	 *     static HackedField<Object, Map<String, IBinder>> ServiceManager_sCache;
	 *
	 *     static void defineAndVerify() {
	 *         try {
	 *             ContextImpl_mPackageManager = Hack.into("android.app.ContextImpl").field("mPackageManager").ofType(PackageManager.class);
	 *             ServiceManager_sCache = Hack.into("android.os.ServiceManager").staticField("sCache").ofGenericType(Map.class)
	 *             
	 *             ...
	 *         } catch (HackAssertionException e) {
	 *             // Report the failure and activate fall-back strategy.
	 *             ...
	 *         }
	 *     }
	 * }
	 * <pre>
	 * Thus we can verify them all together in an early application initialization stage. If any assertion
	 * failed, a fall-back strategy is suggested. */
	public static abstract class HackDeclaration {

		/** This exception is purposely defined as "protected" and not extending Exception to avoid
		 * developers unconsciously catch it outside the centralized hacks declaration, which results
		 * in potentially pre-checked usage of hacks. */
		public static class HackAssertionException extends Throwable {

			private Class<?> clazz;
			private String mHackedFieldName;			
			private String mHackedMethodName;
	
			public HackAssertionException(final String e) { super(e); }
			public HackAssertionException(final Exception e) { super(e); }

			@Override public String toString() {
				return getCause() != null ? getClass().getName() + ": " + getCause() : super.toString();
			}			
			
			public Class<?> getHackedClass() {
				return clazz;
			}
			
			public HackAssertionException setHackedClass(final Class<?> hacked_class) {
				clazz = hacked_class; return this;
			}		
			
			public String getHackedMethodName() {
				return mHackedMethodName;
			}
			
			public HackAssertionException setHackedMethodName(final String method) {
				this.mHackedMethodName = method; return this;
			}
			
			public String getHackedFieldName() {
				return mHackedFieldName;
			}
			
			public HackAssertionException setHackedFieldName(final String field) {
				this.mHackedFieldName = field; return this;
			}

			private static final long serialVersionUID = 1L;
		}
	}

	/** Use {@link Hack#setAssertionFailureHandler(AssertionFailureHandler) } to set the global handler */
	public interface AssertionFailureHandler {
		
		/** @return whether the failure is handled and no need to throw out, return false to let it thrown */
		boolean onAssertionFailure(HackDeclaration.HackAssertionException failure);
	}

	public static class HackedField<C, T> {

		/** Assert the field type */
		@SuppressWarnings("unchecked")
		public <T2> HackedField<C, T2> ofType(final Class<T2> type) throws HackDeclaration.HackAssertionException {
			if (mField != null && ! type.isAssignableFrom(mField.getType()))
				fail(new HackDeclaration.HackAssertionException(new ClassCastException(mField + " is not of type " + type)));
			return (HackedField<C, T2>) this;
		}

		@SuppressWarnings("unchecked")
		public HackedField<C, T> ofType(final String type_name) throws HackDeclaration.HackAssertionException {
			try {
				return (HackedField<C, T>) ofType(Class.forName(type_name));
			} catch (final ClassNotFoundException e) {
				fail(new HackDeclaration.HackAssertionException(e)); return this;
			}
		}

		public HackedTargetField<T> on(final C target) {
			if (target == null) throw new IllegalArgumentException("target is null");
			return onTarget(target);
		}

		HackedTargetField<T> onTarget(final @Nullable C target) { return new HackedTargetField<>(mField, target); }

		/** Get current value of this field */
		public T get(final C instance) {
			try {
				@SuppressWarnings("unchecked") final T value = (T) mField.get(instance);
				return value;
			} catch (final IllegalAccessException e) {
				e.printStackTrace();
				return null; /* Should never happen */ }
		}

		/**
		 * Set value of this field
		 * 
		 * <p>No type enforced here since most type mismatch can be easily tested and exposed early.</p>
		 */
		public void set(final C instance,final Object value) {
			try {
				mField.set(instance, value);
			} catch (final IllegalAccessException ignored) {}	// Should never happen
		}

		/** @param modifiers the modifiers this field must have */
		HackedField(final Class<C> clazz, final String name, final int modifiers) throws HackDeclaration.HackAssertionException {
			Field field = null;
			try {
				if (clazz == null) return;
				field = clazz.getDeclaredField(name);
				if (Modifier.isStatic(modifiers) != Modifier.isStatic(field.getModifiers()))
					fail(new HackDeclaration.HackAssertionException(field + (Modifier.isStatic(modifiers) ? " is not static" : "is static")).setHackedFieldName(name));
				if (modifiers > 0 && (field.getModifiers() & modifiers) != modifiers)
					fail(new HackDeclaration.HackAssertionException(field + " does not match modifiers: " + modifiers).setHackedFieldName(name));
				if (! field.isAccessible()) field.setAccessible(true);
			} catch (final NoSuchFieldException e) {
				final HackDeclaration.HackAssertionException hae = new HackDeclaration.HackAssertionException(e);
				hae.setHackedClass(clazz);
				hae.setHackedFieldName(name);
				fail(hae);
			} finally { mField = field; }
		}
		private final Field mField;
		
		public Field getField() {
			return mField;
		}
	}

	public static class HackedTargetField<T> {

		public T get() {
			try {
				@SuppressWarnings("unchecked") final T value = (T) mField.get(mInstance);
				return value;
			} catch (final IllegalAccessException e) { return null; }	// Should never happen
		}

		public void set(final T value) {
			try {
				mField.set(mInstance, value);
			} catch (final IllegalAccessException ignored) {}			// Should never happen
		}

		HackedTargetField(final Field field, final @Nullable Object instance) {
			mField = field;
			mInstance = instance;
		}

		private final Field mField;
		private final Object mInstance;		// Instance type is already checked
	}

	public interface HackedMethod<C, R> {

		R invoke(C target, Object... args) throws IllegalArgumentException, InvocationTargetException;
		HackedTargetMethod on(Object target);
	}

	public interface HackedTargetMethod<R> {
		R invoke(Object... args) throws IllegalArgumentException, InvocationTargetException;
	}

	private static class HackedMethodImpl<C, R> implements HackedMethod<C, R> {

		public R invoke(final C target, final Object... args) throws IllegalArgumentException, InvocationTargetException {
			try {
				@SuppressWarnings("unchecked") final R obj = (R) mMethod.invoke(target, args);
				return obj;
			} catch (final IllegalAccessException e) { /* Should never happen */
				return null;
			}
		}

		@Override public HackedTargetMethod on(final Object target) {
			return new HackedTargetMethodImpl(mMethod, target);
		}

		HackedMethodImpl(final Class<C> clazz, final String name, final Class<?>[] arg_types, final int modifiers) throws HackDeclaration.HackAssertionException {
			Method method = null;
			try {
				if (clazz == null) return;
				method = clazz.getDeclaredMethod(name, arg_types);
				if (Modifier.isStatic(modifiers) != Modifier.isStatic(method.getModifiers()))
					fail(new HackDeclaration.HackAssertionException(method + (Modifier.isStatic(modifiers) ? " is not static" : "is static")).setHackedMethodName(name));
				if (modifiers > 0 && (method.getModifiers() & modifiers) != modifiers)
					fail(new HackDeclaration.HackAssertionException(method + " does not match modifiers: " + modifiers).setHackedMethodName(name));
				if (! method.isAccessible()) method.setAccessible(true);
			} catch (final NoSuchMethodException e) {
				final HackDeclaration.HackAssertionException hae = new HackDeclaration.HackAssertionException(e);
				hae.setHackedClass(clazz);
				hae.setHackedMethodName(name);
				fail(hae);
			} finally { mMethod = method; }
		}

		protected final Method mMethod;

		public Method getMethod() {
			return mMethod;
		}		
	}

	private static class HackedTargetMethodImpl<T> implements HackedTargetMethod<T> {

		private HackedTargetMethodImpl(final Method mMethod, final Object mTarget) { this.mMethod = mMethod; this.mTarget = mTarget; }

		@Override public T invoke(final Object... args) throws IllegalArgumentException, InvocationTargetException {
			return null;
		}

		protected final Method mMethod;
		protected final Object mTarget;
	}

	public static class HackedConstructor {

		protected Constructor<?> mConstructor;

		HackedConstructor(final Class<?> clazz, final Class<?>[] arg_types) throws HackDeclaration.HackAssertionException {
			try {
				if (clazz == null) return;
				mConstructor = clazz.getDeclaredConstructor(arg_types);
			} catch (final NoSuchMethodException e) {
				final HackDeclaration.HackAssertionException hae = new HackDeclaration.HackAssertionException(e);
				hae.setHackedClass(clazz);
				fail(hae);
			}
		}

		public Object getInstance(final Object... arg_types) throws IllegalArgumentException, InvocationTargetException {
			try {
				mConstructor.setAccessible(true);
				return mConstructor.newInstance(arg_types);
			} catch (final Exception e) {
				throw new InvocationTargetException(e);
			}
		}
	}
	
	public static class HackedClass<C> {

		public <T> HackedField<C, T> field(final String name) throws HackDeclaration.HackAssertionException {
			return new HackedField<C, T>(mClass, name, 0) {};
		}

		public <T> HackedTargetField<T> staticField(final String name) throws HackDeclaration.HackAssertionException {
			return new HackedField<C, T>(mClass, name, Modifier.STATIC).onTarget(null);
		}

		public <T> HackedMethod<C, T> method(final String name, final Class<?>... arg_types) throws HackDeclaration.HackAssertionException {
			return new HackedMethodImpl<>(mClass, name, arg_types, 0);
		}

		public <T> HackedMethod<C, T> staticMethod(final String name, final Class<?>... arg_types) throws HackDeclaration.HackAssertionException {
			return new HackedMethodImpl<>(mClass, name, arg_types, Modifier.STATIC);
		}

		public HackedConstructor constructor(final Class<?>... arg_types) throws HackDeclaration.HackAssertionException {
			return new HackedConstructor(mClass, arg_types);
		}
		
		public HackedClass(final Class<C> clazz) { mClass = clazz; }

		protected Class<C> mClass;
	}

	public static <T> HackedClass<T> into(final Class<T> clazz) {
		return new HackedClass<>(clazz);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> HackedClass<T> into(final String class_name) throws HackDeclaration.HackAssertionException {
		try {
			return new HackedClass(Class.forName(class_name));
		} catch (final ClassNotFoundException e) {
			fail(new HackDeclaration.HackAssertionException(e));
			return new HackedClass(null);	// TODO: Better solution to avoid null?
		}
	}
	
	private static void fail(final HackDeclaration.HackAssertionException e) throws HackDeclaration.HackAssertionException {
		if (sFailureHandler == null || ! sFailureHandler.onAssertionFailure(e)) throw e;
	}

	/** Specify a handler to deal with assertion failure, and decide whether the failure should be thrown. */
	public static void setAssertionFailureHandler(final AssertionFailureHandler handler) {
		sFailureHandler = handler;
	}

	private Hack() {}

	private static AssertionFailureHandler sFailureHandler;
}
