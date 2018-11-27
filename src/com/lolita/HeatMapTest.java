package com.lolita;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.HashMap;

/**
 * Two-pass parallelization of heat map. First reduces observations to be
 * grouped by time stamp. Then, performs a scan in parallel to create a
 * decayed final heat map.
 */
public class HeatMapTest {
    public static final int THREAD_THRESHOLD = 20; // Number of threads used
    private static final int DIM = 20;  // Dimension of grid
    private static JFrame application;  // GUI
    private static JButton button;  // Button on GUI
    private static final String REPLAY = "Replay"; // Action for button
    private static Color[][] grid; // 2D grid to be displayed
    private static final Color COLD = new Color(0x20, 0xB2, 0xAA),
            HOT = new Color(0xDC, 0x14, 0x3C); // Color codes
    private static Long current; // Index of current timestamp
    public static ObsTally reduceResults; // Reduced observation results
    public static Map<Long, int[]> finalHeatMap =
            new HashMap<>(); // Scanned observation results

    public static void main(String[] args) throws InterruptedException {
        // Read in observations from file
        final String FILENAME = "obs_uniform_spray.dat";
        ArrayList<Observation> observations = readDataFromFile(FILENAME);

        // Reduces observations by timestamp
        HeatMapScan2 testScan = new HeatMapScan2(observations,
                THREAD_THRESHOLD);
        reduceResults = testScan.getReduction();

        // Call parallel scan on each time stamp to get decayed data series
        ExecutorService threads = Executors.newCachedThreadPool();
        for (Long i = 1L; i <= reduceResults.heatmap.size(); i++) {
            threads.execute(new ParallelScan(reduceResults, i, finalHeatMap));
        }

        // Print results of parallel scan to grid
        current = 1L;
        printToGrid();
    }

    /**
     * Reads Observations from file into an array.
     * @param fileName
     * @return list of Observations
     */
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

    /**
     * Event handler for printing the grid.
     */
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

    /**
     * Creates a new grid to be printed with scan results.
     * @throws InterruptedException
     */
    private static void printToGrid() throws InterruptedException {
        grid = new Color[DIM][DIM];
        application = new JFrame();
        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fillGrid(grid);

        ColoredGrid gridPanel = new ColoredGrid(grid);
        application.add(gridPanel, BorderLayout.CENTER);

        button = new JButton(REPLAY);
        button.addActionListener(new BHandler());
        application.add(button, BorderLayout.PAGE_END);

        application.setSize(DIM * 25, DIM * 25);
        application.setVisible(true);
        application.repaint();
        animate();
    }

    /**
     * For each array in the scan results, print the grid.
     * @throws InterruptedException
     */
    private static void animate() throws InterruptedException {
        button.setEnabled(false);
        for (int i = 0; i < finalHeatMap.size(); i++) {
            fillGrid(grid);
            current++;
            application.repaint();
            Thread.sleep(200);
        }
        button.setEnabled(true);
        application.repaint();
    }

    /**
     * For the passed in two dimensional array, interprets the results of a
     * reduce/scan into different colors per pixel of grid.
     * @param grid  2D array of Color
     */
    private static void fillGrid(Color[][] grid) {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                int[] cells = finalHeatMap.get(current);
                grid[r][c] = interpolateColor(cells[r * DIM + c], COLD, HOT);
            }
        }
    }

    /**
     * Returns a Color based on passed in number.
     * @param ratio Some double value
     * @param a Cold color, blue
     * @param b Hot color, red
     * @return a new Color based on the value
     */
    private static Color interpolateColor(double ratio, Color a, Color b) {
        if (ratio == 4.0) {
            ratio = 1;
        } else if (ratio == 3.0) {
            ratio = .75;
        } else if (ratio == 2.0) {
            ratio = .5;
        } else if (ratio == 1.0) {
            ratio = .25;
        } else {
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
}