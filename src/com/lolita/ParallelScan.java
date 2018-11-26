package com.lolita;

import java.util.Arrays;
import java.util.Map;

/**
 *
 */
class ParallelScan implements Runnable {
    public Long time;
    Map<Long, int[]> heatmap;
    public int [] cells;
    public int decay;
    public Map<Long, int[]> finalHeatMap;

    public ParallelScan(ObsTally tally, Long time, Map<Long, int[]>
            finalHeatMap) {
        this.time = time;
        this.cells = tally.heatmap.get(this.time);
        this.heatmap = tally.heatmap;
        this.decay = 3;
        this.finalHeatMap = finalHeatMap;
    }

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
        finalHeatMap.put(time, newCells);
    }

    private int[] copyObservations(int[] src) {
        int [] dest = new int[src.length];
        System.arraycopy(src, 0, dest, 0, src.length);
        return dest;
    }
}


