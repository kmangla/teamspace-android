package com.ts.messagespace;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.SyncStateContract;
import android.support.v4.app.FragmentActivity;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by pratyus on 2/12/15.
 */
public class Utils {
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_APP_VERSION = "appVersion";
    private static String signedInUserPhoneNumber;

    public static void sendSMS(Context context, String phoneNumber, String message)
    {
        if (phoneNumber == null || message == null) {
            Utils.trackEvent("Exception", "PushNotificationDropped",
                    "Utils:sendSMS-EmptyPhoneOrMessage");
            return;
        }

        // Log the phone number and message
        Utils.addDevLog(context, "Utils:sendSMS() Sent message to phone number ending in ..." +
                        phoneNumber.substring(phoneNumber.length() - 3, phoneNumber.length()) +
                        " where the message was ending in ..." +
                        message.substring(message.length() / 2, message.length())
        );

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    private static String TEXT = "description";
    private static String SENDER = "sentBy";

    public static void sendToServer(final Context context, String phoneNumber, String message)
    {
        if (phoneNumber == null || message == null) {
            // Log the phone number and message
            Utils.addDevLog(context, "Utils:sendToServer() Error: phoneNumber or message was null");
            return;
        }

        final String url =  NetworkRoutes.getRouteBase() + NetworkRoutes.ROUTE_MESSAGE;
        final HashMap<String, String> params  = new HashMap<String, String>();
        params.put(TEXT, message);
        params.put(SENDER, phoneNumber);

        // Log the phone number and message
        Utils.addDevLog(context, "Utils:sendToServer() Sending post request to server because of " +
                        "incoming SMS from phone number ending with ..." +
                        phoneNumber.substring(phoneNumber.length() - 3, phoneNumber.length()) +
                        " where the message was ending in ..." +
                        message.substring(message.length() / 2, message.length())
        );

        NetworkingLayer.getInstance(context).makePostRequest(
                url,
                params,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String msg = "NetworkingLayer.makePostRequest() executed for url: " + url + " with params: " + params
                                + " and got response: " + response;
                        Log.d("MessageSpace", msg);
                        Utils.addDevLog(context, msg);
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMsg = "NetworkingLayer.makePostRequest() failed for url " + url + " and params: " + params
                                + " with error " + error;
                        Log.d("MessageSpace", errorMsg);
                        Utils.addDevLog(context, errorMsg);

                        error.printStackTrace();
                        Utils.trackEvent("Exception", "EmployeeReplyDropped", "Utils:sendToServer-Failed");
                    }
                });

    }

    private static String REGID = "regID";
    private static String DEVICEID = "deviceID";
    private static String APPID = "appID";


    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
     * or CCS to send messages to your app.
     */
    public static void sendRegistrationIdToBackend(Context context, String regid) {
        if (regid == null) {
            return;
        }

        // /token?regID=<token>&phone=<number>
        final String url = NetworkRoutes.getRouteBase() + NetworkRoutes.ROUTE_TOKEN;
        final HashMap<String, String> params  = new HashMap<String, String>();
        params.put(REGID, regid);
        params.put(DEVICEID, Utils.getSelfPhoneNumber(context));
        params.put(APPID, "1");

        NetworkingLayer.getInstance(context).makePostRequest(
                url,
                params,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("MessageSpace", "sendRegistrationIdToBackend() POST response for url: " + url + " params: " + params
                                + " got response: " + response);
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("MessageSpace", "sendRegistrationIdToBackend() POST failed for url " + url + " params: " + params
                                + " with error " + error);
                        error.printStackTrace();
                        Utils.trackEvent("Exception", "GCMRegistrationFailed", "Utils:sendRegistrationIdToBackend");
                    }
                });
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    public static String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i("MessageSpace", "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i("MessageSpace", "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    public static SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return context.getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    public static boolean isStringNotEmpty(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        return true;
    }

    public static boolean isStringEmpty(String str) {
        return !isStringNotEmpty(str);
    }

    public static String getSelfPhoneNumber(Context context) {
        if (isStringNotEmpty(signedInUserPhoneNumber)) {
            return signedInUserPhoneNumber;
        }

        TelephonyManager tMgr = (TelephonyManager) MessageSpaceApplication.getAppContext().
                getSystemService(Context.TELEPHONY_SERVICE);
        signedInUserPhoneNumber = tMgr.getLine1Number();

        // Error case - some devices might not return phone number correctly
        if (Utils.isStringEmpty(signedInUserPhoneNumber)) {
            // Maybe user had manually entered his phone number during registration.
            // Try to find that entry.
            signedInUserPhoneNumber = Utils.readStringFromSharedPrefs(context, "EMPLOYEE_PHONE");
            return signedInUserPhoneNumber;
        }

        // Success case
        String countryCode = getCountryZipCode();

        // If phone number includes country code, make sure it starts with +
        if (signedInUserPhoneNumber.length() > 10 && signedInUserPhoneNumber.indexOf("+") < 0) {
            signedInUserPhoneNumber = "+" + signedInUserPhoneNumber;
        } else if (signedInUserPhoneNumber.length() <= 10) {
            signedInUserPhoneNumber = "+" + countryCode + signedInUserPhoneNumber;
        }
        return signedInUserPhoneNumber;
    }

    public static String getCountryZipCode() {
        String CountryID="";
        String CountryZipCode="";

        TelephonyManager manager = (TelephonyManager) MessageSpaceApplication.getAppContext().getSystemService(Context.TELEPHONY_SERVICE);
        //getNetworkCountryIso
        CountryID= manager.getSimCountryIso().toUpperCase();
        String[] rl = MessageSpaceApplication.getAppContext().getResources().getStringArray(R.array.CountryCodes);
        for(int i=0;i<rl.length;i++){
            String[] g=rl[i].split(",");
            if(g[1].trim().equals(CountryID.trim())){
                CountryZipCode=g[0];
                break;
            }
        }
        return CountryZipCode;
    }

    public static void saveSelfPhoneNumber(String value) {
        signedInUserPhoneNumber = value;
        writeStringToSharedPrefs(MessageSpaceApplication.getAppContext(), "EMPLOYEE_PHONE", value);
    }

    public static void saveServer(Context context, String s) {
        if (s == null || s.length() == 0) {
            return;
        }

        writeStringToSharedPrefs(context, "server", s);
    }

    public static String getServer(Context context) {
        String server = readStringFromSharedPrefs(context, "server");
        if (server == null || server.length() == 0) {
            server = "teamspace.herokuapp.com";
        }

        return server;
    }

    public static String getPort(Context context) {
        String serverPort = readStringFromSharedPrefs(context, "port");

        if (serverPort == null || serverPort.equalsIgnoreCase(":0")) {
            serverPort = "";
        }

        return serverPort;
    }

    public static void savePort(Context context, String s) {
        if (s == null || s.length() == 0) {
            return;
        }
        if (s.indexOf(":") != 0) {
            s = ":" + s;
        }
        writeStringToSharedPrefs(context, "port", s);
    }

    public static void writeStringToSharedPrefs(Context context, String key, String value) {
        SharedPreferences sharedPref = context.
                getSharedPreferences("userdetails", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String readStringFromSharedPrefs(Context context, String key) {
        SharedPreferences sharedPref = context.
                getSharedPreferences("userdetails", Context.MODE_PRIVATE);
        return sharedPref.getString(key, null);
    }

    public static void trackPageView(String pageName) {
        // Get tracker.
        Tracker t = MessageSpaceApplication.getTracker(
                MessageSpaceApplication.TrackerName.APP_TRACKER);

        // Set screen name.
        t.setScreenName(pageName);

        // Send a screen view.
        t.send(new HitBuilders.ScreenViewBuilder().build());
        Utils.log("tracker = " + t.hashCode() + " page = " + pageName);
    }

    public static void log(String message) {
        log(null, message);
    }

    public static void log(String tag, String message) {
        if (!BuildConfig.DEBUG) {
            return;
        }

        if (tag == null) {
            tag = "MESSAGESPACE";
        }

        Log.d(tag, message);
    }

    public static void trackEvent(String category, String action, String label) {
        // Get tracker.
        Tracker t = MessageSpaceApplication.getTracker(
                MessageSpaceApplication.TrackerName.APP_TRACKER);

        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());

        Utils.log("tracker = " + t.hashCode() + " category = " + category +
                " action = " + action + " label = " + label);
    }

    public static void addDevLog(Context context, String logMessage) {
        DataManager.getInstance(context).writeLogFile(getCurrentDate() + ": " + logMessage + "\n\n");
    }

    public static String retrieveDevLogs(Context context) {
        return DataManager.getInstance(context).readLogFile();
    }

    public static void clearDevLogs(Context context) {
        DataManager.getInstance(context).deleteLogFile();
    }

    public static String getCurrentDate() {
        long time = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd, HH:mm");
        Date resultdate = new Date(time);
        return sdf.format(resultdate);
    }
}
