package com.lolita;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class GeneralScan<ElemType, TallyType> {
    private static int NUM_THREADS = 16;
    public static final int DEFAULT_THRESHOLD = 4;
    public static final int ROOT = 0;
    private List<ElemType> data;
    private List<TallyType> interior;
    private final ForkJoinPool pool;

    private int n;
    private boolean reduced;
    private int firstData;
    private int threshold;

    public GeneralScan(List<ElemType> data) {
        this(data, NUM_THREADS);
    }

    public GeneralScan(List<ElemType> data, int threadThreshold) {
        this.reduced = false;
        this.data = data;
        this.n = data.size();
        int height = (int)Math.ceil(Math.log(n)/Math.log(2));
        if (1 << height != n)
            throw new java.lang.RuntimeException("Data size must be power of 2");

        this.pool = new ForkJoinPool();
        this.firstData = (1 << height) - 1;
        this.threshold = threadThreshold;

        //System.out.println("threadThreshold is " + threadThreshold);
        System.out.println("firstData is " + firstData);
        //int m = 4 * (1 + firstData/threshold);
        int m = firstData/2;
        System.out.println("m is " + m);
        this.interior = new ArrayList<>(m);

        for (int i = 0; i < m; i++)
            interior.add(init());

    }

    public TallyType getReduction() {
        if (!reduced) {
            pool.invoke(new ComputeReduction(ROOT));
            reduced = true;
        }
        return value(ROOT);
    }

    public List<TallyType> getScan() {
        if (!reduced)
            getReduction();
        ArrayList<TallyType> output = new ArrayList<>(n);
        for (int i = 0; i < data.size(); i++)
            output.add(init());
        pool.invoke(new ComputeScan(ROOT, init(), output));
        return output;
    }

    protected TallyType init() {
        throw new IllegalArgumentException("Should be overridden in child");
    }

    protected TallyType combine(TallyType left, TallyType right) {
        throw new IllegalArgumentException("Should be overridden in child");
    }

    protected TallyType prepare(ElemType datum) {
        throw new IllegalArgumentException("Should be overridden in child");
    }

    protected void accum(TallyType tally, ElemType datum) {
        throw new IllegalArgumentException("Should be overridden in child");
    }

    private TallyType value(int i) {
        TallyType t = init();
        if (i < n-1) {
            return interior.get(i);
        } else {
            accum(t, data.get(i - (n-1)));
            return t;
        }
    }

    private int size() { return (n-1) + n; }

    private int parent(int i) { return (i-1)/2; }

    private int left(int i) { return (i*2)+1; }

    private int right(int i) { return (i*2) + 2; }

    private boolean isLeaf(int i) {
        if(i >= n-1)
            return true;
        else
            return false;
    }

    private boolean hasRight(int i) {
        return right(i) < size();
    }

    //returns index of the first piece of data in (n-1)+n array where data is in n
    private int firstData(int i) {
        if (isLeaf(i))
            return i < firstData ? -1 : i;
        return firstData(left(i));
    }

    //returns index of last piece of data in (n-1)+n arra where data is in n
    private int lastData(int i) {
        if (isLeaf(i)) {
            //if (i < firstData)
                //System.out.println("lastData(" + i + ") returning leaf: " + "-1");
            //else
                //System.out.println("lastData(" + i + ") returning non-leaf: " + i);
            return i < firstData ? -1 : i;
        }
        if (hasRight(i)) {
            int right = lastData(right(i));
            if (right != -1)
                return right;
        }
        return lastData(left(i));
    }

    private int dataCount(int i) {
        System.out.println("In dataCount(" + i + "): " + "lastData is " + lastData(i) + " and firstData is " + firstData(i));
        return lastData(i) - firstData(i);
    }

    //what is the total sum of the collection
    private void reduce(int i) {
        int first = firstData(i), last = lastData(i);
        System.out.println("In REDUCE(" + i + "): " + "first is: " + first + " last is: " + last);
        TallyType tally = init();
        if (first != -1)
            for (int j = first; j <= last; j++)
                accum(tally, data.get(i));
        interior.set(i, tally);
    }

    //for each item, what is the sum of every item seen so far
    private void scan(int i, TallyType tallyPrior, List<TallyType> output) {
        if (isLeaf(i)) {
            output.set(i - (n-1),  combine(tallyPrior, value(i)));
        } else {
            scan(left(i), tallyPrior, output);
            scan(right(i), combine(tallyPrior, value(left(i))), output);
        }
    }

    //Subclass to use thread pool to reduce
    class ComputeReduction extends RecursiveAction {
        private int i;

        public ComputeReduction(int i) { this.i = i; }

        public void compute() {
            if (!isLeaf(i)) {
                //System.out.println("dataCount(" + i + "): " + dataCount(i));
                if (dataCount(i) > threshold) {
                    //System.out.println("ComputeReduce: calling invokeAll on notLeaf(" + i + ") left: " + left(i) + " right: " + right(i));
                    invokeAll(new ComputeReduction(left(i)), new ComputeReduction(right(i)));
                    interior.set(i, combine(value(left(i)), value(right(i)))); //set value from left and right child
                }
                else
                    reduce(i); //accum leaves for this node and set its value in the interior node
            }
        }
    }

    //Subclass to use thread pool to scan
    class ComputeScan extends RecursiveAction {
        private int i;
        private TallyType tallyPrior;
        private List<TallyType> output;

        public ComputeScan(int i, TallyType tallyPrior, List<TallyType> output) {
            this.i = i;
            this.tallyPrior = tallyPrior;
            this.output = output;
        }

        public void compute() {
            if (isLeaf(i)) {
                System.out.println("Setting output(" + i + ")");
                output.set(i - (n - 1), combine(tallyPrior, value(i)));
            }
            else {
                if (dataCount(i) >= threshold) {
                    System.out.println("datacount(" + i + "):" + dataCount(i) + " is less than threshold: " + threshold);
                    invokeAll(new ComputeScan(left(i), tallyPrior, output),
                            new ComputeScan(right(i), combine(tallyPrior, value(left(i))), output));
                }
                else
                    scan(i, tallyPrior, output);
            }
        }
    }
}
