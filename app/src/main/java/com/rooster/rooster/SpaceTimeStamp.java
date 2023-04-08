package com.rooster.rooster;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import java.time.Instant;

public class SpaceTimeStamp {
    private final double latitude;
    private final double longitude;
    private final double altitude;
    private final long time;

    public SpaceTimeStamp(Context context) {
        double[] currentLocation = getCurrentLocation(context);
        this.latitude = currentLocation[0];
        this.longitude = currentLocation[1];
        this.altitude = currentLocation[2];
        this.time = Instant.now().getEpochSecond();
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public long getTime() {
        return time;
    }

    private double[] getCurrentLocation(Context context) {
        double[] location = { 0.0, 0.0, 0.0 };
        try {
            // get the location manager
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            // check if location permissions are granted
            if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    || context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // get the last known location from the network provider
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastLocation != null) {
                    // get the latitude, longitude, and altitude from the location
                    double lat = lastLocation.getLatitude();
                    double lon = lastLocation.getLongitude();
                    double alt = lastLocation.getAltitude();
                    location[0] = lat;
                    location[1] = lon;
                    location[2] = alt;
                }
            }
        } catch (Exception e) {
            System.out.println("Error getting location: " + e.getMessage());
        }
        return location;
    }
}