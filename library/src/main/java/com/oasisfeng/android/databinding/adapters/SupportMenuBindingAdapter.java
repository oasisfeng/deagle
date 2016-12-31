package com.oasisfeng.android.databinding.adapters;

import android.databinding.BindingAdapter;
import android.support.annotation.MenuRes;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Binding adapter for menu in {@link Toolbar} and {@link ActionMenuView} (support-v7 version)
 *
 * Created by Oasis on 2016/12/28.
 */
public class SupportMenuBindingAdapter {

	@BindingAdapter("menu") public static void inflateMenu(final Toolbar toolbar, final @MenuRes int old_menu, final @MenuRes int new_menu) {
		if (SDK_INT < LOLLIPOP) return;
		if (new_menu == old_menu) return;
		final Menu menu = toolbar.getMenu();
		menu.clear();
		toolbar.inflateMenu(new_menu);
	}

	@BindingAdapter("menu") public static void inflateMenu(final ActionMenuView amv, final @MenuRes int old_menu, final @MenuRes int new_menu) {
		if (SDK_INT < LOLLIPOP) return;
		if (new_menu == old_menu) return;
		final Menu menu = amv.getMenu();
		menu.clear();
		new MenuInflater(amv.getContext()).inflate(new_menu, menu);
	}
}
