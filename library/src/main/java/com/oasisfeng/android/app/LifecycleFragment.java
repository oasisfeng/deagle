package com.oasisfeng.android.app;

import android.app.Fragment;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.ViewModelStore;
import android.arch.lifecycle.ViewModelStoreOwner;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;

/**
 * Extends the native {@link Fragment} with compatibility for lifecycle and view-model in Android Jet Pack.
 *
 * Created by Oasis on 2018/5/17.
 */
@ParametersAreNonnullByDefault
public class LifecycleFragment extends Fragment implements LifecycleOwner, ViewModelStoreOwner {

	@Override public @NonNull ViewModelStore getViewModelStore() {
		if ((SDK_INT >= M ? getContext() : getActivity()) == null) throw new IllegalStateException("Can't access ViewModels from detached fragment");
		if (mViewModelStore == null) mViewModelStore = new ViewModelStore();
		return mViewModelStore;
	}

	private ViewModelStore mViewModelStore;		// TODO: Retain across configuration changes

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
