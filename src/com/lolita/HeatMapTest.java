package com.lolita;

import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;


import javax.swing.JButton;
import javax.swing.JFrame;

public class HeatMapTest {
    public static final int THREAD_THRESHOLD = 15;
    private static final int DIM = 20;
    private static final String REPLAY = "Replay";
    private static JFrame application;
    private static JButton button;
    private static Color[][] grid;
    static private final Color COLD = new Color(0x0a, 0x37, 0x66), HOT = Color.RED;
    private static Long current;
    private static int NUM_OBSERVATIONS = 0; //to do a scan of at a time
    public static Map<Long, int[]> finalHeatMap = new HashMap<>();
    public static ObsTally reduceResults;

    private static List<ObservationTally> scanResults;
    private static List<ObsTally> scanResults2;

    public static void main(String[] args) throws InterruptedException {
        // Read in observations from file
        final String FILENAME = "obs_uniform_spray.dat";
        ArrayList<Observation> observations = readDataFromFile(FILENAME);

        // Tally up observations
        /*HeatMapScan testReduce = new HeatMapScan(observations,
                THREAD_THRESHOLD, NUM_OBSERVATIONS);
        scanResults = testReduce.getScan();*/

        // Print observations

        //Reduces observations by timestamp
        HeatMapScan2 testScan = new HeatMapScan2(observations,
                THREAD_THRESHOLD, NUM_OBSERVATIONS);
        reduceResults = testScan.getReduction();

        //Print out last tally
        //System.out.println("ObsTally: \n" + reduceResults);

        /* prints results of reduce
        for (Long i = 1L; i <= reduceResults.heatmap.size(); i++) {
            System.out.println(i + ": " + Arrays.toString(reduceResults.heatmap
                    .get
                    (i)));
        } */

        ExecutorService threads = Executors.newCachedThreadPool();
        current = 1L;

        //Call parallel scan on each time stamp
        for (Long i = 1L; i <= reduceResults.heatmap.size(); i++) {
            threads.execute(new ParallelScan(reduceResults, i));
        }

        System.out.println("Scan finished. Printing scan results.");

        /*
        for (Map.Entry<Long, int[]> entry: finalHeatMap.entrySet()) {
            Long currTime = entry.getKey();
            int[] currCells = entry.getValue();
            System.out.println(currTime + ": " + Arrays.toString(currCells));
        }*/


        printToGrid();
    }

    private static ArrayList<Observation> readDataFromFile(String fileName) {
        ArrayList<Observation> data = new ArrayList<>();
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
            Observation obs = (Observation) in.readObject();
            while (!obs.isEOF()) {
                obs = (Observation) in.readObject();
                if (!obs.isEOF())
                    data.add(obs);
            }
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("reading from " + fileName + "failed: " + e);
            e.printStackTrace();
            System.exit(1);
        }
        return data;
    }

    static class BHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (REPLAY.equals(e.getActionCommand())) {
                current = 1L;
                new Thread() {
                    public void run() {
                        try {
                            animate();
                        } catch (InterruptedException e) {
                            System.exit(0);
                        }
                    }
                }.start();
            }
        }
    }

    private static void printToGrid() throws
            InterruptedException {
        grid = new Color[DIM][DIM];
        application = new JFrame();
        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fillGrid(grid);

        ColoredGrid gridPanel = new ColoredGrid(grid);
        application.add(gridPanel, BorderLayout.CENTER);

        button = new JButton(REPLAY);
        button.addActionListener(new BHandler());
        application.add(button, BorderLayout.PAGE_END);

        application.setSize(DIM * DIM, DIM * DIM);
        application.setVisible(true);
        application.repaint();
        animate();
    }


    private static void animate() throws InterruptedException {
        button.setEnabled(false);
        for (int i = 0; i < scanResults2.size(); i++) {
            fillGrid(grid);
            current++;
            application.repaint();
            Thread.sleep(50); //25 for faster
        }
        button.setEnabled(true);
        application.repaint();
    }

    private static void fillGrid(Color[][] grid) {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                int [] cells = finalHeatMap.get(current);
                //System.out.println("r*DIM*c: " + r*DIM*c);
                grid[r][c] = interpolateColor(cells[r*DIM*c], COLD, HOT);
            }
        }
    }

    private static Color interpolateColor(double ratio, Color a, Color b) {
        if (ratio >= -1.0) {
            ratio = 1.0;
        }
        else if (ratio >= 0.5) {
            ratio = 0.75;
        }
        else if (ratio > 0) {
            ratio = 0.25;
        }
        else {
            ratio = 0;
        }
        int ax = a.getRed();
        int ay = a.getGreen();
        int az = a.getBlue();
        int cx = ax + (int) ((b.getRed() - ax) * ratio);
        int cy = ay + (int) ((b.getGreen() - ay) * ratio);
        int cz = az + (int) ((b.getBlue() - az) * ratio);
        return new Color(cx, cy, cz);
    }

    /**
     *
     */
    static class ParallelScan implements Runnable {
        public Long time;
        Map<Long, int[]> heatmap;
        public int [] cells;

        public ParallelScan(ObsTally tally, Long time) {
            this.time = time;
            this.cells = tally.heatmap.get(time);
            this.heatmap = tally.heatmap;
        }

        @Override
        public void run() {
            Long minute = time - 3;
            if (minute <= 0) {
                minute = 1L;
            }

            int counter = 1;
            for (Long i = this.time; i > minute; i--) {
                weight(this.heatmap.get(i), counter);
                counter++;
            }
            finalHeatMap.put(time, cells);
        }

        private void weight(int[] observations, int weight) {
            for (int i = 0; i < observations.length; i++) {
                cells[i] += observations[i]/weight;
            }
        }
    }

}


