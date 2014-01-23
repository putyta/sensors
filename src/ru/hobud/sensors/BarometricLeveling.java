package ru.hobud.sensors;

/**
 * Created by mi on 19.01.14.
 */
public class BarometricLeveling {
    private static  final double ALPHA_AIR = 3.665e-3; // коэффициент теплового расширения воздуха при T=0-100ºC
    private double p0;
    private double h0;
    private double t;

    public BarometricLeveling() {
        p0 = -1;
        h0 = 0;
        t = 20;
    }

    public void setH0(double h0) {
        this.h0 = h0;
    }

    public void setP0(double p0) {
        this.p0 = p0;
    }

    public void setT(double t) {
        this.t = t + 273.15;
    }

    public double getValue(double p) {
        double res = 0;
        if (p0 < 0) {
            p0 = p;
        }
        // Δh = 18400 ∙ (1+αt) lg (p1/p2)
        res = 18400 * (1 + ALPHA_AIR * t) * Math.log(p0 / p) + h0;
        return res;
    }

}
