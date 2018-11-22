package com.lolita;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class HeatMapTest {
    public static final int THREAD_THRESHOLD = 10;
    private static final int DIM = 20;
    private static final String REPLAY = "Replay";
    private static JFrame application;
    private static JButton button;
    private static Color[][] grid;
    static private final Color COLD = new Color(0x0a, 0x37, 0x66), HOT = Color.RED;
    static private int offset = 0;
    private static int current;
    private static int NUM_OBSERVATIONS = 10; //to do a scan of at a time

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

        HeatMapScan2 testScan = new HeatMapScan2(observations,
                THREAD_THRESHOLD, NUM_OBSERVATIONS);
        ObsTally reduceResults = testScan.getReduction();
        System.out.println("ObsTally: \n" + reduceResults);


        /*
        System.out.println("scanResults2.size: " + scanResults2.size());
        for (int i = 0; i < scanResults2.size(); i++) {
            for (int j = 0; j < scanResults2.get(i).heatmap.size(); j++) {
                System.out.println("i is " + i + " and the size of the " +
                        "heatmap is " + j);
            }
        }*/

        //printToGrid(scanResults2);

        // Print out scan
        /*
        for (int i = 0; i < observations.size(); i++) {
            System.out.println(observations.get(i) + " and " + scanResults
                    .get(i));
        }
        */
        //System.out.println(scanResults.size());
        current = 0;

        // Print scan results to heat map
        //printToGrid(scanResults);
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

    private static void printToGrid(List<ObsTally> scanResults2) throws
            InterruptedException {
        grid = new Color[DIM][DIM];
        application = new JFrame();
        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fillGrid(grid);

        ColoredGrid gridPanel = new ColoredGrid(grid);
        application.add(gridPanel, BorderLayout.CENTER);

        button = new JButton(REPLAY);
        button.addActionListener(new CGDemo.BHandler());
        application.add(button, BorderLayout.PAGE_END);

        application.setSize(DIM * 30, (int)(DIM * 30));
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
            Thread.sleep(25);
        }
        button.setEnabled(true);
        current = 0;
        application.repaint();
    }

    private static void fillGrid(Color[][] grid) {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {


                //grid[r][c] = interpolateColor(
                 //       scanResults2.get(current).heatmap.get(current)
                 //       .getCell(1.0, r, c), COLD, HOT);
            }
        }
    }

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
