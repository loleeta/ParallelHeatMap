package com.lolita;


import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * HW5: creates a heatmap from a set of Observations.
 * Up to step 7.
 */
public class HeatMap7 {
    public static final int THREAD_THRESHOLD = 10;  // Number of threads to use
    private static final int DIM = 20;  // Dimension of grid
    private static JFrame application;  // GUI
    private static JButton button;  // Button on GUI
    private static final String REPLAY = "Replay"; // Action for button
    private static Color[][] grid; // 2D grid to be displayed
    static private final Color COLD = new Color(0x20, 0xB2, 0xAA),
            HOT = new Color(0xDC, 0x14, 0x3C); // Colors for heatmap
    private static int current; // Index of current timestamp
    private static List<ObservationTally> scanResults; // Results from scan

    public static void main(String[] args) throws InterruptedException {
        // Read in observations from file
        final String FILENAME = "obs_uniform_spray_step7.dat";
        ArrayList<Observation> observations = readDataFromFile(FILENAME);

        // Tally up observations
        HeatMapScan testReduce = new HeatMapScan(observations,
                THREAD_THRESHOLD);
        scanResults = testReduce.getScan();

        // Print to grid
        current = 0;
        printGrid();
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
     * Creates a new grid to be printed with scan results.
     * @throws InterruptedException
     */
    private static void printGrid() throws InterruptedException {
        grid = new Color[DIM][DIM];
        application = new JFrame();
        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fillGrid(grid);

        ColoredGrid gridPanel = new ColoredGrid(grid);
        application.add(gridPanel, BorderLayout.CENTER);

        button = new JButton(REPLAY);
        button.addActionListener(new CGDemo.BHandler());
        application.add(button, BorderLayout.PAGE_END);

        application.setSize(DIM * 25, (int)(DIM * 25));
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
        for (int i = 0; i < scanResults.size(); i++) {
            fillGrid(grid);
            current++;
            application.repaint();
            Thread.sleep(25);
        }
        button.setEnabled(true);
        current = 0;
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
                grid[r][c] = interpolateColor(scanResults.get(current)
                        .getCell(r, c), COLD, HOT);
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
        ratio = Math.min(ratio, 1.0);
        int ax = a.getRed();
        int ay = a.getGreen();
        int az = a.getBlue();
        int cx = ax + (int) ((b.getRed() - ax) * ratio);
        int cy = ay + (int) ((b.getGreen() - ay) * ratio);
        int cz = az + (int) ((b.getBlue() - az) * ratio);
        return new Color(cx, cy, cz);
    }
}