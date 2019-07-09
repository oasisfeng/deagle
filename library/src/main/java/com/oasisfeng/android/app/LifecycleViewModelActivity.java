package com.oasisfeng.android.app;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

/**
 * Extends the {@link LifecycleActivity} with compatibility for view-model in Android Jet Pack.
 *
 * Created by Oasis on 2019-7-9.
 */
public class LifecycleViewModelActivity extends LifecycleActivity implements ViewModelStoreOwner {

	@Override public @NonNull ViewModelStore getViewModelStore() {
		if (getApplication() == null) {
			throw new IllegalStateException("Your activity is not yet attached to the "
					+ "Application instance. You can't request ViewModel before onCreate call.");
		}
		if (mViewModelStore == null) {
			mViewModelStore = new ViewModelStore();
		}
		return mViewModelStore;
	}

	private ViewModelStore mViewModelStore;
}
