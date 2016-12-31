package com.oasisfeng.android.databinding;

import android.databinding.BindingMethod;
import android.databinding.BindingMethods;
import android.support.v7.widget.Toolbar;

/**
 * Binding adapter for {@link Toolbar} (support-library version)
 *
 * Created by Oasis on 2016/12/28.
 */
@BindingMethods({
		@BindingMethod(type = Toolbar.class, attribute = "onMenuItemClick", method = "setOnMenuItemClickListener"),
		@BindingMethod(type = Toolbar.class, attribute = "onNavigationClick", method = "setNavigationOnClickListener"),
})
public class SupportToolbarBindingAdapter {}
