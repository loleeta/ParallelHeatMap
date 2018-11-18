package com.lolita;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * GeneralScan class is a class for a generalized reduce/scan operation on a
 * list of values. The element type and list it works on is passed in.
 *
 * @param <ElemType>
 * @param <TallyType>
 */
public abstract class GeneralScan<ElemType, TallyType> {
    public static final int ROOT_NODE_INDEX = 0; // the root of the "tree"
    private List<ElemType> dataElements; // list of dataElements
    private List<TallyType> interiorTallyNodes; // intermediary nodes
    private final ForkJoinPool pool; // thread service

    private int dataSize; // number of items
    private boolean isReduced;
    private int indexFirstData;
    private int threadForkThreshold; //work of a thread

    /**
     * Constructor for GeneralScan
     *
     * @param dataElements    list of dataElements
     * @param threadThreshold work of a thread
     */
    public GeneralScan(List<ElemType> dataElements, int threadThreshold) {
        this.isReduced = false;
        this.dataElements = dataElements;
        this.dataSize = dataElements.size();
        int height = (int) Math.ceil(Math.log(dataSize) / Math.log(2));
        //if (1 << height != dataSize)
        //   throw new java.lang.RuntimeException("Data dataSize must be " +
        //        "power of 2");

        this.pool = new ForkJoinPool();
        this.indexFirstData = (1 << height) - 1;
        this.threadForkThreshold = threadThreshold;

        //System.out.println("threadThreshold is " + threadThreshold);
        //System.out.println("indexFirstData is " + indexFirstData);
        int m = indexFirstData / 2; //only uses half the interiorTallyNodes nodes
        //System.out.println("m is " + m);
        this.interiorTallyNodes = new ArrayList<>(m);

        for (int i = 0; i < m; i++)
            interiorTallyNodes.add(init());

    }

    /**
     * Creates a thread pool to invoke a reduction of list which will result in
     * a TallyType object that is the reduction at the root
     *
     * @return
     */
    public TallyType getReduction() {
        if (!isReduced) {
            pool.invoke(new ComputeReduction(ROOT_NODE_INDEX));
            isReduced = true;
        }
        return getNodeTally(ROOT_NODE_INDEX);
    }

    /**
     * Creates thread pool to invoke scan of list which will result in
     * an array of values that every item has seen prior to itself
     *
     * @return a list of TallyType objects
     */
    public List<TallyType> getScan() {
        if (!isReduced)
            getReduction();
        ArrayList<TallyType> output = new ArrayList<>(dataSize);
        for (int i = 0; i < dataElements.size(); i++)
            output.add(init());
        pool.invoke(new ComputeScan(ROOT_NODE_INDEX, init(), output));
        return output;
    }

    /**
     * Method for creating a blank TallyType object to be overridden in
     * child class
     *
     * @return a new TallyType object
     */
    protected abstract TallyType init();

    /**
     * Method for combining values to be overridden in child class
     *
     * @param left
     * @param right
     * @return a TallyType object
     */
    protected abstract TallyType combine(TallyType left, TallyType right);

    /**
     * Method for creating a new TallyType object to be overridden in
     * child class
     *
     * @param datum
     * @return a new TallyType object
     */
    protected abstract TallyType prepare(ElemType datum);

    /**
     * Method for accumulating values to be overridden in child class
     *
     * @param tally
     * @param datum
     */
    protected abstract void accum(TallyType tally, ElemType datum);

    /**
     * Creates a TallyType object with the given getNodeTally
     *
     * @param i the current node
     * @return a TallyType object
     */
    private TallyType getNodeTally(int i) {
        TallyType t = init();
        if (i < dataSize - 1) {
            return interiorTallyNodes.get(i);
        } else {
            accum(t, dataElements.get(i - (dataSize - 1)));
            return t;
        }
    }

    /**
     * Finds the dataSize of the entire array where dataElements is n and
     * interiorTallyNodes is n-1
     *
     * @return an integer representing dataSize
     */
    private int size() {
        return (dataSize - 1) + dataSize;
    }

    /**
     * Finds the left child of the given index
     *
     * @param i the current node
     * @return an integer that is its left child
     */
    private int left(int i) {
        return (i * 2) + 1;
    }

    /**
     * Finds the right child of the given index
     *
     * @param i the current node
     * @return an integer that is its right child
     */
    private int right(int i) {
        return (i * 2) + 2;
    }

    /**
     * Checks whether a given index is a leaf node
     *
     * @param i the current node
     * @return true if it is a leaf, false otherwise
     */
    private boolean isLeaf(int i) {
        return i >= dataSize - 1;
    }

    /**
     * Checks whether node has a right child
     *
     * @param i the current node
     * @return true if it has a right child, false otherwise
     */
    private boolean hasRight(int i) {
        return right(i) < size();
    }

    /**
     * Finds the index of the first dataElements item in (n-1)+n array where dataElements is n
     *
     * @param i the current interiorTallyNodes node
     * @return an intenger representing the index of the first leaf it has
     */
    private int firstData(int i) {
        if (isLeaf(i))
            return i < indexFirstData ? -1 : i;
        return firstData(left(i));
    }

    /**
     * Finds the index of the last dataElements item in (n-1)+n array where dataElements is n
     *
     * @param i the current interiorTallyNodes node
     * @return an intenger representing the index of the last leaf it has
     */
    private int lastData(int i) {
        if (isLeaf(i)) {
            return i < indexFirstData ? -1 : i;
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
     *
     * @param i the current interiorTallyNodes node
     * @return an integer representing number of leaves it has
     */
    private int dataCount(int i) {
        //System.out.println("In dataCount(" + i + "): " + "lastData is " +
        //        lastData(i) + " and indexFirstData is " + indexFirstData(i));
        return lastData(i) - firstData(i);
    }

    /**
     * Finds the total sum value of the collection so far
     *
     * @param i the current node
     */
    private void reduce(int i) {
        int first = firstData(i), last = lastData(i);
        //System.out.println("In REDUCE(" + i + "): " + "first is: " +
        //                      first + " last is: " + last);
        TallyType tally = init();
        if (first != -1)
            for (int j = first; j <= last; j++)
                accum(tally, dataElements.get(i));
        interiorTallyNodes.set(i, tally);
    }

    /**
     * Finds, for each item, what is the sum of every item so far.
     *
     * @param i          current item
     * @param tallyPrior getNodeTally from item to the left
     * @param output     list to write scan getNodeTally to
     */
    private void scan(int i, TallyType tallyPrior, List<TallyType> output) {
        int first = firstData(i), last = lastData(i);
        //System.out.println("In SCAN(" + i + "): " + "first is: " +
        //                      first + " last is: " + last);
        if (first != -1)
            for (int j = first; j <= last; j++) {
                tallyPrior = combine(tallyPrior, getNodeTally(j));
                output.set(j - indexFirstData, tallyPrior);
            }
    }

    /**
     * Subclass that uses thread pool to thread reducing of list of Tallys
     */
    class ComputeReduction extends RecursiveAction {
        private int i; //current index of the array

        /**
         * Constructor for the class
         *
         * @param i current index
         */
        public ComputeReduction(int i) {
            this.i = i;
        }

        /**
         * Recursively computes the sum getNodeTally for each item in the array by
         * forking and using tight loops to accumulate dataElements into a Tally that
         * is stored and sent up to parent.
         */
        public void compute() {
            if (!isLeaf(i)) {
                //System.out.println("dataCount(" + i + "): " + dataCount(i));
                if (dataCount(i) > threadForkThreshold) { //if amount of work in this leaf is > threadForkThreshold, fo
                    //System.out.println("ComputeReduce: calling invokeAll on " + i + " left: "
                    //                      + left(i) + " right: " + right(i));
                    invokeAll(new ComputeReduction(left(i)), new ComputeReduction(right(i)));
                    interiorTallyNodes.set(i, combine(getNodeTally(left(i)), getNodeTally(right(i)))); //set getNodeTally from left and right child
                } else
                    reduce(i); //accum leaves for this node and set its getNodeTally in the interiorTallyNodes node
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
         *
         * @param i          the current index/node
         * @param tallyPrior the previous Tally
         * @param output     array of Scan values so far
         */
        public ComputeScan(int i, TallyType tallyPrior, List<TallyType> output) {
            this.i = i;
            this.tallyPrior = tallyPrior;
            this.output = output;
        }

        /**
         * Recursively calculates the scan getNodeTally for each item in the array by
         * forking and using tight loops to do scan.
         */
        public void compute() {
            if (isLeaf(i)) {
                //System.out.println("Setting output(" + i + ")");
                output.set(i - (dataSize - 1), combine(tallyPrior, getNodeTally(i)));
            } else {
                if (dataCount(i) > threadForkThreshold) { //if amount of leaves > threadForkThreshold, fork
                    //System.out.println("datacount(" + i + "):" + dataCount(i) +
                    //                      " is less than threadForkThreshold: " + threadForkThreshold);
                    invokeAll(new ComputeScan(left(i), tallyPrior, output),
                              new ComputeScan(right(i), combine(tallyPrior,
                                     getNodeTally(left(i))), output));
                } else {
                    scan(i, tallyPrior, output);
                }
            }
        }
    }
}
