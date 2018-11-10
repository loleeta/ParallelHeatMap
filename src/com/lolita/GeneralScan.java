package com.lolita;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * GeneralScan class is a class for a generalized reduce/scan operation on a
 * list of values. The element type and list it works on is passed in.
 * @param <ElemType>
 * @param <TallyType>
 */
public class GeneralScan<ElemType, TallyType> {
    private static int NUM_THREADS = 16;            //number of threads allowed
    public static final int ROOT = 0;               //the root of the "tree"
    private List<ElemType> data;                    //list of data
    private List<TallyType> interior;               //intermediary nodes
    private final ForkJoinPool pool;                //thread service

    private int size;                               //number of items
    private boolean reduced;                        //whether list has reduction
    private int firstData;                          //index of first data
    private int threshold;                          //work of a thread

    /**
     * Constructor for GeneralScan
     * @param data                  list of data
     */
    public GeneralScan(List<ElemType> data) {
        this(data, NUM_THREADS);
    }

    /**
     * Constructor for GeneralScan
     * @param data                  list of data
     * @param threadThreshold       work of a thread
     */
    public GeneralScan(List<ElemType> data, int threadThreshold) {
        this.reduced = false;
        this.data = data;
        this.size = data.size();
        int height = (int)Math.ceil(Math.log(size)/Math.log(2));
        if (1 << height != size)
            throw new java.lang.RuntimeException("Data size must be power of 2");

        this.pool = new ForkJoinPool();
        this.firstData = (1 << height) - 1;
        this.threshold = threadThreshold;

        //System.out.println("threadThreshold is " + threadThreshold);
        //System.out.println("firstData is " + firstData);
        int m = firstData/2; //only uses half the interior nodes
        //System.out.println("m is " + m);
        this.interior = new ArrayList<>(m);

        for (int i = 0; i < m; i++)
            interior.add(init());

    }

    /**
     * Creates a thread pool to invoke a reduction of list which will result in
     * a TallyType object that is the reduction at the root
     * @return
     */
    public TallyType getReduction() {
        if (!reduced) {
            pool.invoke(new ComputeReduction(ROOT));
            reduced = true;
        }
        return value(ROOT);
    }

    /**
     * Creates thread pool to invoke scan of list which will result in
     * an array of values that every item has seen prior to itself
     * @return a list of TallyType objects
     */
    public List<TallyType> getScan() {
        if (!reduced)
            getReduction();
        ArrayList<TallyType> output = new ArrayList<>(size);
        for (int i = 0; i < data.size(); i++)
            output.add(init());
        pool.invoke(new ComputeScan(ROOT, init(), output));
        return output;
    }

    /**
     * Method for creating a blank TallyType object to be overridden in
     * child class
     * @return a new TallyType object
     */
    protected TallyType init() {
        throw new IllegalArgumentException("Should be overridden in child");
    }

    /**
     * Method for combining values to be overridden in child class
     * @param left
     * @param right
     * @return a TallyType object
     */
    protected TallyType combine(TallyType left, TallyType right) {
        throw new IllegalArgumentException("Should be overridden in child");
    }

    /**
     * Method for creating a new TallyType object to be overridden in
     * child class
     * @param datum
     * @return a new TallyType object
     */
    protected TallyType prepare(ElemType datum) {
        throw new IllegalArgumentException("Should be overridden in child");
    }

    /**
     * Method for accumulating values to be overridden in child class
     * @param tally
     * @param datum
     */
    protected void accum(TallyType tally, ElemType datum) {
        throw new IllegalArgumentException("Should be overridden in child");
    }

    /**
     * Creates a TallyType object with the given value
     * @param i                     the current node
     * @return a TallyType object
     */
    private TallyType value(int i) {
        TallyType t = init();
        if (i < size-1) {
            return interior.get(i);
        } else {
            accum(t, data.get(i - (size-1)));
            return t;
        }
    }

    /**
     * Finds the size of the entire array where data is n and interior is n-1
     * @return an integer representing size
     */
    private int size() { return (size-1) + size; }

    /**
     * Finds the parent of the given index
     * @param i                     the current node
     * @return an integer that is its parent node
     */
    private int parent(int i) { return (i-1)/2; }

    /**
     * Finds the left child of the given index
     * @param i                     the current node
     * @return an integer that is its left child
     */
    private int left(int i) { return (i*2)+1; }

    /**
     * Finds the right child of the given index
     * @param i                     the current node
     * @return an integer that is its right child
     */
    private int right(int i) { return (i*2) + 2; }

    /**
     * Checks whether a given index is a leaf node
     * @param i                     the current node
     * @return true if it is a leaf, false otherwise
     */
    private boolean isLeaf(int i) {
        if(i >= size-1)
            return true;
        else
            return false;
    }

    /**
     * Checks whether node has a right child
     * @param i                     the current node
     * @return true if it has a right child, false otherwise
     */
    private boolean hasRight(int i) {
        return right(i) < size();
    }

    /**
     * Finds the index of the first data item in (n-1)+n array where data is n
     * @param i                     the current interior node
     * @return an intenger representing the index of the first leaf it has
     */
    private int firstData(int i) {
        if (isLeaf(i))
            return i < firstData ? -1 : i;
        return firstData(left(i));
    }

    /**
     * Finds the index of the last data item in (n-1)+n array where data is n
     * @param i                     the current interior node
     * @return an intenger representing the index of the last leaf it has
     */
    private int lastData(int i) {
        if (isLeaf(i)) {
            return i < firstData ? -1 : i;
        }
        if (hasRight(i)) {
            int right = lastData(right(i));
            if (right != -1)
                return right;
        }
        return lastData(left(i));
    }

    /**
     * Finds the number of leaf values that this node has access to
     * @param i                     the current interior node
     * @return an integer representing number of leaves it has
     */
    private int dataCount(int i) {
        //System.out.println("In dataCount(" + i + "): " + "lastData is " +
        //              lastData(i) + " and firstData is " + firstData(i));
        return lastData(i) - firstData(i);
    }

    /**
     * Finds the total sum value of the collection so far
     * @param i                     the current node
     */
    private void reduce(int i) {
        int first = firstData(i), last = lastData(i);
        //System.out.println("In REDUCE(" + i + "): " + "first is: " +
        //                      first + " last is: " + last);
        TallyType tally = init();
        if (first != -1)
            for (int j = first; j <= last; j++)
                accum(tally, data.get(i));
        interior.set(i, tally);
    }

    /**
     * Finds, for each item, what is the sum of every item so far.
     * @param i                     current item
     * @param tallyPrior            value from item to the left
     * @param output                list to write scan value to
     */
    private void scan(int i, TallyType tallyPrior, List<TallyType> output) {
        int first = firstData(i), last = lastData(i);
        //System.out.println("In SCAN(" + i + "): " + "first is: " +
        //                      first + " last is: " + last);
        if (first != -1)
            for (int j = first; j <= last; j++) {
                tallyPrior = combine(tallyPrior, value(j));
                output.set(j - firstData, tallyPrior);
            }
    }

    /**
     * Subclass that uses thread pool to thread reducing of list of Tallys
     */
    class ComputeReduction extends RecursiveAction {
        private int i;                      //current index of the array

        /**
         * Constructor for the class
         * @param i                         current index
         */
        public ComputeReduction(int i) { this.i = i; }

        /**
         * Recursively computes the sum value for each item in the array by
         * forking and using tight loops to accumulate data into a Tally that
         * is stored and sent up to parent.
         */
        public void compute() {
            if (!isLeaf(i)) {
                //System.out.println("dataCount(" + i + "): " + dataCount(i));
                if (dataCount(i) > threshold) { //if amount of work in this leaf is > threshold, fork
                    //System.out.println("ComputeReduce: calling invokeAll on " + i + " left: "
                    //                      + left(i) + " right: " + right(i));
                    invokeAll(new ComputeReduction(left(i)), new ComputeReduction(right(i)));
                    interior.set(i, combine(value(left(i)), value(right(i)))); //set value from left and right child
                }
                else
                    reduce(i); //accum leaves for this node and set its value in the interior node
            }
        }
    }

    /**
     * Subclass that uses thread pool to thread scanning of list of Tallys
     */
    class ComputeScan extends RecursiveAction {
        private int i;                      //current index of array
        private TallyType tallyPrior;       //the Tally before this node
        private List<TallyType> output;     //array of all Tally values

        /**
         * Constructor for ComputeScan
         * @param i                         the current index/node
         * @param tallyPrior                the previous Tally
         * @param output                    array of Scan values so far
         */
        public ComputeScan(int i, TallyType tallyPrior, List<TallyType> output) {
            this.i = i;
            this.tallyPrior = tallyPrior;
            this.output = output;
        }

        /**
         * Recursively calculates the scan value for each item in the array by
         * forking and using tight loops to do scan.
         *
         */
        public void compute() {
            if (isLeaf(i)) {
                System.out.println("Setting output(" + i + ")");
                output.set(i - (size-1), combine(tallyPrior, value(i)));
            }
            else {
                if (dataCount(i) > threshold) { //if amount of leaves > threshold, fork
                    //System.out.println("datacount(" + i + "):" + dataCount(i) +
                    //                      " is less than threshold: " + threshold);
                    invokeAll(new ComputeScan(left(i), tallyPrior, output),
                            new ComputeScan(right(i), combine(tallyPrior, value(left(i))), output));
                }
                else
                    scan(i, tallyPrior, output);
            }
        }
    }
}
