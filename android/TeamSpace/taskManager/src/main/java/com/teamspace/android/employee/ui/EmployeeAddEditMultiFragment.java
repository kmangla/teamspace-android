/**
 * Fragment for Creating/Updating information about an Employee
 */

package com.teamspace.android.employee.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.QuickContactBadge;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.teamspace.android.R;
import com.teamspace.android.caching.DataManager;
import com.teamspace.android.caching.DataManagerCallback;
import com.teamspace.android.caching.DatabaseCache;
import com.teamspace.android.common.ui.TaskManagerApplication;
import com.teamspace.android.models.MigratedEmployee;
import com.teamspace.android.models.QuickContactHelperTask;
import com.teamspace.android.models.QuickContactHelperTask.ContactBadge;
import com.teamspace.android.utils.Constants;
import com.teamspace.android.utils.Utils;

// This class handles editing of existing employee as well as adding new employees. If the
// caller invokes this class for editing purpose, employeeID is passed in for the employee being edited

public class EmployeeAddEditMultiFragment extends Fragment implements OnItemSelectedListener {

	private MigratedEmployee employee;
	private int countryIndexInCountryArray = 0;
	Button saveButton;
	String employeeID = "";
	String employeeName = "";
	String employeePhone = "";
	boolean editMode;
    boolean mImportedFromContacts;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Since we need to fetch employee data from the database which might require some time
		// and must be done on background thread, we allow the caller to pass us few basic
		// information like employee name and number which we can instantly show in the UI
		// while more details about the employee are fetched from the database.
		Bundle extras = getActivity().getIntent().getExtras();
		if (extras != null) {
			employeeID = extras.getString(Constants.EMPLOYEE_ID);
			employeeName = extras.getString(Constants.EMPLOYEE_NAME);
			employeePhone = extras.getString(Constants.EMPLOYEE_PHONE);
			if (Utils.isStringNotEmpty(employeeID)) {
				editMode = true;
			}
		}
		
		final TaskManagerApplication applicationContext = ((TaskManagerApplication) getFragmentActivity()
				.getApplication());

        // If we are in the employee edit flow, we fetch the employee data from database on background thread
		if (Utils.isStringNotEmpty(employeeID)) {
			AsyncTask<String, Void, MigratedEmployee> asyncTask = new AsyncTask<String, Void, MigratedEmployee>() {
				@Override
				protected MigratedEmployee doInBackground(String... employeeIDs) {
					Utils.log("Trying to fetch employee details from database cache for emp id: " + employeeIDs[0]);
					return DatabaseCache.getInstance(applicationContext).getMigratedEmployeeBlockingCall(employeeIDs[0]);
				}

				@Override
                // After the background thread is done fetching data, we refresh UI
			    protected void onPostExecute(MigratedEmployee cachedEmployee) {
					employee = cachedEmployee;
					refreshUIForEmployee();
			    }
			};
			asyncTask.execute(employeeID);
		} else {
            // If we are in the employee add flow, we create a new employee object
			employee = new MigratedEmployee();
		}
		
		getActivity().setTitle(R.string.add_employee);
		setRetainInstance(true);
	}

    @Override
    public void onResume() {
        super.onResume();
        if (editMode) {
            Utils.trackPageView("EmployeeEdit");
        } else {
            Utils.trackPageView("EmployeeAdd");
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.employee_multi_add_fragment, container, false);
//		contact_pic = (QuickContactBadge) v.findViewById(R.id.contact_image);
        TextView header = (TextView) v.findViewById(R.id.page_title);

		// If we are editing an existing employee, we don't need import
		// functionality
		if (editMode) {
            header.setText(getString(R.string.edit_emp));
		}

		saveButton = (Button) v.findViewById(R.id.done_button);
		saveButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				TaskManagerApplication applicationContext = ((TaskManagerApplication) getFragmentActivity()
						.getApplication());

                // Prevent user from accidentally clicking the "Done" button twice
                saveButton.setEnabled(false);

				employee.setPhoneWithContryCode("+11111111111"); // "dummy value to be filled by pratyus"
				employee.setName("dummy value to be filled by pratyus");
				employee.setLastUpdated("dummy value to be filled by pratyus");
				employee.setUserID(Utils.getSignedInUserId());
				employee.setCompanyID(Utils.getSignedInUserId());
				
				DataManager dataMgr = DataManager.getInstance(applicationContext);

                // Block the UI till employee creation succeeds or fails
                final ProgressDialog progress;
                progress = new ProgressDialog(v.getContext());
                progress.setTitle("Contacting Server");
                progress.setMessage("Please wait while we propagate the changes to our servers. This might take up to a minute.");
                progress.setCancelable(false);
                progress.setMax(200);
                progress.show();

                // If editing an existing employee
				if (Utils.isStringNotEmpty(employeeID)) {
					employee.setEmployeeID(employeeID);
					dataMgr.updateEmployee(employee, new DataManagerCallback() {

						@Override
						public void onSuccess(String response) {
                            if (progress != null) {
                                progress.dismiss();
                            }
                            employeeUpdateDone(false);
						}

						@Override
						public void onFailure(String response) {
							// Notify user about the error
							Toast.makeText(
									v.getContext(),
									v.getContext().getResources()
											.getString(
													R.string.error_employee_update_failed),
									Toast.LENGTH_SHORT).show();
                            if (progress != null) {
                                progress.dismiss();
                            }
                            saveButton.setEnabled(true);
						}
					});					
				} else {
                    // If creating a new employee
					employee.setEmployeeID(Constants.EMPTY_STRING);
					employee.setDesignation(Constants.EMPLOYEE);
					employee.setTaskCount("0");

					dataMgr.createEmployee(employee, new DataManagerCallback() {

                        @Override
                        public void onSuccess(String response) {
                            if (progress != null) {
                                progress.dismiss();
                            }
                            employeeUpdateDone(true);
                        }

                        @Override
                        public void onFailure(String response) {
                            // Notify user about the error
                            Toast.makeText(
                                    v.getContext(),
                                    v.getContext().getResources()
                                            .getString(
                                                    R.string.error_employee_create_failed),
                                    Toast.LENGTH_SHORT).show();
                            if (progress != null) {
                                progress.dismiss();
                            }
                            saveButton.setEnabled(true);
                        }
                    });
                }

                if (editMode) {
                    Utils.trackEvent("Tracking", "EmployeeEdited",
                            "EmployeeAddEditFragment:saveButtonClicked");
                } else {
                    Utils.trackEvent("Tracking", "EmployeeAdded",
                            "EmployeeAddEditFragment:saveButtonClicked");
                }
			}
		});

		return v;
	}

    // Close the UI after employee has been updated / created
    private void employeeUpdateDone(boolean returnNewEmployee) {
        Intent newIntent = new Intent();

        if (returnNewEmployee) {
            newIntent.putExtra(Constants.EMPLOYEE_NAME, employee.getName());
            newIntent.putExtra(Constants.EMPLOYEE_PHONE, employee.getPhoneWithCountryCode());
        }

        // Refresh the task lists to display this change
        DataManager.getInstance(getActivity()).insertData(Constants.REFRESH_ALL_TASKS, true);
        DataManager.getInstance(getActivity()).insertData(Constants.REFRESH_EMP, true);

        getFragmentActivity().setResult(Activity.RESULT_OK, newIntent);
        getFragmentActivity().finish();
    }

    private void refreshUIForEmployee() {
        // To be filled by pratyus
	}

	@Override
    // This method gets called when user selects a contact from phone's contact book during "import employee from contacts" flow
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// If selected via Contact Picker get Name and Number and autofill.
		if(resultCode == getActivity().RESULT_OK){
			Uri contactData = data.getData();
			Cursor cursor =  getActivity().getContentResolver().query(contactData, null, null, null, null);
			cursor.moveToFirst();

			String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
			String number =  cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));

			Utils.log("Cursor returned: " + name + " : " + number);

            if (!Utils.isStringNotEmpty(name)) {
                name = getString(R.string.not_available);
            }

            if (!Utils.isStringNotEmpty(number)) {
                number = getString(R.string.not_available);
            }

            mImportedFromContacts = true;
		}
	}
	
	public FragmentActivity getFragmentActivity() {
		return this.getActivity();
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		countryIndexInCountryArray = position;
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub
		
	}
}
