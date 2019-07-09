package com.oasisfeng.android.app;

import android.app.Fragment;
import android.os.Bundle;

import javax.annotation.ParametersAreNonnullByDefault;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

/**
 * Extends the native {@link Fragment} with compatibility for lifecycle in Android Jet Pack.
 *
 * Created by Oasis on 2018/5/17.
 */
@ParametersAreNonnullByDefault
public class LifecycleFragment extends Fragment implements LifecycleOwner {

	@Override public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	/* Lifecycle */

	@Override public @NonNull Lifecycle getLifecycle() { return mLifecycleRegistry; }

	@Override public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
	}
	@Override public void onStart() {
		super.onStart();
		mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
	}
	@Override public void onResume() {
		super.onResume();
		mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
	}
	@Override public void onPause() {
		super.onPause();
		mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
	}
	@Override public void onStop() {
		super.onStop();
		mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
	}
	@Override public void onDestroy() {
		super.onDestroy();
		mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
	}

	private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
}
