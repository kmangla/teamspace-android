package com.android.teamspace.caching.dataupdaters;

import java.util.HashMap;

import android.content.Context;
import android.util.Log;

import com.android.teamspace.caching.DataManager;
import com.android.teamspace.caching.DataManagerCallback;
import com.android.teamspace.caching.DatabaseCache;
import com.android.teamspace.models.MigratedEmployee;
import com.android.teamspace.networking.NetworkRoutes;
import com.android.teamspace.networking.NetworkingLayer;
import com.android.teamspace.utils.Utils;
import com.android.volley.Response;
import com.android.volley.VolleyError;

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
		final String url = NetworkRoutes.ROUTE_BASE + NetworkRoutes.ROUTE_EMPLOYEE;
		final HashMap<String, String> params = mEmployee.toMapObject();
		NetworkingLayer.getInstance(mContext).makePostRequest(
				url,
				params,
				new Response.Listener<String>() {
				    @Override
				    public void onResponse(String response) {
				    	Utils.log("createEmployee() POST response for url: " + url + " params: " + params
								+ " got response: " + response);
				    	mEmployee.setEmployeeID(response);
						DatabaseCache.getInstance(mContext).setMigratedEmployeeBlockingCall(mEmployee);
						if (mCallback != null) {
							mCallback.onSuccess(response);
						}
				    }
				},		
				new Response.ErrorListener() {

				    @Override
				    public void onErrorResponse(VolleyError error) {
				    	Utils.log("createEmployee() POST failed");
				    	error.printStackTrace();
				    	if (mCallback != null) {
				    		mCallback.onFailure(error.getLocalizedMessage());
						}
				    }  
				});
	}
	
	public void update() {
		// Update the employee in the cache
		DatabaseCache.getInstance(mContext).updateMigratedEmployee(mContext, mEmployee);
		
		// Update the employee on the server
		String url = NetworkRoutes.ROUTE_BASE + NetworkRoutes.ROUTE_EMPLOYEE + 
				":" + mEmployee.getEmployeeID();
		HashMap<String, String> params = mEmployee.toMapObject();
		NetworkingLayer.getInstance(mContext).makePostRequest(
				url,
				params,
				new Response.Listener<String>() {
				    @Override
				    public void onResponse(String response) {
				    	Utils.log("updateEmployee() POST response: " + response);
				    	if (mCallback != null) {
				    		mCallback.onSuccess(response);
				    	}
				    }
				},		
				new Response.ErrorListener() {

				    @Override
				    public void onErrorResponse(VolleyError error) {
				    	Utils.log("updateEmployee() POST failed");
				    	error.printStackTrace();
				    	
				    	// We had already updated the employee in the cache. We
						// now need to fix our cache
						DataManager.getInstance(mContext).fetchEmployeesForUser(
								Utils.getSignedInUserId(), null);
						
						if (mCallback != null) {
							mCallback.onFailure(error.getLocalizedMessage());
						}
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
		String url = NetworkRoutes.ROUTE_BASE + NetworkRoutes.ROUTE_EMPLOYEE + 
				"/:" + employeeId + "?key=" + Utils.getSignedInUserPhoneNumber();
		NetworkingLayer.getInstance(mContext).makeDeleteRequest(
				url,
				new Response.Listener<String>() {
				    @Override
				    public void onResponse(String response) {
				    	Utils.log("deleteEmployee() DELETE response: " + response);
				    	if (mCallback != null) {
				    		mCallback.onSuccess(response);
				    	}
				    }
				},		
				new Response.ErrorListener() {

				    @Override
				    public void onErrorResponse(VolleyError error) {
				    	Utils.log("deleteEmployee() DELETE failed");
				    	error.printStackTrace();
				    	
						// We had already deleted the employee from cache. We
						// now need to fix our cache
						DataManager.getInstance(mContext).fetchEmployeesForUser(
								Utils.getSignedInUserId(), null);
						
						if (mCallback != null) {
							mCallback.onFailure(error.getMessage());
				    	}
				    }  
				});
	}
}
