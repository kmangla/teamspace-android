package com.teamspace.android.models;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.teamspace.android.caching.DatabaseCache;
import com.teamspace.android.common.ui.TaskManagerApplication;
import com.teamspace.android.utils.Constants;
import com.teamspace.android.utils.ISO8601DateParser;
import com.teamspace.android.utils.Utils;

public class MigratedTask {

	private String taskID;
	private String status;
	private String title;
	private String userID;
    private boolean sendReminderNow;
	private String employeeID;
	private String employeeName;
	private String employeeNumber;
	private String assignedBy;
	private String companyID;
	private long frequency;
	private long createdOn;
	private long lastReminder;
	private long lastUpdate;
	private long lastSeen;
	private long updateCount;
    private long forceReminder;
    private long markUpdated;

    public long getFav() {
        return fav;
    }

    public void setFav(long fav) {
        this.fav = fav;
    }

    private long fav;
    private String markUpdatedText;

    private long priority;
    private MigratedMessage lastMessage;
	
	private static String TASK_ID = "taskID";
	private static String STATUS = "status";
	private static String DESCRIPTION = "description";
	private static String TITLE = "title";
	private static String USER_ID = "userID";
	private static String EMPLOYEE_ID = "employeeID";
	private static String EMPLOYEE_NAME = "employeeName";
	private static String EMPLOYEE_NUMBER = "employeeNumber";
	private static String COMPANY_ID = "companyID";
	private static String FREQUENCY = "frequency";
    private static String FORCE_REMINDER = "forceReminder";
    private static String MARK_UPDATED = "markUpdated";
    private static String MARK_UPDATED_TEXT = "markUpdatedText";
	private static String CREATED_AT = "createdAt";
	private static String LAST_REMINDER = "lastReminder";
	private static String UPDATED_AT = "updatedAt";
	private static String LAST_SEEN = "lastSeen";
	private static String UPDATE_COUNT = "updateCount";
    private static String LAST_MESSAGE = "last_message";
	private static String ASSIGNED_BY = "assignedBy";
	private static String ID = "id";
	private static String ASSIGNED_TO = "assignedTo";
    private static String LAST_MSG = "lastMessage";
    private static String PRIORITY = "priority";
    private static String FAV = "fav";
    private static String SEND_REMINDER_NOW = "sendReminderNow";


    public boolean isCreationPending() {
        return (updateCount < 0);
    }
	public long getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(long createdOn) {
		this.createdOn = createdOn;
	}
	public long getLastReminder() {
		return lastReminder;
	}
	public void setLastReminder(long lastReminder) {
		this.lastReminder = lastReminder;
	}
	public long getLastUpdate() {
		return lastUpdate;
	}
	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	public long getLastSeen() {
		return lastSeen;
	}
	public long getUpdateCount() {
		return updateCount;
	}
	public void setLastSeen(long lastSeen) {
		this.lastSeen = lastSeen;
	}
	public String getTaskID() {
		return taskID;
	}
	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getTitle() {
		return title;
	}
    public MigratedMessage getLastMessage() {
        return lastMessage;
    }
    public void setLastMessage(MigratedMessage lastMessage) {
        lastMessage.setTaskID(this.taskID);
        this.lastMessage = lastMessage;
    }
	public void setTitle(String title) {
		this.title = title;
	}
	public void setUpdateCount(long count) {
		this.updateCount = count;
	}
	public boolean getRemindNow() {
		return sendReminderNow;
	}
	public void setRemindNow(boolean remindNow) {
		this.sendReminderNow = remindNow;
	}
    public String getUserID() {
        return userID;
    }
    public void setUserID(String userID) {
        this.userID = userID;
    }
    public long getPriority() {
        return priority;
    }
    public void setPriority(long priority) {
        this.priority = priority;
    }
    public long getForceReminder() {
        return forceReminder;
    }
    public void setForceReminder(long forceReminder) {
        this.forceReminder = forceReminder;
    }
    public long getMarkUpdated() {
        return markUpdated;
    }
    public void setMarkUpdated(long markUpdated) {
        this.markUpdated = markUpdated;
    }
	public String getEmployeeID() {
		return employeeID;
	}
	public String getEmployeeName() {
		return employeeName;
	}
	public String getEmployeeNumber() {
		return employeeNumber;
	}
	public void setEmployeeID(String employeeID) {
        if (Utils.isStringEmpty(employeeID)) {
            return;
        }

        this.employeeID = employeeID;

        if (employeeID.equalsIgnoreCase(Utils.getSignedInUserId())) {
            // Assign the task to self
            this.employeeName = Utils.getSignedInUserName();
            this.employeeNumber = Utils.getSignedInUserPhoneNumber();
        } else {
            // Assign the task to employee
            MigratedEmployee emp = DatabaseCache.getInstance(
                    TaskManagerApplication.getAppContext())
                    .getMigratedEmployeeBlockingCall(employeeID);
            if (emp != null) {
                this.employeeName = emp.getName();
                this.employeeNumber = emp.getPhoneWithCountryCode();
            } else {
                Utils.log("Error: Could not find employee with id " + employeeID);
            }
        }
	}
	public void setEmployeeName(String employeeName) {
		this.employeeName = employeeName;
	}
	public void setEmployeeNumber(String employeeNumber) {
		this.employeeNumber = employeeNumber;
	}
	public String getCompanyID() {
		return companyID;
	}
	public void setCompanyID(String companyID) {
		this.companyID = companyID;
	}
	public long getFrequency() {
		return frequency;
	}
	public void setFrequency(long frequency) {
		this.frequency = frequency;
	} 
	
	@Override
	public String toString() {
		return this.taskID;
	}
	
	public JSONObject toJSONObject() throws JSONException { 
		JSONObject obj  = new JSONObject();
		obj.put(TASK_ID, this.getTaskID());
		obj.put(USER_ID, this.getUserID());
		obj.put(COMPANY_ID, this.getCompanyID());
		obj.put(EMPLOYEE_ID, this.getEmployeeID());
		obj.put(EMPLOYEE_NAME, this.getEmployeeName());
		obj.put(EMPLOYEE_NUMBER, this.getEmployeeNumber());
		obj.put(TITLE, this.getTitle());
		obj.put(STATUS, this.getStatus());
		obj.put(FREQUENCY, this.getFrequency());
		obj.put(CREATED_AT, ISO8601DateParser.toString(new Date(this.getCreatedOn())));
		obj.put(LAST_REMINDER, ISO8601DateParser.toString(new Date(this.getLastReminder())));
		obj.put(UPDATED_AT, ISO8601DateParser.toString(new Date(this.getLastUpdate())));
		obj.put(LAST_SEEN, ISO8601DateParser.toString(new Date(this.getLastSeen())));
		obj.put(UPDATE_COUNT, this.getUpdateCount() + "");
        obj.put(LAST_MESSAGE, this.getLastMessage() + "");

		return obj;
	}
	
	// These are the params required in POST requests to the server for creating or
	// updating a particular task.
	public HashMap<String, String> toMapObject() { 
		HashMap<String, String> obj  = new HashMap<String, String>();
		if (this.getTaskID() != null) {
			obj.put(TASK_ID, this.getTaskID());
		}
		obj.put(EMPLOYEE_ID, this.getEmployeeID());
		obj.put(TITLE, this.getTitle());
		obj.put(STATUS, this.getStatus());
		obj.put(FREQUENCY, this.getFrequency() + "");
		obj.put(Constants.KEY, Utils.getSignedInUserKey());
        obj.put(FORCE_REMINDER, String.valueOf(this.getForceReminder()));
        obj.put(MARK_UPDATED, String.valueOf(this.getMarkUpdated()));
        obj.put(FAV, String.valueOf(this.getFav()));
        obj.put(SEND_REMINDER_NOW, String.valueOf(this.getRemindNow()));
        if (this.getMarkUpdatedText() != null){
            obj.put(MARK_UPDATED_TEXT, this.getMarkUpdatedText());
        }
		return obj;
	}
	
	public static MigratedTask parseJSON(JSONObject object) throws JSONException, ParseException {
		MigratedTask task = new MigratedTask();
		task.taskID = object.getString(ID);
		task.userID = object.getString(ASSIGNED_BY);
		task.companyID = object.getString(ASSIGNED_BY);		
		task.title = object.getString(TITLE);
		task.status = object.getString(STATUS);
		task.frequency = object.getLong(FREQUENCY);
		
		try {
			task.updateCount = object.getLong(UPDATE_COUNT);
		} catch (JSONException e) {
			task.updateCount = 0; 
		}

        try {
            task.fav = object.getLong(FAV);
        } catch (JSONException e) {
            task.fav = 0;
        }

		try {
			task.assignedBy = object.getString(ASSIGNED_BY);
		} catch (JSONException e) {
			Utils.log("Json parsing exception in MigratedTask - assignedBy field not found in server response");
            task.assignedBy = Utils.getSignedInUserId();
		}

        try {
            task.priority = object.getLong(PRIORITY);
        } catch (JSONException e) {
            Utils.log("Json parsing exception in MigratedTask - priority field not found in server response");
            task.priority = 0;
        }

        try {
            task.lastMessage  = MigratedMessage.parseJSON(object.getJSONObject(LAST_MSG));
            task.lastMessage.setTaskID(task.taskID);
        } catch (JSONException e) {
            Utils.log("Json parsing exception in MigratedTask - lastMessage field is not found in server responses");
        }

		try {
			MigratedEmployee emp = MigratedEmployee.parseJSON(object.getJSONObject(ASSIGNED_TO));
			task.setEmployeeID(emp.getEmployeeID());
			task.setEmployeeName(emp.getName());
			task.setEmployeeNumber(emp.getPhoneWithCountryCode());
		} catch (JSONException e) {
            Utils.log("Json parsing exception in MigratedTask - assignedTo field is not MigratedEmployee in server response");
            task.setEmployeeID(Constants.EMPTY_STRING);
		}

        // If the ASSIGNED_TO field did not match correctly, try to find employeeId in that field.
        // This is workaround for server issue: https://github.com/pratyus/ts/issues/1
        if (Utils.isStringEmpty(task.getEmployeeID())) {
            try {
                task.setEmployeeID(object.getString(ASSIGNED_TO));
            } catch (JSONException e) {
                Utils.log("Json parsing exception in MigratedTask - assignedTo field is not found in server response");
            }
        }

		try {
			task.createdOn = ((Date)ISO8601DateParser.parse(object.getString(CREATED_AT))) .getTime();
		} catch (JSONException e) {
			task.createdOn = 0; 
		}
		try { 
			task.lastReminder = ((Date)ISO8601DateParser.parse(object.getString(LAST_REMINDER))) .getTime();
		} catch (JSONException e) { 
			task.lastReminder = 0; 
		}
		try {
			task.lastUpdate = ((Date)ISO8601DateParser.parse(object.getString(UPDATED_AT))) .getTime();
		} catch (JSONException e) {
			task.lastUpdate = 0;
		}
		try {
			task.lastSeen = ((Date)ISO8601DateParser.parse(object.getString(LAST_SEEN))) .getTime();
		} catch (JSONException e) {
			task.lastSeen = 0; 
		}

		return task;
	}
	public void setAssignedTo1(MigratedEmployee assignedTo) {
	}
	public String getAssignedBy() {
		return assignedBy;
	}
	public void setAssignedBy(String assignedBy) {
		this.assignedBy = assignedBy;
	}

    public String getMarkUpdatedText() {
        return markUpdatedText;
    }

    public void setMarkUpdatedText(String markUpdatedText) {
        this.markUpdatedText = markUpdatedText;
    }
}
