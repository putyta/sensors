package ru.hobud.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoStart extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
//      context.startService(new Intent(context, SamplingService.class));
//      SensorManager sensorManager = (SensorManager) context.getSystemService(android.content.Context.SENSOR_SERVICE);
//      Sensor preassureMeter = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

//      Toast.makeText(context, "AutoStart - set the alarm...",
//          Toast.LENGTH_SHORT).show();
      if (!SamplingService.isRunning(context)) {
        SamplingAlarm.SetAlarm(context);
      }
    }
  }

}
