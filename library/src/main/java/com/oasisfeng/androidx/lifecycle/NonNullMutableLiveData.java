package com.oasisfeng.androidx.lifecycle;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

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
