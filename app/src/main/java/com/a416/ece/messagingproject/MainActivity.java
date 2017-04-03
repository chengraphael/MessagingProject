package com.a416.ece.messagingproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.BoolRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    public static final String BACKEND = "http://ec2-34-207-106-58.compute-1.amazonaws.com:3000";
    //    public static final String BACKEND = "http://192.168.99.0:3000";
    public static ArrayAdapter adapter;
    public static ArrayList<String> list;
    public static ListView listView;
    public static EditText groupIdEditText;
    public static SharedPreferences sharedPref;
    public static String groupId = "";
    public static Handler checkMessages;

    static class UIHandler extends Handler {
        public UIHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            list.add((String) msg.obj);
            adapter.notifyDataSetChanged();
            listView.setSelection(adapter.getCount() - 1);
        }
    }

    Handler h = new UIHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listView);
        groupIdEditText = (EditText) findViewById(R.id.groupIdText);

        Context context = getApplicationContext();
        sharedPref = context.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        Log.v("Start", "wtf");
        if (!sharedPref.contains(getString(R.string.userid))) {
            new CreateUserTask(this, false).execute(BACKEND + "/user/create", "PUT", "");
        } else {
            String userid = sharedPref.getString(getString(R.string.userid), "0");
            new ConnectTask(this, false).execute(BACKEND + "/user/" + userid, "GET", "");
        }
        Log.v("UserID", sharedPref.getString(getString(R.string.userid), "nope"));

        String[] values = new String[]{"Android", "iPhone", "WindowsMobile",
                "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
                "Linux", "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux",
                "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux", "OS/2",
                "Android", "iPhone", "WindowsMobile"};

        list = new ArrayList<>();
        for (String value : values) {
            list.add(value);
        }
        adapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1, list);

        listView.setAdapter(adapter);
        listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        listView.setStackFromBottom(true);

        EditText messageText = (EditText) findViewById(R.id.messageText);
        messageText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    listView.setSelection(adapter.getCount() - 1);
                }
            }
        });
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        return networkInfo != null && networkInfo.isConnected();
    }

    public void showGroup(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.messageText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void joinGroup(View view) throws ExecutionException, InterruptedException {
        String groupIdText = groupIdEditText.getText().toString();
        String userid = sharedPref.getString(getString(R.string.userid), "0");
        if (!userid.equals("0") && groupId.isEmpty()) {
            String result = new JoinGroupTask(this, false, groupIdText, userid)
                    .execute(BACKEND + "/group/" + groupIdText, "GET").get();
        }
    }

    public void quitGroup(View view) {
        String groupIdText = groupIdEditText.getText().toString();
        String userid = sharedPref.getString("userid", "0");
        if (!userid.equals("0") && !groupId.isEmpty()) {
            new ConnectTask(this, false)
                    .execute(BACKEND + "/group/" + groupIdText + "/quit",
                            "POST",
                            "{\"userid\": " + userid + "}");
            groupId = "";
            groupIdEditText.setText(groupId);
        }
    }

    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.messageText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public class GetGroupTask extends AsyncTask<String, Void, String> {
        private Context UIContext;
        private boolean repeat;

        public GetGroupTask(Context context, boolean repeat) {
            this.UIContext = context;
            this.repeat = repeat;
        }

        @Override
        protected String doInBackground(String... params) {
            return connectToServer(false, params);
        }

        @Override
        protected void onPostExecute(String result) {

        }
    }

    public class CreateUserTask extends AsyncTask<String, Void, String> {
        private Context UIContext;
        private boolean repeat;

        public CreateUserTask(Context context, boolean repeat) {
            this.UIContext = context;
            this.repeat = repeat;
        }

        @Override
        protected String doInBackground(String... params) {
            return connectToServer(false, params);
        }

        @Override
        protected void onPostExecute(String result) {
            SharedPreferences.Editor editor = sharedPref.edit();
            if (result != "") {
                editor.putString(getString(R.string.userid), result);
                editor.apply();
            }
            Log.v("KK", sharedPref.getString(getString(R.string.userid), "nope"));
        }
    }

    public class JoinGroupTask extends AsyncTask<String, Void, String> {
        private Context UIContext;
        private boolean repeat;
        private String groupIdText;
        private String userid;

        public JoinGroupTask(Context context, boolean repeat, String groupIdText, String userid) {
            this.UIContext = context;
            this.repeat = repeat;
            this.groupIdText = groupIdText;
            this.userid = userid;
        }

        @Override
        protected String doInBackground(String... params) {
            return connectToServer(false, params);
        }

        @Override
        protected void onPostExecute(String result) {
            if (!result.contains(groupIdText) || result.equals("[]")) {
                new JoinGroupTask(UIContext, false, groupIdText, userid)
                        .execute(
                                BACKEND + "/group/create",
                                "POST",
                                "{\"userid\": " + userid + "}"
                        );
            } else {
                groupIdText = result;
                new ConnectTask(UIContext, false)
                        .execute(
                                BACKEND + "/group/" + groupIdText + "/join",
                                "POST",
                                "{\"userid\": " + userid + "}"
                        );
                groupIdEditText.setText(groupIdText);
                groupId = result;
            }
            if (repeat) {
                Handler h = new Handler();
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        new ConnectTask(UIContext, true).execute();
                    }
                };
                h.postDelayed(r, 5000);
            }
        }
    }

    public class ConnectTask extends AsyncTask<String, Void, String> {
        private Context UIContext;
        private boolean repeat;

        public ConnectTask(Context context, boolean repeat) {
            this.UIContext = context;
            this.repeat = repeat;
        }

        @Override
        protected String doInBackground(String... params) {
            return connectToServer(this.repeat, params);
        }

        @Override
        protected void onPostExecute(String result) {
            if (repeat) {
                Handler h = new Handler();
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        new ConnectTask(UIContext, true).execute();
                    }
                };
                h.postDelayed(r, 5000);
            }
        }
    }

    private String connectToServer(boolean repeat, String... params) {
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

        if (repeat && !string.isEmpty()) {
            Message msg = h.obtainMessage(0, string);
            h.sendMessage(msg);
        }

        return string;
    }

}
