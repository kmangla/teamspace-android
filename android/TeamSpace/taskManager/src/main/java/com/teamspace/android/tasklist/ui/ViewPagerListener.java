package com.teamspace.android.tasklist.ui;

import com.teamspace.android.models.MigratedEmployee;

/**
 * Created by pratyus on 1/17/16.
 */
public interface ViewPagerListener {
    public void employeeSelected(MigratedEmployee employee);
    public MigratedEmployee getSelectedEmployee();
}
