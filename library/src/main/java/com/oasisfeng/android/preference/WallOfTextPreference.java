/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oasisfeng.android.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

public class WallOfTextPreference extends Preference {

	@SuppressWarnings("unused") public WallOfTextPreference(final Context context) { super(context); }
	@SuppressWarnings("unused") public WallOfTextPreference(final Context context, final AttributeSet attrs) { super(context, attrs); }
	@SuppressWarnings("unused") public WallOfTextPreference(final Context context, final AttributeSet attrs, final int defStyleAttr) { super(context, attrs, defStyleAttr); }
	@TargetApi(LOLLIPOP) public WallOfTextPreference(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) { super(context, attrs, defStyleAttr, defStyleRes); }

	@Override protected void onBindView(final View view) {
		super.onBindView(view);
		final TextView summary = (TextView) view.findViewById(android.R.id.summary);
		summary.setMaxLines(20);
		setSelectable(false);
	}
}