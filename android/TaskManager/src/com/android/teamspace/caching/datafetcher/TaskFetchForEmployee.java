package com.android.teamspace.caching.datafetcher;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.teamspace.caching.DataManager;
import com.android.teamspace.caching.DataManagerCallback;
import com.android.teamspace.caching.DatabaseCache;
import com.android.teamspace.interfaces.DataFetchInterface;
import com.android.teamspace.models.MigratedTask;
import com.android.teamspace.models.Task;
import com.android.teamspace.networking.NetworkRoutes;
import com.android.teamspace.networking.NetworkingLayer;
import com.android.teamspace.utils.Utils;
import com.android.volley.Response;
import com.android.volley.VolleyError;

public class TaskFetchForEmployee implements DataFetchInterface {

	private String employeeID;
	private Context context;
	
	public TaskFetchForEmployee(Context context, String employeeID) {
		this.employeeID = employeeID;
		this.context = context;
	}
	
	@Override
	public void fetchDataFromServer(final DataManagerCallback callback) {
		final String dataStoreKey = "tasks_from_server_" + System.nanoTime();
		// Update the cache with the fresh data
		final String url = NetworkRoutes.ROUTE_BASE + NetworkRoutes.ROUTE_TASK
					 + "?employeeID=" + employeeID + "&key=" + Utils.getSignedInUserPhoneNumber();
		
		NetworkingLayer.getInstance(context).makeGetJSONArrayRequest(
		url,
		new Response.Listener<JSONArray>() {
		    @Override
		    public void onResponse(JSONArray response) {
		    	// We now have the network response. No need to look in the cache anymore.
		    	if (callback != null) {
		    		callback.networkResponseReceived = true;
		    	}
		    	
		    	Utils.log("TaskFetchForEmployee GET response for url: " + url
						+ " got response: " + response);
		    	
				// Call the callback
				DatabaseCache.getInstance(context).deleteAllMigratedTasksBlockingCall(employeeID);
				ArrayList<MigratedTask> tasks = new ArrayList<MigratedTask>();
				for(int i = 0; i < response.length(); i++)
				{
					try {
				      JSONObject object = response.getJSONObject(i);
				      MigratedTask task = MigratedTask.parseJSON(object);
				      tasks.add(task);
				      DatabaseCache.getInstance(context).setMigratedTask(task);
					} catch (JSONException e) {
						Log.e("TaskFetchForEmployee fetchDataFromServer json_parsing_exception", e.getMessage());
					} catch (java.text.ParseException e) {
						Log.e("TaskFetchForEmployee fetchDataFromServer parsing_exception", e.getMessage());
					}
				}
				DataManager.getInstance(context).insertData(dataStoreKey, tasks);

				if (callback != null) {
					callback.onDataReceivedFromServer(dataStoreKey);
				}
		    }
		},		
		new Response.ErrorListener() {

		    @Override
		    public void onErrorResponse(VolleyError error) {
		    	Utils.log("fetchDataFromServer() network call failed fetching tasks for a given empoyee " + error);
		    	error.printStackTrace();
		    }  
		});
	}

	@Override
	public void fetchDataFromDatabaseCache(final DataManagerCallback callback) {
		AsyncTask<String, Void, ArrayList<MigratedTask>> asyncTask = new AsyncTask<String, Void, ArrayList<MigratedTask>>() {
			@Override
			protected ArrayList<MigratedTask> doInBackground(String... employee) {
				return DatabaseCache.getInstance(context).getMigratedTasksForEmployeeBlockingCall(employee[0]);
			}
			
			@Override
		    protected void onPostExecute(ArrayList<MigratedTask> tasks) {
				// Now we have the data from cache. We return this cached data
				// only if the network has not already returned fresh data.
				if (callback != null && !callback.networkResponseReceived) {
					String dataStoreKey = "tasks_from_database_" + System.nanoTime();
					DataManager.getInstance(context).insertData(dataStoreKey, tasks);
					callback.onDataReceivedFromCache(dataStoreKey);
				}
		    }
		};
		asyncTask.execute(employeeID);

	}

}
