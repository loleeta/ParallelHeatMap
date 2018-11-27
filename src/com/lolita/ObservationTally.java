package com.lolita;

import java.util.List;


/**
 * ObservationTally is a wrapper for an integer array that represents the
 * observations seen.
 * HW5
 */
class ObservationTally {
    private static final int DIM = 20; // Dimension of heat map
    private static final int IRRELEVANCE_THRESHOLD = 30; // How many to throw
    // away
    public int[] cells; // The array representing all the observations
    public Observation[] history; // Array implementation of queue
    public int front, back; // Pointers for queue

    /**
     * Constructor initializes variables to handle observations
     */
    public ObservationTally() {
        cells = new int[DIM * DIM];
        history = new Observation[IRRELEVANCE_THRESHOLD+1];
        front = 0;
        back = 0;
    }

    /**
     * Constructor which takes in a single observation
     * @param o An observation
     */
    public ObservationTally(Observation o) {
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
    private void incrementCell(int r, int c) {
        cells[r * DIM + c]++;
    }

    /**
     * Decrements the value for a cell in array
     * @param r Row of array
     * @param c Column of array
     */
    private void decrementCell(int r, int c) {
        cells[r * DIM + c]--;
    }

    /**
     * Given a row and column, return the value at that index
     * @param r Row of array
     * @param c Column of array
     * @return
     */
    public int getCell(int r, int c) {
        return cells[r * DIM + c];
    }

    /**
     * Given two ObservationTally, combine their values together and return
     * a new ObservationTally
     * @param a One ObservationTally
     * @param b Another ObservationTally
     * @return a new ObservationTally
     */
    public static ObservationTally combine(ObservationTally a, ObservationTally b) {
        ObservationTally t = new ObservationTally();
        for (int i = 0; i < t.cells.length; i++) {
            t.cells[i] = a.cells[i] + b.cells[i];
        }
        return t;
    }

    /**
     * Given an observation, increment its value in the tally. If the queue
     * is full, dequeue and decrement the returned item.
     * @param o An Observation
     */
    public void accum(Observation o) {
        incrementCell(place(o.x), place(o.y));
        if (fullQueue()) {
            Observation oldObs = dequeue();
            decrementCell(place(oldObs.x), place(oldObs.y));
        }
        enqueue(o);
    }

    /**
     * Place an Observation into the queue and increment the pointer.
     * @param o An Observation
     */
    private void enqueue(Observation o) {
        if(!fullQueue()) {
            history[back] = o;
            back = nextQueue(back);
        }
    }

    /**
     * Remove an item from the queue and return it.
     * @return An Observation
     */
    private Observation dequeue() {
        if (!emptyQueue()) {
            Observation o = history[front];
            front = nextQueue(front);
            return o;
        }
        return null;
    }

    /**
     * Return whether the queue is full.
     * @return True if full, False otherwise
     */
    private boolean fullQueue() {
        return nextQueue(back) == front;
    }

    /**
     * Return whether the queue is empty.
     * @return True if empty, False otherwise
     */
    private boolean emptyQueue() {
        return front == back;
    }

    /**
     * Returns the next index as a wrap-around from the end of the queue
     * @param i Index in the array
     * @return
     */
    public static int nextQueue(int i) {
        return (i+1)% (IRRELEVANCE_THRESHOLD+1);
    }

    /**
     * Returns String representation of the object.
     * @return a String
     */
    public String toString() {
        return Integer.toString(front) + ":" + Integer.toString(back);
    }
}

/**
 * Subclass of GeneralScan which does a reduce/scan on Observations.
 */
class HeatMapScan extends GeneralScan<Observation, ObservationTally> {
    /**
     * Constructor.
     * @param raw A List of data
     * @param threshold
     */
    public HeatMapScan(List<Observation> raw, int threshold) {
        super(raw, threshold);
    }

    /**
     * Creates and returns an empty ObservationTally.
     * @return An ObservationTally object
     */
    @Override
    protected ObservationTally init() {
        return new ObservationTally();
    }

    /**
     * Creates and returns an ObservationTally based on Observation passed in.
     * @param o An Observation
     * @return An ObservationTally object
     */
    @Override
    protected ObservationTally prepare(Observation o) {
        return new ObservationTally(o);
    }

    /**
     * Given two ObservationTallys, combine them into a new ObservationTally.
     * @param left An ObservationTally
     * @param right Another ObservationTally
     * @return A new ObservationTally
     */
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

    /** Given an Observation, add its data into an ObservationTally
     * @param tally An ObservationTally
     * @param o An Observation
     */
    @Override
    protected void accum(ObservationTally tally, Observation o) {
        tally.accum(o);
    }
}