package com.lolita;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Reduce class based loosely on Fig. 5.4 in Snyder/Lin Principles of Parallel
 * Programming.
 *
 * This does a Shwarz-like reduction. It does as much as possible in a tight
 * loop within each thread before joining the results in the inter-thread tree
 * cap. The tree cap itself is implemented in a single-level array corresponding
 * to its lowest level (just above the tight-loop work of each thread). Then the
 * combining at each level abandons every other active array element from the
 * level below. Since we are moving up from the leaves to the root, the previous
 * levels can be abandoned once the values are read and incorporated in the
 * level above. Eventually, only the root, nodeVal[0] is active and it contains
 * the global reduction value. See Figure 5.1 for a visual.
 *
 * @param <ElemType> data array element datatype
 * @param <TallyType> tally datatype (result of reduction)
 */
public class Reduce<ElemType, TallyType extends Tally<ElemType>> {

    /**
     * Constructor for the Reduce class. Pattern is that you first construct it and
     * then call the reduce method.
     *
     * @param data    data elements to reduce
     * @param threadP number of threads
     * @param factory template for all the Tally objects
     */
    public Reduce(List<ElemType> data, int threadP, TallyType factory) {
        if (!(threadP > 0 && ((threadP & (threadP - 1)) == 0)))
            throw new IllegalArgumentException("threadP must be a power of 2 (for now)");

        this.data = data;
        n = data.size();
        this.threadP = threadP;
        tallyFactory = factory;
        this.nodeValue = new Object[threadP];
        for (int t = 0; t < threadP; t++)
            nodeValue[t] = new ArrayBlockingQueue<TallyType>(1);
    }

    /**
     * Get the reduction for the whole data array. Computed in parallel based on
     * threadP set in the ctor.
     *
     * @return reduction of data passed into the ctor.
     */
    public TallyType reduce() {
        try {

            // start the threads
            for (int t = 0; t < threadP; t++) {
                Thread thread = new Thread(new Task(t));
                thread.start();
            }
            // wait for result at root and return it
            return getNode(0);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @class Task - this is the nested class run by each thread
     */
    class Task implements Runnable {
        @Override
        /**
         * Thread task.
         */
        public void run() {
            try {
                /*
                 * Calculate this thread's portion of the data reduction. This is the Scwarz
                 * tight loop.
                 */
                TallyType tally = newTally();
                for (int i = start; i < end; i++)
                    tally.accum(data.get(i));

                /*
                 * Combine in a tree cap, then place the value in the Node array to be picked up
                 * by parent. -- the more you are like a power of 2, the longer you live root is
                 * thread 0, level 1 is threads 0 and P/2, level 2 is 0, P/4, P/2, 3P/4, etc.
                 */
                for (int stride = 1; stride < threadP && index % (2 * stride) == 0; stride *= 2)
                    tally.combine(getNode(index + stride));
                setNode(index, tally);
                return;

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /**
         * Constructor for Task -- just figures out which part of the data it owns
         *
         * @param threadi
         */
        public Task(int threadi) {
            index = threadi;
            size = n / threadP; // n and threadn are in the enclosing class
            start = size * threadi;
            end = (threadi == threadP - 1 ? n : start + size);
        }

        // per thread instance data:
        private int index, size, start, end;
    }

    // shared instance data:
    private int n, threadP;				// number of elements, number of threads
    private List<ElemType> data; 		// the data to reduce
    private TallyType tallyFactory; 	// template for new tally objects
    private Object[] nodeValue; 		// use an array of Object b/c of Java generic issues

    @SuppressWarnings("unchecked")
    private BlockingQueue<TallyType> findNode(int i) {
        return ((BlockingQueue<TallyType>) nodeValue[i]);
    }

    private void setNode(int i, TallyType tally) throws InterruptedException {
        findNode(i).put(tally);
    }

    private TallyType getNode(int i) throws InterruptedException {
        return findNode(i).take();
    }

    @SuppressWarnings("unchecked")
    private TallyType newTally() {
        return (TallyType) tallyFactory.init();
    }
}
