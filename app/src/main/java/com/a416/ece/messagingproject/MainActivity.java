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
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    public static final String BACKEND = "http://ec2-34-207-106-58.compute-1.amazonaws.com:3000";
    //    public static final String BACKEND = "http://192.168.99.0:3000";
    public static ArrayAdapter adapter;
    public static ArrayList<String> messageJsons = new ArrayList<String>();
    public static ArrayList<String> messages = new ArrayList<String>();
    public static ListView listView;
    public static EditText groupIdEditText;
    public static EditText messageText;
    public static SharedPreferences sharedPref;
    public static String groupId = "";
    public static Handler checkMessages;
    public static Runnable checkMessageRunnable;
    public static String status = "Down";
    public static ArrayList<String[]> messageQueue = new ArrayList<String[]>();
    Handler h = new UIHandler();

    private static class UIHandler extends Handler {
        private UIHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            messages.add((String) msg.obj);
            adapter.notifyDataSetChanged();
            listView.setSelection(adapter.getCount() - 1);
        }
    }

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
            new CreateUserTask(this, false).execute(BACKEND + "/user/create", "POST", "{\"username\":\"" + UUID.randomUUID().toString() + "\"}");
        } else {
            String userid = sharedPref.getString(getString(R.string.userid), "0");
            new ConnectTask(this, false).execute(BACKEND + "/user/" + userid, "GET", "");
        }
        Log.v("UserID", sharedPref.getString(getString(R.string.userid), "nope"));

        String[] values = new String[]{};

        for (String value : values) {
            messages.add(value);
        }
        adapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1, messages);

        listView.setAdapter(adapter);
        listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        listView.setStackFromBottom(true);

        messageText = (EditText) findViewById(R.id.messageText);
        messageText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    listView.setSelection(adapter.getCount() - 1);
                }
            }
        });

        TextView statusTextView = (TextView) findViewById(R.id.statusTextView);
        statusTextView.setText(status);

        updateNetworkStatus(this);
    }

    public void updateNetworkStatus(final Context context) {
        Handler networkStatus = new Handler();
        Runnable networkStatusRunnable = new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable()) {
                    status = "Up";
                } else {
                    status = "Down";
                }
                TextView statusTextView = (TextView) findViewById(R.id.statusTextView);
                statusTextView.setText(status);
                updateNetworkStatus(context);
                String userid = sharedPref.getString(getString(R.string.userid), "0");
                new ConnectTask(context, false).execute(BACKEND + "/user/" + userid, "GET", "");
            }
        };
        networkStatus.postDelayed(networkStatusRunnable, 5000);
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
        String message = groupIdEditText.getText().toString();
        intent.putExtra("groupId", message);
        startActivity(intent);
    }

    public void joinGroup(View view) throws ExecutionException, InterruptedException {
        String groupIdText = groupIdEditText.getText().toString();
        String userid = sharedPref.getString(getString(R.string.userid), "0");
        if (!userid.equals("0")) {
            new JoinGroupTask(this, false, groupIdText, userid)
                    .execute(BACKEND + "/group/" + groupIdText, "GET");
        }
    }

    public void quitGroup(View view) {
        String groupIdText = groupIdEditText.getText().toString();
        String userid = sharedPref.getString("userid", "0");
        if (!userid.equals("0") && !groupIdText.isEmpty()) {
            if (checkMessageRunnable != null) {
                checkMessages.removeCallbacks(checkMessageRunnable);
            }
            new ConnectTask(this, false)
                    .execute(BACKEND + "/group/" + groupIdText + "/quit",
                            "POST",
                            "{\"userid\": " + userid + "}");
            groupId = "";
            groupIdEditText.setText(groupId);
        }
    }

    public void sendMessage(View view) {
        String groupIdText = groupIdEditText.getText().toString();
        String userid = sharedPref.getString("userid", "0");
        String message = messageText.getText().toString();
        if (!userid.equals("0") && !groupIdText.isEmpty()) {
            String jsonMessage = "{\"userid\": " + userid + ", \"message\": \"" + message + "\"}";
            messageQueue.add(new String[] {"Pending", jsonMessage});
            messageText.setText("");
            sendMessage(this, 0);
        }
    }

    public void sendMessage(final Context context, int time) {
        final String groupIdText = groupIdEditText.getText().toString();
        final Handler sendMessageHandler = new Handler();
        Runnable sendMessageRunnable = new Runnable() {
            @Override
            public void run() {
                if (!messageQueue.isEmpty()) {
                    String[] message = messageQueue.get(0);
                    new ConnectTask(context, false)
                            .execute(BACKEND + "/group/" + groupIdText + "/newMessage",
                                    "POST",
                                    message[1]);
                    if (status.equals(("Up"))) {
                        messageQueue.remove(0);
                    }
                    sendMessage(context, 1000);
                }
            }
        };
        sendMessageHandler.postDelayed(sendMessageRunnable, time);
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

    private class CreateUserTask extends AsyncTask<String, Void, String> {
        private Context UIContext;
        private boolean repeat;

        private CreateUserTask(Context context, boolean repeat) {
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
            if (!result.equals("")) {
                editor.putString(getString(R.string.userid), result);
                editor.apply();
            }
            Log.v("KK", sharedPref.getString(getString(R.string.userid), "nope"));
        }
    }

    private class JoinGroupTask extends AsyncTask<String, Void, String> {
        private Context UIContext;
        private boolean repeat;
        private String groupIdText;
        private String userid;

        private JoinGroupTask(Context context, boolean repeat, String groupIdText, String userid) {
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
            if (groupIdText.isEmpty() || result.equals("[]")) {
                new CreateGroupTask(UIContext, false)
                        .execute(
                                BACKEND + "/group/create",
                                "POST",
                                "{\"userid\": " + userid + ", \"groupName\": \"" + UUID.randomUUID().toString() + "\"}"
                        );
            } else {
                new ConnectTask(UIContext, false)
                        .execute(
                                BACKEND + "/group/" + groupIdText + "/join",
                                "POST",
                                "{\"userid\": " + userid + "}"
                        );
                groupIdEditText.setText(groupIdText);
                groupId = groupIdText;

                if (status.equals("Up")) {
                    new GetMessagesTask(UIContext, true).execute(BACKEND + "/group/" + groupId, "GET");
                }
            }
            messageJsons.clear();
            messages.clear();
            TextView statusTextView = (TextView) findViewById(R.id.statusTextView);
            statusTextView.setText(status);
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

    private class CreateGroupTask extends AsyncTask<String, Void, String> {
        private Context UIContext;
        private boolean repeat;

        private CreateGroupTask(Context context, boolean repeat) {
            this.UIContext = context;
            this.repeat = repeat;
        }

        @Override
        protected String doInBackground(String... params) {
            return connectToServer(this.repeat, params);
        }

        @Override
        protected void onPostExecute(String result) {
            groupIdEditText.setText(result);
            groupId = result;
            TextView statusTextView = (TextView) findViewById(R.id.statusTextView);
            statusTextView.setText(status);
            new GetMessagesTask(UIContext, true).execute(BACKEND + "/group/" + groupId, "GET");
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

    private class GetMessagesTask extends AsyncTask<String, Void, String> {
        private Context UIContext;
        private boolean repeat;
        String[] params;

        private GetMessagesTask(Context context, boolean repeat) {
            this.UIContext = context;
            this.repeat = repeat;
        }

        @Override
        protected String doInBackground(String... params) {
            this.params = params;
            String result = connectToServer(this.repeat, params);
            JSONArray jsonArray = null;
            try {
                jsonArray = new JSONArray(result);
                JSONArray messageArray = jsonArray.getJSONObject(0).getJSONArray("messages");
                ArrayList<String> newMessageJsons = new ArrayList<String>();
                if (messageArray != null) {
                    for (int i = 0; i < messageArray.length(); i++) {
                        newMessageJsons.add(messageArray.getString(i));
                    }
                }
                if (messageJsons.size() == 0) {
                    messageJsons.addAll(newMessageJsons);
                } else {
                    newMessageJsons = new ArrayList<String>(newMessageJsons.subList(messageJsons.size(), newMessageJsons.size()));
                    messageJsons.addAll(newMessageJsons);
                }
                for (String s : newMessageJsons) {
                    JSONObject messageJson = new JSONObject(s);
                    String m = messageJson.getString("message");
                    String u = messageJson.getString("userid");
                    Message msg = h.obtainMessage(0, u + ": " + m);
                    h.sendMessage(msg);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            TextView statusTextView = (TextView) findViewById(R.id.statusTextView);
            statusTextView.setText(status);
            if (repeat) {
                checkMessages = new Handler();
                checkMessageRunnable = new Runnable() {
                    @Override
                    public void run() {
                        new GetMessagesTask(UIContext, true).execute(params);
                    }
                };
                checkMessages.postDelayed(checkMessageRunnable, 1000);
            }
        }
    }


    private class ConnectTask extends AsyncTask<String, Void, String> {
        private Context UIContext;
        private boolean repeat;

        private ConnectTask(Context context, boolean repeat) {
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
            status = "Up";
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder builder = new StringBuilder();

            String inputString;
            while ((inputString = bufferedReader.readLine()) != null) {
                builder.append(inputString);
            }

            string = builder.toString();

        } catch (IOException e) {
            status = "Down";
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return string;
    }

}
