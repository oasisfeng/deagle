package com.oasisfeng.android.databinding;

import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.appcompat.widget.Toolbar;

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
