package com.lolita;

import java.util.List;

/**
 * Tally which is a wrapper for a Double
 */
class DoubleTally {
    public double value = 0.0;

    public DoubleTally()
    {
    }

    /**
     * Constructor for DoubleTally
     * @param value
     */
    public DoubleTally(double value) {
        this.value = value;
    }
}


/**
 * Subclass for GeneralScan
 */
class SumScan extends GeneralScan<Double, DoubleTally> {
    /**
     * Constructor
     *
     * @param raw             data that is passed in
     * @param threadThreshold amount of work per thread
     */
    public SumScan(List<Double> raw, int threadThreshold, int num) {
        super(raw, threadThreshold, num);
        //need to change this back
    }

    /**
     * Creates an empty Tally object
     *
     * @return Tally object
     */
    @Override
    protected DoubleTally init() {
        return new DoubleTally();
    }

    /**
     * Given a value, create a Tally with that value
     *
     * @param datum a double
     * @return a new Tally
     */
    @Override
    protected DoubleTally prepare(Double datum) {
        return new DoubleTally(datum);
    }

    /**
     * Given two Tally objects, combine their values
     *
     * @param left  one Tally
     * @param right another Tally
     * @return Tally
     */
    @Override
    protected DoubleTally combine(DoubleTally left, DoubleTally right) {
        return new DoubleTally(left.value + right.value);
    }

    /**
     * Calls parent class accum to get sum of items seen so far
     *
     * @param tally Current Tally item
     * @param datum node to accum to
     */
    @Override
    protected void accum(DoubleTally tally, Double datum) {
        tally.value += datum;
    }
}
