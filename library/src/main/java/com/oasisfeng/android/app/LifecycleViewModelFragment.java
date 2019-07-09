package com.oasisfeng.android.app;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;

/**
 * Extends the {@link LifecycleFragment} with compatibility for view-model in Android Jet Pack.
 *
 * Created by Oasis on 2019-7-9.
 */
public class LifecycleViewModelFragment extends LifecycleFragment implements ViewModelStoreOwner {

	@Override public @NonNull ViewModelStore getViewModelStore() {
		if ((SDK_INT >= M ? getContext() : getActivity()) == null) throw new IllegalStateException("Can't access ViewModels from detached fragment");
		if (mViewModelStore == null) mViewModelStore = new ViewModelStore();
		return mViewModelStore;
	}

	private ViewModelStore mViewModelStore;		// TODO: Retain across configuration changes
}
