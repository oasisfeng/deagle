package com.oasisfeng.hack;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Java reflection helper optimized for hacking non-public APIs.
 *
 * It's suggested to declare all hacks in a centralized point, typically as static fields in a class.
 * Then call it during application initialization, thus they are verified all together in an early stage.
 * If any assertion failed, a fall-back strategy is suggested.
 *
 * <p>https://gist.github.com/oasisfeng/75d3774ca5441372f049818de4d52605
 *
 * @see Demo
 *
 * @author Oasis
 */
@SuppressWarnings({"Convert2Lambda", "WeakerAccess", "unused"})
public class Hack {

	/** This exception is purposely defined as "protected" and not extending Exception to avoid
	 * developers unconsciously catch it outside the centralized hacks declaration, which results
	 * in potentially pre-checked usage of hacks. */
	public static class AssertionException extends Throwable {

		private Class<?> mClass;
		private Field mHackedField;
		private Method mHackedMethod;
		private String mHackedFieldName;
		private String mHackedMethodName;
		private Class<?>[] mParamTypes;

		AssertionException(final String e) { super(e); }
		AssertionException(final Exception e) { super(e); }

		@Override public String toString() {
			return getCause() != null ? getClass().getName() + ": " + getCause() : super.toString();
		}

		public String getDebugInfo() {
			final StringBuilder info = new StringBuilder(getCause() != null ? getCause().toString() : super.toString());
			final Throwable cause = getCause();
			if (cause instanceof NoSuchMethodException) {
				info.append(" Potential candidates:");
				final int initial_length = info.length();
				final String name = getHackedMethodName();
				if (name != null) {
					for (final Method method : getHackedClass().getDeclaredMethods())
						if (method.getName().equals(name))			// Exact name match
							info.append(' ').append(method);
					if (info.length() == initial_length)
						for (final Method method : getHackedClass().getDeclaredMethods())
							if (method.getName().startsWith(name))	// Name prefix match
								info.append(' ').append(method);
					if (info.length() == initial_length)
						for (final Method method : getHackedClass().getDeclaredMethods())
							if (! method.getName().startsWith("-"))	// Dump all but generated methods
								info.append(' ').append(method);
				} else for (final Constructor<?> constructor : getHackedClass().getDeclaredConstructors())
					info.append(' ').append(constructor);
			} else if (cause instanceof NoSuchFieldException) {
				info.append(" Potential candidates:");
				final int initial_length = info.length();
				final String name = getHackedFieldName();
				final Field[] fields = getHackedClass().getDeclaredFields();
				for (final Field field : fields)
					if (field.getName().equals(name))				// Exact name match
						info.append(' ').append(field);
				if (info.length() == initial_length) for (final Field field : fields)
					if (field.getName().startsWith(name))			// Name prefix match
						info.append(' ').append(field);
				if (info.length() == initial_length) for (final Field field : fields)
					if (! field.getName().startsWith("$"))			// Dump all but generated fields
						info.append(' ').append(field);
			}
			return info.toString();
		}

		public Class<?> getHackedClass() {
			return mClass;
		}

		AssertionException setHackedClass(final Class<?> hacked_class) {
			mClass = hacked_class; return this;
		}

		public Method getHackedMethod() {
			return mHackedMethod;
		}

		AssertionException setHackedMethod(final Method method) {
			mHackedMethod = method;
			return this;
		}

		public String getHackedMethodName() {
			return mHackedMethodName;
		}

		AssertionException setHackedMethodName(final String method) {
			mHackedMethodName = method;
			return this;
		}

		public Class<?>[] getParamTypes() { return mParamTypes; }

		AssertionException setParamTypes(final Class<?>[] param_types) {
			mParamTypes = param_types;
			return this;
		}

		public Field getHackedField() {
			return mHackedField;
		}

		AssertionException setHackedField(final Field field) {
			mHackedField = field;
			return this;
		}

		public String getHackedFieldName() {
			return mHackedFieldName;
		}

		AssertionException setHackedFieldName(final String field) {
			mHackedFieldName = field;
			return this;
		}

		private static final long serialVersionUID = 1L;
	}

	/** Placeholder for unchecked exception */
	public class Unchecked extends RuntimeException {}

	/** Use {@link Hack#setAssertionFailureHandler(AssertionFailureHandler) } to set the global handler */
	public interface AssertionFailureHandler {
		void onAssertionFailure(AssertionException failure);
	}

	public static class HackedField<C, T> {

		/** Assert the field type */
		public <T2> HackedField<C, T2> ofType(final Class<T2> type) {
			if (mField != null && ! type.isAssignableFrom(mField.getType()))
				fail(new AssertionException(new ClassCastException(mField + " is not of type " + type)).setHackedField(mField));
			@SuppressWarnings("unchecked") final HackedField<C, T2> casted = (HackedField<C, T2>) this;
			return casted;
		}

		public HackedField<C, T> ofType(final String type_name) {
			try {
				@SuppressWarnings("unchecked") final HackedField<C, T> casted = mField == null ? this
						: (HackedField<C, T>) ofType(Class.forName(type_name, false, mField.getDeclaringClass().getClassLoader()));
				return casted;
			} catch (final ClassNotFoundException e) {
				fail(new AssertionException(e)); return this;
			}
		}

		/** Fallback to the given value if this field is unavailable at runtime */
		public HackedField<C, T> fallbackTo(final T value) {
			mFallbackValue = value;
			return this;
		}

		@SuppressWarnings("unchecked") public @Nullable Class<T> getType() {
			return mField != null ? (Class<T>) mField.getType() : null;
		}

		public HackedTargetField<T> on(final C target) {
			if (target == null) throw new IllegalArgumentException("target is null");
			return onTarget(target);
		}

		private HackedTargetField<T> onTarget(final @Nullable C target) { return new HackedTargetField<>(mField, target); }

		/** Get current value of this field */
		public T get(final C instance) {
			try {
				if (mField == null) return mFallbackValue;
				@SuppressWarnings("unchecked") final T value = (T) mField.get(instance);
				return value;
			} catch (final IllegalAccessException e) { return null; }	// Should never happen
		}

		/**
		 * Set value of this field
		 * 
		 * <p>No type enforced here since most type mismatch can be easily tested and exposed early.</p>
		 */
		public void set(final C instance,final Object value) {
			try {
				if (mField != null) mField.set(instance, value);
			} catch (final IllegalAccessException ignored) {}	// Should never happen
		}

		/** @param modifiers the modifiers this field must have */
		HackedField(final Class<C> clazz, final String name, final int modifiers) {
			Field field = null;
			try {
				if (clazz == null) return;
				field = clazz.getDeclaredField(name);
				if (Modifier.isStatic(modifiers) != Modifier.isStatic(field.getModifiers()))
					fail(new AssertionException(field + (Modifier.isStatic(modifiers) ? " is not static" : "is static")).setHackedFieldName(name));
				if (modifiers > 0 && (field.getModifiers() & modifiers) != modifiers)
					fail(new AssertionException(field + " does not match modifiers: " + modifiers).setHackedFieldName(name));
				if (! field.isAccessible()) field.setAccessible(true);
			} catch (final NoSuchFieldException e) {
				final AssertionException hae = new AssertionException(e);
				hae.setHackedClass(clazz);
				hae.setHackedFieldName(name);
				fail(hae);
			} finally { mField = field; }
		}

		public @Nullable Field getField() { return mField; }

		private final @Nullable Field mField;
		private @Nullable T mFallbackValue;
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

		public <TT> HackedTargetField<TT> ofType(final Class<TT> type) {
			if (mField != null && ! type.isAssignableFrom(mField.getType()))
				fail(new AssertionException(new ClassCastException(mField + " is not of type " + type)).setHackedField(mField));
			@SuppressWarnings("unchecked") final HackedTargetField<TT> casted = (HackedTargetField<TT>) this;
			return casted;
		}

		public HackedTargetField<T> ofType(final String type_name) {
			try { @SuppressWarnings("unchecked")
				final HackedTargetField<T> casted = (HackedTargetField<T>) ofType(Class.forName(type_name, false, mField.getDeclaringClass().getClassLoader()));
				return casted;
			} catch (final ClassNotFoundException e) {
				fail(new AssertionException(e)); return this;
			}
		}

		HackedTargetField(final Field field, final @Nullable Object instance) {
			mField = field;
			mInstance = instance;
		}

		private final Field mField;
		private final Object mInstance;		// Instance type is already checked
	}

	public interface HackedInvokable<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> {
		@CheckResult <TT1 extends Throwable> HackedInvokable<R, C, TT1, T2, T3> throwing(Class<TT1> type);
		@CheckResult <TT1 extends Throwable, TT2 extends Throwable> HackedInvokable<R, C, TT1, TT2, T3> throwing(Class<TT1> type1, Class<TT2> type2);
		@CheckResult <TT1 extends Throwable, TT2 extends Throwable, TT3 extends Throwable> HackedInvokable<R, C, TT1, TT2, TT3> throwing(Class<TT1> type1, Class<TT2> type2, Class<TT3> type3);

		@Nullable HackedMethod0<R, C, T1, T2, T3> withoutParams();
		@Nullable <A1> HackedMethod1<R, C, T1, T2, T3, A1> withParam(Class<A1> type);
		@Nullable <A1, A2> HackedMethod2<R, C, T1, T2, T3, A1, A2> withParams(Class<A1> type1, Class<A2> type2);
		@Nullable <A1, A2, A3> HackedMethod3<R, C, T1, T2, T3, A1, A2, A3> withParams(Class<A1> type1, Class<A2> type2, Class<A3> type3);
		@Nullable <A1, A2, A3, A4> HackedMethod4<R, C, T1, T2, T3, A1, A2, A3, A4> withParams(Class<A1> type1, Class<A2> type2, Class<A3> type3, Class<A4> type4);
		@Nullable <A1, A2, A3, A4, A5> HackedMethod5<R, C, T1, T2, T3, A1, A2, A3, A4, A5> withParams(Class<A1> type1, final Class<A2> type2, final Class<A3> type3, final Class<A4> type4, final Class<A5> type5);
		@Nullable HackedMethodN<R, C, T1, T2, T3> withParams(Class<?>... types);
	}

	public interface HackedMethod<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> extends HackedInvokable<R, C, T1,T2,T3> {
		/** Fallback to the given value if this field is unavailable at runtime. (Optional) */
		@CheckResult NonNullHackedMethod<R, C, T1, T2, T3> fallbackReturning(R return_value);

		@CheckResult <TT1 extends Throwable> HackedMethod<R, C, TT1, T2, T3> throwing(Class<TT1> type);
		@CheckResult <TT1 extends Throwable, TT2 extends Throwable> HackedMethod<R, C, TT1, TT2, T3> throwing(Class<TT1> type1, Class<TT2> type2);
		@CheckResult <TT1 extends Throwable, TT2 extends Throwable, TT3 extends Throwable> HackedMethod<R, C, TT1, TT2, TT3> throwing(Class<TT1> type1, Class<TT2> type2, Class<TT3> type3);
		@CheckResult HackedMethod<R, C, Exception, T2, T3> throwing(Class<?>... types);
	}

	@SuppressWarnings("NullableProblems")	// Force to NonNull
	public interface NonNullHackedMethod<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> extends HackedMethod<R, C, T1,T2,T3> {
		/** Optional */
		@CheckResult <RR> HackedMethod<RR, C, T1, T2, T3> returning(Class<RR> type);

		@NonNull HackedMethod0<R, C, T1, T2, T3> withoutParams();
		@NonNull <A1> HackedMethod1<R, C, T1, T2, T3, A1> withParam(Class<A1> type);
		@NonNull <A1, A2> HackedMethod2<R, C, T1, T2, T3, A1, A2> withParams(Class<A1> type1, Class<A2> type2);
		@NonNull <A1, A2, A3> HackedMethod3<R, C, T1, T2, T3, A1, A2, A3> withParams(Class<A1> type1, Class<A2> type2, Class<A3> type3);
		@NonNull <A1, A2, A3, A4> HackedMethod4<R, C, T1, T2, T3, A1, A2, A3, A4> withParams(Class<A1> type1, Class<A2> type2, Class<A3> type3, Class<A4> type4);
		@NonNull <A1, A2, A3, A4, A5> HackedMethod5<R, C, T1, T2, T3, A1, A2, A3, A4, A5> withParams(Class<A1> type1, final Class<A2> type2, final Class<A3> type3, final Class<A4> type4, final Class<A5> type5);
		@NonNull HackedMethodN<R, C, T1, T2, T3> withParams(Class<?>... types);
	}

	public interface HackedMethod0<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> {
		@CheckResult HackInvocation<R, C, T1, T2, T3> invoke();
	}
	public interface HackedMethod1<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1> {
		@CheckResult HackInvocation<R, C, T1, T2, T3> invoke(A1 arg);
	}
	public interface HackedMethod2<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1, A2> {
		@CheckResult HackInvocation<R, C, T1, T2, T3> invoke(A1 arg1, A2 arg2);
	}
	public interface HackedMethod3<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1, A2, A3> {
		@CheckResult HackInvocation<R, C, T1, T2, T3> invoke(A1 arg1, A2 arg2, A3 arg3);
	}
	public interface HackedMethod4<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1, A2, A3, A4> {
		@CheckResult HackInvocation<R, C, T1, T2, T3> invoke(A1 arg1, A2 arg2, A3 arg3, A4 arg4);
	}
	public interface HackedMethod5<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1, A2, A3, A4, A5> {
		@CheckResult HackInvocation<R, C, T1, T2, T3> invoke(A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5);
	}
	public interface HackedMethodN<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> {
		@CheckResult HackInvocation<R, C, T1, T2, T3> invoke(Object... args);
	}

	public static class HackInvocation<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> {

		HackInvocation(final Invokable invokable, final Object... args) {
			this.invokable = invokable;
			this.args = args;
		}

		public R on(final @NonNull C target) throws T1, T2, T3 { return onTarget(target); }
		public R statically() throws T1, T2, T3 { return onTarget(null); }

		private R onTarget(final C target) throws T1 {
			try {
				@SuppressWarnings("unchecked") final R result = (R) invokable.invoke(target, args);
				return result;
			} catch (final IllegalAccessException e) { throw new RuntimeException(e);	// Should never happen
			} catch (final InstantiationException e) { throw new RuntimeException(e);
			} catch (final InvocationTargetException e) {
				final Throwable ex = e.getTargetException();
				//noinspection unchecked
				throw (T1) ex;
			}
		}

		private final Invokable invokable;
		private final Object[] args;
	}

	interface Invokable<C> {
		Object invoke(C target, Object[] args) throws InvocationTargetException, IllegalAccessException, InstantiationException;
	}

	private static class HackedMethodImpl<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> implements NonNullHackedMethod<R, C, T1, T2, T3> {

		HackedMethodImpl(final Class<?> clazz, @Nullable final String name, final int modifiers) {
			//noinspection unchecked, to be compatible with HackedClass.staticMethod()
			mClass = (Class<C>) clazz;
			mName = name;
			mModifiers = modifiers;
		}

		@Override public <RR> HackedMethod<RR, C, T1, T2, T3> returning(final Class<RR> type) {
			mReturnType = type;
			@SuppressWarnings("unchecked") final HackedMethod<RR, C, T1, T2, T3> casted = (HackedMethod<RR, C, T1, T2, T3>) this;
			return casted;
		}

		@Override public NonNullHackedMethod<R, C, T1, T2, T3> fallbackReturning(final R value) {
			mFallbackReturnValue = value; mHasFallback = true; return this;
		}

		@Override public <TT extends Throwable> HackedMethod<R, C, TT, T2, T3> throwing(final Class<TT> type) {
			mThrowTypes = new Class[] { type };
			@SuppressWarnings("unchecked") final HackedMethod<R, C, TT, T2, T3> casted = (HackedMethod<R, C, TT, T2, T3>) this;
			return casted;
		}

		@Override public <TT1 extends Throwable, TT2 extends Throwable> HackedMethod<R, C, TT1, TT2, T3>
				throwing(final Class<TT1> type1, final Class<TT2> type2) {
			mThrowTypes = new Class<?>[] { type1, type2 };
			Arrays.sort(mThrowTypes);
			@SuppressWarnings("unchecked") final HackedMethod<R, C, TT1, TT2, T3> cast = (HackedMethod<R, C, TT1, TT2, T3>) this;
			return cast;
		}

		@Override public <TT1 extends Throwable, TT2 extends Throwable, TT3 extends Throwable> HackedMethod<R, C, TT1, TT2, TT3>
				throwing(final Class<TT1> type1, final Class<TT2> type2, final Class<TT3> type3) {
			mThrowTypes = new Class<?>[] { type1, type2, type3 };
			Arrays.sort(mThrowTypes);
			@SuppressWarnings("unchecked") final HackedMethod<R, C, TT1, TT2, TT3> cast = (HackedMethod<R, C, TT1, TT2, TT3>) this;
			return cast;
		}

		@Override public HackedMethod<R, C, Exception, T2, T3> throwing(final Class<?>... types) {
			mThrowTypes = types;
			Arrays.sort(mThrowTypes);
			@SuppressWarnings("unchecked") final HackedMethod<R, C, Exception, T2, T3> cast = (HackedMethod<R, C, Exception, T2, T3>) this;
			return cast;
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public HackedMethod0<R, C, T1, T2, T3> withoutParams() {
			final Invokable method = findInvokable();
			return method == null ? null : new HackedMethod0<R, C, T1, T2, T3>() {
				@Override public HackInvocation<R, C, T1, T2, T3> invoke() {
					return new HackInvocation<>(method);
				}
			};
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public <A1> HackedMethod1<R, C, T1, T2, T3, A1> withParam(final Class<A1> type) {
			final Invokable method = findInvokable(type);
			return method == null ? null : new HackedMethod1<R, C, T1, T2, T3, A1>() {
				@Override public HackInvocation<R, C, T1, T2, T3> invoke(final A1 arg) {
					return new HackInvocation<>(method, arg);
				}
			};
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public <A1, A2> HackedMethod2<R, C, T1, T2, T3, A1, A2> withParams(final Class<A1> type1, final Class<A2> type2) {
			final Invokable method = findInvokable(type1, type2);
			return method == null ? null : new HackedMethod2<R, C, T1, T2, T3, A1, A2>() {
				@Override public HackInvocation<R, C, T1, T2, T3> invoke(final A1 arg1, final A2 arg2) {
					return new HackInvocation<>(method, arg1, arg2);
				}
			};
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public <A1, A2, A3> HackedMethod3<R, C, T1, T2, T3, A1, A2, A3> withParams(final Class<A1> type1, final Class<A2> type2, final Class<A3> type3) {
			final Invokable method = findInvokable(type1, type2, type3);
			return method == null ? null : new HackedMethod3<R, C, T1, T2, T3, A1, A2, A3>() {
				@Override public HackInvocation<R, C, T1, T2, T3> invoke(final A1 arg1, final A2 arg2, final A3 arg3) {
					return new HackInvocation<>(method, arg1, arg2, arg3);
				}
			};
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public <A1, A2, A3, A4> HackedMethod4<R, C, T1, T2, T3, A1, A2, A3, A4> withParams(final Class<A1> type1, final Class<A2> type2, final Class<A3> type3, final Class<A4> type4) {
			final Invokable method = findInvokable(type1, type2, type3, type4);
			return method == null ? null : new HackedMethod4<R, C, T1, T2, T3, A1, A2, A3, A4>() {
				@Override public HackInvocation<R, C, T1, T2, T3> invoke(final A1 arg1, final A2 arg2, final A3 arg3, final A4 arg4) {
					return new HackInvocation<>(method, arg1, arg2, arg3, arg4);
				}
			};
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public <A1, A2, A3, A4, A5> HackedMethod5<R, C, T1, T2, T3, A1, A2, A3, A4, A5> withParams(final Class<A1> type1, final Class<A2> type2, final Class<A3> type3, final Class<A4> type4, final Class<A5> type5) {
			final Invokable method = findInvokable(type1, type2, type3, type4, type5);
			return method == null ? null : new HackedMethod5<R, C, T1, T2, T3, A1, A2, A3, A4, A5>() {
				@Override public HackInvocation<R, C, T1, T2, T3> invoke(final A1 arg1, final A2 arg2, final A3 arg3, final A4 arg4, final A5 arg5) {
					return new HackInvocation<>(method, arg1, arg2, arg3, arg4, arg5);
				}
			};
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public HackedMethodN<R, C, T1, T2, T3> withParams(final Class<?>... types) {
			final Invokable method = findInvokable(types);
			return method == null ? null : new HackedMethodN<R, C, T1, T2, T3>() {
				@Override public HackInvocation<R, C, T1, T2, T3> invoke(final Object... args) {
					return new HackInvocation<>(method, args);
				}
			};
		}

		private @Nullable Invokable findInvokable(final Class<?>... param_types) {
			final int modifiers; final Invokable invokable; final AccessibleObject accessible; final Class<?>[] ex_types;
			try {
				if (mName != null) {
					final Method method = mClass.getDeclaredMethod(mName, param_types);
					modifiers = method.getModifiers(); invokable = new InvokableMethod(method); accessible = method;
					ex_types = method.getExceptionTypes();
					if (Modifier.isStatic(mModifiers) != Modifier.isStatic(method.getModifiers()))
						fail(new AssertionException(method + (Modifier.isStatic(mModifiers) ? " is not static" : "is static")).setHackedMethod(method));
					if (mReturnType != null && ! method.getReturnType().equals(mReturnType))
						fail(new AssertionException("Return type mismatch: " + method));
				} else {
					final Constructor<C> ctor = mClass.getDeclaredConstructor(param_types);
					modifiers = ctor.getModifiers(); invokable = new InvokableConstructor<>(ctor); accessible = ctor;
					ex_types = ctor.getExceptionTypes();
				}
			} catch (final NoSuchMethodException e) {
				fail(new AssertionException(e).setHackedClass(mClass).setHackedMethodName(mName).setParamTypes(param_types));
				if (! mHasFallback) return null;
				return new FallbackInvokable(mFallbackReturnValue);
			}

			if (mModifiers > 0 && (modifiers & mModifiers) != mModifiers)
				fail(new AssertionException(invokable + " does not match modifiers: " + mModifiers).setHackedMethodName(mName));

			if (mThrowTypes == null && ex_types.length > 0 || mThrowTypes != null && ex_types.length == 0)
				fail(new AssertionException("Checked exception(s) not match: " + invokable));
			else if (mThrowTypes != null) {
				Arrays.sort(ex_types);
				if (! Arrays.equals(ex_types, mThrowTypes))
					fail(new AssertionException("Checked exception(s) not match: " + invokable));
			}

			if (! accessible.isAccessible()) accessible.setAccessible(true);
			return invokable;
		}

		private final Class<C> mClass;
		private final @Nullable String mName;		// Null for constructor
		private final int mModifiers;
		private Class<?> mReturnType;
		private Class<?>[] mThrowTypes;
		private R mFallbackReturnValue;
		private boolean mHasFallback = true;		// Default to true for method returning void
	}

	private static class InvokableMethod<C> implements Invokable<C> {

		InvokableMethod(final Method method) { this.method = method; }

		public Object invoke(final C target, final Object[] args) throws IllegalAccessException,
				IllegalArgumentException, InvocationTargetException {
			return method.invoke(target, args);
		}

		@Override public String toString() { return method.toString(); }

		private final Method method;
	}

	private static class InvokableConstructor<C> implements Invokable<C> {

		InvokableConstructor(final Constructor<C> method) { this.constructor = method; }

		public Object invoke(final C target, final Object[] args) throws InstantiationException,
				IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return constructor.newInstance(args);
		}

		@Override public String toString() { return constructor.toString(); }

		private final Constructor<C> constructor;
	}

	private static class FallbackInvokable<C> implements Invokable<C> {

		FallbackInvokable(final Object value) { mValue = value; }

		@Override public Object invoke(final C target, final Object[] args) throws InvocationTargetException, IllegalAccessException, InstantiationException {
			return mValue;
		}

		private final Object mValue;
	}

	public static class HackedClass<C> {

		public <T> HackedField<C, T> field(final String name) {
			return new HackedField<C, T>(mClass, name, 0) {};	// Anonymous derived class ensures
		}

		public <T> HackedTargetField<T> staticField(final String name) {
			return new HackedField<C, T>(mClass, name, Modifier.STATIC).onTarget(null);
		}

		public @CheckResult NonNullHackedMethod<Void, C, Unchecked, Unchecked, Unchecked> method(final String name) {
			return new HackedMethodImpl<>(mClass, name, 0);
		}

		public @CheckResult NonNullHackedMethod<Void, Void, Unchecked, Unchecked, Unchecked> staticMethod(final String name) {
			return new HackedMethodImpl<>(mClass, name, Modifier.STATIC);
		}

		public @CheckResult HackedInvokable<C, Void, Unchecked, Unchecked, Unchecked> constructor() {
			return new HackedMethodImpl<>(mClass, null, 0);
		}
		
		HackedClass(final Class<C> clazz) { mClass = clazz; }

		private final Class<C> mClass;
	}

	public static <T> HackedClass<T> into(final Class<T> clazz) {
		return new HackedClass<>(clazz);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> HackedClass<T> into(final String class_name) {
		try {
			return new HackedClass(Class.forName(class_name));
		} catch (final ClassNotFoundException e) {
			fail(new AssertionException(e));
			return new HackedClass(null);	// TODO: Better solution to avoid null?
		}
	}
	
	private static void fail(final AssertionException e) {
		if (sFailureHandler != null) sFailureHandler.onAssertionFailure(e);
	}

	/** Specify a handler to deal with assertion failure, and decide whether the failure should be thrown. */
	public static AssertionFailureHandler setAssertionFailureHandler(final AssertionFailureHandler handler) {
		final AssertionFailureHandler old = sFailureHandler;
		sFailureHandler = handler;
		return old;
	}

	private Hack() {}

	private static AssertionFailureHandler sFailureHandler;

	/** This is a simple demo for the common usage of {@link Hack} */
	@SuppressWarnings("unused")
	private static class Demo {

		@SuppressWarnings({"FieldCanBeLocal", "UnnecessarilyQualifiedStaticUsage"})
		static class Hacks {

			/** Call this method before any hack is used, usually in your application initialization */
			static void defineAndVerify() {
				Hack.setAssertionFailureHandler(new AssertionFailureHandler() { @Override public void onAssertionFailure(final AssertionException failure) {
					Log.w("Demo", "Partially incompatible: " + failure.getDebugInfo());
					// Report the incompatibility silently.
					//...
				}});
				Demo_ctor = Hack.into(Demo.class).constructor().withParam(int.class);
				Demo_methodThrows = Hack.into(Demo.class).method("methodThrows").returning(Void.class).throwing(InterruptedException.class, IOException.class).withoutParams();
				Demo_staticMethod = Hack.into(Demo.class).staticMethod("methodWith2Params").returning(boolean.class).withParams(int.class, String.class);
				Demo_mField = Hack.into(Demo.class).field("mField").ofType(boolean.class).fallbackTo(false);
				Demo_sField = Hack.into(Demo.class).staticField("sField").ofType(String.class);
			}

			static HackedMethod1<Demo, Void, Unchecked, Unchecked, Unchecked, Integer> Demo_ctor;
			static Hack.HackedMethod0<Void, Demo, InterruptedException, IOException, Unchecked> Demo_methodThrows;
			static Hack.HackedMethod2<Boolean, Void, Unchecked, Unchecked, Unchecked, Integer, String> Demo_staticMethod;
			static @Nullable HackedField<Demo, Boolean> Demo_mField;		// Optional hack may be null if assertion failed
			static @Nullable HackedTargetField<String> Demo_sField;
		}

		static void demo() {
			final Demo demo = Hacks.Demo_ctor.invoke(0).statically();
			try {
				Hacks.Demo_methodThrows.invoke().on(demo);
			} catch (final InterruptedException | IOException e) {	// The checked exceptions declared by throwing() in hack definition.
				e.printStackTrace();
			}
			Hacks.Demo_staticMethod.invoke(1, "xx").statically();
		}

		Demo(final int flags) {}

		private void methodThrows() throws InterruptedException, IOException {}
		static boolean staticMethod(final int a, final String c) { return false; }
		boolean mField;
		static String sField;
	}
}
