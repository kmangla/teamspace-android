/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.teamspace.android.employee.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import com.teamspace.android.R;
import com.teamspace.android.caching.BitmapCache;
import com.teamspace.android.caching.DataManager;
import com.teamspace.android.caching.DataManagerCallback;
import com.teamspace.android.caching.DatabaseCache;
import com.teamspace.android.models.MigratedEmployee;
import com.teamspace.android.models.QuickContactHelperTask;
import com.teamspace.android.utils.Constants;
import com.teamspace.android.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

public class ContactsListFragment extends ListFragment implements AdapterView.OnItemSelectedListener {

    private MyListAdapter mAdapter;

    // Defines a tag for identifying log entries
    private static final String TAG = "ContactsListFragment";
    TextView emptyView;
    TextView pageTitle;
    TextView pageSubtitle;
    EditText searchText;
    Button searchButton;
    Button cancelSearch;
    private boolean searchShowing;
    Handler handler = new Handler();
    Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            if (searchShowing) {
                searchContacts();
            }
        }
    };

    /**
     * Fragments require an empty constructor.
     */
    public ContactsListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        // Inflate the list fragment layout
        View view = inflater.inflate(R.layout.contact_list_fragment, container, false);
        emptyView = (TextView) view.findViewById(android.R.id.empty);
        emptyView.setText(getString(R.string.loading_text));
        pageTitle = (TextView) view.findViewById(R.id.page_title);
        pageSubtitle = (TextView) view.findViewById(R.id.page_subtitle);
        searchText = (EditText) view.findViewById(R.id.search_text);
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (handler != null) {
                    handler.removeCallbacks(myRunnable);
                }

                handler.postDelayed(myRunnable, 500);
            }
        });
        searchButton = (Button) view.findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchContacts();
            }
        });

        cancelSearch = (Button) view.findViewById(R.id.cancel_button);
        cancelSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideSearchUI();
            }
        });

        ListView listview = (ListView) view.findViewById(android.R.id.list);
        mAdapter = new MyListAdapter(getActivity(), listview, emptyView);
        setListAdapter(mAdapter);
        Button doneButton = (Button) view.findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAdapter.batchCreateEmployees(view, getActivity());
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Utils.trackPageView(Constants.EMP_LIST_PV);
    }

    private void hideSearchUI() {
        searchShowing = false;

        if (handler != null) {
            handler.removeCallbacks(myRunnable);
        }

        searchText.setText(Constants.EMPTY_STRING);
        searchText.setVisibility(View.GONE);
        cancelSearch.setVisibility(View.GONE);
        searchButton.setVisibility(View.VISIBLE);
        pageTitle.setVisibility(View.VISIBLE);
        pageSubtitle.setVisibility(View.VISIBLE);

        mAdapter.filterItems(Constants.EMPTY_STRING);
    }

    private void showSearchUI() {
        searchShowing = true;
        searchText.setVisibility(View.VISIBLE);
        cancelSearch.setVisibility(View.VISIBLE);
        searchButton.setVisibility(View.GONE);
        pageTitle.setVisibility(View.GONE);
        pageSubtitle.setVisibility(View.GONE);
    }

    private void searchContacts() {
        // If search view is not showing, show it
        if (!searchShowing) {
            showSearchUI();
            return;
        }

        // If search edit box was already showing, try to search using the string.

        // Error handling
        String searchStr = searchText.getText().toString();
        if (searchStr == null || searchStr.length() == 0) {
            return;
        }

        // Search
        mAdapter.filterItems(searchStr);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        mAdapter.itemSelected(view, i);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private static class MyListAdapter extends ArrayAdapter<ContactInfo> {

        private final Context mContext;
        private final ListView mListView;
        private HashSet<ContactInfo> selectedEmployees;
        private TextView mEmptyView;
        ArrayList<ContactInfo> allContacts;

        public MyListAdapter(final Context context, ListView listView, TextView emptyView) {
            super(context, 0);
            mContext = context;
            mListView = listView;
            refreshEmployeeList(context);
            selectedEmployees = new HashSet<ContactInfo>();
            mEmptyView = emptyView;
        }

        private void refreshEmployeeList(final Context context) {
            AsyncTask<String, Void, ArrayList<ContactInfo>> asyncTask = new AsyncTask<String, Void, ArrayList<ContactInfo>>() {
                @Override
                protected ArrayList<ContactInfo> doInBackground(String... employee) {
                    String countryCode = Utils.getSignedInUserCountryCode();

                    HashSet<String> empNameSet = new HashSet<>();
                    ArrayList<MigratedEmployee> empList = DatabaseCache.getInstance(context).getMigratedEmployeesBlockingCall();
                    for (int i = 0; i <empList.size(); i++) {
                        empNameSet.add(empList.get(i).getName());
                    }

                    ArrayList<ContactInfo> contactList = new ArrayList<ContactInfo>();
                    Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                    while (phones.moveToNext()) {
                        ContactInfo tempInfo = new ContactInfo();
                        tempInfo.name=phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        tempInfo.phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        // If framework did not return valid contact, move on to next contact
                        if (Utils.isStringEmpty(tempInfo.phoneNumber) || Utils.isStringEmpty(tempInfo.name)) {
                            continue;
                        }

                        // Filter out any non-numeric chars from the phone number
                        tempInfo.phoneNumber = tempInfo.phoneNumber.replaceAll("[^\\d.]", "");

                        // Just extract last 10 digits
                        if (tempInfo.phoneNumber.length() > 10) {
                            tempInfo.phoneNumber = tempInfo.phoneNumber.substring(tempInfo.phoneNumber.length() - 10);
                        }

                        // Append country code
                        tempInfo.phoneNumber = Utils.getSignedInUserCountryCode() + tempInfo.phoneNumber;

                        // If the contact is not already added as employee, add it now
                        if (!empNameSet.contains(tempInfo.name)) {
                            contactList.add(tempInfo);
                            com.teamspace.android.utils.Utils.log("phoneNumber = " + tempInfo.phoneNumber + " country: " + countryCode);
                        }
                    }
                    phones.close();
                    Collections.sort(contactList, new Comparator<ContactInfo>() {
                        @Override
                        public int compare(ContactInfo lhs, ContactInfo rhs) {
                            return lhs.name.compareTo(rhs.name);
                        }
                    });

                    return contactList;
                }

                @Override
                protected void onPostExecute(ArrayList<ContactInfo> employees) {
                    allContacts = employees;
                    refreshUIForData(context, employees);
                }
            };
            asyncTask.execute(Utils.getSignedInUserId());
        }

        protected void refreshUIForData(Context context, ArrayList<ContactInfo> data) {
            final int position = 0;

            // Clear the old data from adapter and insert new data.
            clear();
            for (int i = 0; i < data.size(); i++) {
                ContactInfo emp = data.get(i);
                add(emp);
            }

            if (data.size() == 0) {
                mEmptyView.setText(getContext().getString(R.string.no_contacts));
            }

            Utils.refreshListWithoutLosingScrollPosition(mListView, this);
        }

        @Override
        public long getItemId(final int position) {
            return getItem(position).hashCode();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;

            final ContactInfo employee = getItem(position);
            final String empName = employee.name;
            final String empNumber = employee.phoneNumber;

            // If there is no view to reuse, create a new one and setup its viewholder
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.contact_list_item, parent, false);
                EmployeeViewHolder vh = new EmployeeViewHolder(view);
                view.setTag(vh);
            }

            final EmployeeViewHolder viewHolder = (EmployeeViewHolder) view.getTag();
            if (viewHolder == null || !(viewHolder instanceof EmployeeViewHolder)) {
                return view;
            }

            if (viewHolder.populateImageTask != null) {
                viewHolder.populateImageTask.cancel(true);
                viewHolder.populateImageTask = null;
            }

            viewHolder.text1.setText(empName);
            viewHolder.text2.setText(empNumber);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    itemSelected(view, position);
                }
            });
            viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (viewHolder.checkBox.isChecked()) {
                        selectedEmployees.add(employee);
                    } else {
                        selectedEmployees.remove(employee);
                    }
                }
            });

            if (selectedEmployees.contains(employee)) {
                viewHolder.checkBox.setChecked(true);
            } else {
                viewHolder.checkBox.setChecked(false);
            }

            // If we found a bitmap in the cache for this employee (by phone number),
            // we do not show initials. We directly show the bitmap image. Otherwise
            // we show the initials and try to load the bitmap image in background.
            Bitmap cachedPic = BitmapCache.getInstance().retrieveData(empNumber);
            if (cachedPic != null) {
                viewHolder.pic.setVisibility(View.VISIBLE);
                viewHolder.initials.setVisibility(View.GONE);
                viewHolder.pic.setImageBitmap(cachedPic);
            } else {
                viewHolder.pic.setVisibility(View.GONE);
                viewHolder.initials.setVisibility(View.VISIBLE);
                String initials = Utils.extractInitialsFromName(empName);
                viewHolder.initials.setText(initials);

                // Start a async task to load the employee pic if available.
                QuickContactHelperTask.ContactBadge badge = new QuickContactHelperTask.ContactBadge(mContext, viewHolder.pic,
                        empNumber,
                        viewHolder.initials,
                        Utils.extractInitialsFromName(empName));
                viewHolder.populateImageTask = new QuickContactHelperTask();
                viewHolder.populateImageTask.execute(badge);
            }

            return view;
        }

        private int employeesCreated = 0;

        public void batchCreateEmployees(final View v, final Activity currentActivity) {
            if (selectedEmployees == null || selectedEmployees.size() == 0) {
                currentActivity.finish();
                return;
            }

            // Block the UI till employee creation succeeds or fails
            final ProgressDialog progress;
            progress = new ProgressDialog(v.getContext());
            progress.setTitle("Contacting Server");
            progress.setMessage("Please wait while we propagate the changes to our servers. This might take up to a minute.");
            progress.setCancelable(false);
            progress.setMax(200);
            progress.show();

            Iterator<ContactInfo> iterator = selectedEmployees.iterator();
            final int totalEmpToCreate = selectedEmployees.size();
            while (iterator.hasNext()) {
                ContactInfo tempContactInfo = iterator.next();
                MigratedEmployee newEmp = new MigratedEmployee();
                newEmp.setName(tempContactInfo.name);
                newEmp.setPhoneWithContryCode(tempContactInfo.phoneNumber);
                newEmp.setEmployeeID(Constants.EMPTY_STRING);
                newEmp.setDesignation(Constants.EMPLOYEE);
                newEmp.setTaskCount("0");
                newEmp.setUserID(Utils.getSignedInUserId());
                newEmp.setCompanyID(Utils.getSignedInUserId());

                DataManager dataMgr = DataManager.getInstance(v.getContext());
                dataMgr.createEmployee(newEmp, new DataManagerCallback() {

                    @Override
                    public void onSuccess(String response) {
                        employeesCreated++;

                        if (employeesCreated >= totalEmpToCreate && progress != null) {
                            progress.dismiss();
                            currentActivity.finish();
                        }
                    }

                    @Override
                    public void onFailure(String response) {
                        employeesCreated++;

                        // Notify user about the error
                        Toast.makeText(
                                v.getContext(),
                                v.getContext().getResources()
                                        .getString(
                                                R.string.error_employee_create_failed),
                                Toast.LENGTH_SHORT).show();

                        if (employeesCreated >= totalEmpToCreate && progress != null) {
                            progress.dismiss();
                            currentActivity.finish();
                        }
                     }
                });
            }
        }

        public void itemSelected(View view, int position) {
            ContactInfo employee = getItem(position);
            EmployeeViewHolder viewHolder = (EmployeeViewHolder) view.getTag();
            viewHolder.checkBox.toggle();
            if (viewHolder.checkBox.isChecked()) {
                selectedEmployees.add(employee);
            } else {
                selectedEmployees.remove(employee);
            }
        }

        public void filterItems(String searchString) {
            if (allContacts == null) {
                return;
            }

            // Reset filter
            if (searchString == null || searchString.length() == 0) {
                refreshUIForData(mContext, allContacts);
                return;
            }

            // Filter list
            ArrayList<ContactInfo> filteredEmp = new ArrayList<>();
            for (int i = 0; i < allContacts.size(); i++) {
                ContactInfo tempInfo = allContacts.get(i);
                if (tempInfo.name.toUpperCase().contains(searchString.toUpperCase())) {
                    filteredEmp.add(tempInfo);
                }
            }
            refreshUIForData(mContext, filteredEmp);
        }

        private class EmployeeViewHolder {
            TextView text1;
            TextView text2;
            TextView initials;;
            QuickContactBadge pic;
            CheckBox checkBox;

            QuickContactHelperTask populateImageTask;

            public EmployeeViewHolder(View view) {
                text1 = (TextView) view.findViewById(android.R.id.text1);
                text2 = (TextView) view.findViewById(android.R.id.text2);
                initials = (TextView) view.findViewById(R.id.initials);
                pic = (QuickContactBadge) view.findViewById(android.R.id.icon);
                checkBox = (CheckBox) view.findViewById(R.id.create_multiple);
            }
        }
    }

    public static class ContactInfo {
        String name;
        String phoneNumber;
    }

}