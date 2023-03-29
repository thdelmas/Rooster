package com.rooster.rooster;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.rooster.rooster.*;

public class MainActivity extends AppCompatActivity implements OpenWeatherData.WeatherDataCallback {

    private static final int PERMISSION_REQUEST_CODE = 123;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private OpenWeatherData openWeatherData;
    private TextView altitudeTextView;
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView timeTextView;
    private TextView placeTextView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SpaceTimeStamp spaceTimeStamp = new SpaceTimeStamp(this);
        altitudeTextView = findViewById(R.id.location_altitude);
        latitudeTextView = findViewById(R.id.location_latitude);
        longitudeTextView = findViewById(R.id.location_longitude);
        timeTextView = findViewById(R.id.location_time);
        placeTextView = findViewById(R.id.location_place);
        altitudeTextView.setText(String.valueOf(spaceTimeStamp.getAltitude()));
        latitudeTextView.setText(String.valueOf(spaceTimeStamp.getLatitude()));
        longitudeTextView.setText(String.valueOf(spaceTimeStamp.getLongitude()));
        timeTextView.setText(String.valueOf(spaceTimeStamp.getTime()));
        OpenWeatherData openWeatherData = new OpenWeatherData(spaceTimeStamp, this);
    }

    @Override
    public void onWeatherDataReceived(OpenWeatherData weatherData) {
        placeTextView.setText(weatherData.getPlaceName());
    }

    // Stop location updates when the activity is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}