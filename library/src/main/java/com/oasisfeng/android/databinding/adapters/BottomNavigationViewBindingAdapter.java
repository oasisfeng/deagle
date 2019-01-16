package com.oasisfeng.android.databinding.adapters;

import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingListener;
import androidx.databinding.InverseBindingMethod;
import androidx.databinding.InverseBindingMethods;

/**
 * Binding adapter for {@link BottomNavigationView}
 *
 * Created by Oasis on 2018/5/16.
 */
@ParametersAreNonnullByDefault @SuppressWarnings("unused") @InverseBindingMethods(
		@InverseBindingMethod(type = BottomNavigationView.class, attribute = "selectedItemId", event = "android:selectedItemIdAttrChanged"))
public class BottomNavigationViewBindingAdapter {

	@BindingAdapter(value = {"onNavigationItemSelected", "selectedItemIdAttrChanged" }, requireAll = false)
	public static void setOnItemSelectedListener(final BottomNavigationView view, final @Nullable OnNavigationItemSelectedListener listener,
												 final @Nullable InverseBindingListener notifier) {
		if (listener == null && notifier == null) view.setOnNavigationItemSelectedListener(null);
		else view.setOnNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {
			@Override public boolean onNavigationItemSelected(final MenuItem item) {
				if (view.getSelectedItemId() == item.getItemId()) return true;
				if (notifier != null) view.post(new Runnable() { @Override public void run() {
					notifier.onChange();			// Async to avoid infinite loop
				}});
				return listener == null || listener.onNavigationItemSelected(item);
			}
		});
	}
}
