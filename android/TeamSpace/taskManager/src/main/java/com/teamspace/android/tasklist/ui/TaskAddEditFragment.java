/**
 * Fragment for Creating/Updating information about an Employee
 */

package com.teamspace.android.tasklist.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.teamspace.android.R;
import com.teamspace.android.caching.DataManager;
import com.teamspace.android.caching.DataManagerCallback;
import com.teamspace.android.caching.DatabaseCache;
import com.teamspace.android.common.ui.TaskManagerApplication;
import com.teamspace.android.employee.ui.EmployeeAddEditActivity;
import com.teamspace.android.models.MigratedEmployee;
import com.teamspace.android.models.MigratedTask;
import com.teamspace.android.utils.Constants;
import com.teamspace.android.utils.Utils;

public class TaskAddEditFragment extends Fragment implements
		OnItemSelectedListener {

	private MigratedTask task;
	EditText taskTitleEditText;
    RadioButton dailyButton;
    RadioButton monthlyButton;
    RadioButton weeklyButton;
    CheckBox createMultiple;
    TextView createMultipleText;
	Spinner empSpinner;

	Button saveButton;
    Button saveDraft;

	String taskId = "";
	String taskTitle = "";
	String taskEmployeeName = "";
    String mFilteredEmployeeId;

	ArrayList<MigratedEmployee> allEmployees;
	String[] allEmployeeNames;
	private boolean hideKeyboard;
	private boolean editMode;
    private static final int ADD_EMPLOYEE = 0;

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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.task_add_fragment, container, false);

		taskTitleEditText = (EditText) v.findViewById(R.id.task_title);

		if (Utils.isStringNotEmpty(taskTitle)) {
			taskTitleEditText.setText(taskTitle);
		}

        dailyButton = (RadioButton) v.findViewById(R.id.daily);
        weeklyButton = (RadioButton) v.findViewById(R.id.weekly);
        monthlyButton = (RadioButton) v.findViewById(R.id.monthly);
        dailyButton.setChecked(true);

        createMultiple = (CheckBox) v.findViewById(R.id.create_multiple);
        createMultipleText = (TextView) v.findViewById(R.id.create_multiple_text);
        createMultiple.setChecked(true);

		// Add spinner for selecting employee
		empSpinner = (Spinner) v.findViewById(R.id.employee_spinner);
        populateEmployeeSpinner(v.getContext(), null);
		empSpinner.setOnItemSelectedListener(this);

        if (allEmployees.size() > 0 && editMode == false) {
            taskTitleEditText.setText(allEmployees.get(0).getTaskBlob());
        }

		// Create an ArrayAdapter using the frequency array
		ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter
				.createFromResource(getActivity(), R.array.frequency_array,
						R.layout.task_spinner_item_white);
		freqAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		saveButton = (Button) v.findViewById(R.id.save_button);
        saveDraft = (Button) v.findViewById(R.id.draft_button);

        saveDraft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                MigratedEmployee emp = allEmployees.get(empSpinner
                        .getSelectedItemPosition());
                updateTaskBlobForEmployee(taskTitleEditText.getText().toString(), emp, v, true);
            }
        });

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
        if (allEmployees == null || allEmployees.size() == 0) {
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

        MigratedEmployee emp = allEmployees.get(empSpinner
                .getSelectedItemPosition());
        if (Utils.isStringEmpty(emp.getEmployeeID()) &&
                Utils.isStringNotEmpty(emp.getPhoneWithCountryCode()) &&
                Utils.isStringNotEmpty(emp.getName())) {
            MigratedEmployee actualEmp = DatabaseCache.getInstance(getActivity()).
                    getMigratedEmployeeByPhoneAndName(emp.getPhoneWithCountryCode(), emp.getName());
            if (actualEmp != null) {
                emp.setEmployeeID(actualEmp.getEmployeeID());
            }
        }

        // Clear the draft if any for this employee since we are now creating the tasks for real
        updateTaskBlobForEmployee("", emp, v, false);

        // If somehow the task is assigned to "Add New Employee", do nothing.
        if (Utils.isStringEmpty(emp.getEmployeeID()) || emp.getEmployeeID().equalsIgnoreCase("0000")) {
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
            task.setEmployeeID(emp.getEmployeeID());
            task.setEmployeeName(emp.getName());
            task.setEmployeeNumber(emp.getPhoneWithCountryCode());

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

        getFragmentActivity().setResult(Activity.RESULT_OK, newIntent);
        getFragmentActivity().finish();
    }

    private void updateTaskBlobForEmployee(String blobToCache, MigratedEmployee emp, final View v, final boolean showToast) {
        if (allEmployees == null || allEmployees.size() == 0) {
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

        emp.setTaskBlob(blobToCache);
        DataManager dataMgr = DataManager.getInstance(getActivity().getApplicationContext());
        dataMgr.updateEmployee(emp, new DataManagerCallback() {

            @Override
            public void onSuccess(String response) {
                if (showToast) {
                    Toast.makeText(
                            v.getContext(),
                            v.getContext().getResources()
                                    .getString(
                                            R.string.task_draft_saved),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String response) {
                // Notify user about the error
                Toast.makeText(
                        v.getContext(),
                        v.getContext().getResources()
                                .getString(
                                        R.string.error_task_draft_failed),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateEmployeeSpinner(final Context context, MigratedEmployee newEmp) {
        allEmployees = DatabaseCache.getInstance(context)
                .getMigratedEmployeesBlockingCall();

        if (allEmployees.size() == 0) {
            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            alert.setTitle(context.getString(R.string.create_employee));
            alert.setMessage(context.getString(R.string.create_employee_before_task));

            alert.setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // If there are no employees, take the user to "add employee" screen.
                    Intent i = new Intent(context, EmployeeAddEditActivity.class);
                    context.startActivity(i);
                }
            });

            alert.show();
        }

//        MigratedEmployee self = new MigratedEmployee();
//        self.setEmployeeID(Utils.getSignedInUserId());
//        self.setName(Utils.getSignedInUserName());
//        self.setPhoneWithContryCode(Utils.getSignedInUserPhoneNumber());
//        allEmployees.add(0, self);

        if (newEmp != null) {
            allEmployees.add(0, newEmp);
        }

        // If we have been asked to restrict the spinner for a particular employee, do so.
        if (Utils.isStringNotEmpty(mFilteredEmployeeId)) {
            ArrayList<MigratedEmployee> filteredEmployees = new ArrayList<MigratedEmployee>();
            for (int i = 0; i < allEmployees.size(); i++) {
                if (allEmployees.get(i).getEmployeeID().equalsIgnoreCase(mFilteredEmployeeId)) {
                    filteredEmployees.add(allEmployees.get(i));
                    allEmployees = filteredEmployees;
                    break;
                }
            }
        }

        allEmployeeNames = new String[allEmployees.size()];

        for (int i = 0; i < allEmployees.size(); i++) {
            allEmployeeNames[i] = allEmployees.get(i).getName();
            Utils.log(allEmployeeNames[i]);
        }

        // Create an ArrayAdapter using the list of employees
        ArrayAdapter<String> empAdapter = new ArrayAdapter<String>(
                getActivity(), R.layout.task_spinner_item_white);
        empAdapter.addAll(allEmployeeNames);
        empAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        empSpinner.setAdapter(empAdapter);
        empSpinner.setSelection(0);
    }

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
            Utils.trackPageView("TaskEdit");
        } else {
            Utils.trackPageView("TaskAdd");
        }

        populateEmployeeSpinner(getActivity(), null);
	}

	private void refreshUIForTask() {
		taskTitleEditText.setText(task.getTitle());

		for (int i = 0; i < allEmployees.size(); i++) {
			if (allEmployees.get(i).getEmployeeID()
					.equalsIgnoreCase(task.getEmployeeID())) {
				empSpinner.setSelection(i);
				break;
			}
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

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
//        if (parent == empSpinner && position == 1) {
//            Intent i = new Intent(view.getContext(), EmployeeAddEditActivity.class);
//            startActivityForResult(i, ADD_EMPLOYEE);
//        }

        if (parent == empSpinner && editMode == false) {
            MigratedEmployee emp = allEmployees.get(position);
            taskTitleEditText.setText(emp.getTaskBlob());
        }
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub

	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ADD_EMPLOYEE:
                Utils.log("onActivityResult " + requestCode);
                Bundle extras = null;
                if (data != null) {
                    extras = data.getExtras();
                }
                if (extras != null) {
                    // Create a fake employee row and add it so that the UI looks
                    // responsive to the user.
                    String employeeName = extras.getString(Constants.EMPLOYEE_NAME);
                    String employeePhone = extras.getString(Constants.EMPLOYEE_PHONE);
                    MigratedEmployee newEmp = new MigratedEmployee();
                    newEmp.setName(employeeName);
                    newEmp.setPhoneWithContryCode(employeePhone);
                    newEmp.setTaskCount("0");
                    newEmp.setLastUpdated(Utils.getCurrentDate());

                    populateEmployeeSpinner(getActivity(), newEmp);
                }

                break;
            default:
                break;
        }
    }
}
