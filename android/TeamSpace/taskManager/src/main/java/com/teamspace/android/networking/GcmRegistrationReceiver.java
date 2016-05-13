package com.teamspace.android.networking;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.widget.Toast;

import com.teamspace.android.caching.DataManager;
import com.teamspace.android.utils.Utils;

/**
 * Created by vivek on 2/18/15.
 */
public class GcmRegistrationReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
//        Toast.makeText(context, "I'm running", Toast.LENGTH_SHORT).show();
        GCMUtils.getInstance().registerInBackground(context);
    }
}
