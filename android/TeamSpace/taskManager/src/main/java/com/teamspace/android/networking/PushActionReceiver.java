package com.teamspace.android.networking;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.teamspace.android.caching.DataManager;
import com.teamspace.android.models.MetricsObject;
import com.teamspace.android.utils.Utils;

/**
 * Created by vivek on 2/18/15.
 */
public class PushActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        DataManager.getInstance(context).fireMetric(new MetricsObject("Push-DISMISSED", "Dismissed by user"));
    }
}
