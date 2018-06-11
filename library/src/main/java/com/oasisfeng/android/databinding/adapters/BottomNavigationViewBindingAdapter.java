package com.oasisfeng.android.databinding.adapters;

import android.databinding.BindingAdapter;
import android.databinding.InverseBindingListener;
import android.databinding.InverseBindingMethod;
import android.databinding.InverseBindingMethods;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener;
import android.view.MenuItem;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

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
