package com.teamspace.android.caching.dataupdaters;

import java.text.ParseException;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.teamspace.android.R;
import com.teamspace.android.caching.DataManager;
import com.teamspace.android.caching.DataManagerCallback;
import com.teamspace.android.caching.DatabaseCache;
import com.teamspace.android.models.MigratedEmployee;
import com.teamspace.android.networking.NetworkRoutes;
import com.teamspace.android.networking.NetworkingLayer;
import com.teamspace.android.utils.Utils;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

public class EmployeeUpdater {
	private Context mContext;
	private MigratedEmployee mEmployee;
	private DataManagerCallback mCallback;
	
	public EmployeeUpdater(Context context, MigratedEmployee employee, DataManagerCallback callback) {
		this.mContext = context;
		this.mEmployee = employee;
		this.mCallback = callback;
	}

	public void create() {
		final String url = NetworkRoutes.getRouteBase() + NetworkRoutes.ROUTE_EMPLOYEE + "?cb=" + String.valueOf(System.nanoTime());
		final HashMap<String, String> params = mEmployee.toMapObject();
		NetworkingLayer.getInstance(mContext).makePostRequest(
				url,
				params,
				new Response.Listener<String>() {
				    @Override
				    public void onResponse(String response) {
				    	Utils.log("createEmployee() POST response for url: " + url + " params: " + params
								+ " got response: " + response);
                        try {
                            mEmployee = MigratedEmployee.parseJSON(new JSONObject(response));
                            DatabaseCache.getInstance(mContext).setMigratedEmployeeBlockingCall(mEmployee);
                            if (mCallback != null) {
                                mCallback.onSuccess(response);
                            }
                            String message = mContext.getResources().getString(R.string.employee_creation_sms_1) + " +" +
                                    mEmployee.getPairedNumber() + ". " +
                                    mContext.getResources().getString(R.string.employee_creation_sms_2) + " " +
                                    Utils.getSignedInUserName();
                            Utils.sendSMS(mContext, mEmployee.getPhoneWithCountryCode(), message);
                        } catch (ParseException e) {
                            // Notify user about the error
                            Toast.makeText(
                                    mContext,
                                    mContext.getResources()
                                            .getString(
                                                    R.string.error_employee_failed_parse),
                                    Toast.LENGTH_LONG).show();
                            Utils.logErrorToServer(mContext, url,
                                    200,
                                    null,
                                    "Failed to create employee because server's response could not be parsed even though server returned 200");
                        } catch (JSONException e) {
                            // Notify user about the error
                            Toast.makeText(
                                    mContext,
                                    mContext.getResources()
                                            .getString(
                                                    R.string.error_employee_failed_json),
                                    Toast.LENGTH_LONG).show();
                            Utils.logErrorToServer(mContext, url,
                                    200,
                                    null,
                                    "Failed to create employee because server's response could not be JSON parsed even though server returned 200");
                        }
				    }
				},		
				new Response.ErrorListener() {

				    @Override
				    public void onErrorResponse(VolleyError error) {
                        Utils.log("createEmployee() POST failed for url: " + url + " params: " + params
                                + " got error: " + error);
                        // Notify user about the error
                        Toast.makeText(
                                mContext,
                                mContext.getResources()
                                        .getString(
                                                R.string.error_employee_create_failed),
                                Toast.LENGTH_LONG).show();
				    	error.printStackTrace();
				    	if (mCallback != null) {
				    		mCallback.onFailure(error.getLocalizedMessage());
						}
                        Utils.logErrorToServer(mContext, url,
                                error.networkResponse != null ? error.networkResponse.statusCode : -1,
                                error.networkResponse != null ? error.networkResponse.toString() : null,
                                "Failed to create employee because server returned error");
				    }  
				});
	}
	
	public void update() {
		// Update the employee in the cache
		DatabaseCache.getInstance(mContext).updateMigratedEmployee(mContext, mEmployee);
		
		// Update the employee on the server
		final String url = NetworkRoutes.getRouteBase() + NetworkRoutes.ROUTE_EMPLOYEE +
				"/" + mEmployee.getEmployeeID();
		final HashMap<String, String> params = mEmployee.toMapObject();
		NetworkingLayer.getInstance(mContext).makePostRequest(
				url,
				params,
				new Response.Listener<String>() {
				    @Override
				    public void onResponse(String response) {
				    	Utils.log("updateEmployee() POST on url " + url + " with params " + params + " got response: " + response);
				    	if (mCallback != null) {
				    		mCallback.onSuccess(response);
				    	}
				    }
				},		
				new Response.ErrorListener() {

				    @Override
				    public void onErrorResponse(VolleyError error) {
                        Utils.log("updateEmployee() POST on url " + url + " with params " + params + " failed");
                                error.printStackTrace();
				    	
				    	// We had already updated the employee in the cache. We
						// now need to fix our cache
						DataManager.getInstance(mContext).fetchEmployeesForUser(
								Utils.getSignedInUserId(), null);
						
						if (mCallback != null) {
							mCallback.onFailure(error.getLocalizedMessage());
						}
                        Utils.logErrorToServer(mContext, url,
                                error.networkResponse != null ? error.networkResponse.statusCode : -1,
                                error.networkResponse != null ? error.networkResponse.toString() : null,
                                "Failed to update employee because server returned error");
				    }  
				});
	}

	public void delete() {
		delete(mEmployee.getEmployeeID());
	}

	public void delete(final String employeeId) {
		// Delete the employee from cache
		DatabaseCache.getInstance(mContext).deleteMigratedEmployee(mContext, employeeId);

		// Delete the employee from server
		final String url = NetworkRoutes.getRouteBase() + NetworkRoutes.ROUTE_EMPLOYEE +
				"/" + employeeId + "?key=" + Utils.getSignedInUserKey();
		NetworkingLayer.getInstance(mContext).makeDeleteRequest(
				url,
				new Response.Listener<String>() {
				    @Override
				    public void onResponse(String response) {
				    	Utils.log("deleteEmployee() DELETE for url " + url + " got response: " + response);
				    	if (mCallback != null) {
				    		mCallback.onSuccess(response);
				    	}
				    }
				},		
				new Response.ErrorListener() {

				    @Override
				    public void onErrorResponse(VolleyError error) {
                        Utils.log("deleteEmployee() DELETE for url " + url + " failed");
				    	error.printStackTrace();
				    	
						// We had already deleted the employee from cache. We
						// now need to fix our cache
						DataManager.getInstance(mContext).fetchEmployeesForUser(
								Utils.getSignedInUserId(), null);
						
						if (mCallback != null) {
							mCallback.onFailure(error.getMessage());
				    	}
                        Utils.logErrorToServer(mContext, url,
                                error.networkResponse != null ? error.networkResponse.statusCode : -1,
                                error.networkResponse != null ? error.networkResponse.toString() : null,
                                "Failed to delete employee because server returned error");
				    }  
				});
	}
}
