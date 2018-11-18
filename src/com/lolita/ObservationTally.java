package com.lolita;

import java.util.List;

/*
class Tally {
    private static final int DIM = 20;
    private int[] cells;
    private Observation[] history;
    private int front, back;

    public Tally() {
        this.cells = new int[DIM * DIM];

    }

    private int place(double place) {
        return (int) ((place  + 1.0) / (2.0 / DIM));
    }

    private void incrementCell(int r, int c) {
        cells[r * DIM + c]++;
    }

    public int getCell(int r, int c) {
        return cells[r * DIM + c];
    }

    public static Tally combine(Tally a, Tally b) {
        Tally t = new Tally();
        for (int i = 0; i < t.cells.length; i++) {
            t.cells[i] = a.cells[i] + b.cells[i];
        }
        return t;
    }

    public void accum(Observation o) {
        incrementCell(place(o.x), place(o.y));
    }
}*/




class ObservationTally {
    public double x = 0.0, y = 0.0;

    public ObservationTally() {}

    public ObservationTally(Observation o) {
        this.x = o.x;
        this.y = o.y;
    }

    public ObservationTally(Double d1, Double d2) {
        this.x = d1;
        this.y = d2;
    }

    public String toString() {
        return "Tally - x: " + x + ", y: " + y;
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
        return new ObservationTally(left.x + right.x, left.y + right.y);
    }

    @Override
    protected void accum(ObservationTally tally, Observation o) {
        tally.x += o.x;
        tally.y += o.y;
    }
}