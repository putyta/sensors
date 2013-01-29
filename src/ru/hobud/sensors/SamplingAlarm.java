package ru.hobud.sensors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.LinkedList;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.widget.Toast;

public class SamplingAlarm extends BroadcastReceiver {

  private static final int HISTORY_SIZE = 1000;
  private SensorManager sensorManager;
  private Sensor pressuremeter;
  static int count = 0;
  private SensorsSampler sensorsSampler = null; 

  public SamplingAlarm(SensorManager sm, Sensor pr) {
    sensorManager = sm;
    pressuremeter = pr;
    sensorsSampler = new SensorsSampler(new Runnable() {
      
      @Override
      public void run() {
        sensorManager.unregisterListener(sensorsSampler);
      }
    });
  }

  // проверяет доступно ли внешнее хранилище на чтение
  private static boolean externalStorageAvailable() {
    boolean mExternalStorageAvailable = false;
    String state = Environment.getExternalStorageState();

    if (Environment.MEDIA_MOUNTED.equals(state)) {
      mExternalStorageAvailable = true;
    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      mExternalStorageAvailable = true;
    } else {
      mExternalStorageAvailable = false;
    }
    return mExternalStorageAvailable;
  }

  public class SensorHistory extends LinkedList<Double> {

    /**
     * 
     */
    private static final long serialVersionUID = 2064249086664097671L;
    private String filename;

    public SensorHistory(String fn) {
      filename = fn;
      if (externalStorageAvailable()) {
        File f = new File(filename);
        if (f.exists()) {
          try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            String line = reader.readLine();
            while (line != null) {
              add(Double.parseDouble(line));
              if (size() > HISTORY_SIZE)
                remove(0);
              line = reader.readLine();
            }
            reader.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }

    public void push(double v) {
      add(v);
      if (size() > HISTORY_SIZE)
        remove(0);
    }

    public void flush() {
      if (externalStorageAvailable()) {
        try {
          RandomAccessFile file = new RandomAccessFile(filename, "rw");
          for (double v : this) {
            file.writeBytes(String.format("%f\n", v));
          }
          file.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void close() throws IOException {
      flush();
    }
  }

  private class SensorsSampler implements SensorEventListener {
    private SensorHistory preassureHistory = null;
    private SensorHistory altitudeHistory = null;
    private SensorHistory temperatureHistory = null;
    private Runnable callback = null;

    public SensorsSampler(Runnable callback) {
      preassureHistory = new SensorHistory("/mnt/sdcard/Sensors/preassure.dat");
      altitudeHistory = new SensorHistory("/mnt/sdcard/Sensors/altitude.dat");
      temperatureHistory = new SensorHistory("/mnt/sdcard/Sensors/temperature.dat");
      this.callback = callback;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
      double preassure = event.values[0];
      double altitude = event.values[1];
      double temperature = event.values[2];
      preassureHistory.push(preassure);
      altitudeHistory.push(altitude);
      temperatureHistory.push(temperature);
      preassureHistory.flush();
      altitudeHistory.flush();
      temperatureHistory.flush();
      if(callback != null) {
        callback.run();
      }
    }

  }

  @Override
  public void onReceive(Context context, Intent intent) {
//    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
//    wl.acquire();

    // Чтение одной выборки и сохранение в файле
    sensorManager.registerListener(sensorsSampler, pressuremeter, SensorManager.SENSOR_DELAY_FASTEST);
    Toast.makeText(context, "Alarm !!!!!!!!!! " + count++, Toast.LENGTH_LONG).show(); 

//    wl.release();
  }

  public void SetAlarm(Context context) {
    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    Intent i = new Intent(context, SamplingAlarm.class);
    PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
    am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 / 6, pi);
  }

  public void CancelAlarm(Context context) {
    Intent intent = new Intent(context, SamplingAlarm.class);
    PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    alarmManager.cancel(sender);
  }

}
