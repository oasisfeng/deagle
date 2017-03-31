package com.oasisfeng.android.databinding.adapters;

import android.databinding.BindingAdapter;
import android.support.annotation.MenuRes;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ActionMenuView;
import android.widget.Toolbar;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Binding adapter for menu in {@link Toolbar} and {@link ActionMenuView}
 *
 * Created by Oasis on 2016/11/30.
 */
@SuppressWarnings("unused")
public class MenuBindingAdapter {

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
