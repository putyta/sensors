package ru.hobud.sensors;

import java.util.ArrayList;
import java.util.LinkedList;

import ru.hobud.sensors.SamplingService.SensorHistory;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

public class PressureMonitor extends Activity implements SensorEventListener {

    private static final double PASCAL_TO_STOPB = 760.0 / 1013.25;

    public class SmoothingWindow extends ArrayList<Double> {
        /**
         *
         */
        private static final long serialVersionUID = 2876342446518129513L;
        private int winSize;

        public SmoothingWindow(int win) {
            winSize = win;
        }

        public Double push(Double val) {
            Double res = 0.0;
            if (size() >= winSize)
                remove(0);
            add(val);
            for (Double v : this) {
                res += v;
            }
            return res / size();
        }
    }

    private static final int HISTORY_SIZE = 500;
    private static final int SMOOTH_WINDOW_SIZE = 10;
    private SensorManager sensorManager;
    private Sensor pressuremeter;
    TextView textViewPressure = null;

    RadioGroup radioGroup = null;

    private XYPlot preHistoryPlot = null;
    private SimpleXYSeries pressureHistorySeries = null;
    private LinkedList<Double> pressureHistory;
    private SmoothingWindow pressureSmoothingWin = new SmoothingWindow(SMOOTH_WINDOW_SIZE);

    private LinkedList<Double> altitudeHistory;
    private SmoothingWindow altitudeSmoothingWin = new SmoothingWindow(SMOOTH_WINDOW_SIZE);

    private LinkedList<Double> tempHistory;
    private SmoothingWindow tempSmoothingWin = new SmoothingWindow(SMOOTH_WINDOW_SIZE);

    private BarometricLeveling barometricLeveling;

    private Context ctx;

    private int sensorId = 0;

    {
        pressureHistory = new LinkedList<Double>();
        pressureHistorySeries = new SimpleXYSeries("Pressure");

        altitudeHistory = new LinkedList<Double>();

        tempHistory = new LinkedList<Double>();

        barometricLeveling = new BarometricLeveling();
    }

    private XYPlot longHistoryPlot = null;
    private SimpleXYSeries longHistorySeries = null;

    {
        longHistorySeries = new SimpleXYSeries("Pressure");
    }

    private BroadcastReceiver intentReceiver;

    public PressureMonitor() {
        ctx = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pressure_monitor);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        pressuremeter = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        textViewPressure = (TextView) findViewById(R.id.textViewPressure);
        radioGroup = (RadioGroup) findViewById(R.id.parameterSwitch);
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.preasure)
                    sensorId = 0;
                else if (checkedId == R.id.altitude)
                    sensorId = 1;
                plotLongHistory();
            }
        });
        setTitle("Pressure");

        // setup the APR History plot:
        preHistoryPlot = (XYPlot) findViewById(R.id.pressurePlot);
        preHistoryPlot.setRangeBoundaries(900, 1300, BoundaryMode.AUTO);
        preHistoryPlot.setDomainBoundaries(0, 100, BoundaryMode.AUTO);
        preHistoryPlot.addSeries(pressureHistorySeries, new LineAndPointFormatter(Color.BLACK, Color.RED, null, new PointLabelFormatter(
                Color.TRANSPARENT)));
        preHistoryPlot.setDomainStepValue(5);
        preHistoryPlot.setTicksPerRangeLabel(3);
//        preHistoryPlot.getDomainLabelWidget().pack();
//        preHistoryPlot.setRangeLabel("Temperature (ºC)");
        preHistoryPlot.getRangeLabelWidget().pack();
        preHistoryPlot.setRangeLabel("Pressure (hPa)");
        preHistoryPlot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ctx, "CLICK", Toast.LENGTH_SHORT).show();
            }
        });

        longHistoryPlot = (XYPlot) findViewById(R.id.longHistoryPlot);
        longHistoryPlot.setRangeBoundaries(900, 1300, BoundaryMode.AUTO);
        longHistoryPlot.setDomainBoundaries(0, 100, BoundaryMode.AUTO);
        longHistoryPlot.addSeries(longHistorySeries, new LineAndPointFormatter(Color.BLACK, Color.BLUE, null, new PointLabelFormatter(
                Color.TRANSPARENT)));
        longHistoryPlot.setDomainStepValue(5);
        longHistoryPlot.setTicksPerRangeLabel(3);
//        longHistoryPlot.getDomainLabelWidget().pack();
//        longHistoryPlot.setRangeLabel("Temperature (ºC)");
        longHistoryPlot.getRangeLabelWidget().pack();
        longHistoryPlot.setRangeLabel("Pressure (hPa)");

        // TEST
        if (!SamplingService.isRunning(this)) {
            SamplingAlarm.SetAlarm(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_pressure_monitor, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        float[] data = new float[pressureHistory.size()];
        for (int i = 0; i < pressureHistory.size(); i++) {
            data[i] = pressureHistory.get(i).floatValue();
        }
        outState.putFloatArray("Pressure", data);

        data = new float[pressureSmoothingWin.size()];
        for (int i = 0; i < pressureSmoothingWin.size(); i++) {
            data[i] = pressureSmoothingWin.get(i).floatValue();
        }
        outState.putFloatArray("Pressure Smoothing Window", data);

        data = new float[altitudeHistory.size()];
        for (int i = 0; i < altitudeHistory.size(); i++) {
            data[i] = altitudeHistory.get(i).floatValue();
        }
        outState.putFloatArray("Altitude", data);

        data = new float[altitudeSmoothingWin.size()];
        for (int i = 0; i < altitudeSmoothingWin.size(); i++) {
            data[i] = altitudeSmoothingWin.get(i).floatValue();
        }
        outState.putFloatArray("Altitude Smoothing Window", data);

        data = new float[tempHistory.size()];
        for (int i = 0; i < tempHistory.size(); i++) {
            data[i] = tempHistory.get(i).floatValue();
        }
        outState.putFloatArray("Temperature", data);

        data = new float[tempSmoothingWin.size()];
        for (int i = 0; i < tempSmoothingWin.size(); i++) {
            data[i] = tempSmoothingWin.get(i).floatValue();
        }
        outState.putFloatArray("Temperature Smoothing Window", data);

    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        float[] data = inState.getFloatArray("Pressure");
        if (data != null) {
            pressureHistory.clear();
            for (float f : data) {
                pressureHistory.add((double) f);
            }
        }
        data = inState.getFloatArray("Pressure Smoothing Window");
        if (data != null) {
            pressureSmoothingWin.clear();
            for (float f : data) {
                pressureSmoothingWin.push((double) f);
            }
        }

        data = inState.getFloatArray("Altitude");
        if (data != null) {
            altitudeHistory.clear();
            for (float f : data) {
                altitudeHistory.add((double) f);
            }
        }
        data = inState.getFloatArray("Altitude Smoothing Window");
        if (data != null) {
            altitudeSmoothingWin.clear();
            for (float f : data) {
                altitudeSmoothingWin.push((double) f);
            }
        }

        data = inState.getFloatArray("Temperature");
        if (data != null) {
            tempHistory.clear();
            for (float f : data) {
                tempHistory.add((double) f);
            }
        }
        data = inState.getFloatArray("Temperature Smoothing Window");
        if (data != null) {
            tempSmoothingWin.clear();
            for (float f : data) {
                tempSmoothingWin.push((double) f);
            }
        }
        plotLongHistory();
    }

    private void plotPressureHistory() {
        SensorHistory pressureLongHistory = new SensorHistory(Environment.getExternalStorageDirectory().getPath() + "/Sensors/pressure.dat");
        longHistoryPlot.setTitle("Pressure");
        longHistoryPlot.setRangeLabel("Pressure (hPa)");
        longHistoryPlot.getRangeLabelWidget().pack();
        longHistorySeries.setModel(pressureLongHistory, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
        longHistoryPlot.redraw();
    }

    private void plotAltitudeHistory() {
        SensorHistory altitudeLongHistory = new SensorHistory(Environment.getExternalStorageDirectory().getPath() + "/Sensors/altitude.dat");
        longHistoryPlot.setTitle("Altitude");
        longHistoryPlot.setRangeLabel("Altitude (m)");
        longHistoryPlot.getRangeLabelWidget().pack();
        longHistorySeries.setModel(altitudeLongHistory, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
        longHistoryPlot.redraw();
    }

    private void plotTemperatureHistory() {
        SensorHistory temperatureLongHistory = new SensorHistory(Environment.getExternalStorageDirectory().getPath() + "/Sensors/temperature.dat");
        longHistoryPlot.setRangeLabel("Temperature (ºC)");
        longHistoryPlot.getRangeLabelWidget().pack();
        longHistorySeries.setModel(temperatureLongHistory, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
        longHistoryPlot.redraw();
    }

    private void plotLongHistory() {
        if (sensorId == 0) {
            plotPressureHistory();
        } else if (sensorId == 1) {
            plotAltitudeHistory();
        } else {
            plotTemperatureHistory();
        }
    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, pressuremeter, SensorManager.SENSOR_DELAY_NORMAL);
        intentReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                plotLongHistory();
            }

        };
        // registering our receiver
        this.registerReceiver(intentReceiver, new IntentFilter(SamplingService.NEW_DATA_INTENT));
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        this.unregisterReceiver(this.intentReceiver);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        String valuesString = "";
        double pressure = event.values[0];
        double altitude = barometricLeveling.getValue(pressure);
        valuesString += String.format("Давление: %.0f гПа (%.0f мм.рт.ст.), высота: %.1fм", pressure, pressure*PASCAL_TO_STOPB, altitude);

//        int counter = 0;
//        for (float value : event.values) {
//            valuesString += String.format("%d) %f ", counter++, value);
//        }
//        valuesString += " , " + pressureHistory.size();
        textViewPressure.setText(valuesString);

        double value = pressureSmoothingWin.push(pressure);//(double) event.values[0]);
        // Number[] series1Numbers = {event.values[0], event.values[1],
        // event.values[2]};
        // get rid the oldest sample in history:
        if (pressureHistory.size() > HISTORY_SIZE) {
            pressureHistory.removeFirst();
        }
        // add the latest history sample:
        pressureHistory.addLast(value);// event.values[0]);

        value = altitudeSmoothingWin.push(altitude);//(double) event.values[1]);
        if (altitudeHistory.size() > HISTORY_SIZE) {
            altitudeHistory.removeFirst();
        }
        altitudeHistory.addLast(value);// event.values[0]);

        value = tempSmoothingWin.push((double) event.values[2]);
        if (tempHistory.size() > HISTORY_SIZE) {
            tempHistory.removeFirst();
        }
        tempHistory.addLast(value);
        //if (cooler++ % 2 == 0) return;
        // update the plot with the updated history Lists:
        if (sensorId == 0) {
            pressureHistorySeries.setModel(pressureHistory, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
            preHistoryPlot.setRangeLabel("Pressure (hPa)");
        } else if (sensorId == 1) {
            pressureHistorySeries.setModel(altitudeHistory, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
            preHistoryPlot.setRangeLabel("Altitude (m)");
        } else {
            pressureHistorySeries.setModel(tempHistory, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
            preHistoryPlot.setRangeLabel("Temperature (ºC)");
        }

        // redraw the Plots:
        preHistoryPlot.redraw();
    }

}
