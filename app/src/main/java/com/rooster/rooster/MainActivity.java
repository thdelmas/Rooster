package com.rooster.rooster;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private SpaceTimeStamp spaceTimeStamp;
    private OpenWeatherData openWeatherData;
    private TextView altitudeTextView;
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView timeTextView;
    private TextView placeTextView;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check if location permissions are granted
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // initialize the SpaceTimeStamp object
            this.spaceTimeStamp = new SpaceTimeStamp(this);
            updateLayoutInfo(this.openWeatherData, this.spaceTimeStamp);
        } else {
            // request location permissions
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    // handle the result of the location permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // location permissions granted, initialize the SpaceTimeStamp object
                spaceTimeStamp = new SpaceTimeStamp(this);
            } else {
                // location permissions not granted, show a message and finish the activity
                Toast.makeText(this, "Location permissions not granted", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void updateLayoutInfo(OpenWeatherData data, SpaceTimeStamp timeStamp) {
        altitudeTextView = findViewById(R.id.location_altitude);
        latitudeTextView = findViewById(R.id.location_latitude);
        longitudeTextView = findViewById(R.id.location_longitude);
        timeTextView = findViewById(R.id.location_time);
        placeTextView = findViewById(R.id.location_place);
        altitudeTextView.setText(String.valueOf(spaceTimeStamp.getAltitude()));
        latitudeTextView.setText(String.valueOf(spaceTimeStamp.getLatitude()));
        longitudeTextView.setText(String.valueOf(spaceTimeStamp.getLongitude()));
        timeTextView.setText(String.valueOf(spaceTimeStamp.getTime()));
        //placeTextView.setText(String.valueOf(openWeatherData.getPlaceName()));
    }


    private void setSunriseAlarm(long timeInMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {

            Intent intent = new Intent(this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 123, intent, PendingIntent.FLAG_IMMUTABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    // Schedule exact alarms
                    AlarmManager.AlarmClockInfo alarm = new AlarmManager.AlarmClockInfo(timeInMillis, pendingIntent);
                    alarmManager.setAlarmClock(alarm, pendingIntent);
                } else {
                    // Ask users to grant the permission in the corresponding settings page
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            }
        }
    }
}

