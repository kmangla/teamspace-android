package com.teamspace.android.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.teamspace.android.caching.DataManager;
import com.teamspace.android.common.ui.TaskManagerApplication;
import com.teamspace.android.utils.Constants;
import com.teamspace.android.utils.Utils;

public class MigratedEmployee {
	private String name;
	private String phone;
	private String companyID;
	private String userID;
	private String employeeID;
	private String designation;
	private String taskCount;
	private String lastUpdated;
    private String pairedNumber;
    private String taskBlob;
    private String countryCode;
	
	private static String NAME = "name";
	private static String PHONE = "phone";
	private static String COMPANY_ID = "companyID";
	private static String USER_ID = "userID";
	private static String EMPLOYEE_ID = "employeeID";
	private static String DESIGNATION = "designation";
    private static String TASK_BLOB = "taskBlob";
	private static String TASK_COUNT = "taskCount";
	private static String LAST_UPDATED = "lastUpdated";
    private static String COUNTRY_CODE = "countryCode";

    public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
    public String getTaskBlob() {
        return Utils.readStringFromSharedPrefs(Constants.EMPLOYEE_DRAFT + employeeID);
    }
    public void setTaskBlob(String blob) {
        this.taskBlob = blob;
    }
	public String getPhoneWithCountryCode() {
		return phone;
	}
	public void setPhoneWithContryCode(String phone) {
		this.phone = phone;
	}
    public String getPairedNumber() {
        return pairedNumber;
    }
    public void setPairedNumber(String pairedNumber) {
        this.pairedNumber = pairedNumber;
    }
	
	public String getPhoneWithoutCountryCode(Context context) {
		if (phone != null && phone.length() > 3) {
			return Utils.removeCountryPrefixFromPhoneNumber(context, phone);
		} else {
			return "";
		}
	}
	
	public String getCompanyID() {
		return companyID;
	}
	public void setCompanyID(String companyID) {
		this.companyID = companyID;
	}
	public String getUserID() {
		return userID;
	}
	public void setUserID(String userID) {
		this.userID = userID;
	}
	public String getEmployeeID() {
		return employeeID;
	}
	public void setEmployeeID(String employeeID) {
		this.employeeID = employeeID;
	}
	public String getDesignation() {
		return designation;
	}
	
	public String getTaskCount() {
		return taskCount;
	}
	
	public String getLastUpdated() {
		return lastUpdated;
	}
	
	public void setDesignation(String designation) {
		this.designation = designation;
	}
	
	public void setTaskCount(String count) {
		this.taskCount = count;
	}
	
	public void setLastUpdated(String xDaysAgo) {
		this.lastUpdated = xDaysAgo;
	}
	
	@Override
	public String toString() {
		return this.employeeID;
	}

    public HashMap<String, String> toMapObject() {
		HashMap<String, String> obj = new HashMap<String, String>();
		obj.put(NAME, this.getName());
		obj.put(PHONE, this.getPhoneWithCountryCode());
		if (this.getEmployeeID() != null) {
			obj.put(EMPLOYEE_ID, this.getEmployeeID());
		}
		obj.put(Constants.KEY, Utils.getSignedInUserKey());
        if (this.getTaskBlob() != null) {
            obj.put(TASK_BLOB, this.getTaskBlob());
        }
		return obj;
	}
	public static MigratedEmployee parseJSON(JSONObject object) throws JSONException, ParseException {
		MigratedEmployee employee = new MigratedEmployee();
		employee.name = object.getString(NAME);
		employee.phone = object.getString(PHONE);				
		employee.employeeID = object.getString("id");
		employee.designation = object.optString("accountType", "Employee");
        employee.taskCount = object.getString("taskCount");

		long time = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd, HH:mm");
        Date resultdate = new Date(time);
        String createdAt = object.getString("createdAt");
        
        employee.lastUpdated = object.optString("updatedAt", createdAt).substring(0, 10);

        try {
            employee.countryCode = object.getString(COUNTRY_CODE);
            employee.pairedNumber = object.getString("pairedNumber");
            employee.taskBlob = object.getString("taskBlob");
        } catch (JSONException e) {

        }
        
        try {
            MigratedEmployee manager = MigratedEmployee.parseJSON(object.getJSONObject("manager"));
			employee.userID = manager.getEmployeeID();
			employee.companyID = manager.getEmployeeID();
		} catch (JSONException e) {
			employee.userID = Utils.getSignedInUserId();
			employee.companyID = Utils.getSignedInUserId();
		}
        
		return employee;
	}

    public String getCountryCode() {
        return countryCode;
    }
}
