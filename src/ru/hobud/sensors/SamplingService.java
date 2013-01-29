package ru.hobud.sensors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;

public class SamplingService extends Service {
  SamplingAlarm alarm = null;

  public void onCreate() {
    super.onCreate();
    SensorManager sensorManager = (SensorManager) getSystemService(android.content.Context.SENSOR_SERVICE);
    Sensor preassureMeter = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
    alarm = new SamplingAlarm(sensorManager,preassureMeter); 
  }

  public void onStart(Context context, Intent intent, int startId) {
    alarm.SetAlarm(context);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
