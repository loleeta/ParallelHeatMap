/*
 * Kevin Lundeen
 * Fall 2018, CPSC 5600, Seattle University
 * This is free and unencumbered software released into the public domain.
 */
package com.lolita;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Step 4 from HW5
 */
public class SomeData {
    public static void main(String[] args) {
        uniformSpray(); //data for HW6
        oldUniformSpray(); //data for HW5
    }

    /**
     * Writes to file a set of Observations consisting of time, row, col values
     */
    public static void uniformSpray() {
        final String FILENAME = "obs_uniform_spray.dat";
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILENAME));
            int t = 0;
            int count = 0;
            for (double r = -0.95; r <= 0.95; r += 0.1) {
                t++;
                for (double c = r; c <= 0.95; c += 0.1) {
                    count++;
                    out.writeObject(new Observation(t, c, r));
                    out.writeObject(new Observation(t, -c, r));
                }
            }

            for (double r = -0.95; r <= 0.95; r += 0.1) {
                t++;
                for (double c = r; c <= 0.95; c += 0.1) {
                    count++;
                    out.writeObject(new Observation(t, c, r));
                    out.writeObject(new Observation(t, c, -r));
                }
            }

            out.writeObject(new Observation());  // to mark EOF
            System.out.println("Count of data: " + count);
            out.close();
        } catch (IOException e) {
            System.out.println("Writing to " + FILENAME + "failed: " + e);
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Wrote " + FILENAME);
    }

    /**
     * Writes to file a set of Observations consisting of time, row, col values
     */
    public static void oldUniformSpray() {
        final String FILENAME = "obs_uniform_spray_step7.dat";
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILENAME));
            int t = 0;
            int count = 0;
            for (double r = -0.95; r <= 0.95; r += 0.1) {
                t++;
                for (double c = -0.95; c <= 0.95; c += 0.1) {
                    count++;
                    out.writeObject(new Observation(t, c, r));
                }
            }
            out.writeObject(new Observation());  // to mark EOF
            System.out.println("Count is " + count);
            out.close();
        } catch (IOException e) {
            System.out.println("Writing to " + FILENAME + "failed: " + e);
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Wrote " + FILENAME);

    }
}