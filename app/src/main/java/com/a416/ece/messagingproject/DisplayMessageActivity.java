package com.a416.ece.messagingproject;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class DisplayMessageActivity extends AppCompatActivity {
    public static String groupId = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);

        Intent intent = getIntent();
        groupId = intent.getStringExtra("groupId");

        new ConnectTask().execute(MainActivity.BACKEND + "/group/" + groupId + "/parsed", "GET");
    }

    private class ConnectTask extends AsyncTask<String, Void, String> {
        private ConnectTask() {
        }

        @Override
        protected String doInBackground(String... params) {
            return connectToServer(params);
        }

        @Override
        protected void onPostExecute(String result) {
            String output = "";
            TextView textView = (TextView) findViewById(R.id.textView);
            try {
                JSONArray array = new JSONArray(result);
                for (int i = 0; i < array.length(); i++) {
                    output += array.getString(i) + "\n";
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            textView.setText(output);
        }
    }

    private String connectToServer(String... params) {
        String string = "";
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(params[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            if (params[1].equals("POST")) {
                urlConnection.setReadTimeout(5000);
                urlConnection.setConnectTimeout(5000);
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/json");

                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(params[2]);
                writer.flush();
                writer.close();
                os.flush();
                os.close();
            } else if (params[1].equals("PUT")) {
                urlConnection.setReadTimeout(5000);
                urlConnection.setConnectTimeout(5000);
                urlConnection.setRequestMethod("PUT");
            }

            InputStream stream = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder builder = new StringBuilder();

            String inputString;
            while ((inputString = bufferedReader.readLine()) != null) {
                builder.append(inputString);
            }

            string = builder.toString();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return string;
    }

}
