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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

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
    public static final String BACKEND = "http://ec2-54-145-217-121.compute-1.amazonaws.com:3000";
    //    public static final String BACKEND = "http://192.168.99.0:3000";
    public static ArrayAdapter adapter;
    public static ArrayList<String> list;
    public static ListView listView;
    public static SharedPreferences sharedPref;

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


        Button showGroup = (Button) findViewById(R.id.button2);
        showGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                try {
//                    new ConnectTask(getApplicationContext(), false).execute(BACKEND + "/user/create", "PUT").get();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                } catch (ExecutionException e) {
//                    e.printStackTrace();
//                }
            }
        });

        Context context = getApplicationContext();
        sharedPref = context.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        Log.v("KK", "wtf");
        if (!sharedPref.contains(getString(R.string.userid))) {
            String result = "";
            try {
                result = new ConnectTask(getApplicationContext(), false).execute(BACKEND + "/user/create", "PUT", "").get();
                SharedPreferences.Editor editor = sharedPref.edit();
                if (result != "") {
                    editor.putString(getString(R.string.userid), result);
                    editor.apply();
                }
                Log.v("KK", sharedPref.getString("userid", "nope"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        Log.v("KK", sharedPref.getString("userid", "nope"));

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

    public void createGroup(View view) {
        String result = "";
        new ConnectTask(getApplicationContext(), false).execute(BACKEND + "group/create", "POST");
    }

    public void joinGroup(View view) {
        EditText editText = (EditText) findViewById(R.id.groupIdText);
        String groupId = editText.getText().toString();
        String result = "";
        String userid = sharedPref.getString("userid", "0");
        if (!userid.equals("0")) {
            new ConnectTask(getApplicationContext(), false).execute(BACKEND + "group/" + groupId + "/join", "POST", "{\"userid\": " + userid + "}");
        }
    }

    public void quitGroup(View view) {
        EditText editText = (EditText) findViewById(R.id.groupIdText);
        String groupId = editText.getText().toString();
        String result = "";
        new ConnectTask(getApplicationContext(), false).execute(BACKEND + "group/" + groupId + "/quit", "POST", "");
    }

    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.messageText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
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
            String string = "";
            URL url = null;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                if (params[1] == "POST") {
                    urlConnection.setReadTimeout(5000);
                    urlConnection.setConnectTimeout(5000);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setDoOutput(true);

                    OutputStream os = urlConnection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(params[2]);
                    writer.flush();
                    writer.close();
                    os.flush();
                    os.close();
                } else if (params[1] == "PUT") {
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

                JSONObject topLevel = new JSONObject(builder.toString());
                string = builder.toString();

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            if (repeat) {
                Message msg = h.obtainMessage(0, string);
//                Message msg = h.obtainMessage(0, "swag");
                h.sendMessage(msg);
            }

            return string;
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


}
