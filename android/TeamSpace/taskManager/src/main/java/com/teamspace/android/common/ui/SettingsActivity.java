package com.teamspace.android.common.ui;

import android.support.v4.app.Fragment;

public class SettingsActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() {
		return new SettingsFragment();
	}

}
