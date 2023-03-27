package com.rooster.rooster;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String alarmType = intent.getStringExtra("alarm_type");
        Toast.makeText(context, "Alarm for " + alarmType + "!", Toast.LENGTH_SHORT).show();
    }
}
