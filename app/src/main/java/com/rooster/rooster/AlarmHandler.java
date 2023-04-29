package com.rooster.rooster;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class AlarmHandler {
    public static void setAlarmClock(Context context, long sunriseTimeMillis) {
        String alarmName = "Sunrise";
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("label", alarmName); // Add alarm name as extra data
        int requestCode = alarmName.hashCode(); // Use alarm name hashcode as unique identifier
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_MUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(sunriseTimeMillis);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        Log.d("AlarmHandler", "Setting " + alarmName + " alarm for " + hour + ":" + minute);

        // Set the alarm using AlarmManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, sunriseTimeMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, sunriseTimeMillis, pendingIntent);
        }
        Log.d("AlarmHandler", alarmName + " set for " + hour + ":" + minute);
    }
}
