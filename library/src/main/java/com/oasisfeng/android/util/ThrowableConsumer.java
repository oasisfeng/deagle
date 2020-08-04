package com.oasisfeng.android.util;

/**
 * {@link java.util.function.Consumer} with {@link Throwable}
 *
 * Created by Oasis on 2016/11/24.
 */
public interface ThrowableConsumer <T, E extends Throwable> {
	void accept(T t) throws E;
}