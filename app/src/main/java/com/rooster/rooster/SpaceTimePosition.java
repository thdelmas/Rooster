package com.rooster.rooster;
import java.util.Date;

public class SpaceTimePosition {
    private double altitude;
    private double longitude;
    private double latitude;
    private Date time;

    public SpaceTimePosition(double altitude, double latitude, double longitude, Date time) {
        this.altitude = altitude;
        this.longitude = longitude;
        this.latitude = latitude;
        this.time = time;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String toString() {
        return "SpaceTime: altitude=" + altitude + ", longitude=" + longitude + ", latitude=" + latitude + ", time=" + time;
    }
}
