package com.a416.ece.messagingproject;

import android.os.AsyncTask;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConnectTask extends AsyncTask<String, Void, String> {
    private String result;

    public ConnectTask(String result) {
        this.result = result;
    }

    @Override
    protected String doInBackground(String... params) {
        String string = "";
        try {
            URL url = new URL(params[0]);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            InputStream stream = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder builder = new StringBuilder();

            String inputString;
            while ((inputString = bufferedReader.readLine()) != null) {
                builder.append(inputString);
            }

            JSONObject topLevel = new JSONObject(builder.toString());
            string = builder.toString();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return string;
    }

    @Override
    protected void onPostExecute(String result) {
        this.result = result;
    }
}
