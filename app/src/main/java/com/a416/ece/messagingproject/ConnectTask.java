package com.a416.ece.messagingproject;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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



//public class ConnectTask extends AsyncTask<String, Void, String> {
//    private Context UIContext;
//    private boolean repeat;
//
//    public ConnectTask(Context context, boolean repeat) {
//        this.UIContext = context;
//        this.repeat = repeat;
//    }
//
//    @Override
//    protected String doInBackground(String... params) {
//        String string = "";
////        try {
////            URL url = new URL(params[0]);
////            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
////
////            InputStream stream = new BufferedInputStream(urlConnection.getInputStream());
////            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
////            StringBuilder builder = new StringBuilder();
////
////            String inputString;
////            while ((inputString = bufferedReader.readLine()) != null) {
////                builder.append(inputString);
////            }
////
////            JSONObject topLevel = new JSONObject(builder.toString());
////            string = builder.toString();
////
////        } catch (IOException | JSONException e) {
////            e.printStackTrace();
////        }
//
//        Handler mainHandler = new Handler(Looper.getMainLooper()){};
////        Message msg = mainHandler.obtainMessage(0, string);
//        Message msg = mainHandler.obtainMessage(0, "swag");
//        mainHandler.sendMessage(msg);
//
//        return string;
//    }
//
//    @Override
//    protected void onPostExecute(String result) {
//        if(repeat){
//            Handler h = new Handler();
//            Runnable r = new Runnable() {
//                @Override
//                public void run() {
//                    new ConnectTask(UIContext, true).execute();
//                }
//            };
//            h.postDelayed(r, 5000);
//        }
//
//    }
//}
