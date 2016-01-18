package com.teamspace.android.common.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.teamspace.android.R;
import com.teamspace.android.caching.DataManager;
import com.teamspace.android.caching.DataManagerCallback;
import com.teamspace.android.employee.ui.EmployeeAddEditActivity;
import com.teamspace.android.models.MetricsObject;
import com.teamspace.android.models.MigratedEmployee;
import com.teamspace.android.models.MigratedTask;
import com.teamspace.android.registration.ui.PhoneNumberRegistrationActivity;
import com.teamspace.android.tasklist.ui.AllTasksListViewActivity;
import com.teamspace.android.tasklist.ui.TaskAddEditActivity;
import com.teamspace.android.tasklist.ui.TaskAddViewPagerActivity;
import com.teamspace.android.utils.Constants;
import com.teamspace.android.utils.Utils;

import java.util.ArrayList;

/**
 * Created by vivek on 2/22/15.
 */
public class LauncherActivity extends FragmentActivity {
    final Context context = this;
    EMPLOYEE employeeState;
    TASK taskState;
    private long loopTime;
    private int TASK_CREATION_THRESHOLD = 2;

    enum EMPLOYEE {
        UNKNOWN,
        CREATED,
        NOT_CREATED
    }

    enum TASK {
        UNKNOWN,
        CREATED_LESS_THAN_THRESHOLD,
        CREATED_MORE_THAN_THRESHOLD,
        NOT_CREATED
    }

    enum STATE {
        LOOP,
        LOGIN,
        CREATE_EMPLOYEE_AND_TASK,
        CREATE_TASK,
        VIEW_TASK,
        CREATE_EMP
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        fetchTasksAndEmployees();
        checkStateAndLoopIfNeeded(getIntent());
    }

    private void fireLaunchMetrics(String pageName) {
        // Fire metric if from push
        if (getIntent().getExtras() != null &&
                getIntent().getExtras().getBoolean(Constants.IS_FROM_PUSH)) {
            DataManager.getInstance(this).fireMetric(new MetricsObject(pageName, "open_from_push"));
        } else {
            DataManager.getInstance(this).fireMetric(new MetricsObject(pageName, "open"));
        }
    }

    private void checkStateAndLoopIfNeeded(final Intent intent) {
        loopTime = System.currentTimeMillis();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                STATE state = fetchStateFromStateMachine(intent);

                switch (state) {
                    case LOGIN:
                        fireLaunchMetrics("LoginPage");
                        Intent i = new Intent(context, PhoneNumberRegistrationActivity.class);
                        startActivity(i);
                        finish();
                        break;
                    case CREATE_EMPLOYEE_AND_TASK:
                        fireLaunchMetrics("EmployeeCreationPage");
                        Intent []j = new Intent[3];
                        j[0] = new Intent(context, AllTasksListViewActivity.class);
                        j[1] = new Intent(context, TaskAddViewPagerActivity.class);
                        j[2] = new Intent(context, EmployeeAddEditActivity.class);
                        startActivities(j);
                        finish();
                        break;
                    case CREATE_TASK:
                        fireLaunchMetrics("TaskCreationPage");
                        Intent []k = new Intent[2];
                        k[0] = new Intent(context, AllTasksListViewActivity.class);
                        k[1] = new Intent(context, TaskAddViewPagerActivity.class);
                        startActivities(k);
                        finish();
                        break;
                    case VIEW_TASK:
                        fireLaunchMetrics("TaskListPage");
                        Intent l = new Intent(context, AllTasksListViewActivity.class);
                        startActivity(l);
                        finish();
                        break;
                    case CREATE_EMP:
                        fireLaunchMetrics("EmployeeCreationPage");
                        Intent []m = new Intent[3];
                        m[0] = new Intent(context, AllTasksListViewActivity.class);
                        m[2] = new Intent(context, EmployeeAddEditActivity.class);
                        startActivities(m);
                        finish();
                        break;
                    default:
                        checkStateAndLoopIfNeeded(intent);
                }
            }
        }, 1000);
    }

    private STATE fetchStateFromStateMachine(Intent intent) {
        if (Utils.isStringEmpty(Utils.getSignedInUserId())) {
            return STATE.LOGIN;
        }

        if (System.currentTimeMillis() - loopTime > 10000) {
            return STATE.VIEW_TASK;
        }

        if (intent.getExtras() != null) {
            String deepLink = intent.getExtras().getString(Constants.DEEPLINK);
            if (Utils.isStringNotEmpty(deepLink)) {
                if (deepLink.equalsIgnoreCase(Constants.TASK_CREATION)) {
                    return STATE.CREATE_TASK;
                } else if (deepLink.equalsIgnoreCase(Constants.EMP_CREATION)) {
                    return STATE.CREATE_EMP;
                }
            }
        }

        if (employeeState == EMPLOYEE.UNKNOWN || taskState == TASK.UNKNOWN) {
            return STATE.LOOP;
        }

        if (employeeState == EMPLOYEE.NOT_CREATED) {
            return STATE.CREATE_EMPLOYEE_AND_TASK;
        }

        if (taskState == TASK.NOT_CREATED || taskState == TASK.CREATED_LESS_THAN_THRESHOLD) {
            return STATE.CREATE_TASK;
        }

        return STATE.VIEW_TASK;

    }

    private void fetchTasksAndEmployees() {
        employeeState = EMPLOYEE.UNKNOWN;
        taskState = TASK.UNKNOWN;

        // Load tasks from cache or network to figure out if the user has created any tasks
        DataManager.getInstance(this).fetchTasksForUser(Utils.getSignedInUserId(),
            new DataManagerCallback() {
                @Override
                public void onDataReceivedFromCache(String dataStoreKey) {
                    if (dataStoreKey == null) {
                        return;
                    }

                    ArrayList<MigratedTask> data = (ArrayList<MigratedTask>) DataManager
                            .getInstance(context).retrieveData(
                                    dataStoreKey);

                    if (data.size() > TASK_CREATION_THRESHOLD) {
                        taskState = TASK.CREATED_MORE_THAN_THRESHOLD;
                    } else if (data.size() > 0) {
                        taskState = TASK.CREATED_LESS_THAN_THRESHOLD;
                    }
                }

                @Override
                public void onDataReceivedFromServer(String dataStoreKey) {
                    if (dataStoreKey == null) {
                        return;
                    }
                    ArrayList<MigratedTask> data = (ArrayList<MigratedTask>) DataManager
                            .getInstance(context).retrieveData(
                                    dataStoreKey);

                    if (data.size() > TASK_CREATION_THRESHOLD) {
                        taskState = TASK.CREATED_MORE_THAN_THRESHOLD;
                    } else if (data.size() > 0) {
                        taskState = TASK.CREATED_LESS_THAN_THRESHOLD;
                    } else {
                        taskState = TASK.NOT_CREATED;
                    }
                }
            });

        // Load employees from cache or network to figure out if the user has created any employees
        DataManager.getInstance(this).fetchEmployeesForUser(Utils.getSignedInUserId(),
                new DataManagerCallback() {

                    @Override
                    public void onDataReceivedFromCache(String dataStoreKey) {
                        if (dataStoreKey == null) {
                            return;
                        }
                        ArrayList<MigratedEmployee> data = (ArrayList<MigratedEmployee>) DataManager
                                .getInstance(context).retrieveData(
                                        dataStoreKey);

                        if (data.size() > 0) {
                            employeeState = EMPLOYEE.CREATED;
                        }
                    }

                    @Override
                    public void onDataReceivedFromServer(String dataStoreKey) {
                        if (dataStoreKey == null) {
                            return;
                        }
                        ArrayList<MigratedEmployee> data = (ArrayList<MigratedEmployee>) DataManager
                                .getInstance(context).retrieveData(
                                        dataStoreKey);

                        if (data.size() > 0) {
                            employeeState = EMPLOYEE.CREATED;
                        } else {
                            employeeState = EMPLOYEE.NOT_CREATED;
                        }
                    }
                });
    }
}
