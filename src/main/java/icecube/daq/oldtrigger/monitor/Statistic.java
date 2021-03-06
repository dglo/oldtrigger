/*
 * class: Statistic
 *
 * Version $Id: Statistic.java 14204 2013-02-11 19:52:57Z dglo $
 *
 * Date: May 7 2006
 *
 * (c) 2006 IceCube Collaboration
 */

package icecube.daq.oldtrigger.monitor;

/**
 * This class implements a simple statistic, keeping track of the average value as
 * well as min and max values.
 *
 * @version $Id: Statistic.java 14204 2013-02-11 19:52:57Z dglo $
 * @author pat
 */
public class Statistic
{

    private double sum;
    private long count;

    private double min;
    private double max;

    private boolean newMin;
    private boolean newMax;

    public Statistic() {
        sum = 0;
        count = 0;
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        newMin = false;
        newMax = false;
    }

    public void add(double value) {
        sum += value;
        count++;

        if (value < min) {
            min = value;
            newMin = true;
        } else {
            newMin = false;
        }

        if (value > max) {
            max = value;
            newMax = true;
        } else {
            newMax = false;
        }
    }

    public double getAverage() {
        if (count < 1) {
            return 0;
        }
        return sum/((double) count);
    }

    public double getSum() {
        return sum;
    }

    public long getCount() {
        return count;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public boolean isNewMin() {
        return newMin;
    }

    public boolean isNewMax() {
        return newMax;
    }

}
