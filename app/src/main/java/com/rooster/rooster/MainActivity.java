package com.rooster.rooster;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import okhttp3.Callback;
import okhttp3.Call;

import okhttp3.Response;
import java.io.IOException;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "YOUR_API_KEY";
    private static final String OPENWEATHERMAP_API_BASE_URL = "https://api.openweathermap.org";
    private static final String GEO_API_PATH = "/geo/1.0/reverse";
    private static final String WEATHER_API_PATH = "/data/2.5/weather";
    private Context context;

    private String nearestCity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);

        String alarmType = "sunrise"; // or "dawn"
        intent.putExtra("alarm_type", alarmType);
        int requestCode = 0;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        long timeInMillis = 0; // TODO: Calculate the time until sunrise or dawn in milliseconds

        // Replace setExact with setExactAndAllowWhileIdle for Android 6.0 and later
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeInMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeInMillis, pendingIntent);
        }

        getCurrentLocation(); // Retrieve current location and set sunrise and dawn alarms
    }

    public void setSunriseAlarm(View view) {
        Toast.makeText(this, "Setting sunrise alarm", Toast.LENGTH_SHORT).show();
        // TODO: Implement setting the sunrise alarm
    }

    public void setDawnAlarm(View view) {
        Toast.makeText(this, "Setting dawn alarm", Toast.LENGTH_SHORT).show();
        // TODO: Implement setting the dawn alarm
    }

    private void getCurrentLocation() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            try {
                                getNearestCity(location.getLatitude(), location.getLongitude());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    private void getNearestCity(double latitude, double longitude) throws IOException {
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(OPENWEATHERMAP_API_BASE_URL + GEO_API_PATH).newBuilder();
        urlBuilder.addQueryParameter("lat", String.valueOf(latitude));
        urlBuilder.addQueryParameter("lon", String.valueOf(longitude));
        urlBuilder.addQueryParameter("limit", "1");
        urlBuilder.addQueryParameter("appid", "YOUR_API_KEY");

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();

                // Parse the JSON response to get the nearest city
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    JSONArray results = jsonObject.getJSONArray("results");
                    JSONObject firstResult = results.getJSONObject(0);
                    String nearestCity = firstResult.getString("name");

                    // Get the sunrise and dawn times for the nearest city
                    getSunriseAndDawnTimes(nearestCity);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void getSunriseAndDawnTimes(String city) throws IOException {
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(OPENWEATHERMAP_API_BASE_URL + WEATHER_API_PATH).newBuilder();
        urlBuilder.addQueryParameter("q", city);
        urlBuilder.addQueryParameter("appid", "YOUR_API_KEY");

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        String json = response.body().string();

        // Parse the JSON response to get the sunrise and dawn times
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONObject sys = jsonObject.getJSONObject("sys");
            long sunriseTimestamp = sys.getLong("sunrise");
            long dawnTimestamp = sunriseTimestamp - 3600 * 1; // Subtract 1 hour from sunrise
            String sunriseTime = convertTimestampToTime(sunriseTimestamp);
            String dawnTime = convertTimestampToTime(dawnTimestamp);

            // Update the sunrise button with the sunrise time
            updateButton("Sunrise", sunriseTime);

            // Update the dawn button with the dawn time
            updateButton("Dawn", dawnTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String convertTimestampToTime(long timestamp) {
        Date date = new Date(timestamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(date);
    }

    private void updateButton(String buttonText, String time) {
        Button button;
        if (buttonText.equals("Sunrise")) {
            button = findViewById(R.id.sunrise_button);
        } else {
            button = findViewById(R.id.dawn_button);
        }
        button.setText(buttonText + " (" + time + ")");
    }
}

