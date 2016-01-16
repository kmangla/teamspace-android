package com.teamspace.android.models;

import com.teamspace.android.utils.ISO8601DateParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;

public class MetricsObject {

    private static final String PAGE_NAME = "pageName";
    private static final String METRIC_NAME = "metricName";

    private String pageName;
    private String metricName;

    public MetricsObject(String pageName, String metricName) {
        this.metricName = metricName;
        this.pageName = pageName;
    }

    public HashMap<String, String> toMapObject() {
		HashMap<String, String> obj  = new HashMap<String, String>();
		obj.put(PAGE_NAME, pageName);
		obj.put(METRIC_NAME, metricName);
		return obj;
	}
}
