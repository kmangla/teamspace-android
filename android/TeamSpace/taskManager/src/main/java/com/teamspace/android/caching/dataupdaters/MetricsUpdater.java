package com.teamspace.android.caching.dataupdaters;

import android.content.Context;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.teamspace.android.caching.DataManagerCallback;
import com.teamspace.android.caching.DatabaseCache;
import com.teamspace.android.models.MetricsObject;
import com.teamspace.android.models.MigratedMessage;
import com.teamspace.android.networking.NetworkRoutes;
import com.teamspace.android.networking.NetworkingLayer;
import com.teamspace.android.utils.Utils;

import org.json.JSONObject;

import java.util.HashMap;

public class MetricsUpdater {

	private Context context;

	public MetricsUpdater(Context context) {
		this.context = context;
	}

	public void fire(MetricsObject metric) {
		final String url = NetworkRoutes.getRouteBase() + NetworkRoutes.ROUTE_METRICS;
		final HashMap<String, String> params = metric.toMapObject();
		NetworkingLayer.getInstance(context).makePostRequest(
				url,
				params,
				new Response.Listener<String>() {
				    @Override
				    public void onResponse(String response) {

				    }
				},		
				new Response.ErrorListener() {

				    @Override 
				    public void onErrorResponse(VolleyError error) {
						Utils.log("fire() Metric POST failed for url " + url + " params: " + params
								+ " with error " + error);
						error.printStackTrace();
                        Utils.logErrorToServer(context, url,
                                error.networkResponse != null ? error.networkResponse.statusCode : -1,
                                error.networkResponse != null ? error.networkResponse.toString() : null,
                                "Failed to post metric because server returned error");
				    }  
				});
	}
}
