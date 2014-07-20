package umd.lu.thesis.helper;

import umd.lu.thesis.objects.Point;

/**
 * 
 * @author lousia
 */
public class MathUtils {
    public static double distance(Point p1, Point p2) {
        double distance = 3963.0 * Math.acos(
                Math.sin(p1.getLat()/57.2958) * Math.sin(p2.getLat()/57.2958)
                        + Math.cos(p1.getLat()/57.2958) * Math.cos(p2.getLat()/57.2958) * Math.cos(p2.getLon()/57.2958 - p1.getLon()/57.2958));
        return distance;
    }
}
