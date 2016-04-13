package com.teamspace.android.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.teamspace.android.BuildConfig;
import com.teamspace.android.R;
import com.teamspace.android.caching.DataManager;
import com.teamspace.android.caching.DataManagerCallback;
import com.teamspace.android.caching.DatabaseCache;
import com.teamspace.android.common.ui.MainActivityWithTabs;
import com.teamspace.android.common.ui.TaskManagerApplication;
import com.teamspace.android.employee.ui.EmployeeAddEditActivity;
import com.teamspace.android.models.BadgeData;
import com.teamspace.android.models.MigratedEmployee;
import com.teamspace.android.models.UserAuthData;
import com.teamspace.android.networking.NetworkRoutes;
import com.teamspace.android.networking.NetworkingLayer;
import com.teamspace.android.tasklist.ui.AllTasksListViewActivity;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.teamspace.android.tasklist.ui.TaskAddEditActivity;

import me.leolin.shortcutbadger.ShortcutBadger;

public class Utils {

    private static String signedInUserId;
    private static String serverIP;
    private static String serverPort;
    private static String signedInUserKey;
    private static String signedInUserPhoneNumber;
    private static String signedInUserName;
    private static String signedInUserCountryCode;

    public static int getColor(Context context, String colorName) {
        if ("Light Orange".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(android.R.color.holo_orange_light);
        } else if ("Dark Orange".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(android.R.color.holo_orange_dark);
        } else if ("Orange".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(android.R.color.holo_orange_light);
        } else if ("Dark Red".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(android.R.color.holo_red_dark);
        } else if ("Red".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(android.R.color.holo_red_light);
        } else if ("Purple".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(android.R.color.holo_purple);
        } else if ("Black".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(android.R.color.black);
        } else if ("White".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(android.R.color.white);
        } else if ("Light Blue".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(android.R.color.holo_blue_light);
        } else if ("Light Green".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(android.R.color.holo_green_light);
        } else if ("Dark Gray".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(android.R.color.darker_gray);
        } else if ("Light Gray".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(android.R.color.darker_gray);
        } else if ("Default".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(R.color.header_background);
        } else if ("Transparent".equalsIgnoreCase(colorName)) {
            return context.getResources().getColor(android.R.color.transparent);
        }

        return context.getResources().getColor(android.R.color.white);
    }

    public static void callPhoneNumber(Context context, String empNumber) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + empNumber));
        context.startActivity(callIntent);
    }

    public static String extractInitialsFromName(String name) {
        if (!Utils.isStringNotEmpty(name)) {
            return Constants.EMPTY_STRING;
        }

        name = name.toUpperCase();

        char firstInitial = name.charAt(0);
        char lastInitial = name.charAt((name.indexOf(" ")) + 1);
        String initials = String.valueOf(firstInitial) + String.valueOf(lastInitial);
        return initials;
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

    /**
     * Normalize a number returned by Contact Picker to remove any prefixes.
     * @param number
     * @return
     */
    public static String removeCountryPrefixFromPhoneNumber(Context context, String number) {
        if (number.startsWith("0")) {
            return number.substring(1);
        }

        // Note: If +91 and +9 are two valid country codes, this loop will
        // remove the one that occurs first in the country_code_array
        String [] countryCodeArray = context.getResources().getStringArray(R.array.country_code_array);
        for (int i = 0; i < countryCodeArray.length; i++) {
            String code = countryCodeArray[i];
            if (number.startsWith(code)) {
                return number.substring(code.length());
            }
        }

        return number;
    }

    public static String getCountryCodeForCountry(Context context, int countryIndex) {
        String [] countryCodeArray = context.getResources().getStringArray(R.array.country_code_array);
        if (countryIndex >= countryCodeArray.length) {
            countryIndex = 0;
        }
        return countryCodeArray[countryIndex];
    }

    public static void refreshListWithoutLosingScrollPosition(ListView listview, ArrayAdapter adapter) {
        if (adapter != null && listview != null && adapter.getCount() > 0) {
            int index = listview.getFirstVisiblePosition();
            View v = listview.getChildAt(0);
            int top = (v == null) ? 0 : (v.getTop() - listview.getPaddingTop());

            adapter.notifyDataSetChanged();

            listview.setSelectionFromTop(index, top);
        }
    }

    public static void logErrorToServer(Context context, String errorUrl, int serverResponseCode,
                                        String serverResponse, String description) {
        if (description == null && errorUrl == null) {
            return;
        }

        // /error?userID=<userID>&phone=<number>&url=<url>&serverCode=<serverCode>&serverResponse=<serverResponse>&desc=<desc>
        final String url = NetworkRoutes.getRouteBase() + NetworkRoutes.ROUTE_ERROR;
        final HashMap<String, String> params  = new HashMap<String, String>();
        if (getSignedInUserId() != null) {
            params.put(Constants.USERID, getSignedInUserId());
        } else if (getSignedInUserPhoneNumber() != null) {
            params.put(Constants.USER_PHONE, getSignedInUserPhoneNumber());
        } else {
            params.put(Constants.USERID, "Unknown user");
        }

        if (errorUrl != null) {
            params.put(Constants.URL, errorUrl);
        }
        params.put(Constants.SERVER_CODE, Integer.toString(serverResponseCode));
        if (serverResponse != null) {
            params.put(Constants.SERVER_RESPONSE, serverResponse);
        }
        if (description != null) {
            params.put(Constants.DESCRIPTION, description);
        }

        NetworkingLayer.getInstance(context).makePostRequest(
                url,
                params,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Utils.log("logErrorToServer() POST response for url: " + url + " params: " + params
                                + " got response: " + response);
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Utils.log("logErrorToServer() POST failed for url " + url + " params: " + params
                                + " with error " + error);
                        error.printStackTrace();
                    }
                });

    }

    public static void log(String message) {
        log(null, message);
    }

    public static void log(String tag, String message) {
        if (!BuildConfig.DEBUG) {
            return;
        }

        if (tag == null) {
            tag = "TEAMSPACE";
        }

        Log.d(tag, message);
    }

    public static String getCurrentDate() {
        long time = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd, HH:mm");
        Date resultdate = new Date(time);
        return sdf.format(resultdate);
    }

    public static String getDateAndTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd, HH:mm");
        Date resultdate = new Date(time);
        return sdf.format(resultdate);
    }

    public static void setTextViewTextAndVisibility(TextView tv, String text) {
        if (tv == null) {
            return;
        }

        if (Utils.isStringNotEmpty(text)) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(text);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    public static String getSignedInUserName() {
        if (isStringNotEmpty(signedInUserName)) {
            return signedInUserName;
        }

        signedInUserName = "Self";

        Cursor c = TaskManagerApplication.getAppContext().getContentResolver().
                query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
        try {
            c.moveToFirst();
            signedInUserName = c.getString(c.getColumnIndex("display_name"));
        } catch (Exception e) {
            Utils.log("Could not find self user name - defaulting to [Self]");
            signedInUserName = null;
        }

        c.close();

        // Error case - some devices might not return user name correctly
        if (Utils.isStringEmpty(signedInUserName)) {
            // Maybe user had manually entered his name during registration.
            // Try to find that entry.
            signedInUserName = Utils.readStringFromSharedPrefs(Constants.EMPLOYEE_NAME);
        }

        if (Utils.isStringEmpty(signedInUserName)) {
            signedInUserName = "Self";
        }

        Utils.log("Name = " + signedInUserName);
        return signedInUserName;
    }

    public static String getSignedInUserCountryCode() {
        if (isStringNotEmpty(signedInUserCountryCode)) {
            return signedInUserCountryCode;
        }

        signedInUserCountryCode = readStringFromSharedPrefs(Constants.COUNTRY_CODE);

        if (isStringNotEmpty(signedInUserCountryCode)) {
            return signedInUserCountryCode;
        }

        MigratedEmployee self = DatabaseCache.getInstance(TaskManagerApplication.getAppContext()).
                getMigratedEmployeeBlockingCall(getSignedInUserId());
        signedInUserCountryCode = self.getCountryCode();

        if (isStringEmpty(signedInUserCountryCode)) {
            signedInUserCountryCode = "+91";
        }

        // Cache it on disk
        writeStringToSharedPrefs(Constants.COUNTRY_CODE, signedInUserCountryCode);

        return signedInUserCountryCode;
    }

    public static String getSignedInUserPhoneNumber() {
        if (isStringNotEmpty(signedInUserPhoneNumber)) {
            return signedInUserPhoneNumber;
        }

        TelephonyManager tMgr = (TelephonyManager) TaskManagerApplication.getAppContext().
                getSystemService(Context.TELEPHONY_SERVICE);
        signedInUserPhoneNumber = tMgr.getLine1Number();

        // Error case - some devices might not return phone number correctly
        if (Utils.isStringEmpty(signedInUserPhoneNumber)) {
            // Maybe user had manually entered his phone number during registration.
            // Try to find that entry.
            signedInUserPhoneNumber = Utils.readStringFromSharedPrefs(Constants.EMPLOYEE_PHONE);
            return signedInUserPhoneNumber;
        }

        // Success case
        String countryCode = getSignedInUserCountryCode();

        // If phone number includes country code, make sure it starts with +
        if (signedInUserPhoneNumber.length() > 10 &&
                signedInUserPhoneNumber.indexOf("+") < 0 &&
                signedInUserPhoneNumber.startsWith(countryCode))
        {
            signedInUserPhoneNumber = "+" + signedInUserPhoneNumber;
        } else if (signedInUserPhoneNumber.length() <= 10) {
            signedInUserPhoneNumber = countryCode + signedInUserPhoneNumber;
        }

        return signedInUserPhoneNumber;
    }

    public static String getCountryZipCodeDeprecated() {
        String CountryID="";
        String CountryZipCode="";

        TelephonyManager manager = (TelephonyManager) TaskManagerApplication.getAppContext().getSystemService(Context.TELEPHONY_SERVICE);
        //getNetworkCountryIso
        CountryID= manager.getSimCountryIso().toUpperCase();
        String[] rl = TaskManagerApplication.getAppContext().getResources().getStringArray(R.array.CountryCodes);
        for(int i=0;i<rl.length;i++){
            String[] g=rl[i].split(",");
            if(g[1].trim().equals(CountryID.trim())){
                CountryZipCode=g[0];
                break;
            }
        }
        return CountryZipCode;
    }

    public static String getSignedInUserKey() {
//        if (true) {
//            return "5548fbd7cd008003003f04ab"; // khbai
//        }

//        if (true) {
//            return "5709e3799b7ceb03001869db"; // gan
//        }

        if (isStringNotEmpty(signedInUserKey)) {
            return signedInUserKey;
        }

        signedInUserKey = readStringFromSharedPrefs(Constants.USER_KEY);
        return signedInUserKey;
    }

    public static void setSignedInUserKey(String key) {
        if (key == null) {
            return;
        }

        signedInUserKey = key;
        writeStringToSharedPrefs(Constants.USER_KEY, key);
    }

    public static String getSignedInUserId() {
//        if (true) {
//            return "5548fbd7cd008003003f04ab"; // kbhai
//        }

//        if (true) {
//            return "5709e3799b7ceb03001869db"; // gan
//        }

        if (isStringNotEmpty(signedInUserId)) {
            return signedInUserId;
        }

        signedInUserId = readStringFromSharedPrefs(Constants.USER_ID);
        return signedInUserId;
    }

    public static void setSignedInUserId(String userId) {
        if (userId == null) {
            return;
        }

        signedInUserId = userId;
        writeStringToSharedPrefs(Constants.USER_ID, userId);
    }

    public static void writeStringToSharedPrefs(String key, String value) {
        SharedPreferences sharedPref = TaskManagerApplication.getAppContext().
                getSharedPreferences("userdetails", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String readStringFromSharedPrefs(String key) {
        SharedPreferences sharedPref = TaskManagerApplication.getAppContext().
                getSharedPreferences("userdetails", Context.MODE_PRIVATE);
        return sharedPref.getString(key, null);
    }

    public static String getServerIP() {
        if (isStringNotEmpty(serverIP)) {
            return serverIP;
        }

        serverIP = readStringFromSharedPrefs(Constants.SERVER_IP);

        if (isStringEmpty(serverIP)) {
            serverIP = "teamspace.herokuapp.com";
            saveServerIP(serverIP);
        }

        return serverIP;
    }

    public static String getServerPort() {
        if (isStringNotEmpty(serverPort)) {
            return serverPort;
        }

        serverPort = readStringFromSharedPrefs(Constants.SERVER_PORT);

        if (serverPort == null || serverPort.equalsIgnoreCase(":0")) {
            serverPort = Constants.EMPTY_STRING;
        }

        return serverPort;
    }

    public static void saveServerIP(String ip) {
        if (!isStringNotEmpty(ip)) {
            return;
        }

        serverIP = ip;
        writeStringToSharedPrefs(Constants.SERVER_IP, serverIP);
    }

    public static void saveServerPort(String port) {
        if (isStringEmpty(port)) {
            return;
        }

        if (port.indexOf(":") != 0) {
            port = ":" + port;
        }

        serverPort = port;
        writeStringToSharedPrefs(Constants.SERVER_PORT, serverPort);
    }

    public static void closeOtherRows(SwipeListView swipelistview, int position, int totalCount) {
        // Close all other rows
        for (int i = 0; i < totalCount; i++) {
            if (i != position) {
                swipelistview.closeAnimate(i);
            }
        }
    }

    public static View getRowFromListView(SwipeListView swipelistview, int wantedPosition) {
        int firstPosition = swipelistview.getFirstVisiblePosition() - swipelistview.getHeaderViewsCount();
        int wantedChild = wantedPosition - firstPosition;

        if (wantedChild < 0 || wantedChild >= swipelistview.getChildCount()) {
            return null;
        }

        return swipelistview.getChildAt(wantedChild);
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

    public static void saveSelfPhoneNumber(String value) {
        signedInUserPhoneNumber = value;
        writeStringToSharedPrefs(Constants.EMPLOYEE_PHONE, value);
    }

    public static void saveSelfUserName(String value) {
        writeStringToSharedPrefs(Constants.EMPLOYEE_NAME, value);
    }

    public static void playNotificationSound() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(TaskManagerApplication.getAppContext(), notification);
        r.play();
    }

    public static void trackPageView(String pageName) {
        // Get tracker.
        Tracker t = TaskManagerApplication.getTracker(
                TaskManagerApplication.TrackerName.APP_TRACKER);

        // Set screen name.
        t.setScreenName(pageName);

        // Send a screen view.
        t.send(new HitBuilders.ScreenViewBuilder().build());
        Utils.log("tracker = " + t.hashCode() + " page = " + pageName);
    }

    public static void trackEvent(String category, String action, String label) {
        // Get tracker.
        Tracker t = TaskManagerApplication.getTracker(
                TaskManagerApplication.TrackerName.APP_TRACKER);

        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());

        Utils.log("tracker = " + t.hashCode() + " category = " + category + " action = " + action);
    }

    public static void sendSMS(Context context, String phoneNumber, String message) {
        if (phoneNumber == null || message == null) {
            return;
        }

        if (NetworkingLayer.getInstance(context).isRoaming()) {
            Utils.logErrorToServer(context, "sending automated sms from owners phone",
                    -1,
                    null,
                    "Failed to send automated sms from owner's device because user " + getSignedInUserId() + " is on roaming.");
            return;
        }

        Utils.log("Phone" + phoneNumber + "Sending message: " + message);

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    public static void sendCustomSMS(Context context, String employeeNumber) {
        if (employeeNumber == null) {
            return;
        }
        Intent sendIntent = new Intent(Intent.ACTION_VIEW);
        sendIntent.setData(Uri.parse("sms:"));
        sendIntent.setType("vnd.android-dir/mms-sms");
        sendIntent.putExtra("address", employeeNumber);
        sendIntent.putExtra("sms_body", context.getString(R.string.warn_emp) + " -" + Utils.getSignedInUserName());
        context.startActivity(sendIntent);
    }

    public static boolean isSamsungDevice(Context context) {
        // This is the content uri for the BadgeProvider
        Uri uri = Uri.parse("content://com.sec.badge/apps");

        Cursor c = context.getContentResolver().query(uri, null, null, null, null);

        // This indicates the provider doesn't exist and you probably aren't running
        // on a Samsung phone running TWLauncher. This has to be outside of try/finally block
        if (c == null) {
            return false;
        }

        return true;
    }

    private static void addAppIconBadgeSamsung(Context context, int count) {
        if (!isSamsungDevice(context)) {
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("package", context.getPackageName());
        // Name of your activity declared in the manifest as android.intent.action.MAIN.
        // Must be fully qualified name as shown below
        cv.put("class", "com.teamspace.android.common.ui.LauncherActivity");
        cv.put("badgecount", count); // integer count you want to display

        // Execute insert
        context.getContentResolver().insert(Uri.parse("content://com.sec.badge/apps"), cv);
    }

    public static void addAppIconBadge(final Context context) {
        DataManager dataMgr = DataManager.getInstance(context);
        dataMgr.getBadgeCount(new DataManagerCallback() {
            public void onDataReceivedFromServer(String dataStoreKey) {
                if (dataStoreKey == null) {
                    return;
                }
                BadgeData data = (BadgeData)
                        DataManager.getInstance(context).retrieveData(dataStoreKey);
                if (data == null || data.getBadgeCount() == 0) {
                    return;
                }

                ShortcutBadger.applyCount(context, data.getBadgeCount()); //for 1.1.4
                addAppIconBadgeSamsung(context, data.getBadgeCount());
            }
        });
    }

    public static void clearAppIconBadge(Context context) {
        ShortcutBadger.removeCount(context); //for 1.1.4
        clearAppIconBadgeSamsung(context);
    }

    private static void clearAppIconBadgeSamsung(Context context) {
        if (!isSamsungDevice(context)) {
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("badgecount", 0);
        context.getContentResolver().update(
                Uri.parse("content://com.sec.badge/apps"),
                cv, "package=?",
                new String[] {
                        context.getPackageName()
                }
        );
    }

    /**
     * Enables strict mode. This should only be called when debugging the application and is useful
     * for finding some potential bugs or best practice violations.
     */
    @TargetApi(11)
    public static void enableStrictMode() {
        // Strict mode is only available on gingerbread or later
        if (Utils.hasGingerbread()) {

            // Enable all thread strict mode policies
            StrictMode.ThreadPolicy.Builder threadPolicyBuilder =
                    new StrictMode.ThreadPolicy.Builder()
                            .detectAll()
                            .penaltyLog();

            // Enable all VM strict mode policies
            StrictMode.VmPolicy.Builder vmPolicyBuilder =
                    new StrictMode.VmPolicy.Builder()
                            .detectAll()
                            .penaltyLog();

            // Use builders to enable strict mode policies
            StrictMode.setThreadPolicy(threadPolicyBuilder.build());
            StrictMode.setVmPolicy(vmPolicyBuilder.build());
        }
    }

    /**
     * Uses static final constants to detect if the device's platform version is Gingerbread or
     * later.
     */
    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    /**
     * Uses static final constants to detect if the device's platform version is Honeycomb or
     * later.
     */
    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    /**
     * Uses static final constants to detect if the device's platform version is Honeycomb MR1 or
     * later.
     */
    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    /**
     * Uses static final constants to detect if the device's platform version is ICS or
     * later.
     */
    public static boolean hasICS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }
}
