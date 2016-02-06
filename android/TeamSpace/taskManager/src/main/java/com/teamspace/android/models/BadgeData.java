package com.teamspace.android.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.HashMap;

public class BadgeData {
	private int badgeCount;

	private static String COUNT = "count";

	public int getBadgeCount() {
		return badgeCount;
	}
	public void setBadgeCount(int count) {
		this.badgeCount = count;
	}

	@Override
	public String toString() {
		return String.valueOf(badgeCount);
	}

	public JSONObject toJSONObject() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put(COUNT, this.getBadgeCount());
		return obj;
	}
	public HashMap<String, String> toMapObject() {
		HashMap<String, String> obj = new HashMap<String, String>();
		obj.put(COUNT, String.valueOf(getBadgeCount()));
		return obj;
	}

    public static BadgeData parseJSON(JSONObject object) throws JSONException, ParseException {
        BadgeData data = new BadgeData();
        try {
            data.badgeCount = object.getInt(COUNT);
        } catch (Exception e) {
            data.badgeCount = 0;
        }

        return data;
    }
}
