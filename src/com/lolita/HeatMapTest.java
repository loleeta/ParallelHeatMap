package com.lolita;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class HeatMapTest {
    public static final int N = 1024;
    public static final int THRESHOLD = 100;
    public static final int DIM = 150;
    private static final String REPLAY = "Replay";
    private static JFrame application;
    private static JButton button;
    private static Color[][] grid;
    static private final Color COLD = new Color(0xB0, 0xE0, 0xE6),
            HOT = new Color(0xDD, 0xA0, 0xDD);
    private static final double HOT_CALIB = 1.0;
    static private int offset = 0;
    private static int current;

    private static List<ObservationTally> scanResults;

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        final String FILENAME = "obs_uniform_spray.dat";
        ArrayList<Observation> observations = new ArrayList<>();
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILENAME));
            Observation obs = (Observation) in.readObject();
            while (!obs.isEOF()) {
                obs = (Observation) in.readObject();
                observations.add(obs);
            }
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("reading from " + FILENAME + "failed: " + e);
            e.printStackTrace();
            System.exit(1);
        }

        //Tally up observations
        HeatMapScan testReduce = new HeatMapScan(observations, THRESHOLD);
        scanResults = testReduce.getScan();
        for (int i = 0; i < observations.size(); i++) {
            System.out.printf("+ %s = \n\t\t%s%n", observations.get(i),
                    scanResults
                    .get(i));
        }

        /*
        current = 0;

        grid = new Color[DIM][DIM];
        application = new JFrame();
        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fillGrid(grid);

        ColoredGrid gridPanel = new ColoredGrid(grid);
        application.add(gridPanel, BorderLayout.CENTER);

        button = new JButton(REPLAY);
        button.addActionListener(new CGDemo.BHandler());
        application.add(button, BorderLayout.PAGE_END);

        application.setSize(DIM * 4, (int)(DIM * 4.4));
        application.setVisible(true);
        application.repaint();
        animate();
        */
    }

    /*private static void animate() throws InterruptedException {
        button.setEnabled(false);
        for (int i = 0; i < DIM; i++) {
            fillGrid(grid);
            application.repaint();
            Thread.sleep(50);
        }
        button.setEnabled(true);
        application.repaint();
    }

    private static void fillGrid(Color[][] grid) {
        int pixels = grid.length * grid[0].length;
        for (int r = 0; r < grid.length; r++)
            for (int c = 0; c < grid[r].length; c++)
                grid[r][c] = interpolateColor(scanResults.get(current)
                                .getCell(r, c),
                        COLD, HOT);
    }

    private static Color interpolateColor(double ratio, Color a, Color b) {
        int ax = a.getRed();
        int ay = a.getGreen();
        int az = a.getBlue();
        int cx = ax + (int) ((b.getRed() - ax) * ratio);
        int cy = ay + (int) ((b.getGreen() - ay) * ratio);
        int cz = az + (int) ((b.getBlue() - az) * ratio);
        return new Color(cx, cy, cz);
    }*/

}
