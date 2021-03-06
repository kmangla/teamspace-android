package com.teamspace.android.common.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.teamspace.android.caching.DatabaseCache;
import com.teamspace.android.caching.dataupdaters.TaskUpdater;
import com.teamspace.android.models.Employee;
import com.teamspace.android.models.EmployeeState;
import com.teamspace.android.models.MigratedTask;
import com.teamspace.android.models.Reply;
import com.teamspace.android.models.Task;

public class TaskManagerApplication extends Application {

	DatabaseCache taskManagerData;
	private static Context appContext;

	private long lastRunTime = 0;

	public long getLastRunTime() {
		return lastRunTime;
	}

	public void setLastRunTime(long lastRunTime) {
		this.lastRunTime = lastRunTime;
	}
	
	public static Context getAppContext() {
        return TaskManagerApplication.appContext;
    }

    /**
     * Enum used to identify the tracker that needs to be used for tracking.
     *
     * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
     * storing them all in Application object helps ensure that they are created only once per
     * application instance.
     */
    public enum TrackerName {
        APP_TRACKER // Tracker used only in this app.
    }

    static HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    public static synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(getAppContext());
            Tracker t = analytics.newTracker("UA-61068335-1");
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }

	@Override
	public void onCreate() {
		super.onCreate();
		
		appContext = this.getApplicationContext();
		taskManagerData = DatabaseCache.getInstance(this
				.getApplicationContext());
	}

	public ArrayList<Employee> getEmployees() { 
		return taskManagerData.getEmployees();
	}

	public Employee getEmployee(String employeeId) {
		return taskManagerData.getEmployee(employeeId);
	}

	public ArrayList<Task> getTasks(String employee) {
		return taskManagerData.getTasks(employee);
	}

	public ArrayList<Task> getCompletedTasks(String employee) {
		return taskManagerData.getCompletedTasks(employee);
	}

	public Task getTaskByID(long taskID) {
		return taskManagerData.getTaskByID(taskID);
	}

	public Employee getEmployeeByPhone(String number) {
		return taskManagerData.getEmployeeByPhone(number);
	}

	public EmployeeState getEmployeeState(String employeeID) {
		return taskManagerData.getEmployeeState(employeeID);
	}

	public ArrayList<Reply> getRepliesForTask(long taskID) {
		return taskManagerData.getRepliesForTask(taskID);
	}

	public boolean addTask(Task task) {
		return taskManagerData.addTask(task);
	}

	public boolean updateEmployee(Employee employee) {
		return taskManagerData.updateEmployee(employee);
	}

	public boolean addEmployee(Employee employee) {
		return taskManagerData.addEmployee(employee);
	}

	public void deleteEmployee(Employee employee) {
		taskManagerData.deleteEmployee(employee);
	}

	public int getUnreadRepliesForTask(Task task) {
		return taskManagerData.getUnreadRepliesForTask(task);
	}

	public int getPendingTasksCount(Employee employee) {
		return taskManagerData.getPendingTasksCount(employee);
	}

	public int getUnreadRepliesForEmployee(Employee employee) {
		return taskManagerData.getUnreadRepliesForEmployee(employee);
	}

	public int getIncompleteTasksCount(Employee employee) {
		return taskManagerData.getIncompleteTasksCount(employee);
	}

	public boolean updateEmployeeState(EmployeeState employeeState,
			String employeeID) {
		return taskManagerData.updateEmployeeState(employeeState, employeeID);
	}

	public boolean updateTask(Task task) {
		return taskManagerData.updateTask(task);
	}

	public boolean insertTaskPause(Task task) {
		return taskManagerData.insertTaskPause(task);
	}

	public boolean removeTaskPause(Task task) {
		return taskManagerData.removeTaskPause(task);
	}

	public boolean updateTaskPause(Task task, long pausedTill) {
		return taskManagerData.updateTaskPause(task, pausedTill);
	}

	public boolean isTaskPaused(Task task) {
		return taskManagerData.isTaskPaused(task);
	}

}
