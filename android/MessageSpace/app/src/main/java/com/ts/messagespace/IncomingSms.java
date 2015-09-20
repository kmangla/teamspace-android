package com.ts.messagespace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by pratyus on 2/20/15.
 */
public class IncomingSms extends BroadcastReceiver {
    // Get the object of SmsManager
    final SmsManager sms = SmsManager.getDefault();

    public void onReceive(Context context, Intent intent) {
        Utils.addDevLog(context, "IncomingSms:onReceive() Received some SMS on this device. Could be any non-TS sms...");

        // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();

        if (bundle != null) {

            final Object[] pdusObj = (Object[]) bundle.get("pdus");

            if (pdusObj.length == 0) {
                Utils.addDevLog(context, "IncomingSms:onReceive() Received some SMS on this device but " +
                        "pdusObj.length == 0 so could not do anything");
            }

            for (int i = 0; i < pdusObj.length; i++) {

                SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                String phoneNumber = currentMessage.getDisplayOriginatingAddress();

                String senderNum = phoneNumber;
                String message = currentMessage.getDisplayMessageBody();

                Utils.trackEvent("Tracking", "SMSReceived", "IncomingSms:onReceive");
                Utils.addDevLog(context, "IncomingSms:onReceive() Will make Post call to our servers for this SMS");

                Utils.sendToServer(context, senderNum, message);
            } // end for loop
        } else {
            Utils.addDevLog(context, "IncomingSms:onReceive() Received some SMS on this device but " +
                    "bundle was null so could not do anything");
        }

        Utils.trackEvent("Exception", "EmployeeReplyDropped", "IncomingSms:onReceive");
    }
}
