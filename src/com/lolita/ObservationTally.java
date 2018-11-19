package com.lolita;

import java.util.List;


class ObservationTally {
    private static final int DIM = 20;
    private static final int IRRELEVANCE_THRESHOLD = 30;
    public int[] cells;
    public Observation[] history;
    public int front, back;

    public ObservationTally() {
        cells = new int[DIM * DIM];
        history = new Observation[IRRELEVANCE_THRESHOLD+1];
        front = 0;
        back = 0;
    }

    public ObservationTally(Observation o) {
        this();
        accum(o);
    }

    //translates double into the index of the array
    private int place(double place) {
        return (int) ((place  + 1.0) / (2.0 / DIM));
    }

    private void incrementCell(int r, int c) {
        cells[r * DIM + c]++;
    }

    private void decrementCell(int r, int c) {
        cells[r * DIM + c]--;
    }

    public int getCell(int r, int c) {
        return cells[r * DIM + c];
    }

    public static ObservationTally combine(ObservationTally a, ObservationTally b) {
        ObservationTally t = new ObservationTally();
        for (int i = 0; i < t.cells.length; i++) {
            t.cells[i] = a.cells[i] + b.cells[i];
        }
        return t;
    }

    public void accum(Observation o) {
        if (o != null) {
            incrementCell(place(o.x), place(o.y));
            if (fullQueue()) {
                Observation oldObs = dequeue();
                if (oldObs != null)
                    decrementCell(place(oldObs.x), place(oldObs.y));
            }
            enqueue(o);
        }
    }

    private void enqueue(Observation o) {
        if(!fullQueue()) {
            history[back] = o;
            back = nextQueue(back);
        }
    }

    private Observation dequeue() {
        if (!emptyQueue()) {
            Observation o = history[front];
            front = nextQueue(front);
            return o;
        }
        return null;
    }

    private boolean fullQueue() {
        return nextQueue(back) == front;
    }

    private boolean emptyQueue() {
        return front == back;
    }

    public static int nextQueue(int i) {
        return (i+1)% (IRRELEVANCE_THRESHOLD+1);
    }

    public String toString() {
        return Integer.toString(front) + ":" + Integer.toString(back);
     }
}

class HeatMapScan extends GeneralScan<Observation, ObservationTally> {
    public HeatMapScan(List<Observation> raw, int threshold) {
        super(raw, threshold);
    }

    @Override
    protected ObservationTally init() {
        return new ObservationTally();
    }

    @Override
    protected ObservationTally prepare(Observation o) {
        return new ObservationTally(o);
    }

    @Override
    protected ObservationTally combine(ObservationTally left, ObservationTally right) {
        ObservationTally tally = new ObservationTally();
        for (int leftFront = left.front; leftFront != left.back; leftFront =
                left.nextQueue(leftFront)) {
            tally.accum(left.history[leftFront]);
        }
        for (int rightFront = right.front; rightFront != right.back;
             rightFront = right.nextQueue(rightFront)) {
            tally.accum(right.history[rightFront]);
        }
        return tally;
    }

    @Override
    protected void accum(ObservationTally tally, Observation o) {
        tally.accum(o);
    }
}