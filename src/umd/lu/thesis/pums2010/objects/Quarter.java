/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010.objects;

/**
 *
 * @author Home
 */
public enum Quarter {

    FIRST(0), SECOND(1), THIRD(2), FOURTH(3);

    private int value;

    private Quarter(int v) {
        value = v;
    }

    public int getValue() {
        return value;
    }

    public static final int itemCount = 4;
}

/**
 * System.out.println(TravelMode.FIRST.getValue());
 */