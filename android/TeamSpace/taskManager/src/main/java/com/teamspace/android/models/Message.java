package com.teamspace.android.models;

import com.teamspace.android.utils.Constants;
import com.teamspace.android.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by vivek on 2/19/15.
 */
public class Message {
    public String text;
    public String ntype;
    public MigratedEmployee user;
    public String taskID;

    public static Message parseJSON(JSONObject msgObject) {
        Message msg = new Message();
        try {
            msg.text = msgObject.getString("text");
            msg.ntype = msgObject.getString("ntype");

            try {
                msg.taskID = msgObject.getString("taskID");
            } catch (Exception e) {
                msg.taskID = Constants.EMPTY_STRING;
            }

            try {
                JSONObject obj = msgObject.getJSONObject("user");
                msg.user = MigratedEmployee.parseJSON(obj);
            } catch (Exception e) {
                try {
                    JSONArray arr = msgObject.getJSONArray("user");
                    JSONObject obj = arr.getJSONObject(0);
                    msg.user = MigratedEmployee.parseJSON(obj);
                } catch (Exception e1) {
                    Utils.log("Exception while parsing user in push payload in Message class: " + e.toString());
                    msg.user = new MigratedEmployee();
                    msg.user.setName(Utils.getSignedInUserName());
                    msg.user.setUserID(Utils.getSignedInUserId());
                }
            }
        } catch (Exception e) {
            Utils.log("Exception while parsing push payload in Message class: " + e.toString());
        }
        return msg;
    }
}
