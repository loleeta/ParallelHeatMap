package com.lolita;


import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class HeatMap7 {
    public static final int THREAD_THRESHOLD = 10;
    private static final int DIM = 20;
    private static final String REPLAY = "Replay";
    private static JFrame application;
    private static JButton button;
    private static Color[][] grid;
    static private final Color COLD = new Color(0x20, 0xB2, 0xAA), HOT = new
            Color(0xDC, 0x14, 0x3C);
    static private int offset = 0;
    private static int current;

    private static List<ObservationTally> scanResults;

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        // Read in observations from file
        final String FILENAME = "obs_uniform_spray_step7.dat";
        ArrayList<Observation> observations = readDataFromFile(FILENAME);

        // Tally up observations
        HeatMapScan testReduce = new HeatMapScan(observations,
                THREAD_THRESHOLD);
        scanResults = testReduce.getScan();

        // Print out scan
        /*
        for (int i = 0; i < observations.size(); i++) {
            System.out.println(observations.get(i) + " and " + scanResults
                    .get(i));
        }
        */
        System.out.println(scanResults.size());
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

        application.setSize(DIM * 25, (int)(DIM * 25));
        application.setVisible(true);
        application.repaint();
        animate();

    }

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

    private static void fillGrid(Color[][] grid) {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                grid[r][c] = interpolateColor(scanResults.get(current)
                        .getCell(r, c), COLD, HOT);
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