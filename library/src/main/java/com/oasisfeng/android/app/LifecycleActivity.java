package com.oasisfeng.android.app;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

/**
 * Extends the native {@link Activity} with compatibility for lifecycle in Android Jet Pack.
 *
 * Created by Oasis on 2018/5/17.
 */
public class LifecycleActivity extends Activity implements LifecycleOwner {

	/* Lifecycle */

	@Override public @NonNull Lifecycle getLifecycle() { return mLifecycleRegistry; }

	@Override protected void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
	}
	@Override protected void onStart() {
		super.onStart();
		mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
	}
	@Override protected void onResume() {
		super.onResume();
		mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
	}
	@Override protected void onPause() {
		super.onPause();
		mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
	}
	@Override protected void onStop() {
		super.onStop();
		mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
	}
	@Override protected void onDestroy() {
		super.onDestroy();
		mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
	}

	private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
}
