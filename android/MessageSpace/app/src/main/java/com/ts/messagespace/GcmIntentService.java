package com.ts.messagespace;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    static final String TAG = "MessageSpace";

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Utils.trackEvent("Tracking", "PushNotificationReceived", "GcmIntentService:onHandleIntent");
        Utils.addDevLog(this, "GcmIntentService:onHandleIntent() Push received");
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

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
//                sendNotification("Received: " + extras.toString());
                String messages = extras.getString("push");
                MessageList messageList = null;
                try {
                    JSONArray object = new JSONArray(messages);
                    messageList = MessageList.parseJSON(object);
                } catch (JSONException e) {
                    Utils.trackEvent("Exception", "PushNotificationDropped",
                            "GcmIntentService:onHandleIntent-IncorrectPayload");
                    Utils.addDevLog(this, "GcmIntentService:onHandleIntent() Error: Push received " +
                            "but Json parsing exception occurred while parsing the payload");
                    Utils.logErrorToServer(this, 0, "UNKNOWN", "UNKNOWN", "GcmIntentService:onHandleIntent-IncorrectPayload");
                }

                if (messageList != null && messageList.messageList.size() > 0) {
                    for (int i = 0; i < messageList.messageList.size(); i++) {
                        Message msg = messageList.messageList.get(i);
                        Utils.sendSMSWithStatus(this, msg.phone, msg.message);
                        Utils.addDevLog(this, "GcmIntentService:onHandleIntent() Sending SMS...");
                    }
                } else {
                    Utils.addDevLog(this, "GcmIntentService:onHandleIntent() Error: Push received " +
                            "but message array is empty (could be json parsing exception)");
                    Utils.logErrorToServer(this, 0, "UNKNOWN", "UNKNOWN", "GcmIntentService:onHandleIntent-EmptyPayload");
                }
            } else {
                Utils.trackEvent("Exception", "PushNotificationDropped",
                        "GcmIntentService:onHandleIntent-IncorrectMessageType");
                Utils.addDevLog(this, "GcmIntentService:onHandleIntent() Error: Push received but message type was not gcm.");
                Utils.logErrorToServer(this, 0, "UNKNOWN", "UNKNOWN", "GcmIntentService:onHandleIntent-IncorrectMessageType");
            }
        } else {
            Utils.addDevLog(this, "GcmIntentService:onHandleIntent() Error: Push received but extras was empty");
            Utils.logErrorToServer(this, 0, "UNKNOWN", "UNKNOWN", "GcmIntentService:onHandleIntent-EmptyPush");
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }
}

