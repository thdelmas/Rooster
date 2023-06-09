package com.rooster.rooster;
import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private Context mcontext;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Button syncGPSButton;
    private SpaceTimePosition spaceTimePosition = null;
    private OpenWeatherData openWeatherData = null;
    SharedPreferences prefs;
    boolean isAlarmSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mcontext = this.getApplicationContext();
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        prefs = this.getSharedPreferences("com.rooster.rooster", Context.MODE_PRIVATE);
        isAlarmSet = prefs.getBoolean("isAlarmSet", false);

        final Button setAlarmButton = findViewById(R.id.set_alarm);
        if (isAlarmSet) {
            setAlarmButton.setText("Tap to unset alarm");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.e("Checkpoint", "Permission result");
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            Log.e("Checkpoint", "Permission result 2");
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("Checkpoint", "Permission result 3");
                // User granted the permission
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Permission is already granted
                    Toast.makeText(MainActivity.this, "Location permission granted", Toast.LENGTH_SHORT).show();

                    // Request location updates
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            // Do something with the retrieved location
                            Log.e("Location", "Changed");
                            if (openWeatherData != null) {
                                return;
                            }
                            double altitude = location.getAltitude();
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Date time = new Date();
                            TextView altitudeTextview = findViewById(R.id.location_altitude);
                            TextView latitudeTextview = findViewById(R.id.location_latitude);
                            TextView longitudeTextview = findViewById(R.id.location_longitude);
                            TextView timeTextview = findViewById(R.id.location_time);
                            spaceTimePosition = new SpaceTimePosition(altitude, latitude, longitude, time);
                            altitudeTextview.setText(String.valueOf(altitude));
                            latitudeTextview.setText(String.valueOf(latitude));
                            longitudeTextview.setText(String.valueOf(longitude));
                            timeTextview.setText(String.valueOf(time));
                            OpenWeatherData openWeatherData = new OpenWeatherData(mcontext, spaceTimePosition, findViewById(R.id.location_place), findViewById(R.id.set_alarm));
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                        }
                    });
                } else {
                    // Permission is not yet granted, request it from the user
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                }
            }
            } else {
                // User denied the permission
                Toast.makeText(mcontext, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

