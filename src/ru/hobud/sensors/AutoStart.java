package ru.hobud.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class AutoStart extends BroadcastReceiver {

  SamplingAlarm alarm = null;

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
      context.startService(new Intent(context, SamplingService.class));
      SensorManager sensorManager = (SensorManager) context.getSystemService(android.content.Context.SENSOR_SERVICE);
      Sensor preassureMeter = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
      alarm = new SamplingAlarm(sensorManager, preassureMeter);
      alarm.SetAlarm(context);
    }
  }

}
