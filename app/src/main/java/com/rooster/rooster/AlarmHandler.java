package com.rooster.rooster;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AlarmHandler {
    public static void setAlarmClock(Context context, Date sunriseTime) {
        String alarmName = "Sunrise";
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("label", alarmName); // Add alarm name as extra data
        int requestCode = alarmName.hashCode(); // Use alarm name hashcode as unique identifier
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_MUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(sunriseTime);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(calendar.getTime());
        Log.d("AlarmHandler", "Setting " + alarmName + " alarm @ " + formattedDate);
        // Set the alarm using AlarmManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
        Log.d("AlarmHandler",  alarmName + " has been set @ " + formattedDate);
    }
}
