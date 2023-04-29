package com.rooster.rooster;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class OpenWeatherData extends MainActivity {
    private Context mContext;
    private SpaceTimePosition spaceTimePosition;
    public String placeName;
    public Long sunrise;
    private OnDataReceivedListener listener;
    private TextView placeNameView;
    private Button setAlarmSunrise;

    public OpenWeatherData(Context context, SpaceTimePosition spaceTimeStamp, TextView placeView, Button setAlarm) {
        this.mContext = context;
        this.spaceTimePosition = spaceTimeStamp;
        this.placeNameView = placeView;
        this.setAlarmSunrise = setAlarm;
        String apiKey = "76fd89af987189f1a3f1aa84bc06fff1";
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" + this.spaceTimePosition.getLatitude() + "&lon=" + this.spaceTimePosition.getLongitude() + "&appid=" + apiKey + "&units=metric";
        // Make the API call using AsyncTask
        WeatherApiTask task = new WeatherApiTask();
        task.execute(apiUrl);
    }

    public SpaceTimePosition getSpaceTimeStamp() {
        return spaceTimePosition;
    }

    public Long getSunrise() {
        return sunrise;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public void setSunrise(Long sunrise) {
        this.sunrise = sunrise;
    }

    // AsyncTask to make the network call in the background
    protected class WeatherApiTask extends AsyncTask<String, Void, String> {

        private String jsonResponse;

        public WeatherApiTask() {
        }

        @Override
        protected String doInBackground(String... params) {
            this.jsonResponse = "";
            try {
                URL url = new URL(params[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonResponse += line + "\n";
                }
                reader.close();
                urlConnection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return jsonResponse;
        }

        @Override
        protected void onPostExecute(String jsonResponse) {
            try {
                JSONObject jsonObject = new JSONObject(jsonResponse);
                JSONObject sys = jsonObject.getJSONObject("sys");
                String placeName = jsonObject.getString("name");
                long sunrise = sys.getLong("sunrise");
                placeNameView.setText(placeName);
                Date date = new Date(sunrise * 1000L); // convert seconds to milliseconds
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                String formattedTime = sdf.format(date);
                setAlarmSunrise.setText(formattedTime);
                setAlarmSunrise.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        long sunrise2 = (System.currentTimeMillis()/1000) + 60;
                        AlarmHandler.setAlarmClock(mContext, sunrise2 * 1000L);
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setOnDataReceivedListener(OnDataReceivedListener listener) {
        this.listener = listener;
    }


    public String getPlaceName() {
        // call the API in a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                String jsonResponse = "";
                try {
                    String apiKey = "76fd89af987189f1a3f1aa84bc06fff1";
                    String apiUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" + spaceTimePosition.getLatitude() + "&lon=" + spaceTimePosition.getLongitude() + "&appid=" + apiKey + "&units=metric";
                    URL url = new URL(apiUrl);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonResponse += line + "\n";
                    }
                    reader.close();
                    urlConnection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    JSONObject sys = jsonObject.getJSONObject("sys");
                    placeName = jsonObject.getString("name");
                    // get the nearest place name from the API response
                    // call the listener to notify that the data is ready
                    if (listener != null) {
                        listener.onDataReceived();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return placeName;
    }

    public interface OnDataReceivedListener {
        void onDataReceived();
    }

    class WeatherDataCallback {
    }

    // Callback interface for notifying when weather data is received
}