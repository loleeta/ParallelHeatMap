package com.lolita;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tally of Observations using a timestamp.
 * HW6
 */
class ObsTally {
    public HashMap<Long, int[]> heatmap;
    private static final int DIM = 20; // Dimension of heat map

    /**
     * Constructor initializes variables to handle observations
     */
    public ObsTally() {
        heatmap = new HashMap<>();
    }

    /**
     * Constructor which takes in a single observation
     * @param o An observation
     */
    public ObsTally(Observation o) {
        this();
        accum(o);
    }

    /**
     * Translates the double into the index of the array
     * @param place
     * @return index of the array
     */
    private int place(double place) {
        return (int) ((place  + 1.0) / (2.0 / DIM));
    }

    /**
     * Increments the value for a cell in array
     * @param r Row of array
     * @param c Column of array
     */
    private void incrementCell(Long time, int r, int c) {
        int [] map = this.heatmap.get(time);
        map[r * DIM + c]++;
    }

    /**
     * Given two ObsTally, combine their values together and return
     * a new ObsTally
     * @param a One ObsTally
     * @param b Another ObsTally
     * @return a new ObsTally
     */
    public static ObsTally combine(ObsTally a, ObsTally b) {
        ObsTally combinedTally = new ObsTally();
        //first copy all data from A
        for (HashMap.Entry<Long, int[]> entry: a.heatmap.entrySet()) {
            Long time = entry.getKey();
            int[] aHeatMap = entry.getValue();
            combinedTally.heatmap.put(time, aHeatMap);
        }

        //then copy data from B over
        for (HashMap.Entry<Long, int[]> entry: b.heatmap.entrySet()) {
            Long bTime = entry.getKey();
            int[] bHeatMap = entry.getValue();
            //check if timestamp in B is in combinedTally
            //if timestamp exists in combined, add the arrays up
            int [] cHeatMap = combinedTally.heatmap.get(bTime);
            if (cHeatMap == null) {
                combinedTally.heatmap.put(bTime, bHeatMap);
            }
            else {
                for (int i = 0; i < cHeatMap.length; i++) {
                    cHeatMap[i] += bHeatMap[i];
                }
            }
        }
        return combinedTally;
    }

    /**
     * Add an Observation to the heatmap.
     * @param o An Observation
     */
    public void accum(Observation o) {
        //check if heatmap has this timestamp, and retrieve/create new int[]
        //increment the place as usual
        Long time = o.time;
        int [] map = this.heatmap.get(time);
        if (map == null) {
            map = new int[DIM * DIM]; //create a new int array
            this.heatmap.put(time, map);
        }
        incrementCell(time, place(o.x), place(o.y));
    }

    /**
     * String representation of the hash map.
     * @return
     */
    public String toString() {
        String s = "";
        for (HashMap.Entry<Long, int[]> entry: this.heatmap.entrySet
                ()) {
            Long t = entry.getKey();
            int[] hmap = entry.getValue();
            String timeStamp = t + ": " + Arrays.toString(hmap);
            s += timeStamp;
            s += "\n";
        }
        return s;
    }
}

/**
 * Subclass of GeneralScan which does a reduce/scan on Observations.
 */
class HeatMapScan2 extends GeneralScan<Observation, ObsTally> {

    /**
     * Calls parent constructor.
     * @param raw   List of Observations
     * @param threshold Thread threshold for parallelization
     */
    public HeatMapScan2(List<Observation> raw, int threshold) {
        super(raw, threshold);
    }

    /**
     * Creates a new empty ObsTally.
     * @return a new ObsTally
     */
    @Override
    protected ObsTally init() {
        return new ObsTally();
    }

    /**
     * Creates a ObsTally given some data.
     * @param o an Observation
     * @return a new ObsTally
     */
    @Override
    protected ObsTally prepare(Observation o) {
        return new ObsTally(o);
    }

    /**
     * Given two ObsTallys, combines them.
     * @param a One ObsTally
     * @param b Another ObsTally
     * @return a new ObsTally
     */
    @Override
    protected ObsTally combine(ObsTally a, ObsTally b) {
        return ObsTally.combine(a, b);
    }

    /**
     * Adds data of Observation into existing ObsTally.
     * @param tally an ObsTally
     * @param o an Observation
     */
    @Override
    protected void accum(ObsTally tally, Observation o) {
        tally.accum(o);
    }
}
