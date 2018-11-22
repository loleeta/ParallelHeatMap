package com.lolita;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

class ObsTally {
    public HashMap<Long, int[]> heatmap;
    private static final int DIM = 20; // Dimension of heat map

    public ObsTally() {
        heatmap = new HashMap<>();
    }

    public ObsTally(Observation o) {
        this();
        accum(o);
    }

    private int place(double place) {
        return (int) ((place  + 1.0) / (2.0 / DIM));
    }

    private void incrementCell(Long time, int r, int c) {
        int [] map = this.heatmap.get(time);
        map[r * DIM + c]++;
    }

    public int getCell(Long time, int r, int c) {
        int [] map = this.heatmap.get(time);
        return map[r * DIM + c];
    }

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

    public HeatMapScan2(List<Observation> raw, int threshold, int
            numObservations) {
        super(raw, threshold, numObservations);
    }

    @Override
    protected ObsTally init() {
        return new ObsTally();
    }

    @Override
    protected ObsTally prepare(Observation o) {
        return new ObsTally(o);
    }

    @Override
    protected ObsTally combine(ObsTally a, ObsTally b) {
        return ObsTally.combine(a, b);
    }

    @Override
    protected void accum(ObsTally tally, Observation o) {
        tally.accum(o);
    }
}