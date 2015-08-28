package com.teamspace.android.models;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.teamspace.android.utils.ISO8601DateParser;
import com.teamspace.android.utils.Utils;

public class MigratedMessage {

	// Task that was replied to
	private String taskID;
	// Text of reply
	private String text;
	// Employee id for employee that send the reply
	private String employeeID;
	// Time in sec when the reply was received
	private long time;
	// Unique id for the reply
	private String messageID;
    // System generated message
    private boolean systemGenerated;
    // Message sent to receiver
    private boolean notifSent;
	
	private static String FOR_TASK = "forTask";
	private static String TASK_ID = "taskID";
	
	private static String TEXT = "description";
	private static String SENDER = "sentBy";
	private static String EMPLOYEE_ID = "employeeID";
	private static String TIME = "time";
	private static String MESSAGE_ID = "messageID";
	private static String ID = "id";
    private static String SYSTEM_GENERATED = "systemGenerated";
    private static String NOTIFICATION_SENT = "notifSent";
	private static String UPDATED = "updatedAt";
    private static String CREATED = "createdAt";
	
	public String getTaskID() {
		return taskID;
	}
	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getEmployeeID() {
		return employeeID;
	}
	public void setEmployeeID(String employeeID) {
		this.employeeID = employeeID;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public String getMessageID() {
		return messageID;
	}
	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}
    public boolean getSystemGenerated() {
        return systemGenerated;
    }
    public void setSystemGenerated(boolean systemGenerated) {
        this.systemGenerated = systemGenerated;
    }
    public boolean getNotifSent() {
        return notifSent;
    }
    public void setNotifSent(boolean notifSent) {
        this.notifSent = notifSent;
    }

	
//	public JSONObject toJSONObject() throws JSONException { 
//		JSONObject obj  = new JSONObject();
//		obj.put(TASK_ID, this.getTaskID());
//		obj.put(MESSAGE_ID, this.getMessageID());
//		obj.put(TEXT, this.getText());
//		obj.put(TIME, this.getTime());
//		obj.put(EMPLOYEE_ID, this.getEmployeeID());
//		obj.put(TIME, ISO8601DateParser.toString(new Date(this.getTime())));
//
//		return obj;
//	}
	
	public HashMap<String, String> toMapObject() { 
		HashMap<String, String> obj  = new HashMap<String, String>();
		obj.put(TASK_ID, this.getTaskID());
		obj.put(TEXT, this.getText());
		obj.put(SENDER, this.getEmployeeID());
        obj.put(SYSTEM_GENERATED, this.getSystemGenerated() ? "1" : "0");
		return obj;
	}

	public static MigratedMessage parseJSON(JSONObject object) throws JSONException, ParseException {
		MigratedMessage message = new MigratedMessage();
        try {
            message.taskID = object.getString(FOR_TASK);
        } catch (Exception e) {
            message.taskID = object.getString("taskID");
        }

        try {
            message.messageID = object.getString(ID);
        } catch (Exception e) {
            message.messageID = object.getString("messageID");
        }

        try {
            message.text = object.getString(TEXT);
        } catch (Exception e) {
            message.text = object.getString("text");
        }

        try {
            message.systemGenerated = object.getBoolean(SYSTEM_GENERATED);
        } catch (Exception e) {
        }

        try {
            message.notifSent = object.getBoolean(NOTIFICATION_SENT);
        } catch (Exception e) {
        }

        try {
            MigratedEmployee emp = MigratedEmployee.parseJSON(object.getJSONObject(SENDER));
            message.setEmployeeID(emp.getEmployeeID());
        } catch (Exception e) {
            try {
                message.setEmployeeID(object.getString(SENDER));
            } catch (Exception e1) {
                message.setEmployeeID(object.getString("employeeID"));
            }
        }

        try {
			message.time = ((Date)ISO8601DateParser.parse(object.getString(CREATED))).getTime();
		} catch (JSONException e) {
            try {
                Long timeStr = object.getLong("time");
                message.time = new Date(timeStr).getTime();
            } catch (JSONException e1) {
                message.time = System.currentTimeMillis();
            }
		}
		return message;
	}

}
