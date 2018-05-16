package com.oasisfeng.androidx.lifecycle;

import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import java.util.Objects;

/**
 * Created by Oasis on 2018/5/14.
 */
public class NonNullMutableLiveData<T> extends MutableLiveData<T> {

	public NonNullMutableLiveData(final @NonNull T value) {
		super.setValue(Objects.requireNonNull(value));
	}

	@Override public void setValue(final @NonNull T value) {
		super.setValue(Objects.requireNonNull(value));
	}

	@SuppressWarnings("ConstantConditions") @NonNull @Override public T getValue() {
		return super.getValue();
	}
}
