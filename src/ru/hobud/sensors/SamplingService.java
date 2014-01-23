package ru.hobud.sensors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.LinkedList;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;

public class SamplingService extends Service {
    private static boolean loggingOn = false;
    public static final String NEW_DATA_INTENT = "ru.hobud.NEW_DATA";
    SamplingAlarm alarm = null;
    private static final int HISTORY_SIZE = 1000;
    private SensorManager sensorManager;
    private Sensor pressureMeter;
    static int count = 0;
    private SensorsSampler sensorsSampler = null;
    private final static String logFile = Environment.getExternalStorageDirectory().getPath() + "/tmp/sensors.log";

    private BarometricLeveling barometricLeveling;

    static Context ctx;

    public SamplingService() {
        ctx = this;
        barometricLeveling = new BarometricLeveling();
    }

    public static void logMessage(String start_str) {
        RandomAccessFile file = null;
        try {
            if (loggingOn) {
                file = new RandomAccessFile(logFile, "rw");
                file.seek(file.length());
                file.write(start_str.getBytes());
                file.close();
            }
        } catch (Exception e) {
        }
    }

    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SamplingService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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

    public static class SensorHistory extends LinkedList<Double> {

        /**
         *
         */
        private static final long serialVersionUID = 2064249086664097671L;
        private String filename;

        public SensorHistory(String fn) {
            filename = fn;
            String line = "";
            if (externalStorageAvailable()) {
                File f = new File(filename);
                File dir = new File(f.getParent());
                if(!dir.exists()) dir.mkdirs();
                if (f.exists()) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                        line = reader.readLine();
                        line = line.replace(',', '.');
                        while (line != null && !line.isEmpty()) {
                            add(Double.parseDouble(line));
                            if (size() > HISTORY_SIZE)
                                remove(0);
                            line = reader.readLine();
                            line = line.replace(',', '.');
                        }
                        reader.close();
                    } catch (Exception exc) {
                        String s = line + "\n";
                        for (StackTraceElement e : exc.getStackTrace())
                            s += String.format("%s:%d: %s\n", e.getFileName(), e.getLineNumber(), e.toString());
                        logMessage("EXCEPTION reading " + filename + "\n" + exc.toString() + "\n" + s);
                    }
                }
            }
        }

        public void push(double v) {
//      double v1 = Double.NaN;
            add(v);
            if (size() > HISTORY_SIZE) {
//        v1 = 
                remove();
            }
//Toast.makeText(ctx, String.format("Add: %f, remove: %f, size: %d", v, v1, size()), Toast.LENGTH_SHORT).show(); 
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
        private SensorHistory pressureHistory = null;
        private SensorHistory altitudeHistory = null;
        private SensorHistory temperatureHistory = null;
        private Runnable callback = null;
        public boolean oneShotFlag = false;

        public SensorsSampler(Runnable callback) {
            pressureHistory = new SensorHistory(Environment.getExternalStorageDirectory().getPath() + "/Sensors/pressure.dat");
            altitudeHistory = new SensorHistory(Environment.getExternalStorageDirectory().getPath() + "/Sensors/altitude.dat");
            temperatureHistory = new SensorHistory(Environment.getExternalStorageDirectory().getPath() + "/Sensors/temperature.dat");
            this.callback = callback;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (oneShotFlag) {
                double pressure = event.values[0];
                double altitude = barometricLeveling.getValue(pressure); //event.values[1];
                double temperature = event.values[2];
                pressureHistory.push(pressure);
                altitudeHistory.push(altitude);
                temperatureHistory.push(temperature);
                pressureHistory.flush();
                altitudeHistory.flush();
                temperatureHistory.flush();
                if (callback != null) {
                    callback.run();
                }
                oneShotFlag = false;
            }
        }

    }

    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(android.content.Context.SENSOR_SERVICE);
        pressureMeter = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorsSampler = new SensorsSampler(new Runnable() {

            @Override
            public void run() {
                sensorManager.unregisterListener(sensorsSampler);
                Intent intent = new Intent(NEW_DATA_INTENT);
                sendBroadcast(intent);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // PowerManager pm = (PowerManager)
        // context.getSystemService(Context.POWER_SERVICE);
        // PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        // wl.acquire();

        // Чтение одной выборки и сохранение в файле
        sensorsSampler.oneShotFlag = true;
        sensorManager.registerListener(sensorsSampler, pressureMeter, SensorManager.SENSOR_DELAY_NORMAL);

        // wl.release();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
