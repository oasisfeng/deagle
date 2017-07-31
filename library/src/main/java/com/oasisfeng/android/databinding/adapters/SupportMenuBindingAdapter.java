package com.oasisfeng.android.databinding.adapters;

import android.databinding.BindingAdapter;
import android.support.annotation.MenuRes;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPresenter;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;

/**
 * Binding adapter for menu in {@link Toolbar} and {@link ActionMenuView} (support-v7 version)
 *
 * Created by Oasis on 2016/12/28.
 */
public class SupportMenuBindingAdapter {

	public interface MenuAware {
		void onBindMenu(final Menu menu);
		void onShowOverflowMenu(final Menu menu);
	}

	@BindingAdapter(value = {"menu", "menuAware"}, requireAll = false)
	public static void inflateMenu(final Toolbar toolbar, final @MenuRes int last_menu_res, final MenuAware last_menu_aware,
												final @MenuRes int menu_res, final MenuAware menu_aware) {
		if (menu_aware != last_menu_aware && menu_aware != null) {		// setMenuCallbacks() must be called before getMenu() to take effect.
			toolbar.setMenuCallbacks(new MenuPresenter.Callback() {

				@Override public boolean onOpenSubMenu(final MenuBuilder subMenu) {
					if (subMenu == null) menu_aware.onShowOverflowMenu(toolbar.getMenu());
					return false;
				}

				@Override public void onCloseMenu(final MenuBuilder menu, final boolean allMenusAreClosing) {}
			}, null);
		}

		if (menu_res != last_menu_res && menu_res != 0) {
			toolbar.getMenu().clear();
			toolbar.inflateMenu(menu_res);
		}

		if (menu_aware != last_menu_aware && menu_aware != null) {
			final Menu menu = toolbar.getMenu();
			menu_aware.onBindMenu(menu);
		}
	}

	@BindingAdapter(value = {"menu", "menuAware"}, requireAll = false)
	public static void inflateMenu(final ActionMenuView amv, final @MenuRes int last_menu_res, final MenuAware last_menu_aware,
								   final @MenuRes int menu_res, final MenuAware menu_aware) {
		if (menu_aware != last_menu_aware && menu_aware != null) {		// setMenuCallbacks() must be called before getMenu() to take effect.
			amv.setMenuCallbacks(new MenuPresenter.Callback() {

				@Override public boolean onOpenSubMenu(final MenuBuilder subMenu) {
					if (subMenu == null) menu_aware.onShowOverflowMenu(amv.getMenu());
					return false;
				}

				@Override public void onCloseMenu(final MenuBuilder menu, final boolean allMenusAreClosing) {}
			}, null);
		}

		if (menu_res != last_menu_res && menu_res != 0) {
			final Menu menu = amv.getMenu();
			menu.clear();
			new MenuInflater(amv.getContext()).inflate(menu_res, menu);
		}

		if (menu_aware != last_menu_aware && menu_aware != null) {
			final Menu menu = amv.getMenu();
			menu_aware.onBindMenu(menu);
		}
	}
}
