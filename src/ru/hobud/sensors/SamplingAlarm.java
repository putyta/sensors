package ru.hobud.sensors;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SamplingAlarm extends BroadcastReceiver {
  public static final int ALARM_INTERVAL = 1000 * 60 * 5; //milliseconds

  @Override
  public void onReceive(Context context, Intent intent) {
//    Toast.makeText(context, "Alarm received - let's start Sampling Service...", Toast.LENGTH_SHORT).show(); 
//    
    context.startService(new Intent(context, SamplingService.class));
  }

  public static void SetAlarm(Context context) {
    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    Intent i = new Intent(context, SamplingAlarm.class);
    PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
    am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), ALARM_INTERVAL, pi);
  }

  public static void CancelAlarm(Context context) {
    Intent intent = new Intent(context, SamplingAlarm.class);
    PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    alarmManager.cancel(sender);
  }

}
