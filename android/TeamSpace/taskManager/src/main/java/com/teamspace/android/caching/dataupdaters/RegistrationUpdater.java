package com.teamspace.android.caching.dataupdaters;

import android.content.Context;
import android.widget.Toast;

import com.teamspace.android.caching.DataManagerCallback;
import com.teamspace.android.models.MigratedEmployee;
import com.teamspace.android.networking.GCMUtils;
import com.teamspace.android.networking.NetworkRoutes;
import com.teamspace.android.networking.NetworkingLayer;
import com.teamspace.android.utils.Utils;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.util.HashMap;

public class RegistrationUpdater {
	private Context mContext;
	private DataManagerCallback mCallback;

	public RegistrationUpdater(Context context, DataManagerCallback callback) {
		this.mContext = context;
		this.mCallback = callback;
	}

	public void generateOTP() {
		final String url = NetworkRoutes.getRouteBase() + NetworkRoutes.ROUTE_OTP;
        final HashMap<String, String> params = new HashMap<String, String>();
        String phone = Utils.getSignedInUserPhoneNumber();
        if (Utils.isStringEmpty(phone)) {
            Toast.makeText(mContext, "Sorry, cannot validate this phone (unable to read phone number).",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        params.put("phoneNumber", phone);

        NetworkingLayer.getInstance(mContext).makePostRequest(
				url,
				params,
				new Response.Listener<String>() {
				    @Override
				    public void onResponse(String response) {
				    	Utils.log("generateOTP() POST response for url: " + url + " params: " + params
								+ " got response: " + response);
                        if (mCallback != null) {
                            mCallback.onSuccess(response);
                        }
				    }
				},		
				new Response.ErrorListener() {

				    @Override
				    public void onErrorResponse(VolleyError error) {
				    	Utils.log("generateOTP() POST failed for url " + url + " params: " + params);
				    	error.printStackTrace();
				    	if (mCallback != null) {
				    		mCallback.onFailure(error.getLocalizedMessage());
						}
                        Utils.logErrorToServer(mContext, url,
                                error.networkResponse != null ? error.networkResponse.statusCode : -1,
                                error.networkResponse != null ? error.networkResponse.toString() : null,
                                "Failed to generate OTP because server returned error");
				    }  
				});
	}

    public void requestTestPush() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final String url = NetworkRoutes.getRouteBase() + NetworkRoutes.ROUTE_PUSH;
        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("text", "testing");
        params.put("ntype", "taskCreation");
        params.put("senderID", GCMUtils.getRegistrationId(mContext));

        NetworkingLayer.getInstance(mContext).makePostRequest(
                url,
                params,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Utils.log("requestTestPush() POST response for url: " + url + " params: " + params
                                + " got response: " + response);
                        if (mCallback != null) {
                            mCallback.onSuccess(response);
                        }
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Utils.log("requestTestPush() POST failed for url " + url + " params: " + params);
                        error.printStackTrace();
                        if (mCallback != null) {
                            mCallback.onFailure(error.getLocalizedMessage());
                        }
                        Utils.logErrorToServer(mContext, url,
                                error.networkResponse != null ? error.networkResponse.statusCode : -1,
                                error.networkResponse != null ? error.networkResponse.toString() : null,
                                "Failed to request test push because server returned error");
                    }
                });
    }
}
