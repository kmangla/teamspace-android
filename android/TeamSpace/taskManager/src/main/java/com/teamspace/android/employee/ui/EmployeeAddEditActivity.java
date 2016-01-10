package com.teamspace.android.employee.ui;

import com.teamspace.android.common.ui.SingleFragmentActivity;
import com.teamspace.android.utils.Constants;

import android.support.v4.app.Fragment;

public class EmployeeAddEditActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() {
        boolean editMode = getIntent().getStringExtra(Constants.EMPLOYEE_ID) != null ? true : false;
        if (editMode) {
            return new EmployeeAddEditFragment();
        } else {
            return new ContactsListFragment();
        }
	}

}
