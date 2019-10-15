package com.oasisfeng.hack;

import android.util.Log;
import android.util.Pair;

import com.oasisfeng.android.util.Supplier;
import com.oasisfeng.android.util.Suppliers;
import com.oasisfeng.deagle.BuildConfig;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static java.lang.Character.toLowerCase;

/**
 * Java reflection helper optimized for hacking non-public APIs.
 * The core design philosophy behind is compile-time consistency enforcement.
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
@ParametersAreNonnullByDefault @SuppressWarnings({"Convert2Lambda", "WeakerAccess", "unused"})
public class Hack {

	public static Class<?> ANY_TYPE = $.class; private static class $ {}
	@VisibleForTesting static boolean LAZY_RESOLVE = ! BuildConfig.DEBUG;	// Lazy in production if fallback is provided, to reduce initialization cost.

	/**
	 * Define your mirror interface by extending this interface.
	 * Then invoke instance method with {@link #into(Object)}{@link HackedObject#with(Class) .with(Class&lt;M&gt;)}, or static method directly.
	 *
	 * <p>Example:</p>
	 * <pre>
	 * interface HiddenClass extends Mirror&lt;com.foo.HiddenClass&gt; {
	 *    {@literal @}Fallback(-1) int foo(int x);
	 *    {@literal @}Fallback(Fallback.TRUE) boolean bar();
	 *     int getValue();		// Mirror getter for member field "value" (setter can be defined too)
	 *     static int bar(String name) {
	 *         return Hack.mirrorStaticMethod(com.foo.HiddenClass.class, "bar", -1, name);
	 *     }
	 * }
	 * </pre>
	 *
	 * @param <T> the type of your mirror interface
	 * @see #mirrorStaticMethod(Class, String, Object, Object...)
	 */
	public interface Mirror<T> {}
	@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) public @interface SourceClass { String value(); }
	@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface Fallback { int value(); int TRUE = 1; int FALSE = 0; }

	public interface HackedObject {
		<T, M extends Mirror<T>> M with(final Class<M> mirror_class);
	}

	public static HackedObject into(final Object source) {
		return new HackedMirrorObject(source);
	}

	/**
	 * Call this method from within the static interface method of mirror interface.
	 * <p>Support static interface method in both Java 8 and Desugar (Android)</p>
	 *
	 * @see Mirror
	 */
	public static <T, M extends Mirror<T>, R, E extends Exception> R mirrorStaticMethod(final Class<M> mirror, final String method, final R fallback, final Object... args) throws E {
		final String tag = mirror.getName() + "#" + method;
		Method source_method = sStaticMethodCache.get(tag);
		if (source_method == null) {
			source_method = findSourceStaticMethod(mirror, method, args.length);
			if (source_method != null) source_method.setAccessible(true);
			else source_method = NULL_METHOD;
			sStaticMethodCache.put(tag, source_method);
		}
		if (source_method == NULL_METHOD) return fallback;
		try { //noinspection unchecked
			return (R) source_method.invoke(null, args);
		} catch (final IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (final InvocationTargetException ite) {
			final Throwable e = ite.getTargetException();
			try { //noinspection unchecked
				throw (E) e;
			} catch (final ClassCastException cce) {
				throw new RuntimeException(e);
			}
		}
	}

	private static @Nullable <T, M extends Mirror<T>, R, E extends Exception> Method findSourceStaticMethod(
			final Class<M> clazz, final String method_name, final int num_args) {
		final Class<T> source_class = ensureSourceClassFromMirror(clazz);
		try {
			for (final Method method : source_class.getDeclaredMethods()) {
				if (method.getName().equals(method_name) && (method.getModifiers() & Modifier.STATIC) != 0
						&& method.getParameterTypes().length == num_args)	// TODO: Overloads with same number of parameters?
					return source_class.getDeclaredMethod(method_name, method.getParameterTypes());
			}
			throw new IllegalStateException("Mirror method not found: " + clazz.getCanonicalName() + "." + method_name);
		} catch (final NoSuchMethodException | ClassCastException e) {
			fail(new AssertionException(e));
		}
		return null;
	}

	static <T, M extends Mirror<T>> Class<T> ensureSourceClassFromMirror(final Class<M> mirror_class) {
		final Class<T> source_class = getSourceClassFromMirror(mirror_class);
		if (source_class == null) throw new IllegalArgumentException("Not an interface extending Mirror<T>: " + mirror_class);
		return source_class;
	}

	private static @Nullable <T, M extends Mirror<T>> Class<T> getSourceClassFromMirror(final Class<M> mirror_class) {
		final Class<?> cached = sMirrorCache.get(mirror_class);
		if (cached != null) //noinspection unchecked
			return cached != Object.class ? (Class<T>) cached : null;
		final Class<T> source_class = findSourceClassFromMirror(mirror_class);
		sMirrorCache.put(mirror_class, source_class != null ? source_class : Object.class);
		return source_class;
	}

	private static @Nullable <T, M extends Mirror<T>> Class<T> findSourceClassFromMirror(final Class<M> mirror_class) {
		final Type[] generic_interfaces = mirror_class.getGenericInterfaces();	// getGenericInterfaces() has its own cache
		if (generic_interfaces.length == 0) return null;
		final Type generic_interface = generic_interfaces[0];
		if (generic_interface instanceof ParameterizedType) {
			final ParameterizedType mirror_type = (ParameterizedType) generic_interface;
			final Type source_type = mirror_type.getActualTypeArguments()[0];
			if (! (source_type instanceof Class)) throw new IllegalStateException("Generic type must be class");
			//noinspection unchecked
			return (Class<T>) source_type;
		} else {
			final SourceClass target_class = mirror_class.getAnnotation(SourceClass.class);
			if (target_class != null) try { //noinspection unchecked
				return (Class<T>) Class.forName(target_class.value());
			} catch (final ClassNotFoundException e) {
				return null;
			}

			final Class<?> enclosing_class = mirror_class.getEnclosingClass();
			@SuppressWarnings("unchecked") final Class source_enclosing_class = enclosing_class != null ? getSourceClassFromMirror((Class) enclosing_class) : null;
			if (source_enclosing_class == null)
				throw new IllegalArgumentException("Mirror without parameter can only be extended by inner class of Mirror class");
			final Class[] inner_classes = source_enclosing_class.getClasses();
			final String mirror_class_simple_name = mirror_class.getSimpleName();
			for (final Class inner_class : inner_classes)
				if (mirror_class_simple_name.equals(inner_class.getSimpleName())) //noinspection unchecked
					return (Class<T>) inner_class;
			return null;
		}
	}

	// TODO: Use IdleHandler or low-priority thread
	public static void verifyAllMirrorsIn(final Class<?> enclosing) {
		for (final Class<?> inner_class : enclosing.getClasses()) {
			if (! Mirror.class.isAssignableFrom(inner_class)) continue;
			@SuppressWarnings("unchecked") final Class source_class = ensureSourceClassFromMirror((Class) inner_class);
			for (final Method mirror_method : inner_class.getMethods()) try {
				findSourceMethodForMirror(mirror_method, source_class);
			} catch (final NoSuchMethodException e) {
				if (extractFieldGetterOrSetterFromMethod(mirror_method, source_class) != null) continue;
				fail(new AssertionException(e).setHackedClass(into(inner_class)));
			}
			verifyAllMirrorsIn(inner_class);    // Only Mirror class may contain inner Mirror classes
		}
	}

	private static class HackedMirrorObject implements HackedObject {

		@Override public <T, M extends Mirror<T>> M with(final Class<M> mirror_class) {
			final Class<T> source_class = ensureSourceClassFromMirror(mirror_class);
			//noinspection unchecked
			return (M) Proxy.newProxyInstance(mirror_class.getClassLoader(), new Class[] { mirror_class }, new InvocationHandler() {
				@Override public Object invoke(final Object proxy, final Method mirror_method, final Object[] args) throws Throwable {
					final Object source_result;
					try {
						final Method source_method = findSourceMethodForMirror(mirror_method, source_class);	// TODO: Cache
						source_method.setAccessible(true);
						source_result = source_method.invoke(mSource, args);
					} catch (final NoSuchMethodException e) {
						final String mirror_method_name = mirror_method.getName(); final boolean is_getter; final char first_char;
						final Pair<Boolean, Field> accessor = extractFieldGetterOrSetterFromMethod(mirror_method, source_class);	// TODO: Cache
						if (accessor != null) {
							final Field field = accessor.second;
							field.setAccessible(true);
							if (! accessor.first) {		// Setter
								field.set(mSource, args[0]);
								return null;
							} else return field.get(mSource);
						}
						return fallback(mirror_method);
					} catch (final IllegalAccessException e) {
						return fallback(mirror_method);
					} catch (final InvocationTargetException e) {
						throw e.getTargetException();
					}
					if (source_result == null) return null;
					if (source_result instanceof List) {	// TODO: Support more collection containers
						final ParameterizedType mirror_return_type = (ParameterizedType) mirror_method.getGenericReturnType();
						final Type list_type = mirror_return_type.getActualTypeArguments()[0];
						final Class<?> list_raw_type = list_type instanceof ParameterizedType ? (Class<?>) ((ParameterizedType) list_type).getRawType()
								: list_type instanceof Class ? (Class<?>) list_type : null;
						if (list_raw_type == null || ! Mirror.class.isAssignableFrom(list_raw_type)) return source_result;
						// T (in List<T>) is a Mirror class, let's transform the list items.
						final List source_result_list = ((List) source_result), mirrored_result_list = new ArrayList(source_result_list.size());
						for (final Object source_result_item : source_result_list)
							if (source_result_item != null)	//noinspection unchecked
								mirrored_result_list.add(into(source_result_item).with((Class) list_raw_type));	// TODO: Individual item may be derived class of the generic list type
						return mirrored_result_list;
					} else {
						final Class<?> mirror_return_type = mirror_method.getReturnType();
						if (mirror_return_type.isPrimitive() || mirror_return_type.isAssignableFrom(source_result.getClass())) return source_result;
						//noinspection unchecked
						return into(source_result).with((Class) mirror_return_type);
					}
				}

				Object fallback(final Method method) {
					final Class<?> type = method.getReturnType();
					if (! type.isPrimitive()) return null;
					final Fallback fallback_annotation = method.getAnnotation(Fallback.class);
					if (type == boolean.class) return fallback_annotation != null && fallback_annotation.value() > 0;
					else if (type == int.class || type == long.class || type == byte.class || type == char.class
							|| type == float.class || type == double.class || type == short.class)
						return fallback_annotation != null ? cast(fallback_annotation.value(), type) : cast(0, type);
					return null;
				}

				private Object cast(final int value, final Class<?> type) {
					if (type == int.class) return value;
					if (type == long.class) return (long) value;
					if (type == byte.class) return (byte) value;
					if (type == char.class) return (char) value;
					if (type == float.class) return (float) value;
					if (type == double.class) return (double) value;
					if (type == short.class) return (short) value;
					throw new UnsupportedOperationException();
				}

				private Map<Method/* mirror */, Method/* source */> mMethodCache;
			});
		}

		public HackedMirrorObject(final Object source) { mSource = source; }

		private final Object mSource;
	}

	// TODO: Support params in type Object (as placeholder) with @ClassName annotation
	private static Method findSourceMethodForMirror(final Method mirror_method, final Class<?> source_class) throws NoSuchMethodException {
		return source_class.getDeclaredMethod(mirror_method.getName(), mirror_method.getParameterTypes());
	}

	private static @Nullable Pair<Boolean, Field> extractFieldGetterOrSetterFromMethod(final Method mirror_method, final Class<?> source_class) {
		final String mirror_method_name = mirror_method.getName(); final char first_char;
		if (mirror_method_name.length() > 3 && Character.isUpperCase(first_char = mirror_method_name.charAt(3))) {
			final boolean is_getter = mirror_method_name.startsWith("get");
			if (is_getter) {
				if (getParameterCount(mirror_method) != 0) return null;		// Getter should have no parameter
			} else if (! mirror_method_name.startsWith("set") || getParameterCount(mirror_method) != 1) return null;
			try {
				final Field field = source_class.getDeclaredField(toLowerCase(first_char) + mirror_method_name.substring(4));
				return new Pair<>(is_getter, field);
			} catch (final NoSuchFieldException ignored) {}
		}
		return null;
	}

	private static int getParameterCount(final Method mirror_method) {
		return SDK_INT >= O ? mirror_method.getParameterCount() : mirror_method.getParameterTypes().length;
	}

	public static class AssertionException extends Throwable {

		private HackedClass<?> mClass;
		private Field mHackedField;
		private Method mHackedMethod;
		private String mHackedFieldName;
		private @Nullable String mHackedMethodName;
		private Class<?>[] mParamTypes;

		AssertionException(final String e) { super(e); }
		AssertionException(final Exception e) { super(e); }

		@Override public @NonNull String toString() {
			return getCause() != null ? getClass().getName() + ": " + getCause() : super.toString();
		}

		public String getDebugInfo() {
			final StringBuilder info = new StringBuilder(getCause() != null ? getCause().toString() : super.toString());
			final Throwable cause = getCause();
			if (cause instanceof NoSuchMethodException) {
				info.append(" Potential candidates:");
				final int initial_length = info.length();
				final String name = getHackedMethodName();
				final Class<?> clazz = getHackedClass().getRawClass();
				if (name != null) {
					for (final Method method : clazz.getDeclaredMethods())
						if (method.getName().equals(name))			// Exact name match
							info.append(' ').append(method);
					if (info.length() == initial_length)
						for (final Method method : clazz.getDeclaredMethods())
							if (method.getName().startsWith(name))	// Name prefix match
								info.append(' ').append(method);
					if (info.length() == initial_length)
						for (final Method method : clazz.getDeclaredMethods())
							if (! method.getName().startsWith("-"))	// Dump all but generated methods
								info.append(' ').append(method);
				} else for (final Constructor<?> constructor : clazz.getDeclaredConstructors())
					info.append(' ').append(constructor);
			} else if (cause instanceof NoSuchFieldException) {
				info.append(" Potential candidates:");
				final int initial_length = info.length();
				final String name = getHackedFieldName();
				final Field[] fields = getHackedClass().getRawClass().getDeclaredFields();
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

		public HackedClass<?> getHackedClass() {
			return mClass;
		}

		AssertionException setHackedClass(final HackedClass<?> hacked_class) {
			mClass = hacked_class; return this;
		}

		public Method getHackedMethod() {
			return mHackedMethod;
		}

		AssertionException setHackedMethod(final Method method) {
			mHackedMethod = method;
			return this;
		}

		@CheckResult public String getHackedMethodName() {
			return mHackedMethodName;
		}

		@SuppressWarnings("UnusedReturnValue") AssertionException setHackedMethodName(final String method) {
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

	public static class FieldToHack<C> {

		protected @Nullable <T> Field findField(final @Nullable Class<T> type) {
			if (mClass.getRawClass() == ANY_TYPE) return null;		// AnyType as a internal indicator for class not found.
			Field field = null;
			try {
				field = mClass.getRawClass().getDeclaredField(mName);
				if (Modifier.isStatic(mModifiers) != Modifier.isStatic(field.getModifiers())) {
					fail(new AssertionException(field + (Modifier.isStatic(mModifiers) ? " is not static" : " is static")).setHackedFieldName(mName));
					field = null;
				} else if (mModifiers > 0 && (field.getModifiers() & mModifiers) != mModifiers) {
					fail(new AssertionException(field + " does not match modifiers: " + mModifiers).setHackedFieldName(mName));
					field = null;
				} else if (! field.isAccessible()) field.setAccessible(true);
			} catch (final NoSuchFieldException e) {
				final AssertionException hae = new AssertionException(e);
				hae.setHackedClass(mClass);
				hae.setHackedFieldName(mName);
				fail(hae);
			}

			if (type != null && field != null && ! type.isAssignableFrom(field.getType()))
				fail(new AssertionException(new ClassCastException(field + " is not of type " + type)).setHackedField(field));
			return field;
		}

		/** @param modifiers the modifiers this field must have */
		protected FieldToHack(final HackedClass<C> clazz, final String name, final int modifiers) {
			mClass = clazz;
			mName = name;
			mModifiers = modifiers;
		}

		protected final HackedClass<C> mClass;
		protected final String mName;
		protected final int mModifiers;
	}

	public static class MemberFieldToHack<C> extends FieldToHack<C> {

		/** Assert the field type. */
		public @Nullable <T> HackedField<C, T> ofType(final Class<T> type) {
			return ofType(type, false, null);
		}

		public @Nullable <T> HackedField<C, T> ofType(final String type_name) {
			try { //noinspection unchecked
				return ofType((Class<T>) Class.forName(type_name, false, mClass.getRawClass().getClassLoader()));
			} catch (final ClassNotFoundException e) {
				fail(new AssertionException(e));
				return null;
			}
		}

		public @NonNull HackedField<C, Byte> fallbackTo(final byte value) { //noinspection ConstantConditions
			return ofType(byte.class, true, value);
		}
		public @NonNull HackedField<C, Character> fallbackTo(final char value) { //noinspection ConstantConditions
			return ofType(char.class, true, value);
		}
		public @NonNull HackedField<C, Short> fallbackTo(final short value) { //noinspection ConstantConditions
			return ofType(short.class, true, value);
		}
		public @NonNull HackedField<C, Integer> fallbackTo(final int value) { //noinspection ConstantConditions
			return ofType(int.class, true, value);
		}
		public @NonNull HackedField<C, Long> fallbackTo(final long value) { //noinspection ConstantConditions
			return ofType(long.class, true, value);
		}
		public @NonNull HackedField<C, Boolean> fallbackTo(final boolean value) { //noinspection ConstantConditions
			return ofType(boolean.class, true, value);
		}
		public @NonNull HackedField<C, Float> fallbackTo(final float value) { //noinspection ConstantConditions
			return ofType(float.class, true, value);
		}
		public @NonNull HackedField<C, Double> fallbackTo(final double value) { //noinspection ConstantConditions
			return ofType(double.class, true, value);
		}

		/** Fallback to the given value if this field is unavailable at runtime */
		public @NonNull <T> HackedField<C, T> fallbackTo(final @Nullable T value) {
			@SuppressWarnings("unchecked") final Class<T> type = value == null ? null : (Class<T>) value.getClass();
			//noinspection ConstantConditions
			return ofType(type, true, value);
		}

		private <T> HackedField<C, T> ofType(final Class<T> type, final boolean fallback, final @Nullable T fallback_value) {
			if (LAZY_RESOLVE && fallback) return new LazyHackedField<>(this, type, fallback_value);
			final Field field = findField(type);
			return field != null ? new HackedFieldImpl<>(field) : fallback ? new FallbackField<>(type, fallback_value) : null;
		}

		/** @param modifiers the modifiers this field must have */
		private MemberFieldToHack(final HackedClass<C> clazz, final String name, final int modifiers) {
			super(clazz, name, modifiers);
		}
	}

	public static class StaticFieldToHack<C> extends FieldToHack<C> {

		/** Assert the field type. */
		public @Nullable <T> HackedStaticField<T> ofType(final Class<T> type) {
			return ofType(type, false, null);
		}

		public @Nullable <T> HackedStaticField<T> ofType(final String type_name) {
			try { //noinspection unchecked
				return ofType((Class<T>) Class.forName(type_name, false, mClass.getRawClass().getClassLoader()));
			} catch (final ClassNotFoundException e) {
				fail(new AssertionException(e));
				return null;
			}
		}

		/** Fallback to the given value if this field is unavailable at runtime */
		public @NonNull <T> HackedStaticField<T> fallbackTo(final @Nullable T value) {
			@SuppressWarnings("unchecked") final Class<T> type = value == null ? null : (Class<T>) value.getClass();
			//noinspection ConstantConditions
			return ofType(type, true, value);
		}

		private <T> HackedStaticField<T> ofType(final Class<T> type, final boolean fallback, final @Nullable T fallback_value) {
			if (LAZY_RESOLVE && fallback) return new LazyHackedField<>(this, type, fallback_value);
			final Field field = findField(type);
			return field != null ? new HackedFieldImpl<C, T>(field).statically() : fallback ? new FallbackField<C, T>(type, fallback_value) : null;
		}

		/** @param modifiers the modifiers this field must have */
		private StaticFieldToHack(final HackedClass<C> clazz, final String name, final int modifiers) {
			super(clazz, name, modifiers);
		}
	}

	public interface HackedField<C, T> {
		T get(C instance);
		void set(C instance, @Nullable T value);
		HackedTargetField<T> on(C target);
		Class<T> getType();
		boolean isAbsent();
	}

	public interface HackedTargetField<T> {
		T get();
		void set(T value);
		Class<T> getType();
		boolean isAbsent();
	}

	public interface HackedStaticField<T> extends HackedTargetField<T> {}

	private static class HackedFieldImpl<C, T> implements HackedField<C, T> {

		@Override public HackedTargetFieldImpl<T> on(final C target) {
			return onTarget(target);
		}

		private HackedTargetFieldImpl<T> onTarget(final C target) { return new HackedTargetFieldImpl<>(mField, target); }

		private HackedStaticFieldImpl<T> statically() { return new HackedStaticFieldImpl<>(mField); }

		/** Get current value of this field */
		@Override public T get(final C instance) {
			try {
				@SuppressWarnings("unchecked") final T value = (T) mField.get(instance);
				return value;
			} catch (final IllegalAccessException e) { return null; }	// Should never happen
		}

		/**
		 * Set value of this field
		 * 
		 * <p>No type enforced here since most type mismatch can be easily tested and exposed early.</p>
		 */
		@Override public void set(final C instance, final @Nullable T value) {
			try {
				mField.set(instance, value);
			} catch (final IllegalAccessException ignored) {}	// Should never happen
		}

		@Override @SuppressWarnings("unchecked") public @Nullable Class<T> getType() {
			return (Class<T>) mField.getType();
		}
		@Override public boolean isAbsent() { return false; }

		HackedFieldImpl(final @NonNull Field field) { mField = field; }

		public @Nullable Field getField() { return mField; }

		private final @NonNull Field mField;
	}

	private static class FallbackField<C, T> implements HackedField<C, T>, HackedStaticField<T> {

		@Override public T get(final C instance) { return mValue; }
		@Override public void set(final C instance, final @Nullable T value) {}
		@Override public T get() { return mValue; }
		@Override public void set(final T value) {}
		@Override public HackedTargetField<T> on(final C target) { return this; }
		@Override public Class<T> getType() { return mType; }
		@Override public boolean isAbsent() { return true; }

		FallbackField(final Class<T> type, final @Nullable T value) { mType = type; mValue = value; }

		private final Class<T> mType;
		private final T mValue;
	}

	private static class LazyHackedField<C, T> implements HackedField<C, T>, HackedStaticField<T> {

		@Override public T get(final C instance) { return delegate.get().get(instance); }
		@Override public void set(final C instance, final @Nullable T value) { delegate.get().set(instance, value); }
		@Override public HackedTargetField<T> on(final C target) { return delegate.get().on(target); }
		@Override @SuppressWarnings("ConstantConditions") public T get() { return delegate.get().get(null); }
		@Override @SuppressWarnings("ConstantConditions") public void set(final T value) { delegate.get().set(null, value); }
		@Override public Class<T> getType() { return delegate.get().getType(); }
		@Override public boolean isAbsent() { return delegate.get().isAbsent(); }

		LazyHackedField(final FieldToHack<C> field, final Class<T> type, final @Nullable T fallback_value) {
			mField = field;
			mType = type;
			mFallbackValue = fallback_value;
		}

		private final FieldToHack<C> mField;
		private final Class<T> mType;
		private final T mFallbackValue;

		private final Supplier<HackedField<C, T>> delegate = Suppliers.memoize(new Supplier<HackedField<C, T>>() { @Override public HackedField<C, T> get() {
			final Field field = LazyHackedField.this.mField.findField(mType);
			return field != null ? new HackedFieldImpl<>(field) : new FallbackField<>(mType, mFallbackValue);
		}});
	}

	public static class HackedTargetFieldImpl<T> implements HackedTargetField<T> {

		@Override public T get() {
			try {
				@SuppressWarnings("unchecked") final T value = (T) mField.get(mInstance);
				return value;
			} catch (final IllegalAccessException e) { return null; }	// Should never happen
		}

		@Override public void set(final T value) {
			try {
				mField.set(mInstance, value);
			} catch (final IllegalAccessException ignored) {}			// Should never happen
		}

		@Override @SuppressWarnings("unchecked") public @Nullable Class<T> getType() { return (Class<T>) mField.getType(); }
		@Override public boolean isAbsent() { return false; }

		HackedTargetFieldImpl(final Field field, final @Nullable Object instance) {
			mField = field;
			mInstance = instance;
		}

		private final Field mField;
		private final Object mInstance;		// Instance type is already checked
	}

	public static class HackedStaticFieldImpl<T> extends HackedTargetFieldImpl<T> implements HackedStaticField<T> {
		HackedStaticFieldImpl(final Field field) { super(field, null); }
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

	public interface NonNullHackedInvokable<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> extends HackedInvokable<R, C, T1,T2,T3> {
		@CheckResult <TT1 extends Throwable> NonNullHackedInvokable<R, C, TT1, T2, T3> throwing(Class<TT1> type);
		@CheckResult <TT1 extends Throwable, TT2 extends Throwable> NonNullHackedInvokable<R, C, TT1, TT2, T3> throwing(Class<TT1> type1, Class<TT2> type2);
		@CheckResult <TT1 extends Throwable, TT2 extends Throwable, TT3 extends Throwable> NonNullHackedInvokable<R, C, TT1, TT2, TT3> throwing(Class<TT1> type1, Class<TT2> type2, Class<TT3> type3);

		@NonNull HackedMethod0<R, C, T1, T2, T3> withoutParams();
		@NonNull <A1> HackedMethod1<R, C, T1, T2, T3, A1> withParam(Class<A1> type);
		@NonNull <A1, A2> HackedMethod2<R, C, T1, T2, T3, A1, A2> withParams(Class<A1> type1, Class<A2> type2);
		@NonNull <A1, A2, A3> HackedMethod3<R, C, T1, T2, T3, A1, A2, A3> withParams(Class<A1> type1, Class<A2> type2, Class<A3> type3);
		@NonNull <A1, A2, A3, A4> HackedMethod4<R, C, T1, T2, T3, A1, A2, A3, A4> withParams(Class<A1> type1, Class<A2> type2, Class<A3> type3, Class<A4> type4);
		@NonNull <A1, A2, A3, A4, A5> HackedMethod5<R, C, T1, T2, T3, A1, A2, A3, A4, A5> withParams(Class<A1> type1, final Class<A2> type2, final Class<A3> type3, final Class<A4> type4, final Class<A5> type5);
		@NonNull HackedMethodN<R, C, T1, T2, T3> withParams(Class<?>... types);
	}

	public interface HackedMethod<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> extends HackedInvokable<R, C, T1,T2,T3> {
		/** Optional */
		@CheckResult <RR> HackedMethod<RR, C, T1, T2, T3> returning(Class<RR> type);
		/** Fallback to the given value if this field is unavailable at runtime. (Optional) */
		@CheckResult NonNullHackedMethod<R, C, T1, T2, T3> fallbackReturning(@Nullable R return_value);

		@CheckResult <TT1 extends Throwable> HackedMethod<R, C, TT1, T2, T3> throwing(Class<TT1> type);
		@CheckResult <TT1 extends Throwable, TT2 extends Throwable> HackedMethod<R, C, TT1, TT2, T3> throwing(Class<TT1> type1, Class<TT2> type2);
		@CheckResult <TT1 extends Throwable, TT2 extends Throwable, TT3 extends Throwable> HackedMethod<R, C, TT1, TT2, TT3> throwing(Class<TT1> type1, Class<TT2> type2, Class<TT3> type3);
		@CheckResult HackedMethod<R, C, Exception, T2, T3> throwing(Class<?>... types);
	}

	// Force to NonNull
	public interface NonNullHackedMethod<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> extends HackedMethod<R, C, T1,T2,T3>, NonNullHackedInvokable<R, C, T1,T2,T3> {
		/** Optional */
		@CheckResult <RR> HackedMethod<RR, C, T1, T2, T3> returning(Class<RR> type);
		/** Whatever exception or none */
		@CheckResult <TT1 extends Throwable> NonNullHackedMethod<R, C, Exception, T2, T3> throwing();
		@CheckResult <TT1 extends Throwable> NonNullHackedMethod<R, C, TT1, T2, T3> throwing(Class<TT1> type);
		@CheckResult <TT1 extends Throwable, TT2 extends Throwable> NonNullHackedMethod<R, C, TT1, TT2, T3> throwing(Class<TT1> type1, Class<TT2> type2);
		@CheckResult <TT1 extends Throwable, TT2 extends Throwable, TT3 extends Throwable> NonNullHackedMethod<R, C, TT1, TT2, TT3> throwing(Class<TT1> type1, Class<TT2> type2, Class<TT3> type3);
	}

	public static class CheckedHackedMethod<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> {

		CheckedHackedMethod(final Invokable invokable) { mInvokable = invokable; }
		@VisibleForTesting HackInvocation<R, C, T1, T2, T3> invoke(final Object... args) { return new HackInvocation<>(mInvokable, args); }
		/** Whether this hack is absent, thus will be fallen-back when invoked */
		public boolean isAbsent() { return mInvokable.isAbsent(); }

		private final Invokable mInvokable;
	}

	public static class HackedMethod0<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> extends CheckedHackedMethod<R, C, T1,T2,T3> {
		HackedMethod0(final Invokable invokable) { super(invokable); }
		public @CheckResult HackInvocation<R, C, T1, T2, T3> invoke() { return super.invoke(); }
	}
	public static class HackedMethod1<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1> extends CheckedHackedMethod<R, C, T1,T2,T3> {
		HackedMethod1(final Invokable invokable) { super(invokable); }
		public @CheckResult HackInvocation<R, C, T1, T2, T3> invoke(final @Nullable A1 arg) { return super.invoke(arg); }
	}
	public static class HackedMethod2<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1, A2> extends CheckedHackedMethod<R, C, T1,T2,T3> {
		HackedMethod2(final Invokable invokable) { super(invokable); }
		public @CheckResult HackInvocation<R, C, T1, T2, T3> invoke(final @Nullable A1 arg1, final @Nullable A2 arg2) { return super.invoke(arg1, arg2); }
	}
	public static class HackedMethod3<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1, A2, A3> extends CheckedHackedMethod<R, C, T1,T2,T3> {
		HackedMethod3(final Invokable invokable) { super(invokable); }
		public @CheckResult HackInvocation<R, C, T1, T2, T3> invoke(final @Nullable A1 arg1, final @Nullable A2 arg2, final @Nullable A3 arg3) { return super.invoke(arg1, arg2, arg3); }
	}
	public static class HackedMethod4<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1, A2, A3, A4> extends CheckedHackedMethod<R, C, T1,T2,T3> {
		HackedMethod4(final Invokable invokable) { super(invokable); }
		public @CheckResult HackInvocation<R, C, T1, T2, T3> invoke(final @Nullable A1 arg1, final @Nullable A2 arg2, final @Nullable A3 arg3, final @Nullable A4 arg4) { return super.invoke(arg1, arg2, arg3, arg4); }
	}
	public static class HackedMethod5<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1, A2, A3, A4, A5> extends CheckedHackedMethod<R, C, T1,T2,T3> {
		HackedMethod5(final Invokable invokable) { super(invokable); }
		public @CheckResult HackInvocation<R, C, T1, T2, T3> invoke(final @Nullable A1 arg1, final @Nullable A2 arg2, final @Nullable A3 arg3, final @Nullable A4 arg4, final @Nullable A5 arg5) { return super.invoke(arg1, arg2, arg3, arg4, arg5); }
	}
	public static class HackedMethodN<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> extends CheckedHackedMethod<R, C, T1,T2,T3> {
		HackedMethodN(final Invokable invokable) { super(invokable); }
		public @CheckResult HackInvocation<R, C, T1, T2, T3> invoke(final Object... args) { return super.invoke(args); }
	}

	public static class HackInvocation<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> {

		HackInvocation(final Invokable invokable, final Object... args) {
			this.invokable = invokable;
			this.args = args;
		}

		public R on(final @NonNull C target) throws T1, T2, T3 { return onTarget(target); }
		public R statically() throws T1, T2, T3 { return onTarget(null); }

		@SuppressWarnings("RedundantThrows") private R onTarget(final @Nullable C target) throws T1, T2, T3 {
			try {
				@SuppressWarnings("unchecked") final R result = (R) invokable.invoke(target, args);
				return result;
			} catch (final IllegalAccessException/* should never happen */| InstantiationException e) { throw new RuntimeException(e);
			} catch (final InvocationTargetException e) {
				final Throwable ex = e.getTargetException();
				//noinspection unchecked
				throw (T1) ex;		// The casting is actually no-op after erasure, it throws the exception directly.
			}
		}

		private final Invokable invokable;
		private final Object[] args;
	}

	interface Invokable<C> {
		Object invoke(@Nullable C target, Object[] args) throws InvocationTargetException, IllegalAccessException, InstantiationException;
		boolean isAbsent();
	}

	private static class HackedMethodImpl<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> implements NonNullHackedMethod<R, C, T1, T2, T3> {

		HackedMethodImpl(final HackedClass<?> clazz, @Nullable final String name, final int modifiers) {
			//noinspection unchecked, to be compatible with HackedClass.staticMethod()
			mClass = (HackedClass<C>) clazz;
			mName = name;
			mModifiers = modifiers;
		}

		@Override public <RR> HackedMethod<RR, C, T1, T2, T3> returning(final Class<RR> type) {
			mReturnType = type;
			@SuppressWarnings("unchecked") final HackedMethod<RR, C, T1, T2, T3> casted = (HackedMethod<RR, C, T1, T2, T3>) this;
			return casted;
		}

		@Override public NonNullHackedMethod<R, C, T1, T2, T3> fallbackReturning(final @Nullable R value) {
			mFallbackReturnValue = value; mHasFallback = true; return this;
		}

		@Override public NonNullHackedMethod<R, C, Exception, T2, T3> throwing() {
			mThrowTypes = new Class[] {};
			@SuppressWarnings("unchecked") final NonNullHackedMethod<R, C, Exception, T2, T3> casted = (NonNullHackedMethod<R, C, Exception, T2, T3>) this;
			return casted;
		}

		@Override public <TT extends Throwable> NonNullHackedMethod<R, C, TT, T2, T3> throwing(final Class<TT> type) {
			mThrowTypes = new Class[] { type };
			@SuppressWarnings("unchecked") final NonNullHackedMethod<R, C, TT, T2, T3> casted = (NonNullHackedMethod<R, C, TT, T2, T3>) this;
			return casted;
		}

		@Override public <TT1 extends Throwable, TT2 extends Throwable> NonNullHackedMethod<R, C, TT1, TT2, T3>
				throwing(final Class<TT1> type1, final Class<TT2> type2) {
			mThrowTypes = new Class<?>[] { type1, type2 };
			Arrays.sort(mThrowTypes, CLASS_COMPARATOR);
			@SuppressWarnings("unchecked") final NonNullHackedMethod<R, C, TT1, TT2, T3> cast = (NonNullHackedMethod<R, C, TT1, TT2, T3>) this;
			return cast;
		}

		@Override public <TT1 extends Throwable, TT2 extends Throwable, TT3 extends Throwable> NonNullHackedMethod<R, C, TT1, TT2, TT3>
				throwing(final Class<TT1> type1, final Class<TT2> type2, final Class<TT3> type3) {
			mThrowTypes = new Class<?>[] { type1, type2, type3 };
			Arrays.sort(mThrowTypes, CLASS_COMPARATOR);
			@SuppressWarnings("unchecked") final NonNullHackedMethod<R, C, TT1, TT2, TT3> cast = (NonNullHackedMethod<R, C, TT1, TT2, TT3>) this;
			return cast;
		}

		@Override public HackedMethod<R, C, Exception, T2, T3> throwing(final Class<?>... types) {
			mThrowTypes = types;
			Arrays.sort(mThrowTypes, CLASS_COMPARATOR);
			@SuppressWarnings("unchecked") final HackedMethod<R, C, Exception, T2, T3> cast = (HackedMethod<R, C, Exception, T2, T3>) this;
			return cast;
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public HackedMethod0<R, C, T1, T2, T3> withoutParams() {
			final Invokable<C> invokable = buildInvokable();
			return invokable == null ? null : new HackedMethod0<>(invokable);
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public <A1> HackedMethod1<R, C, T1, T2, T3, A1> withParam(final Class<A1> type) {
			final Invokable invokable = buildInvokable(type);
			return invokable == null ? null : new HackedMethod1<>(invokable);
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public <A1, A2> HackedMethod2<R, C, T1, T2, T3, A1, A2> withParams(final Class<A1> type1, final Class<A2> type2) {
			final Invokable invokable = buildInvokable(type1, type2);
			return invokable == null ? null : new HackedMethod2<>(invokable);
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public <A1, A2, A3> HackedMethod3<R, C, T1, T2, T3, A1, A2, A3> withParams(final Class<A1> type1, final Class<A2> type2, final Class<A3> type3) {
			final Invokable invokable = buildInvokable(type1, type2, type3);
			return invokable == null ? null : new HackedMethod3<>(invokable);
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public <A1, A2, A3, A4> HackedMethod4<R, C, T1, T2, T3, A1, A2, A3, A4> withParams(final Class<A1> type1, final Class<A2> type2, final Class<A3> type3, final Class<A4> type4) {
			final Invokable invokable = buildInvokable(type1, type2, type3, type4);
			return invokable == null ? null : new HackedMethod4<>(invokable);
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public <A1, A2, A3, A4, A5> HackedMethod5<R, C, T1, T2, T3, A1, A2, A3, A4, A5> withParams(final Class<A1> type1, final Class<A2> type2, final Class<A3> type3, final Class<A4> type4, final Class<A5> type5) {
			final Invokable invokable = buildInvokable(type1, type2, type3, type4, type5);
			return invokable == null ? null : new HackedMethod5<>(invokable);
		}

		@NonNull @SuppressWarnings("ConstantConditions")
		@Override public HackedMethodN<R, C, T1, T2, T3> withParams(final Class<?>... types) {
			final Invokable invokable = buildInvokable(types);
			return invokable == null ? null : new HackedMethodN<>(invokable);
		}

		private @Nullable Invokable<C> buildInvokable(final Class<?>... param_types) {
			return LAZY_RESOLVE && mHasFallback ? new LazyInvokable<>(this, param_types) : findInvokable(param_types);
		}

		private @Nullable Invokable<C> findInvokable(final Class<?>... param_types) {
			if (mClass.getRawClass() == ANY_TYPE)		// AnyType as a internal indicator for class not found.
				return mHasFallback ? new FallbackInvokable<>(mFallbackReturnValue) : null;

			final int modifiers; Invokable<C> invokable; final AccessibleObject accessible; final Class<?>[] ex_types;
			try {
				if (mName != null) {
					final Method candidate = mClass.getRawClass().getDeclaredMethod(mName, param_types); Method method = candidate;
					ex_types = candidate.getExceptionTypes();
					modifiers = method.getModifiers();
					if (Modifier.isStatic(mModifiers) != Modifier.isStatic(candidate.getModifiers())) {
						fail(new AssertionException(candidate + (Modifier.isStatic(mModifiers) ? " is not static" : "is static")).setHackedMethod(method));
						method = null;
					}
					if (mReturnType != null && mReturnType != ANY_TYPE && ! candidate.getReturnType().equals(mReturnType)) {
						fail(new AssertionException("Return type mismatch: " + candidate));
						method = null;
					}
					if (method != null) {
						invokable = new InvokableMethod<>(method);
						accessible = method;
					} else { invokable = null; accessible = null; }
				} else {
					final Constructor<C> ctor = mClass.getRawClass().getDeclaredConstructor(param_types);
					modifiers = ctor.getModifiers(); invokable = new InvokableConstructor<>(ctor); accessible = ctor;
					ex_types = ctor.getExceptionTypes();
				}
			} catch (final NoSuchMethodException e) {
				final AssertionException failure = new AssertionException(e).setHackedClass(mClass).setParamTypes(param_types);
				if (mName != null) failure.setHackedMethodName(mName);
				fail(failure);
				return mHasFallback ? new FallbackInvokable<>(mFallbackReturnValue) : null;
			}

			if (mModifiers > 0 && (modifiers & mModifiers) != mModifiers) {
				final AssertionException failure = new AssertionException(invokable + " does not match modifiers: " + mModifiers);
				if (mName != null) failure.setHackedMethodName(mName);
				fail(failure);
			}

			if (mThrowTypes == null && ex_types.length > 0 || mThrowTypes != null && mThrowTypes.length > 0 && ex_types.length == 0) {
				fail(new AssertionException("Checked exception(s) not match: " + invokable));
				if (ex_types.length > 0) invokable = null;		// No need to fall-back if expected checked exceptions are absent.
			} else if (mThrowTypes != null && mThrowTypes.length > 0) {		// Empty array stands for "whatever exception or none"
				if (mThrowTypes.length > 1) Arrays.sort(ex_types, CLASS_COMPARATOR);
				if (! Arrays.equals(ex_types, mThrowTypes)) {	// TODO: Check derived relation of exceptions
					fail(new AssertionException("Checked exception(s) not match: " + invokable));
					invokable = null;
				}
			}

			if (invokable == null) {
				if (! mHasFallback) return null;
				return new FallbackInvokable<>(mFallbackReturnValue);
			}

			if (! accessible.isAccessible()) accessible.setAccessible(true);
			return invokable;
		}

		private final HackedClass<C> mClass;
		private final @Nullable String mName;		// Null for constructor
		private final int mModifiers;
		private Class<?> mReturnType;
		private Class<?>[] mThrowTypes;
		private R mFallbackReturnValue;
		private boolean mHasFallback;
		private static final Comparator<Class> CLASS_COMPARATOR = new Comparator<Class>() {
			@Override public int compare(final Class lhs, final Class rhs) {
				return lhs.toString().compareTo(rhs.toString());
			}

			@Override public boolean equals(final @Nullable Object object) {
				return this == object;
			}
		};
	}

	private static class InvokableMethod<C> implements Invokable<C> {

		InvokableMethod(final Method method) { this.method = method; }

		public Object invoke(final @Nullable C target, final Object[] args) throws IllegalAccessException,
				IllegalArgumentException, InvocationTargetException {
			return method.invoke(target, args);
		}

		@Override public boolean isAbsent() { return false; }
		@Override public @NonNull String toString() { return method.toString(); }

		private final Method method;
	}

	private static class InvokableConstructor<C> implements Invokable<C> {

		InvokableConstructor(final Constructor<C> method) { this.constructor = method; }

		public Object invoke(final @Nullable C target, final Object[] args) throws InstantiationException,
				IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return constructor.newInstance(args);
		}

		@Override public boolean isAbsent() { return false; }
		@Override public @NonNull String toString() { return constructor.toString(); }

		private final Constructor<C> constructor;
	}

	private static class FallbackInvokable<C> implements Invokable<C> {

		FallbackInvokable(final @Nullable Object value) { mValue = value; }

		@Override public Object invoke(final @Nullable C target, final Object[] args) { return mValue; }
		@Override public boolean isAbsent() { return true; }

		private final @Nullable Object mValue;
	}

	private static class LazyInvokable<C> implements Invokable<C> {

		LazyInvokable(final HackedMethodImpl<?, C, ?, ?, ?> method, final Class<?>[] param_types) {
			mMethod = method;
			mParamTypes = param_types;
		}

		@Override public Object invoke(final @Nullable C target, final Object[] args) throws InvocationTargetException, IllegalAccessException, InstantiationException {
			return resolveIfNotYet().invoke(target, args);
		}

		@Override public boolean isAbsent() {
			return resolveIfNotYet().isAbsent();
		}

		private Invokable<C> resolveIfNotYet() {
			if (mResolved == null) mResolved = mMethod.findInvokable(mParamTypes);
			return mResolved;
		}

		private final HackedMethodImpl<?, C, ?, ?, ?> mMethod;
		private final Class<?>[] mParamTypes;
		private Invokable<C> mResolved;
	}

	public static class HackedClass<C> {

		public @CheckResult <T> MemberFieldToHack<C> field(final @NonNull String name) {
			return new MemberFieldToHack<>(this, name, 0);
		}

		public @CheckResult <T> StaticFieldToHack<C> staticField(final @NonNull String name) {
			return new StaticFieldToHack<>(this, name, Modifier.STATIC);
		}

		public @CheckResult HackedMethod<Void, C, Unchecked, Unchecked, Unchecked> method(final String name) {
			return new HackedMethodImpl<>(this, name, 0);
		}

		public @CheckResult HackedMethod<Void, Void, Unchecked, Unchecked, Unchecked> staticMethod(final String name) {
			return new HackedMethodImpl<>(this, name, Modifier.STATIC);
		}

		public @CheckResult NonNullHackedInvokable<C, Void, Unchecked, Unchecked, Unchecked> constructor() {
			final HackedMethodImpl<C, Void, Unchecked, Unchecked, Unchecked> constructor = new HackedMethodImpl<>(this, null, 0);
			return constructor.fallbackReturning(null);	// Always fallback to null.
		}

		@SuppressWarnings("unchecked") private Class<C> getRawClass() {
			try {
				return mClass != null ? mClass : (mClass = (Class<C>) Class.forName(mClassName));
			} catch (final ClassNotFoundException e) {
				fail(new AssertionException(e));
				return (Class) ANY_TYPE;		// Use AnyType as a lazy trick to make fallback working and avoid null.
			}
		}

		HackedClass(final Class<C> clazz) { mClass = clazz; mClassName = null; }
		HackedClass(final String classname) { mClassName = classname; }

		private @Nullable Class<C> mClass;
		private final String mClassName;
	}

	public static <T> HackedClass<T> into(final @NonNull Class<T> clazz) {
		return new HackedClass<>(clazz);
	}

	public static <T> HackedClass<T> into(final String class_name) {
		return new HackedClass<>(class_name);
	}

	@SuppressWarnings("unchecked") public static <C> Hack.HackedClass<C> onlyIf(final boolean condition, final Hacking<Hack.HackedClass<C>> hacking) {
		if (condition) return hacking.hack();
		return (Hack.HackedClass<C>) FALLBACK;
	}
	public interface Hacking<T> { T hack(); }
	private static final Hack.HackedClass<?> FALLBACK = new HackedClass<>(ANY_TYPE);

	public static ConditionalHack onlyIf(final boolean condition) {
		return condition ? new ConditionalHack() {
			@Override public <T> HackedClass<T> into(@NonNull final Class<T> clazz) {
				return Hack.into(clazz);
			}
			@Override public <T> HackedClass<T> into(final String class_name) {
				return Hack.into(class_name);
			}
		} : new ConditionalHack() {
			@SuppressWarnings("unchecked") @Override public <T> HackedClass<T> into(@NonNull final Class<T> clazz) {
				return (HackedClass<T>) FALLBACK;
			}
			@SuppressWarnings("unchecked") @Override public <T> HackedClass<T> into(final String class_name) {
				return (HackedClass<T>) FALLBACK;
			}
		};
	}
	public interface ConditionalHack {
		/** WARNING: Never use this method if the target class may not exist when the condition is not met, use {@link #onlyIf(boolean, Hacking)} instead. */
		<T> HackedClass<T> into(final @NonNull Class<T> clazz);
		<T> HackedClass<T> into(final String class_name);
	}

	private static void fail(final AssertionException e) {
		if (sAssertionFailureHandler != null) sAssertionFailureHandler.onAssertionFailure(e);
	}

	/** Specify a handler to deal with assertion failure, and decide whether the failure should be thrown. */
	public static AssertionFailureHandler setAssertionFailureHandler(final @Nullable AssertionFailureHandler handler) {
		final AssertionFailureHandler old = sAssertionFailureHandler;
		sAssertionFailureHandler = handler;
		return old;
	}

	private Hack() {}

	private static AssertionFailureHandler sAssertionFailureHandler;

	/** This is a simple demo for the common usage of {@link Hack} */
	@SuppressWarnings("unused")
	private static class Demo {

		@SuppressWarnings({"FieldCanBeLocal", "UnnecessarilyQualifiedStaticUsage"})
		static class Hacks {

			static {
				Hack.setAssertionFailureHandler(new AssertionFailureHandler() { @Override public void onAssertionFailure(@NonNull final AssertionException failure) {
					Log.w("Demo", "Partially incompatible: " + failure.getDebugInfo());
					// Report the incompatibility silently.
					//...
				}});
				Demo_ctor = Hack.into(Demo.class).constructor().withParam(int.class);
				// Method without fallback (will be null if absent)
				Demo_methodThrows = Hack.into(Demo.class).method("methodThrows").returning(Void.class).fallbackReturning(null)
						.throwing(InterruptedException.class, IOException.class).withoutParams();
				// Method with fallback (will never be null)
				Demo_staticMethod = Hack.into(Demo.class).staticMethod("methodWith2Params").returning(boolean.class)
						.fallbackReturning(false).withParams(int.class, String.class);
				Demo_mField = Hack.into(Demo.class).field("mField").fallbackTo(false);
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

		@SuppressWarnings("RedundantThrows") private void methodThrows() throws InterruptedException, IOException {}
		static boolean staticMethod(final int a, final String c) { return false; }
		boolean mField;
		static String sField;
	}

	private static final Map<Class<?>/* mirror class */, Class<?>/* source class or Object.class for null */> sMirrorCache = new HashMap<>();
	private static final Map<String, Method> sStaticMethodCache = new HashMap<>();
	private static final Method NULL_METHOD;
	static { try { NULL_METHOD = Object.class.getMethod("toString"); } catch (final NoSuchMethodException e) { throw new LinkageError(); }}

	private static final String TAG = "Hack";
}
