package com.lolita;

import java.util.Arrays;
import java.util.Map;

/**
 * HW6: Threaded task to accumulate time decay for heatmap.
 */
class ParallelScan implements Runnable {
    public Long time;   // Timestamp
    Map<Long, int[]> heatmap;   // Map of k,v = Time, Observations
    public int [] cells;    // Array of Observations
    public int decay;   // How far back the timestamps to look at
    public Map<Long, int[]> finalHeatMap;   // Map of final results

    /**
     * Constructor for this Task.
     * @param tally Tally from reduction
     * @param time  Timestamp of this task
     * @param finalHeatMap  HashMap to store results
     */
    public ParallelScan(ObsTally tally, Long time, Map<Long, int[]>
            finalHeatMap) {
        this.time = time;
        this.cells = tally.heatmap.get(this.time);
        this.heatmap = tally.heatmap;
        this.decay = 3;
        this.finalHeatMap = finalHeatMap;
    }

    /**
     * Per time stamp, takes the array of observations, and incorporates
     * previous time stamps by using a decay (3 s).
     */
    @Override
    public void run() {
        //first copy into a new array that will have the decay
        int[] newCells = copyObservations(this.cells);
        for (int i = 0; i < newCells.length; i++) {
            newCells[i] = newCells[i] == 1? 4 : 0;
        }

        if (time == 1L) {
            //do nothing
        }
        else if (time == 2L) {
            //add decay for cells of time-1
            int[] cellsBackOne = copyObservations(this.heatmap.get(time-1));
            for (int i = 0; i < newCells.length; i++) {
                newCells[i] += cellsBackOne[i] == 1? 3:0;
            }
        }
        else if (time == 3L) {
            //add decay for cells of time-1, time-2, time-3
            int[] cellsBackOne = copyObservations(this.heatmap.get(time - 1));
            int[] cellsBackTwo = copyObservations(this.heatmap.get(time - 2));
            for (int i = 0; i < newCells.length; i++) {
                newCells[i] += cellsBackOne[i] == 1? 3:0;
                newCells[i] += cellsBackTwo[i] == 1? 2:0;
            }
        }
        else {
            //add decay for cells of time-1, time-2, time-3
            int[] cellsBackOne = copyObservations(this.heatmap.get(time - 1));
            int[] cellsBackTwo = copyObservations(this.heatmap.get(time - 2));
            int[] cellsBackThree = copyObservations(this.heatmap.get(time - 3));
            for (int i = 0; i < newCells.length; i++) {
                newCells[i] += cellsBackOne[i] == 1? 3:0;
                newCells[i] += cellsBackTwo[i] == 1? 2:0;
                newCells[i] += cellsBackThree[i] == 1? 1:0;
            }
        }

        finalHeatMap.put(time, newCells); //output decayed cells
    }

    /** Deep copy of array.
     * @param src
     * @return
     */
    private int[] copyObservations(int[] src) {
        int [] dest = new int[src.length];
        System.arraycopy(src, 0, dest, 0, src.length);
        return dest;
    }
}


