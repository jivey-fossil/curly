package com.fossil.jivey.curly;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by jivey on 5/10/17.
 */

public class ExecuteCommand extends AsyncTask<String, Void, String> {
    private static final String TAG = "ExecuteCommand";
    private CommandCompleteListener mCommandCompleteListener;
    TimingLogger timings = new TimingLogger(TAG, "Execute Command", true);
    private String urlString;

    public ExecuteCommand(CommandCompleteListener commandCompleteListener) {
        this.mCommandCompleteListener = commandCompleteListener;
    }

    @Override
    protected String doInBackground(String... params) {
        urlString = params[0];
        timings.reset(TAG, params[1]);
        try {
            return downloadData(urlString);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        try {
            timings.addSplit("notify UI");
            timings.dumpToLog();
            mCommandCompleteListener.commandComplete(urlString, result);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String downloadData(String urlString) throws IOException {
        InputStream is = null;
        try {
            timings.addSplit("Waiting to try: " + urlString);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            timings.addSplit("connected");
            is = conn.getInputStream();
            timings.addSplit("data received");
            return convertToString(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private String convertToString(InputStream is) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        timings.addSplit("result string built");
        return new String(total);
    }
}
