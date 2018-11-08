package com.lolita;

import java.util.ArrayList;
import java.lang.Math;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;


public class GeneralScan<ElemType, TallyType extends Tally<ElemType>> {
    public static final int NUM_THREADS = 16;

    private int ROOT = 0;
    private boolean reduced;
    private int n; //size of data, n-1 is size of interior
    private ArrayList<ElemType> data;
    private ArrayList<TallyType> interior;
    private TallyType tallyFactory;
    private int height;

    //public methods
    public GeneralScan(ArrayList<ElemType> data, TallyType factory) {
        this.reduced = false;
        this.n = data.size();
        this.data = data;
        this.height = (int)Math.ceil(Math.log(n)/Math.log(2));
        if (1<<height != n)
            throw new java.lang.RuntimeException("Data size must be pwoer of 2");
        this.interior = new ArrayList<TallyType>(n-1);
        for (int i = 0; i < (n-1); i++)
            interior.set(i, init());
        this.tallyFactory = factory;
    }

    public TallyType getReduction(Integer i) {
        //return getReduction(0)
        Integer param = i != null ? i : 0;
        this.reduced = reduced || reduce(ROOT);
        return value(param);
    }

    public void getScan(ArrayList<TallyType> output) {
        reduced = reduced || reduce(ROOT);
        scan(ROOT, init(), output);
    }

    //protected methods
    //throw an exception newTallyT
    protected TallyType init() {
        return (TallyType) tallyFactory.init();
    }

    protected TallyType prepare(ElemType datum) {
        return (TallyType) tallyFactory.prepare(datum);
    }

    protected TallyType combine(TallyType left, TallyType right) {
       left.combine(right);
       return left;
    }

    //private methods
    private int size() {
        return (n-1) + n;
    }

    private TallyType value(int i) {
        if (i < n-1)
            return interior.get(i);
        else
            return prepare(data.get(i-(n-1)));
    }

    private int parent(int i) {
        return (i-1)/2;
    }

    private int left(int i) {
        return i*2+1;
    }

    private int right(int i) {
        return left(i)+1;
    }

    private boolean isLeaf(int i) {
        return right(i) >= size();
    }

    private boolean reduce(int i) {
        if (!isLeaf(i)) {
            if (i < NUM_THREADS -2) {
                reduce(left(i));
                reduce(right(i));
            }
            interior.set(i, combine(value(left(i)), value(right(i))));
        }
        return true;
    }

    private void scan(int i, TallyType tallyPrior, ArrayList<TallyType> output) {
        if (isLeaf(i)) {
            output.set(i-(n-1), combine(tallyPrior, value(i)));
        } else {
            if (i < NUM_THREADS-2) {
                scan(left(i), tallyPrior, output);
                scan(right(i), combine(tallyPrior, value(left(i))), output);
            }
        }
    }

}
