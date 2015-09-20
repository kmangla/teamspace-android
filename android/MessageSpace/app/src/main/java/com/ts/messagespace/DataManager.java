package com.ts.messagespace;

import android.content.Context;
import android.os.Environment;
import android.support.v4.util.LruCache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by pratyus on 9/20/15.
 */
public class DataManager {

    private Context mContext;
    private static DataManager instance;
    private LruCache<String, Object> dataStore;

    private DataManager(Context context) {
        mContext = context.getApplicationContext();
        int cacheSize = 4 * 1024 * 1024; // 4MiB
        dataStore = new LruCache<String, Object>(cacheSize);
    }

    public static DataManager getInstance(Context context) {
        if (null == instance) {
            instance = new DataManager(context);
        }
        return instance;
    }

    public void insertData(String dataStoreKey, Object data) {
        if (dataStoreKey == null || data == null) {
            return;
        }
        dataStore.put(dataStoreKey, data);
    }

    public Object retrieveData(String dataStoreKey) {
        if (dataStoreKey == null) {
            return null;
        }

        return dataStore.get(dataStoreKey);
    }

    public void removeData(String dataStoreKey) {
        if (dataStoreKey == null) {
            return;
        }

        dataStore.remove(dataStoreKey);
    }

    public void writeLogFile(String text) {
        String filename = "logFile";
        FileOutputStream outputStream;
        try {
            outputStream = mContext.openFileOutput(filename, Context.MODE_APPEND);
            outputStream.write(text.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readLogFile() {
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream in = mContext.openFileInput("logFile");
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String line;

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (Exception e) {
            return "";
        }
        return sb.toString();
    }

    public void deleteLogFile() {
        String dir = mContext.getFilesDir().getAbsolutePath();
        File f0 = new File(dir, "logFile");
        boolean d0 = f0.delete();
    }
}
