/**
 * Fragment for Creating/Updating information about an Employee
 */

package com.teamspace.android.tasklist.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.teamspace.android.R;
import com.teamspace.android.caching.DataManager;
import com.teamspace.android.caching.DataManagerCallback;
import com.teamspace.android.caching.DatabaseCache;
import com.teamspace.android.common.ui.TaskManagerApplication;
import com.teamspace.android.models.MigratedEmployee;
import com.teamspace.android.models.MigratedTask;
import com.teamspace.android.utils.Constants;
import com.teamspace.android.utils.Utils;

public class TaskAddEditFragment extends Fragment {

	private MigratedTask task;
	EditText taskTitleEditText;
    TextView pageSubtitle;
    RadioButton dailyButton;
    RadioButton monthlyButton;
    RadioButton weeklyButton;
    CheckBox createMultiple;
    TextView createMultipleText;

	Button saveButton;
    Button saveDraft;

	String taskId = "";
	String taskTitle = "";
	String taskEmployeeName = "";
    MigratedEmployee selectedEmployee;
    String mFilteredEmployeeId;

	String[] allEmployeeNames;
	private boolean hideKeyboard;
	private boolean editMode;
    private static final int ADD_EMPLOYEE = 0;
    private int prevPosition = 0;
    private TextView pageTitle;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Since we need to fetch task data from the database which might
		// require some time
		// and must be done on background thread, we allow the caller to pass us
		// few basic
		// information like task title which we can instantly show in the UI
		// while more details about the task are fetched from the database.
		Bundle extras = getActivity().getIntent().getExtras();
		if (extras != null) {
			taskId = extras.getString(Constants.TASK_ID);
			taskTitle = extras.getString(Constants.TASK_TITLE);
			taskEmployeeName = extras.getString(Constants.EMPLOYEE_NAME);
            mFilteredEmployeeId = extras.getString(Constants.FILTERED_EMP_ID);
			hideKeyboard = extras.getBoolean(Constants.HIDE_KEYBOARD);
			if (Utils.isStringNotEmpty(taskId)) {
				editMode = true;
			}
            if (Utils.isStringNotEmpty(mFilteredEmployeeId)) {
                selectedEmployee =
                        DatabaseCache.getInstance(getActivity()).
                                getMigratedEmployeeBlockingCall(mFilteredEmployeeId);
            }
		}

		final TaskManagerApplication applicationContext = ((TaskManagerApplication) getFragmentActivity()
				.getApplication());
		if (Utils.isStringNotEmpty(taskId)) {
			AsyncTask<String, Void, MigratedTask> asyncTask = new AsyncTask<String, Void, MigratedTask>() {
				@Override
				protected MigratedTask doInBackground(String... taskIds) {
					Utils.log("Trying to fetch task details from database cache for task id: "
							+ taskIds[0]);
					return DatabaseCache.getInstance(applicationContext)
							.getMigratedTaskBlockingCall(taskIds[0]);
				}

				@Override
				protected void onPostExecute(MigratedTask cachedTask) {
					task = cachedTask;
					refreshUIForTask();
				}
			};
			asyncTask.execute(taskId);
		} else {
			task = new MigratedTask();
		}

		getActivity().setTitle(R.string.task_detail_page);
		setRetainInstance(true);
	}

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (getActivity() instanceof ViewPagerListener) {
                selectedEmployee = ((ViewPagerListener) getActivity()).getSelectedEmployee();
                if (selectedEmployee != null) {
                    pageSubtitle.setText(getString(R.string.add_task) + " " + selectedEmployee.getName());
                    if (!editMode) {
                        taskTitleEditText.setText(selectedEmployee.getTaskBlob());
                    }
                }
            }
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.task_add_fragment, container, false);

		taskTitleEditText = (EditText) v.findViewById(R.id.task_title);

		if (Utils.isStringNotEmpty(taskTitle)) {
			taskTitleEditText.setText(taskTitle);
		} else if (!editMode && selectedEmployee != null) {
            taskTitleEditText.setText(selectedEmployee.getTaskBlob());
        }

        dailyButton = (RadioButton) v.findViewById(R.id.daily);
        weeklyButton = (RadioButton) v.findViewById(R.id.weekly);
        monthlyButton = (RadioButton) v.findViewById(R.id.monthly);
        dailyButton.setChecked(true);
        pageSubtitle = (TextView) v.findViewById(R.id.page_subtitle);
        pageTitle = (TextView) v.findViewById(R.id.page_title);

        createMultiple = (CheckBox) v.findViewById(R.id.create_multiple);
        createMultipleText = (TextView) v.findViewById(R.id.create_multiple_text);
        createMultiple.setChecked(true);

		saveButton = (Button) v.findViewById(R.id.save_button);
        saveDraft = (Button) v.findViewById(R.id.draft_button);

        saveDraft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                updateTaskBlobForEmployee(taskTitleEditText.getText().toString(), selectedEmployee);
                getFragmentActivity().finish();
            }
        });

        if (mFilteredEmployeeId != null && selectedEmployee != null) {
            pageTitle.setText(getString(R.string.add_task) + " " + selectedEmployee.getName());
            pageSubtitle.setVisibility(View.GONE);
        }

        if (editMode) {
            createMultiple.setVisibility(View.GONE);
            createMultipleText.setVisibility(View.GONE);
            createMultiple.setChecked(false);
            saveButton.setText(R.string.save_now);
            saveDraft.setVisibility(View.GONE);
        }

		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
                createTasksForEmployee(v);
            }
		});

		return v;
	}

    private void createTasksForEmployee(final View v) {
        if (selectedEmployee == null) {
            // Notify user about the error
            Toast.makeText(
                    v.getContext(),
                    v.getContext()
                            .getResources()
                            .getString(
                                    R.string.create_employee_before_task),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        TaskManagerApplication applicationContext = ((TaskManagerApplication) getFragmentActivity()
                .getApplication());
        DataManager dataMgr = DataManager
                .getInstance(applicationContext);

        if (Utils.isStringEmpty(selectedEmployee.getEmployeeID()) &&
                Utils.isStringNotEmpty(selectedEmployee.getPhoneWithCountryCode()) &&
                Utils.isStringNotEmpty(selectedEmployee.getName())) {
            MigratedEmployee actualEmp = DatabaseCache.getInstance(getActivity()).
                    getMigratedEmployeeByPhoneAndName(selectedEmployee.getPhoneWithCountryCode(), selectedEmployee.getName());
            if (actualEmp != null) {
                selectedEmployee.setEmployeeID(actualEmp.getEmployeeID());
            }
        }

        // Clear the draft if any for this employee since we are now creating the tasks for real
        final String taskBlobBackup = taskTitleEditText.getText().toString();
        updateTaskBlobForEmployee("", selectedEmployee);

        // If somehow the task is assigned to "Add New Employee", do nothing.
        if (Utils.isStringEmpty(selectedEmployee.getEmployeeID()) || selectedEmployee.getEmployeeID().equalsIgnoreCase("0000")) {
            // Notify user about the error
            Toast.makeText(
                    v.getContext(),
                    v.getContext()
                            .getResources()
                            .getString(
                                    R.string.error_employee_not_valid),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String key = "newTask:" + System.nanoTime();
        ArrayList<MigratedTask> data = new ArrayList<MigratedTask>();
        DataManager.getInstance(getActivity()).insertData(key, data);

        String longTitle = taskTitleEditText.getText().toString();
        String [] split = longTitle.split("\n");
        if (!createMultiple.isChecked()) {
            split = new String[1];
            split[0] = longTitle;
        }

        for (int i = 0; i < split.length; i ++) {
            String tempTitle = split[i];
            tempTitle = tempTitle.trim();
            if (tempTitle.length() == 0) {
                continue;
            }

            task.setTitle(tempTitle);
            task.setEmployeeID(selectedEmployee.getEmployeeID());
            task.setEmployeeName(selectedEmployee.getName());
            task.setEmployeeNumber(selectedEmployee.getPhoneWithCountryCode());

            long freq;

            // Convert freq to seconds
            if (dailyButton.isChecked()) {
                freq = Constants.DAILY;
            } else if (weeklyButton.isChecked()) {
                freq = Constants.WEEKLY;
            } else {
                freq = Constants.MONTHLY;
            }

            task.setFrequency(freq);

            if (Utils.isStringNotEmpty(taskId)) {
                task.setTaskID(taskId);
                data.add(task);
                dataMgr.updateTask(task, new DataManagerCallback() {

                    @Override
                    public void onSuccess(String response) {
                    }

                    @Override
                    public void onFailure(String response) {
                        // Notify user about the error
                        Utils.trackEvent("task", "update", "network_fail");
                        Toast.makeText(
                                v.getContext(),
                                v.getContext()
                                        .getResources()
                                        .getString(
                                                R.string.error_task_update_failed),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                task.setTaskID(Constants.EMPTY_STRING);
                task.setStatus(Constants.OPEN);
                task.setUpdateCount(-1);
                task.setLastUpdate(System.currentTimeMillis());
                data.add(task);
                dataMgr.createTask(task, new DataManagerCallback() {

                    @Override
                    public void onSuccess(String response) {
                    }

                    @Override
                    public void onFailure(String response) {
                        // Notify user about the error
                        Utils.trackEvent("task", "create", "network_fail");
                        Toast.makeText(
                                v.getContext(),
                                v.getContext()
                                        .getResources()
                                        .getString(
                                                R.string.error_task_create_failed),
                                Toast.LENGTH_SHORT).show();

                        // If we were not able to create the tasks, make sure the draft is not lost
                        Utils.writeStringToSharedPrefs(Constants.EMPLOYEE_DRAFT + selectedEmployee.getEmployeeID(), taskBlobBackup);
                    }
                });
            }

            task = new MigratedTask();
        }

        Intent newIntent = new Intent();
        newIntent.putExtra(Constants.TASK_ARRAY, key);

        if (editMode) {
            Utils.trackEvent("Tracking", "TaskEdited",
                    "TaskAddEditFragment:saveButtonClicked");
        } else {
            Utils.trackEvent("Tracking", "TaskAdded",
                    "TaskAddEditFragment:saveButtonClicked");
        }

        // Refresh the main task list to display this new task
        DataManager.getInstance(getActivity()).insertData(Constants.REFRESH_ALL_TASKS, true);

        taskTitleEditText.setText("");
        getFragmentActivity().setResult(Activity.RESULT_OK, newIntent);
        getFragmentActivity().finish();
    }

    private void updateTaskBlobForEmployee(String blobToCache, MigratedEmployee emp) {
        if (selectedEmployee == null) {
            // Notify user about the error
            Toast.makeText(
                    getActivity(),
                    getActivity()
                            .getResources()
                            .getString(
                                    R.string.create_employee_before_task),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        emp.setTaskBlob(blobToCache);
        DataManager dataMgr = DataManager.getInstance(getActivity().getApplicationContext());
        dataMgr.updateEmployee(emp, null);

        // Also save this locally in case there is no network
        Utils.writeStringToSharedPrefs(Constants.EMPLOYEE_DRAFT + emp.getEmployeeID(), blobToCache);
    }

    @Override
    public void onPause() {
        if (!editMode && selectedEmployee != null) {
            updateTaskBlobForEmployee(taskTitleEditText.getText().toString(), selectedEmployee);
        }
        super.onPause();
    }

    @Override
    public void onResume() {
		super.onResume();

		// Check if we need to show the keyboard
		if (hideKeyboard) {
			InputMethodManager imm = (InputMethodManager) getActivity()
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(taskTitleEditText.getWindowToken(), 0);
		} else {
			InputMethodManager imm = (InputMethodManager) getActivity()
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(taskTitleEditText, 0);
		}

        if (editMode) {
            Utils.trackPageView(Constants.TASK_EDIT_PV);
        } else {
            Utils.trackPageView(Constants.TASK_CREATION_PV);
        }
	}

	private void refreshUIForTask() {
		taskTitleEditText.setText(task.getTitle());

        if (selectedEmployee != null) {
            pageSubtitle.setText(getString(R.string.add_task) + " " + selectedEmployee.getName());
        }
		// Convert seconds to frequency (daily, weekly, monthly etc.)
		if (task.getFrequency() <= Constants.DAILY) {
            dailyButton.setChecked(true);
            weeklyButton.setChecked(false);
            monthlyButton.setChecked(false);
		} else if (task.getFrequency() <= Constants.WEEKLY) {
            dailyButton.setChecked(false);
            weeklyButton.setChecked(true);
            monthlyButton.setChecked(false);
		} else {
            dailyButton.setChecked(false);
            weeklyButton.setChecked(false);
            monthlyButton.setChecked(true);
		}
	}

	public FragmentActivity getFragmentActivity() {
		return this.getActivity();
	}
}
