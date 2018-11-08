package com.lolita;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Scan class based on Reduce class. This is very loosely related to
 * Figure 5.7, though that one (including the corrected version on the web)
 * is rife with bugs.
 *
 * @param <ElemType> data array element datatype
 * @param <TallyType> tally datatype (result of reduction)
 */
public class Scan<ElemType, TallyType extends Tally<ElemType>> {

    /**
     * Constructor for the Scan class. Pattern is that you first construct it and
     * then call the scan method.
     *
     * @param data    data elements to reduce
     * @param threadP number of threads
     * @param factory template for all the Tally objects
     */
    public Scan(List<ElemType> data, int threadP, TallyType factory) {
        if (!(threadP > 0 && ((threadP & (threadP - 1)) == 0)))
            throw new IllegalArgumentException("threadP must be a power of 2 (for now)");

        this.data = data;
        n = data.size();
        this.threadP = threadP;
        tallyFactory = factory;
        this.nodeUp = new Object[threadP];
        this.nodeLeft = new Object[threadP];
        this.nodeDown = new Object[threadP];
        for (int t = 0; t < threadP; t++) {
            nodeUp[t] = new ArrayBlockingQueue<TallyType>(1);
            nodeLeft[t] = new ArrayBlockingQueue<TallyType>(1);
            nodeDown[t] = new ArrayBlockingQueue<TallyType>(1);
        }
    }

    /**
     * Get the reduction for the whole data array. Computed in parallel based on
     * threadP set in the ctor.
     *
     * @return scan of data passed into the ctor.
     */
    public List<TallyType> scan() {
        try {

            result = new ArrayList<TallyType>(n);
            for (int i = 0; i < n; i++)
                result.add(null);

            // start the threads
            List<Thread> threads = new ArrayList<Thread>(threadP);
            for (int t = 0; t < threadP; t++) {
                Thread thread = new Thread(new Task(t));
                threads.add(thread);
                thread.start();
            }
            // wait for result and return it
            for (Thread thread : threads)
                thread.join();
            return result;

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
                 * Calculate this thread's portion of the data scan. This is the Shwarz
                 * tight loop.
                 * At this point, these tallies are of a local scan starting at start.
                 * We will add in the prefix tally from all the leaves to the left below
                 * just prior to returning.
                 */
                TallyType tally = newTally();
                for (int i = start; i < end; i++) {
                    // equivalent to prepare() method for each and then combined
                    tally.accum(data.get(i));
                    result.set(i, cloneTally(tally));
                }

                /*
                 * Combine in a tree cap, then place the value in the Node array to be picked up
                 * by parent. -- the more you are like a power of 2, the longer you live root is
                 * thread 0, level 1 is threads 0 and P/2, level 2 is 0, P/4, P/2, 3P/4, etc.
                 */
                for (int stride = 1; stride < threadP && index % (2 * stride) == 0; stride *= 2) {
                    setNodeLeft(index + stride, tally);  // send this to the downward pass where we'll need it
                    tally.combine(getNodeUp(index + stride));
                }
                setNodeUp(index, tally);  // send this to my parent node

                /*
                 * Now we move back down the tree cap with the prefix values (nodeDown array).
                 * My prefix is set by my parent. It never changes as I go down the left side
                 * of my subtree.
                 */
                TallyType prefix;
                if (index == 0)
                    prefix = newTally();
                else
                    prefix = getNodeDown(index);  // wait until my parent tells me my prefix tally

                /*
                 * Now start all my right siblings (I have one at each level going down from the level
                 * I ended up on before.)
                 */
                for (int stride = threadP/2; stride >= 1; stride /= 2)
                    // skip all the strides that are too high up the tree for me
                    if (index % (2 * stride) == 0) {
                        TallyType sibtally = cloneTally(prefix);
                        sibtally.combine(getNodeLeft(index + stride)); // combine with left sibling I sent earlier
                        setNodeDown(index + stride, sibtally);  // start downward stage in my right child's thread
                    }

                /*
                 * Then add in my prefix to all the result tallies.
                 * Before they only had local scan results, but by adding in the prefix,
                 * we get the globally correct results.
                 */
                for (int i = start; i < end; i++)
                    result.get(i).combine(prefix);

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
    private List<TallyType> result;	// scan result
    private TallyType tallyFactory; 	// template for new tally objects
    // synchronization arrays: (of Object b/c of Java generic issues)
    private Object[] nodeUp, 			// reductions going up in first stage
            nodeLeft, 					// reductions of left sibling saved for downward stage
            nodeDown;					// parent-passed prefix values in downward stage

    @SuppressWarnings("unchecked")
    private BlockingQueue<TallyType> findNode(Object[] nodeArray, int i) {
        return ((BlockingQueue<TallyType>) nodeArray[i]);
    }

    private void setNodeUp(int i, TallyType tally) throws InterruptedException {
        findNode(nodeUp, i).put(cloneTally(tally));
    }

    private TallyType getNodeUp(int i) throws InterruptedException {
        return findNode(nodeUp, i).take();
    }

    private void setNodeDown(int i, TallyType tally) throws InterruptedException {
        findNode(nodeDown, i).put(cloneTally(tally));
    }

    private TallyType getNodeDown(int i) throws InterruptedException {
        return findNode(nodeDown, i).take();
    }

    private void setNodeLeft(int i, TallyType tally) throws InterruptedException {
        findNode(nodeLeft, i).put(cloneTally(tally));
    }

    private TallyType getNodeLeft(int i) throws InterruptedException {
        return findNode(nodeLeft, i).take();
    }

    @SuppressWarnings("unchecked")
    private TallyType newTally() {
        return (TallyType) tallyFactory.init();
    }

    @SuppressWarnings("unchecked")
    private TallyType cloneTally(TallyType tally) {
        return (TallyType) tally.clone();
    }
}
