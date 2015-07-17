/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010.objects;

/**
 *
 * @author Home
 */
public enum ModeChoice {

    CAR(0), AIR(1), TRAIN(2);

    private int value;

    private ModeChoice(int v) {
        value = v;
    }

    public int getValue() {
        return value;
    }

    public static final int itemCount = 3;
}
