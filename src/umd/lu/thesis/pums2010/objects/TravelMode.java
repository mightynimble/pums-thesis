/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010.objects;

/**
 *
 * @author Home
 */
public enum TravelMode {

    CAR(1), AIR(2), TRAIN(3);

    private int value;

    private TravelMode(int v) {
        value = v;
    }

    public int getValue() {
        return value;
    }

    public static final int itemCount = 3;
}

/**
 * System.out.println(TravelMode.CAR.getValue());
 */