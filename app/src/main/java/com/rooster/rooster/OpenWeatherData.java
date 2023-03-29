package com.rooster.rooster;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class OpenWeatherData {
    private SpaceTimeStamp spaceTimeStamp;
    private static WeatherDataCallback callback;
    public static String placeName;
    public static Long sunrise;

    public OpenWeatherData(SpaceTimeStamp spaceTimeStamp, WeatherDataCallback callback) {
        // Create the URL for the OpenWeatherMap API call
        this.spaceTimeStamp = spaceTimeStamp;
        this.callback = callback;
        String apiKey = "76fd89af987189f1a3f1aa84bc06fff1";
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" + this.spaceTimeStamp.getLatitude() + "&lon=" + this.spaceTimeStamp.getLongitude() + "&appid=" + apiKey + "&units=metric";
        Log.e("URL", apiUrl);
        // Make the API call using AsyncTask
        new WeatherApiTask().execute(apiUrl);
    }

    public SpaceTimeStamp getSpaceTimeStamp() {
        return spaceTimeStamp;
    }

    public String getPlaceName() {
        return placeName;
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

        @Override
        protected String doInBackground(String... params) {
            String jsonResponse = "";
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
                placeName = jsonObject.getString("name");
                sunrise = sys.getLong("sunrise");
                if (callback != null) {
                    callback.onWeatherDataReceived(OpenWeatherData.this);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
        public interface WeatherDataCallback {
            void onWeatherDataReceived(OpenWeatherData weatherData);
        }

}