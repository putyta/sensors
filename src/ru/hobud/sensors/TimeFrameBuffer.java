package ru.hobud.sensors;

import android.util.Pair;

import java.util.Date;
import java.util.LinkedList;

/**
 * Created by mi on 06.04.14.
 */
public class TimeFrameBuffer {
    private LinkedList<Pair<Date, Double>> values;
    private double length = 1.0;
    TimeFrameBuffer(double length) {
        if(length > 0) {
            this.length = length;
        }
    }

    public void add(double value) {
        Date now = new Date();
    }
}
