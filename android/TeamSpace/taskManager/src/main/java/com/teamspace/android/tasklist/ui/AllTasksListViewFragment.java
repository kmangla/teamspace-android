package com.teamspace.android.tasklist.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.QuickContactBadge;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.teamspace.android.BuildConfig;
import com.teamspace.android.R;
import com.teamspace.android.caching.BitmapCache;
import com.teamspace.android.caching.DataManager;
import com.teamspace.android.caching.DataManagerCallback;
import com.teamspace.android.caching.DatabaseCache;
import com.teamspace.android.chat.ui.MessageListActivity;
import com.teamspace.android.common.ui.SettingsPreferenceFragment;
import com.teamspace.android.employee.ui.EmployeeAddEditActivity;
import com.teamspace.android.employee.ui.EmployeeListViewActivity;
import com.teamspace.android.interfaces.ActionBarResponderInterface;
import com.teamspace.android.models.MetricsObject;
import com.teamspace.android.models.MigratedEmployee;
import com.teamspace.android.models.MigratedMessage;
import com.teamspace.android.models.MigratedTask;
import com.teamspace.android.models.QuickContactHelperTask;
import com.teamspace.android.models.QuickContactHelperTask.ContactBadge;
import com.teamspace.android.networking.GCMUtils;
import com.teamspace.android.networking.GcmRegistrationReceiver;
import com.teamspace.android.utils.Constants;
import com.teamspace.android.utils.Utils;
import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class AllTasksListViewFragment extends Fragment implements OnItemSelectedListener, ActionBarResponderInterface {

    private static final int ADD_TASK = 0;
    private static final int DETAIL_TASK = 1;
    private MyListAdapter mAdapter;
    private int oldItemCount;
    private ImageButton addButton;
    private Button addTaskButton;
    SwipeListView swipelistview;
    private ProgressBar progressBar;
    private int currentSortPreference = 0;
    private Handler uiHandler;
    public static final int NOTIFICATION_ID = 1;
    private int taskRefreshCount = 0;
    private boolean mDelayRefresh;
    private boolean isFreshLogin; // Used for onboarding flow if there are 0 employees
    private boolean isShowing;
    private boolean refreshPending;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equalsIgnoreCase(Constants.TASK_CREATION)) {
                Utils.log("TASK_CREATION Broadcast received in AllTasksListViewFragment");

                if (!isShowing) {
                    refreshPending = true;
                    return;
                }

                if (mAdapter != null) {
                    refreshPending = false;
                    mAdapter.refreshTaskList(context);
                    Utils.log("REFRESHING TASK LIST DUE TO BROADCAST");
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.all_tasks_fragment, null, false);

        if (getActivity().getIntent().getExtras() != null) {
            isFreshLogin = getActivity().getIntent().getExtras().getBoolean(Constants.FRESH_LOGIN);
        }

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter(Constants.TASK_CREATION));

        swipelistview = (SwipeListView) rootView.findViewById(R.id.swipe_list_view);
        View headerView = (View) rootView.findViewById(R.id.all_tasks_header);

        swipelistview.setSwipeListViewListener(new BaseSwipeListViewListener() {
            @Override
            public void onOpened(int position, boolean toRight) {
                if (toRight) {
                    swipelistview.closeAnimate(position);
                }
            }

            @Override
            public void onClosed(int position, boolean fromRight) {
                View view = Utils.getRowFromListView(swipelistview, position);
                if (view != null) {
                    View danger = (View) view.findViewById(R.id.danger_level);
                    danger.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onListChanged() {
            }

            @Override
            public void onMove(int position, float x) {
            }

            @Override
            public void onStartOpen(int position, int action, boolean right) {
                if (!right) {
                    Utils.closeOtherRows(swipelistview, position, mAdapter.getCount());
                }

                View row = Utils.getRowFromListView(swipelistview, position);
                if (row == null) {
                    return;
                }

                View danger = (View) row.findViewById(R.id.danger_level);
                danger.setVisibility(View.GONE);

                TaskViewHolder viewHolder = (TaskViewHolder) row.getTag();
                if (viewHolder == null || !(viewHolder instanceof TaskViewHolder)) {
                    return;
                }

                MigratedTask task = mAdapter.getItem(position);

                if (right || task.isCreationPending()) {
                    viewHolder.markCompleted.setVisibility(View.INVISIBLE);
                    viewHolder.delete.setVisibility(View.INVISIBLE);
                    viewHolder.sendReminder.setVisibility(View.INVISIBLE);
                    viewHolder.markUpdated.setVisibility(View.INVISIBLE);
                } else {
                    viewHolder.markCompleted.setVisibility(View.VISIBLE);
                    viewHolder.delete.setVisibility(View.VISIBLE);
                    viewHolder.sendReminder.setVisibility(View.VISIBLE);
                    if (task.getPriority() > 0) {
                        viewHolder.markUpdated.setVisibility(View.VISIBLE);
                    } else {
                        viewHolder.markUpdated.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onStartClose(int position, boolean right) {
            }

            @Override
            public void onClickFrontView(int position) {
                // Close the rows.
                mAdapter.closeAllRows();
                taskSelected(position);
            }

            @Override
            public void onClickBackView(int position) {
                swipelistview.closeAnimate(position);//when you touch back view it will close
            }

            @Override
            public void onDismiss(int[] reverseSortedPositions) {

            }

        });

        addButton = (ImageButton) headerView.findViewById(R.id.add_button);
        addButton.setVisibility(View.GONE);
        addTaskButton = (Button) headerView.findViewById(R.id.add_task_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });

        ImageButton employeeButton = (ImageButton) headerView.findViewById(R.id.employee_button);
        employeeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchEmployeeActivity();
            }
        });

        ImageButton selfButton = (ImageButton) headerView.findViewById(R.id.self_button);
        selfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerTestPush();
            }
        });
        if (false && Build.FINGERPRINT.startsWith("generic")) {
            selfButton.setVisibility(View.VISIBLE);
        } else {
            selfButton.setVisibility(View.GONE);
        }

        progressBar = (ProgressBar) headerView.findViewById(R.id.task_progress);

        // If user has selected any color preferences, respect that.
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String backgroundColorStr = sharedPref.getString(
                SettingsPreferenceFragment.KEY_PREF_TASK_BK_COLOR,
                SettingsPreferenceFragment.KEY_DEFAULT_TASK_BK_COLOR);
        int backgroundColor = Utils.getColor(getActivity(), backgroundColorStr);

        String textColorStr = sharedPref.getString(
                SettingsPreferenceFragment.KEY_PREF_TASK_TEXT_COLOR,
                SettingsPreferenceFragment.KEY_DEFAULT_TASK_TEXT_COLOR);
        int textColor = Utils.getColor(getActivity(), textColorStr);

        /* Setup the adapter */
        mAdapter = new MyListAdapter(getActivity(), backgroundColor, textColor, swipelistview, isFreshLogin, progressBar);
        swipelistview.setAdapter(mAdapter);

        // Add sort spinner as the header of the list view.
        final Spinner spinner = (Spinner) headerView.findViewById(R.id.sort_spinner);
        ImageButton sortButton = (ImageButton) headerView.findViewById(R.id.sort_button);
        sortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spinner.performClick();
            }
        });
        // Create an ArrayAdapter using the string array and a default spinner
        // layout
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.task_sort_options, R.layout.sort_spinner_item);

        // Specify the layout to use when the list of choices appears
        sortAdapter.setDropDownViewResource(R.layout.sort_spinner_drop_down_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(sortAdapter);
        spinner.setOnItemSelectedListener(this);

        // Set spinner color based on user's preference
//		String sortColorStr = sharedPref.getString(
//				SettingsPreferenceFragment.KEY_PREF_SORT_BK_COLOR,
//				SettingsPreferenceFragment.KEY_DEFAULT_SORT_BK_COLOR);
//		int sortColor = Utils.getColor(getActivity(), sortColorStr);
//		spinner.setBackgroundColor(sortColor);
//		headerView.setBackgroundColor(sortColor);

//		String sortTextColorStr = sharedPref.getString(
//				SettingsPreferenceFragment.KEY_PREF_SORT_TEXT_COLOR,
//				SettingsPreferenceFragment.KEY_DEFAULT_SORT_TEXT_COLOR);
//		int sortTextColor = Utils.getColor(getActivity(), sortTextColorStr);

        // Clear any pending notifications
        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);

        return rootView;
    }

    private void taskSelected(int position) {
        MigratedTask taskSelected = mAdapter.getItem(position);
        if (taskSelected.isCreationPending()) {
            Toast.makeText(
                    getActivity(),
                    R.string.task_creation_pending,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(getActivity(), MessageListActivity.class);
        String selectedTaskId = taskSelected.getTaskID();
        taskSelected.setUpdateCount(0);

        // If we don't have a taskID, it means that the task was just created and we were
        // displaying the dummy stub while the network call was in progress. User clicked
        // on this dummy stub.
        if (!Utils.isStringNotEmpty(selectedTaskId)) {
            MigratedTask tempTask = DatabaseCache.getInstance(getActivity()).
                    getMigratedTaskWithTitleAndEmployeeBlockingCall(
                            taskSelected.getTitle(),
                            taskSelected.getEmployeeNumber());

            // If we could not find this task in database cache, ignore this click.
            if (tempTask == null) {
                return;
            }

            selectedTaskId = tempTask.getTaskID();
        }

        i.putExtra(Constants.TASK_ID, selectedTaskId);
        i.putExtra(Constants.TASK_TITLE, taskSelected.getTitle());
        i.putExtra(Constants.EMPLOYEE_NAME, taskSelected.getEmployeeName());
        i.putExtra(Constants.EMPLOYEE_PHONE, taskSelected.getEmployeeNumber());
        i.putExtra(Constants.HIDE_KEYBOARD, true);
        startActivityForResult(i, DETAIL_TASK);
    }

    private void launchEmployeeActivity() {
        Intent i = new Intent(getActivity(), EmployeeListViewActivity.class);
        startActivity(i);
    }

    private void triggerTestPush() {
        DataManager.getInstance(getActivity()).requestTestPush(null);
    }

    private void launchSelfTasksActivity() {
        Intent i = new Intent(getActivity(), TasksForEmployeeActivity.class);
        i.putExtra(Constants.EMPLOYEE_ID, Utils.getSignedInUserId());
        startActivity(i);
    }

    private static class TaskViewHolder {
        public TextView taskTitle;
        public TextView lastMessage;
        public TextView update;
        public TextView notification;
        public TextView initials;
        public TextView headerText;
        public Button moreButton;
        Button delete;
        Button markCompleted;
        Button markUpdated;
        Button sendReminder;
        ImageButton fav;
        public QuickContactBadge pic;
        public View frontView;
        public View backView;
        QuickContactHelperTask populateImageTask;

        public TaskViewHolder(View view) {
            taskTitle = (TextView) view.findViewById(R.id.title);
            lastMessage = (TextView) view.findViewById(R.id.subtitle);
            update = (TextView) view.findViewById(R.id.details);
            notification = (TextView) view.findViewById(R.id.notification);
            pic = (QuickContactBadge) view.findViewById(R.id.image_view);
            initials = (TextView) view.findViewById(R.id.initials);
            headerText = (TextView) view.findViewById(R.id.header_text);
            moreButton = (Button) view.findViewById(R.id.more_button);
            frontView = (View) view.findViewById(R.id.front);
            backView = (View) view.findViewById(R.id.back);
            delete = (Button) view.findViewById(R.id.swipe_button1);
            markCompleted = (Button) view.findViewById(R.id.swipe_button2);
            markUpdated = (Button) view.findViewById(R.id.swipe_button3);
            sendReminder = (Button) view.findViewById(R.id.swipe_button4);
            fav = (ImageButton) view.findViewById(R.id.star);
        }
    }

    private static class MyListAdapter extends ArrayAdapter<MigratedTask> {

        private final Context mContext;
        private int mBackgroundColor;
        private int mTextColor;
        public boolean flashNewlyAddedRows;
        public long flashRequestTime;
        private SwipeListView mSwipeListView;
        private boolean isFreshLogin;
        private boolean showTaskOnboardingOnResume;
        private int taskRefreshCount = 0;
        private ProgressBar mProgressBar;

        public MyListAdapter(final Context context, int backgroundColor,
                             int textColor, SwipeListView swipeListView, boolean isFreshLogin, ProgressBar progressBar) {
            super(context, 0);
            mContext = context;
            mBackgroundColor = backgroundColor;
            mTextColor = textColor;
            mSwipeListView = swipeListView;
            this.isFreshLogin = isFreshLogin;
            mProgressBar = progressBar;

            refreshTaskList(context);
        }

        private void refreshTaskList(final Context context) {
            taskRefreshCount++;
            mProgressBar.setVisibility(View.VISIBLE);

            DataManager.getInstance(context).fetchAllTasksForUser(Utils.getSignedInUserId(),
                    new DataManagerCallback() {

                        @Override
                        public void onDataReceivedFromCache(String dataStoreKey) {
                            refreshUIForData(context, dataStoreKey, false);
                        }

                        @Override
                        public void onDataReceivedFromServer(String dataStoreKey) {
                            refreshUIForData(context, dataStoreKey, true);
                            if (taskRefreshCount > 0) {
                                taskRefreshCount--;
                            }
                            if (taskRefreshCount == 0) {
                                mProgressBar.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onFailure(String message) {
                            // Notify user about the error
                            Utils.trackEvent("task", "fetch", "network_fail");
                            if (message != null) {
                                Toast.makeText(
                                        context,
                                        message,
                                        Toast.LENGTH_SHORT).show();
                            }
                            if (taskRefreshCount > 0) {
                                taskRefreshCount--;
                            }
                            if (taskRefreshCount == 0) {
                                mProgressBar.setVisibility(View.GONE);
                            }
                        }
                    });
        }

        protected void refreshUIForData(Context context, String dataStoreKey, boolean fromServer) {
            if (dataStoreKey == null) {
                return;
            }
            ArrayList<MigratedTask> data = (ArrayList<MigratedTask>) DataManager
                    .getInstance(context).retrieveData(
                            dataStoreKey);

            final int position = 0;

            // Sort the data before adding it to adapter.
            Collections.sort(data, new Comparator<MigratedTask>() {
                @Override
                public int compare(MigratedTask lhs, MigratedTask rhs) {
                    return compareTasksBasedOnTaskSortArray(lhs, rhs, position);
                }
            });

            // Clear the old data from adapter and insert new data.
            clear();
            for (int i = 0; i < data.size(); i++) {
                MigratedTask task = data.get(i);
                add(task);
            }
            Utils.refreshListWithoutLosingScrollPosition(mSwipeListView, this);

            // If there are no tasks, see if the user needs onboarding
            if (isFreshLogin && fromServer && data.size() == 0) {
//                launchOnboardingFlowIfNeeded();
            }
        }

        private void launchOnboardingFlowIfNeeded() {
            if (!isFreshLogin) {
                return;
            }
            DataManager.getInstance(mContext).fetchEmployeesForUser(Utils.getSignedInUserId(),
                    new DataManagerCallback() {

                @Override
                public void onDataReceivedFromServer(String dataStoreKey) {
                    if (dataStoreKey == null) {
                        return;
                    }

                    ArrayList<MigratedEmployee> data = (ArrayList<MigratedEmployee>) DataManager
                            .getInstance(mContext).retrieveData(
                                    dataStoreKey);

                    if (data.size() == 0) {
                        showTaskOnboardingOnResume = true;
                        launchEmployeeAddEditActivity();
                    } else {
                        launchTaskAddEditActivity();
                    }
                }
            });

        }

        private void launchEmployeeAddEditActivity() {
            AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
            alert.setTitle(mContext.getString(R.string.get_started));
            alert.setMessage(mContext.getString(R.string.get_started_employee));

            alert.setPositiveButton(mContext.getString(R.string.next), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // If there are no employees, take the user to "add employee" screen.
                    Intent i = new Intent(mContext, EmployeeAddEditActivity.class);
                    i.putExtra(Constants.FRESH_LOGIN, true);
                    mContext.startActivity(i);
                }
            });

            alert.setNegativeButton(mContext.getString(R.string.later), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        }

        private void launchTaskAddEditActivity() {
            AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
            alert.setTitle(mContext.getString(R.string.get_started));
            alert.setMessage(mContext.getString(R.string.get_started_task));

            alert.setPositiveButton(mContext.getString(R.string.next), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // If there are employees but no tasks, take user to "add task" screen
                    Intent i = new Intent(mContext, TaskAddEditActivity.class);
                    mContext.startActivity(i);
                }
            });

            alert.setNegativeButton(mContext.getString(R.string.later), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        }

        public static int compareTasksBasedOnTaskSortArray(MigratedTask lhs, MigratedTask rhs, int indexInSortArray) {
            // R.array.task_sort_options corresponds to the following cases.
            switch (indexInSortArray) {
                case 0:
//                    if (lhs.getPriority() < rhs.getPriority()) { // escalation status
//                        return 1;
//                    } else if (lhs.getPriority() > rhs.getPriority()) {
//                        return -1;
//                    } else {
//                        if (lhs.getUpdateCount() < rhs.getUpdateCount()) { // unread notifications
//                            return 1;
//                        } else if (lhs.getUpdateCount() > rhs.getUpdateCount()) {
//                            return -1;
//                        } else {
//                            return lhs.getTitle().compareTo(rhs.getTitle()); // title
//                        }
//                    }

                    // First try to sort based on status
                    if ("closed".equalsIgnoreCase(lhs.getStatus()) && "open".equalsIgnoreCase(rhs.getStatus())) { // open or not
                        return 1;
                    } else if ("open".equalsIgnoreCase(lhs.getStatus()) && "closed".equalsIgnoreCase(rhs.getStatus())) { // open or not
                        return -1;
                    }

                    // First try to sort based on Star
                    if (lhs.getFav() < rhs.getFav()) { // starred or not
                        return 1;
                    } else if (lhs.getFav() > rhs.getFav()) {
                        return -1;
                    }

                    // Try to sort based on creation time
                    if (lhs.getCreatedOn() < rhs.getCreatedOn()) { // created time
                        return 1;
                    } else if (lhs.getCreatedOn() > rhs.getCreatedOn()) {
                        return -1;
                    } else {
                        return lhs.getTitle().compareTo(rhs.getTitle()); // title
                    }
                case 1:
                    if (lhs.getUpdateCount() < rhs.getUpdateCount()) {
                        return 1;
                    } else if (lhs.getUpdateCount() > rhs.getUpdateCount()) {
                        return -1;
                    } else {
                        return lhs.getTitle().compareTo(rhs.getTitle());
                    }
                case 2:
                    if (lhs.getLastUpdate() < rhs.getLastUpdate()) {
                        return 1;
                    } else if (lhs.getLastUpdate() > rhs.getLastUpdate()) {
                        return -1;
                    } else {
                        return lhs.getTitle().compareTo(rhs.getTitle());
                    }
                case 3:
                    if (lhs.getFrequency() > rhs.getFrequency()) {
                        return 1;
                    } else if (lhs.getFrequency() < rhs.getFrequency()) {
                        return -1;
                    } else {
                        return lhs.getTitle().compareTo(rhs.getTitle());
                    }
                case 4:
                    if (lhs.getEmployeeName() == rhs.getEmployeeName()) {
                        return lhs.getTitle().compareTo(rhs.getTitle());
                    }
                    if (lhs.getEmployeeName() == null) {
                        return -1;
                    }
                    if (rhs.getEmployeeName() == null) {
                        return 1;
                    }
                    return lhs.getEmployeeName().compareTo(rhs.getEmployeeName());
                case 5:
                    return lhs.getTitle().compareTo(rhs.getTitle());
                default:
                    return lhs.getTitle().compareTo(rhs.getTitle());
            }
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

            final MigratedTask task = getItem(position);
            final String taskId = task.getTaskID();

            // If there is no view to reuse, create a new one and setup its viewholder
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.swipe_list_row_with_three_buttons, parent, false);
                TaskViewHolder vh = new TaskViewHolder(view);
                view.setTag(vh);
            }

            final TaskViewHolder viewHolder = (TaskViewHolder) view.getTag();
            if (viewHolder == null || !(viewHolder instanceof TaskViewHolder)) {
                return view;
            }

            viewHolder.headerText.setVisibility(View.GONE);

            // Determine if we want to show "Closed tasks" header
            if ("closed".equalsIgnoreCase(task.getStatus()) && position == 0) {
                viewHolder.headerText.setVisibility(View.VISIBLE);
            } else if ("closed".equalsIgnoreCase(task.getStatus()) &&
                    position > 0 &&
                    "open".equalsIgnoreCase(getItem(position - 1).getStatus()))
            {
                viewHolder.headerText.setVisibility(View.VISIBLE);
            }

            if (viewHolder.populateImageTask != null) {
                viewHolder.populateImageTask.cancel(true);
                viewHolder.populateImageTask = null;
            }

            if (task.isCreationPending()) {
                viewHolder.frontView.setBackgroundColor(Color.LTGRAY);
            } else {
                viewHolder.frontView.setBackgroundColor(Color.WHITE);
            }

            Button moreButton = (Button) view.findViewById(R.id.more_button);
            moreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.closeOtherRows(mSwipeListView, position, getCount());
                    mSwipeListView.openAnimate(position);
                }
            });

            if (task.isCreationPending()) {
                viewHolder.markCompleted.setVisibility(View.GONE);
                viewHolder.delete.setVisibility(View.GONE);
                viewHolder.sendReminder.setVisibility(View.GONE);
                viewHolder.markUpdated.setVisibility(View.GONE);
            } else {
                viewHolder.markCompleted.setVisibility(View.VISIBLE);
                viewHolder.delete.setVisibility(View.VISIBLE);
                viewHolder.sendReminder.setVisibility(View.VISIBLE);
                if (task.getPriority() > 0) {
                    viewHolder.markUpdated.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.markUpdated.setVisibility(View.GONE);
                }
            }

            if ("closed".equalsIgnoreCase(task.getStatus())) {
                viewHolder.markCompleted.setVisibility(View.GONE);
                viewHolder.sendReminder.setVisibility(View.GONE);
                viewHolder.markUpdated.setVisibility(View.GONE);
            }

            // If we found a bitmap in the cache for this employee (by phone number), 
            // we do not show initials. We directly show the bitmap image. Otherwise
            // we show the initials and try to load the bitmap image in background.
            Bitmap cachedPic = BitmapCache.getInstance().retrieveData(task.getEmployeeID());
            if (cachedPic != null) {
                viewHolder.pic.setVisibility(View.VISIBLE);
                viewHolder.initials.setVisibility(View.GONE);
                viewHolder.pic.setImageBitmap(cachedPic);
            } else {
                viewHolder.pic.setVisibility(View.GONE);
                viewHolder.initials.setVisibility(View.VISIBLE);
                String initials = Utils.extractInitialsFromName(task.getEmployeeName());
                viewHolder.initials.setText(initials);

                // Start a async task to load the employee pic if available.
                ContactBadge badge = new ContactBadge(mContext, viewHolder.pic,
                        task.getEmployeeID(), viewHolder.initials, initials);
                viewHolder.populateImageTask = new QuickContactHelperTask();
                viewHolder.populateImageTask.execute(badge);
            }

            // Set the content of text views
            viewHolder.taskTitle.setText(task.getTitle());
            String lastMsg = Constants.EMPTY_STRING;
            long lastMsgTime = 0;
            if (task.getLastMessage() != null) {
                MigratedMessage msg = task.getLastMessage();
                lastMsg = msg.getText();
                lastMsgTime = msg.getTime();
            }

            viewHolder.lastMessage.setText(lastMsg);
            viewHolder.lastMessage.setTextColor(Utils.getColor(view.getContext(), "Black"));

            if (lastMsgTime > 0) {
                viewHolder.update.setText(Utils.getDateAndTime(lastMsgTime) + "");
            } else {
                viewHolder.update.setText(Constants.EMPTY_STRING);
            }

            viewHolder.update.setTextColor(Utils.getColor(mContext, "Dark Gray"));

            viewHolder.notification.setVisibility(View.GONE);
            if (task.getUpdateCount() > 0) {
                long updateCount = task.getUpdateCount();
                viewHolder.notification.setVisibility(View.VISIBLE);
                viewHolder.notification.setText("" + updateCount);
            }

            if (task.getFav() == 0) {
                viewHolder.fav.setBackgroundResource(R.drawable.star_off);
            } else {
                viewHolder.fav.setBackgroundResource(R.drawable.star_on);
            }

            viewHolder.fav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    starTask(viewHolder.fav, task);
                }
            });

            viewHolder.delete.setText(view.getContext().getString(R.string.delete));
            viewHolder.delete.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    closeAllRows();
                    deleteTask(v.getContext(), taskId, position);
                }
            });

            viewHolder.markCompleted.setText(view.getContext().getString(R.string.mark_completed));
            viewHolder.markCompleted
                    .setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            closeAllRows();
                            markTaskCompleted(v.getContext(), task, position);
                        }
                    });

            viewHolder.sendReminder.setText(view.getContext().getString(R.string.send_reminder));
            viewHolder.sendReminder
                    .setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            closeAllRows();
                            forceReminderOnTask(v.getContext(), task, position);
                        }
                    });

            viewHolder.markUpdated.setText(view.getContext().getString(R.string.mark_updated));
            viewHolder.markUpdated
                    .setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            closeAllRows();
                            markUpdatedRequestText(v.getContext(), task, position);
                        }
                    });

            View danger = (View) view.findViewById(R.id.danger_level);
            if (task.getPriority() > 75) {
                danger.setBackgroundColor(Utils.getColor(view.getContext(), "Red"));
                viewHolder.lastMessage.setTextColor(Utils.getColor(view.getContext(), "Red"));
                viewHolder.sendReminder.setText(view.getContext().getString(R.string.message_emp));
                viewHolder.sendReminder
                        .setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                closeAllRows();
                                Utils.sendCustomSMS(v.getContext(), task.getEmployeeNumber());
                            }
                        });
            } else if (task.getPriority() > 0) {
                danger.setBackgroundColor(Utils.getColor(view.getContext(), "Orange"));
                viewHolder.lastMessage.setTextColor(Utils.getColor(view.getContext(), "Orange"));
                viewHolder.sendReminder.setText(view.getContext().getString(R.string.message_emp));
                viewHolder.sendReminder
                        .setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                closeAllRows();
                                Utils.sendCustomSMS(v.getContext(), task.getEmployeeNumber());
                            }
                        });
            } else {
                // Priority = 0 (task is on track)
                danger.setBackgroundColor(Utils.getColor(view.getContext(), "Transparent"));
            }

            // For newly added item, change the appearance
            if (position == 0 && flashNewlyAddedRows
                    && (System.currentTimeMillis() - flashRequestTime) < 2000) {
                view.setBackgroundColor(mContext.getResources().getColor(
                        android.R.color.holo_green_light));
                viewHolder.taskTitle.setTextColor(Utils.getColor(mContext, "black"));
                viewHolder.lastMessage.setTextColor(Utils.getColor(mContext,
                        "black"));
                viewHolder.update.setTextColor(Utils.getColor(mContext,
                        "black"));
            }

            return view;
        }

        private void starTask(ImageButton fav, final MigratedTask task) {
            if (task.getFav() == 0) {
                fav.setBackgroundResource(R.drawable.star_on);
                task.setFav(1);
            } else {
                fav.setBackgroundResource(R.drawable.star_off);
                task.setFav(0);
            }

            final Context context = fav.getContext();
            DataManager dataMgr = DataManager.getInstance(context);
            dataMgr.updateTask(task, new DataManagerCallback() {

                @Override
                public void onSuccess(String response) {
                    // Notify user about sorting
                    if (task.getFav() == 0) {
                        Toast.makeText(
                                context,
                                context.getResources().getString(
                                        R.string.task_sorting),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(
                                context,
                                context.getResources().getString(
                                        R.string.task_sorting_up),
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(String response) {
                    // Notify user about the error
                    Toast.makeText(
                            context,
                            context.getResources().getString(
                                    R.string.error_task_update_failed),
                            Toast.LENGTH_SHORT).show();
                    refreshTaskList(context);
                }
            });
        }

        protected void deleteTask(final Context context, String taskId, int position) {
            DataManager dataMgr = DataManager.getInstance(context);
            dataMgr.deleteTask(taskId, new DataManagerCallback() {

                @Override
                public void onSuccess(String response) {
                }

                @Override
                public void onFailure(String response) {
                    // Notify user about the error
                    Toast.makeText(
                            context,
                            context.getResources().getString(
                                    R.string.error_task_update_failed),
                            Toast.LENGTH_SHORT).show();
                    refreshTaskList(context);
                }
            });

            final MigratedTask task = getItem(position);
            if (task.getTaskID().equalsIgnoreCase(taskId)) {
                this.remove(task);
                Utils.refreshListWithoutLosingScrollPosition(mSwipeListView, this);
            }
        }

        protected void markTaskCompleted(final Context context,
                                         MigratedTask task, int position) {
            DataManager dataMgr = DataManager.getInstance(context);
            task.setStatus(Constants.CLOSED);
            dataMgr.updateTask(task, new DataManagerCallback() {

                @Override
                public void onSuccess(String response) {
                }

                @Override
                public void onFailure(String response) {
                    // Notify user about the error
                    Toast.makeText(
                            context,
                            context.getResources().getString(
                                    R.string.error_task_update_failed),
                            Toast.LENGTH_SHORT).show();
                    refreshTaskList(context);
                }
            });

            // Remove the completed task from the listview
            MigratedTask tempTask = getItem(position);
            if (task.getTaskID().equalsIgnoreCase(tempTask.getTaskID())) {
                this.remove(task);
                Utils.refreshListWithoutLosingScrollPosition(mSwipeListView, this);
            } else {
                refreshTaskList(context);
            }
        }

        protected void forceReminderOnTask(final Context context,
                                         MigratedTask task, int position) {
            DataManager dataMgr = DataManager.getInstance(context);
            task.setForceReminder(1);
            dataMgr.updateTask(task, new DataManagerCallback() {

                @Override
                public void onSuccess(String response) {
                    Toast.makeText(
                            context,
                            context.getResources().getString(
                                    R.string.task_force_reminder_requested),
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String response) {
                    // Notify user about the error
                    Toast.makeText(
                            context,
                            context.getResources().getString(
                                    R.string.error_task_update_failed),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void markUpdatedRequestText(final Context context,
                                            final MigratedTask task, final int position) {
            AlertDialog.Builder alert = new AlertDialog.Builder(context);

            alert.setTitle(context.getString(R.string.optional_message));
            alert.setMessage(context.getString(R.string.optional_message_text));

            // Set an EditText view to get user input
            final EditText input = new EditText(context);
            alert.setView(input);

            alert.setPositiveButton(context.getString(R.string.add), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    markUpdated(context, task, position, input.getText().toString());
                }
            });

            alert.setNegativeButton(context.getString(R.string.skip), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    markUpdated(context, task, position, null);
                }
            });

            alert.show();
        }

        protected void markUpdated(final Context context,
                                   final MigratedTask task, int position, String message) {
            DataManager dataMgr = DataManager.getInstance(context);
            task.setMarkUpdated(1);
            task.setMarkUpdatedText(message);
            dataMgr.updateTask(task, new DataManagerCallback() {

                @Override
                public void onSuccess(String response) {
                    Toast.makeText(
                            context,
                            context.getResources().getString(
                                    R.string.task_mark_updated),
                            Toast.LENGTH_SHORT).show();

                    String messageStr = context.getResources().getString(R.string.task_mark_updated_by_owner);
                    MigratedMessage newMessage = new MigratedMessage();
                    newMessage.setTaskID(task.getTaskID());
                    newMessage.setTime(System.currentTimeMillis());
                    newMessage.setEmployeeID(Utils.getSignedInUserId());
                    newMessage.setSystemGenerated(true);
                    newMessage.setText(messageStr);
                    DataManager.getInstance(context).createMessage(newMessage, null);
                    task.setPriority(0);
                    refreshTaskList(context);
                }

                @Override
                public void onFailure(String response) {
                    // Notify user about the error
                    Toast.makeText(
                            context,
                            context.getResources().getString(
                                    R.string.error_task_update_failed),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        protected void closeAllRows() {
            mSwipeListView.closeOpenedItems();
        }

        public void sortAdapterList(final int position) {
            sort(new Comparator<MigratedTask>() {
                @Override
                public int compare(MigratedTask lhs, MigratedTask rhs) {
                    return compareTasksBasedOnTaskSortArray(lhs, rhs, position);
                }
            });

            notifyDataSetChanged();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, final int position,
                               long id) {
        currentSortPreference = position;
        mAdapter.sortAdapterList(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub

    }

    public void addItem() {
        oldItemCount = mAdapter.getCount();
        Intent i = new Intent(getActivity(), TaskAddViewPagerActivity.class);
//        Intent i = new Intent(getActivity(), TaskAddViewPagerActivity.class);
//        startActivityForResult(i, ADD_TASK);
        startActivity(i);
    }

    public void onDestroy() {
        // Unregister since the activity is going away
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    public void onPause() {
        isShowing = false;
        super.onPause();
    }

    // You need to do the Play Services APK check here too.
    @Override
    public void onResume() {
        super.onResume();
        isShowing = true;
        checkPlayServices();
        if (refreshPending) {
            refreshPending = false;
            refreshUI();
        }

        Utils.trackPageView(Constants.TASK_LIST_PV);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public boolean checkPlayServices() {
        if (BuildConfig.DEBUG) {
            return true;
        }

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(),
                        GCMUtils.PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Utils.log("This device is not supported.");
            }
            return false;
        }
        return true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (uiHandler == null) {
            uiHandler = new Handler();
        }

        // Check device for Play Services APK. If check succeeds, proceed with
        //  GCM registration.
        if (checkPlayServices()) {
            setRecurringGCMRegistration();
            String regId = GCMUtils.getInstance().getRegistrationId(getActivity());

            if (Utils.isStringEmpty(regId)) {
                GCMUtils.getInstance().registerInBackground(getActivity());
            } else {
                sendRegistrationIdToBackend(regId);
            }
        } else {
            Utils.log("No valid Google Play Services APK found.");
        }
    }

    private void setRecurringGCMRegistration() {
        // Retrieve a PendingIntent that will perform a broadcast
        Intent alarmIntent = new Intent(getActivity(), GcmRegistrationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, alarmIntent, 0);
        AlarmManager manager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    private void sendRegistrationIdToBackend(final String regId) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                GCMUtils.getInstance().sendRegistrationIdToBackendBlockingCall(getActivity(), regId);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bundle extras = null;
        switch (requestCode) {
            case DETAIL_TASK:
                if (data != null) {
                    extras = data.getExtras();
                }
                if (extras != null) {
                    // Update the task title which was just changed so that the UI looks
                    // responsive to the user.
                    String taskID = extras.getString(Constants.TASK_ID);
                    String title = extras.getString(Constants.TASK_TITLE);
                    String employeeId = extras.getString(Constants.EMPLOYEE_ID);
                    String name = extras.getString(Constants.EMPLOYEE_NAME);
                    String number = extras.getString(Constants.EMPLOYEE_PHONE);
                    MigratedTask editedTask = null;
                    for (int i = 0; i < mAdapter.getCount(); i++) {
                        MigratedTask tmpTask = mAdapter.getItem(i);
                        if (tmpTask.getTaskID().equalsIgnoreCase(taskID)) {
                            editedTask = tmpTask;
                            break;
                        }
                    }
                    if (editedTask != null) {
                        editedTask.setTitle(title);
                        editedTask.setEmployeeID(employeeId);
                        editedTask.setEmployeeName(name);
                        editedTask.setEmployeeNumber(number);
                        editedTask.setLastUpdate(System.currentTimeMillis());
                        mAdapter.sortAdapterList(currentSortPreference);
                    }
                }
                mAdapter.notifyDataSetChanged();
                break;
            case ADD_TASK:
                Utils.log("onActivityResult " + requestCode);
                if (data != null) {
                    extras = data.getExtras();
                }
                if (extras != null) {
                    // Create a fake task row and add it so that the UI looks
                    // responsive to the user.
                    String key = extras.getString(Constants.TASK_ARRAY);
                    ArrayList<MigratedTask> tempTasks = (ArrayList<MigratedTask>) DataManager
                            .getInstance(getActivity()).retrieveData(
                                    key);

                    mAdapter.addAll(tempTasks);
                    mAdapter.sortAdapterList(currentSortPreference);
                }

                if (mAdapter.getCount() > oldItemCount) {
                    mAdapter.flashNewlyAddedRows = true;
                    mAdapter.flashRequestTime = System.currentTimeMillis();
                }

                Utils.refreshListWithoutLosingScrollPosition(swipelistview, mAdapter);
                // When a new task is created, we delay the refresh of the main task list so that
                // server can propagate the newly created task and we get it back correctly when we
                // fetch all tasks
                mDelayRefresh = true;
                break;
            default:
                break;
        }
    }

    public void refreshUI() {
        refreshPending = false;
        mAdapter.refreshTaskList(getActivity());
    }

    @Override
    public void search() {
        // TODO Auto-generated method stub

    }
}
