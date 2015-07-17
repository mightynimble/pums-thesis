/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.simulation.app2000.objects;

/**
 *
 * @author lousia
 */
public enum TripType {

    BUSINESS(0), PLEASURE(1), PERSONAL_BUSINESS(2), HOME(3);

    private int value;

    private TripType(int v) {
        value = v;
    }

    public int getValue() {
        return value;
    }

    public static final int itemCount = 4;
}
