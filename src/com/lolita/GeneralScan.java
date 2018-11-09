package com.lolita;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class GeneralScan<ElemType, TallyType> {
    private static int NUM_THREADS = 16;
    private int ROOT = 0;
    private List<ElemType> data;
    private List<TallyType> interior;
    private int n;
    private boolean reduced;
    private int height;
    private final ForkJoinPool pool;
    private int first;

    public GeneralScan(List<ElemType> data) {
        this(data, NUM_THREADS);
    }

    public GeneralScan(List<ElemType> data, int threads) {
        this.reduced = false;
        this.data = data;
        this.n = data.size();
        this.height = (int) Math.ceil(Math.log(n)/Math.log(2));
        this.NUM_THREADS = threads;

        if (1 << height != n)
            throw new java.lang.RuntimeException("Data size must be power of 2");

        this.pool = new ForkJoinPool();
        this.first = (1 << height) - 1;

        int m = 4 * (1 + first/threads);
        this.interior = new ArrayList<TallyType>(m);

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
        //scan(ROOT, init(), output);
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

    private int firstData(int i) {
        if (isLeaf(i))
            return i < first ? -1 : i;
        return firstData(left(i));
    }

    private int lastData(int i) {
        if (isLeaf(i)) {
            if (i < first)
                System.out.println("lastData(" + i + ") returning leaf: " + "-1");
            else
                System.out.println("lastData(" + i + ") returning non-leaf: " + i);
            return i < first ? -1 : i;
        }
        if (hasRight(i)) {
            int right = lastData(right(i));
            if (right != -1) {
                System.out.println("lastData(" + i + ") returning right: " + right);
                return right;
            }
        }
        System.out.println("lastData(" + i + ") returning left: " + i);
        return lastData(left(i));
    }

    private int dataCount(int i) {
        return lastData(i) - firstData(i);
    }

    //what is the total sum of the collection
    private boolean reduce(int i) {
        if (!isLeaf(i)) {
            reduce(left(i));
            reduce(right(i));
            interior.set(i, combine(value(left(i)), value(right(i))));
        }
        return true;
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

        public ComputeReduction(int i) {
            this.i = i;
        }
        public void compute() {
            if (!isLeaf(i)) {
                if (i < NUM_THREADS-2)
                    invokeAll(new ComputeReduction(left(i)), new ComputeReduction(right(i)));
                else
                    reduce(i);
                interior.set(i, combine(value(left(i)), value(right(i))));
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
            System.out.println("ComputeScan is being called");
        }

        public void compute() {
            if (isLeaf(i))
                output.set(i - (n-1), combine(tallyPrior, value(i)));
            else {
                if (i < NUM_THREADS-2)
                    invokeAll(new ComputeScan(left(i), tallyPrior, output),
                            new ComputeScan(right(i), combine(tallyPrior, value(left(i))), output));
                else {
                    scan(i, tallyPrior, output);
                }
            }
        }
    }
}
