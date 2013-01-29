package ru.hobud.sensors;

import java.lang.reflect.Field;
import java.util.List;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.TextView;

public class MainPanel extends Activity {

  private SensorManager sensorManager;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main_panel);
    TextView textView = (TextView) findViewById(R.id.textView);
    textView.setMovementMethod(new ScrollingMovementMethod());
    
    
    sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
    List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
    String sensorNames = "";
    for(Sensor sensor : sensors) {
      sensorNames += String.format("type: %d, name: '%s'\n", sensor.getType(), sensor.getName());
    }
    TelephonyManager telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
    String devicIMEI = telephonyManager.getDeviceId();
    String phoneNumber = telephonyManager.getLine1Number();
    String androidID = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
    Field[] fields = Build.class.getFields();
    String s = "Fields: ---------------------\n";
    for(Field f: fields) {
      try {
        s += f.getName() + ": " + f.get(null) + "\n";
      } catch (IllegalArgumentException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    textView.setText(String.format("%s\nIMEI: %s\nPhone number: %s\nAndroid ID: %s\nBoard: %s\nBOOTLOADER: %s\nBRAND: %s\nCPU_ABI: %s\nCPU_ABI2: %s\nDEVICE: %s\n%s", 
        sensorNames, devicIMEI, phoneNumber, androidID, Build.BOARD, Build.BOOTLOADER, Build.BRAND, Build.CPU_ABI, Build.CPU_ABI2, Build.DEVICE, s));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.activity_main_panel, menu);

    return true;
  }

}
