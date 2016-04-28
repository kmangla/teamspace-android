package com.teamspace.android.networking;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.teamspace.android.R;
import com.teamspace.android.caching.DataManager;
import com.teamspace.android.common.ui.LauncherActivity;
import com.teamspace.android.models.Message;
import com.teamspace.android.models.MessageList;
import com.teamspace.android.models.MetricsObject;
import com.teamspace.android.tasklist.ui.AllTasksListViewActivity;
import com.teamspace.android.utils.Constants;
import com.teamspace.android.utils.Utils;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONArray;
import org.json.JSONObject;

public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);
        Utils.log("onHandleIntent() called on GcmIntentService");

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // Post notification of received message.
                String messageStr = extras.getString("push");
                Utils.log("Push payload: " + messageStr);
                try {
                    JSONObject object = new JSONObject(messageStr);
                    Message msg = Message.parseJSON(object);
                    Utils.log("text: " + msg.text + " ntype: " + msg.ntype + " taskId: " + msg.taskID);
                    Utils.log(" username: " + msg.user.getName());
                    Utils.addAppIconBadge(this);
                    if ("silentMessage".equalsIgnoreCase(msg.ntype)) {
                        Utils.sendSMS(this, msg.user.getPhoneWithCountryCode(), msg.text);
                        DataManager.getInstance(this).fireMetric(new MetricsObject("Push-silentMessage",
                                msg.text.substring(0, Math.min(3, msg.text.length() - 1)) + "..."));
                    } else if ("taskCreation".equalsIgnoreCase(msg.ntype)) {
                        // Post notification of received message.
                        sendNotification(msg.text, Constants.TASK_CREATION);
                        DataManager.getInstance(this).fireMetric(new MetricsObject("Push-taskCreation",
                                msg.text.substring(0, Math.min(3, msg.text.length() - 1)) + "..."));
                    } else if ("employeeCreation".equalsIgnoreCase(msg.ntype)) {
                        // Post notification of received message.
                        sendNotification(msg.text, Constants.EMP_CREATION);
                        DataManager.getInstance(this).fireMetric(new MetricsObject("Push-employeeCreation",
                                msg.text.substring(0, Math.min(3, msg.text.length() - 1)) + "..."));
                    } else if ("taskList".equalsIgnoreCase(msg.ntype)) {
                        // Post notification of received message.
                        sendNotification(msg.text, null);
                        DataManager.getInstance(this).fireMetric(new MetricsObject("Push-taskList",
                                msg.text.substring(0, Math.min(3, msg.text.length() - 1)) + "..."));
                    } else {
                        // Post notification of received message.
                        sendNotification(msg.user.getName() + ": " + msg.text, null);
                        DataManager.getInstance(this).fireMetric(new MetricsObject("Push",
                                msg.text.substring(0, Math.min(3, msg.text.length() - 1)) + "..."));
                    }
                } catch (Exception e) {
                    Utils.log("Exception while parsing push payload in GcmIntentService" + e.toString());
                    Utils.trackEvent("Exception", "PushNotificationDropped",
                            "TS-GcmIntentService:onHandleIntent-IncorrectPayload");
                    Utils.logErrorToServer(this, "PushPayloadParsing", -1, "Exception" + e.getLocalizedMessage(),
                            Utils.getSignedInUserPhoneNumber());
                }
            } else {
                Utils.trackEvent("Exception", "PushNotificationDropped",
                        "TS-GcmIntentService:onHandleIntent-IncorrectMessageType");
                Utils.logErrorToServer(this, "PushPayloadParsing", -1, "IncorrectType:" + messageType,
                        Utils.getSignedInUserPhoneNumber());
            }
        } else {
            Utils.logErrorToServer(this, "PushPayloadParsing", -1, "NoPayload", Utils.getSignedInUserPhoneNumber());
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg, String launchOnTop) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent pushIntent = new Intent(this, LauncherActivity.class);
        if (launchOnTop != null) {
            pushIntent.putExtra(Constants.DEEPLINK, launchOnTop);
        }
        pushIntent.putExtra(Constants.IS_FROM_PUSH, true);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, pushIntent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_action_paste)
                        .setContentTitle(getString(R.string.app_name))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setAutoCancel(true)
                        .setCategory(Notification.CATEGORY_MESSAGE)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}

