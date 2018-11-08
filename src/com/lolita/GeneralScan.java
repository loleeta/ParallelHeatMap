package com.lolita;

import java.util.ArrayList;
import java.util.List;

public class GeneralScan<ElemType, TallyType> {

    private static final int NUM_THREADS = 16;
    private int ROOT = 0;
    private List<ElemType> data;
    private List<TallyType> interior;
    private int n;
    private boolean reduced;
    private int height;

    public GeneralScan(List<ElemType> data) {
        this.data = data;
        this.n = data.size();
        reduced = false;
        this.height = (int) Math.ceil(Math.log(n)/Math.log(2));

        if (1 << height != n) {
            System.out.println("Not a power of 2.  Exiting");
            System.exit(1);
        }
        this.interior = new ArrayList<TallyType>(n-1);

        for (int i = 0; i < n-1; i++) {
            interior.add(init());
        }
    }

    protected TallyType init() {
        throw new IllegalArgumentException("Have to finish this part");
    }

    protected TallyType combine(TallyType left, TallyType right) {
        throw new IllegalArgumentException("Have to finish this part");
    }

    protected TallyType prepare(ElemType datum) {
        throw new IllegalArgumentException("Have to finish this part");
    }

    protected void accum(TallyType tally, ElemType datum) {
        throw new IllegalArgumentException("Have to finish this part");
    }

    TallyType getReduction() {
        reduced = reduced || reduce(ROOT);
        return value(ROOT);
    }


    boolean reduce(int i) {
        if (!isLeaf(i)) {
            if (i < NUM_THREADS-2) {
                reduce(left(i));
                reduce(right(i));
            } else {
                reduce(left(i));
                reduce(right(i));
            }

            TallyType tally = combine(value(left(i)), value(right(i)));
            interior.set(i,tally);
        }
        return true;
    }

    TallyType value(int i) {
        TallyType t = init();
        if (i < n-1) {
            return interior.get(i);
        } else {
            accum(t, data.get(i - (n-1)));
            return t;
        }
    }

    int size() {
        return (n-1) + n;
    }

    int parent(int i) {
        return (i-1)/2;
    }

    int left(int i) {
        return (i*2)+1;
    }

    int right(int i) {
        return (i*2) + 2;
    }

    boolean isLeaf(int i) {
        if(i >= n-1)
            return true;
        else
            return false;
    }
}
