package com.teamspace.android.employee.ui;

import com.teamspace.android.common.ui.SingleFragmentActivity;

import android.support.v4.app.Fragment;

public class EmployeeAddEditActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() {
		if (true) {
            return new EmployeeAddEditFragment();
        } else {
            return new EmployeeAddEditMultiFragment();
        }
	}

}
