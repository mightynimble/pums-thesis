/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010.math;

import java.util.HashMap;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import java.util.Collections;
//import static org.apache.commons.math3.util.FastMath.expm1;
//import static org.apache.commons.math3.util.FastMath.log;

import java.util.Map;
import umd.lu.thesis.exceptions.InvalidValueException;
import umd.lu.thesis.pums2010.objects.Person2010;
import umd.lu.thesis.simulation.app2000.objects.Person;
import umd.lu.thesis.simulation.app2000.objects.TripType;

/**
 *
 * @author Home
 */
public class LogSum {

    private static final int INVALID_QUARTER = -1;

    private HashMap<String, Double[]> trainMap;

    private HashMap<String, Double[]> airMap;

    private HashMap<String, Double[]> quarterAirMap;

    private HashMap<String, Double[]> businessCarMap;

    private HashMap<String, Double[]> otherCarMap;

    private static final Map<String, Double> coefs;

    private static final Logger sLog = LogManager.getLogger(LogSum.class);

    static {

        Map<String, Double> aMap = new HashMap<>();

        aMap.put(TripType.BUSINESS.name() + "-cost1", -0.0325);
        aMap.put(TripType.BUSINESS.name() + "-cost2", -0.00934);
        aMap.put(TripType.BUSINESS.name() + "-cost3", -0.00662);
        aMap.put(TripType.BUSINESS.name() + "-cost4", -0.00370);
        aMap.put(TripType.BUSINESS.name() + "-cost5", -0.00278);
        aMap.put(TripType.BUSINESS.name() + "-time", -0.0356);
        aMap.put(TripType.BUSINESS.name() + "-ascAir", -0.440);
        aMap.put(TripType.BUSINESS.name() + "-ascTrain", -2.93);

        aMap.put(TripType.PLEASURE.name() + "-cost1", -0.00947);
        aMap.put(TripType.PLEASURE.name() + "-cost2", -0.00434);
        aMap.put(TripType.PLEASURE.name() + "-cost3", -0.000900);
        aMap.put(TripType.PLEASURE.name() + "-cost4", -0.000335);
        aMap.put(TripType.PLEASURE.name() + "-time", -0.0590);
        aMap.put(TripType.PLEASURE.name() + "-ascAir", -2.95);
        aMap.put(TripType.PLEASURE.name() + "-ascTrain", -3.56);

        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-cost1", -0.0127);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-cost2", -0.00570);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-cost3", -0.00396);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-cost4", -0.00276);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-cost5", -0.00108);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-time", -0.0328);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-ascAir", -1.49);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-ascTrain", -3.75);

        coefs = Collections.unmodifiableMap(aMap);
    }

    public LogSum(HashMap<String, Double[]> trainMap,
                  HashMap<String, Double[]> airMap,
                  HashMap<String, Double[]> quarterAirMap,
                  HashMap<String, Double[]> businessCarMap,
                  HashMap<String, Double[]> otherCarMap) {
        this.trainMap = trainMap;
        this.airMap = airMap;
        this.quarterAirMap = quarterAirMap;
        this.businessCarMap = businessCarMap;
        this.otherCarMap = otherCarMap;
    }

    public double calculateLogsum(Person2010 p, int o, int d, TripType type, int quarter) throws InvalidValueException {
        if(type == TripType.BUSINESS) {
            if((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) == null : quarterAirMap.get(getKey(o, d)) == null)
               && trainMap.get(getKey(o, d)) != null
               && businessCarMap.get(getKey(o, d)) != null) {
                return log(exp(uCar(p, o, d, type)) + exp(uTrain(p, o, d, type)));
            }
            else if((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) != null : quarterAirMap.get(getKey(o, d)) != null)
                    && trainMap.get(getKey(o, d)) == null
                    && businessCarMap.get(getKey(o, d)) != null) {
                return log(exp(uCar(p, o, d, type)) + exp(uAir(p, o, d, type, quarter)));
            }
            else if((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) == null : quarterAirMap.get(getKey(o, d)) == null)
                    && trainMap.get(getKey(o, d)) == null
                    && businessCarMap.get(getKey(o, d)) != null) {
                return log(exp(uCar(p, o, d, type)));
            }
            else if((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) != null : quarterAirMap.get(getKey(o, d)) != null)
                    && trainMap.get(getKey(o, d)) != null
                    && businessCarMap.get(getKey(o, d)) != null) {
                return log(exp(uCar(p, o, d, type)) + exp(uTrain(p, o, d, type)) + exp(uAir(p, o, d, type, quarter)));
            }
            else {
                throw new InvalidValueException("Cannot find car time/cost/stopnights/etc. businessCarMap.get(Integer.toString(o) + \"-\" + Integer.toString(d) returns NULL. (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", uCar: " + uCar(p, o, d, type) + ", uAir: " + uAir(p, o, d, type, quarter) + ", uTrain: " + uTrain(p, o, d, type) + ")");
            }
        }
        else if(type == TripType.PERSONAL_BUSINESS || type == TripType.PLEASURE) {
            if((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) == null : quarterAirMap.get(getKey(o, d)) == null)
               && trainMap.get(getKey(o, d)) != null
               && otherCarMap.get(getKey(o, d)) != null) {
                return log(exp(uCar(p, o, d, type)) + exp(uTrain(p, o, d, type)));
            }
            else if((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) != null : quarterAirMap.get(getKey(o, d)) != null)
                    && trainMap.get(getKey(o, d)) == null
                    && otherCarMap.get(getKey(o, d)) != null) {
                return log(exp(uCar(p, o, d, type)) + exp(uAir(p, o, d, type, quarter)));
            }
            else if((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) == null : quarterAirMap.get(getKey(o, d)) == null)
                    && trainMap.get(getKey(o, d)) == null
                    && otherCarMap.get(getKey(o, d)) != null) {
                return log(exp(uCar(p, o, d, type)));
            }
            else if((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) != null : quarterAirMap.get(getKey(o, d)) != null)
                    && trainMap.get(getKey(o, d)) != null
                    && otherCarMap.get(getKey(o, d)) != null) {
                return log(exp(uCar(p, o, d, type)) + exp(uTrain(p, o, d, type)) + exp(uAir(p, o, d, type, quarter)));
            }
            else {
                throw new InvalidValueException("Cannot find car time/cost/stopnights/etc. otherCarMap.get(Integer.toString(o) + \"-\" + Integer.toString(d) returns NULL. (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", uCar: " + uCar(p, o, d, type) + ", uAir: " + uAir(p, o, d, type, quarter) + ", uTrain: " + uTrain(p, o, d, type) + ")");
            }
        }
        else {
            throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                                            + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ")");
        }
    }

    double uCar(Person2010 p, int o, int d, TripType type) throws InvalidValueException {
        if(p.getIncLevel() < 1 || p.getIncLevel() > 3) {
            throw new InvalidValueException(
                    "Invalid incLevel: " + p.getIncLevel()
                    + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: "
                    + d + ", incLevel: " + p.getIncLevel() + ")");
        }
        try {
            if(type == TripType.BUSINESS) {
                double tcc = tourCarCost(p.getIncLevel(), o, d, type);
                if(tcc == Double.POSITIVE_INFINITY) {
                    return Double.NEGATIVE_INFINITY;
                }
                return coefs.get(type.name() + "-cost1") * tcc * (tcc <= 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost2") * tcc * (tcc <= 332 && tcc > 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost3") * tcc * (tcc <= 476 && tcc > 332 ? 1 : 0)
                       + coefs.get(type.name() + "-cost4") * tcc * (tcc <= 620 && tcc > 476 ? 1 : 0)
                       + coefs.get(type.name() + "-cost5") * tcc * (tcc > 620 ? 1 : 0)
                       + coefs.get(type.name() + "-time") * tourCarTime(o, d, type);
            }
            else if(type == TripType.PLEASURE) {
                double tcc = tourCarCost(p.getIncLevel(), o, d, type);
                if(tcc == Double.POSITIVE_INFINITY) {
                    return Double.NEGATIVE_INFINITY;
                }
                return coefs.get(type.name() + "-cost1") * tcc * (tcc <= 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost2") * tcc * (tcc <= 312 && tcc > 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost3") * tcc * (tcc <= 436 && tcc > 312 ? 1 : 0)
                       + coefs.get(type.name() + "-cost4") * tcc * (tcc > 436 ? 1 : 0)
                       + coefs.get(type.name() + "-time") * tourCarTime(o, d, type);
            }
            else if(type == TripType.PERSONAL_BUSINESS) {
                double tcc = tourCarCost(p.getIncLevel(), o, d, type);
                if(tcc == Double.POSITIVE_INFINITY) {
                    return Double.NEGATIVE_INFINITY;
                }
                return coefs.get(type.name() + "-cost1") * tcc * (tcc <= 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost2") * tcc * (tcc <= 312 && tcc > 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost3") * tcc * (tcc <= 436 && tcc > 312 ? 1 : 0)
                       + coefs.get(type.name() + "-cost4") * tcc * (tcc <= 560 && tcc > 436 ? 1 : 0)
                       + coefs.get(type.name() + "-cost5") * tcc * (tcc > 560 ? 1 : 0)
                       + coefs.get(type.name() + "-time") * tourCarTime(o, d, type);
            }
            else {
                throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                                                + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
            }
        }
        catch (NullPointerException ex) {
            // uCar not found
            sLog.debug("NullPointerException: uCar not found. Args: p.getPid: " + p.getPid() + ", o: " + o + ", d: " + d + ", TripType: " + type.name() + ". ");
            return Double.NEGATIVE_INFINITY;
        }
    }

    double uTrain(Person2010 p, int o, int d, TripType type) throws InvalidValueException {
        if(p.getIncLevel() < 1 || p.getIncLevel() > 3) {
            throw new InvalidValueException("Invalid incLevel: " + p.getIncLevel()
                                            + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
        }
        try {
            if(type == TripType.BUSINESS) {
                double ttc = tourTrainCost(o, d);
                if(ttc == Double.POSITIVE_INFINITY) {
                    return Double.NEGATIVE_INFINITY;
                }
                return coefs.get(type.name() + "-ascTrain")
                       + coefs.get(type.name() + "-cost1") * ttc * (ttc <= 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost2") * ttc * (ttc <= 312 && ttc > 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost3") * ttc * (ttc <= 476 && ttc > 312 ? 1 : 0)
                       + coefs.get(type.name() + "-cost4") * ttc * (ttc <= 620 && ttc > 476 ? 1 : 0)
                       + coefs.get(type.name() + "-cost5") * ttc * (ttc > 620 ? 1 : 0)
                       + coefs.get(type.name() + "-time") * tourTrainTime(o, d);
            }
            else if(type == TripType.PLEASURE) {
                double ttc = tourTrainCost(o, d);
                if(ttc == Double.POSITIVE_INFINITY) {
                    return Double.NEGATIVE_INFINITY;
                }
                return coefs.get(type.name() + "-ascTrain")
                       + coefs.get(type.name() + "-cost1") * ttc * (ttc <= 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost2") * ttc * (ttc <= 332 && ttc > 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost3") * ttc * (ttc <= 436 && ttc > 332 ? 1 : 0)
                       + coefs.get(type.name() + "-cost4") * ttc * (ttc > 436 ? 1 : 0)
                       + coefs.get(type.name() + "-time") * tourTrainTime(o, d);
            }
            else if(type == TripType.PERSONAL_BUSINESS) {
                double ttc = tourTrainCost(o, d);
                if(ttc == Double.POSITIVE_INFINITY) {
                    return Double.NEGATIVE_INFINITY;
                }
                return coefs.get(type.name() + "-ascTrain")
                       + coefs.get(type.name() + "-cost1") * ttc * (ttc <= 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost2") * ttc * (ttc <= 312 && ttc > 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost3") * ttc * (ttc <= 436 && ttc > 312 ? 1 : 0)
                       + coefs.get(type.name() + "-cost4") * ttc * (ttc <= 560 && ttc > 436 ? 1 : 0)
                       + coefs.get(type.name() + "-cost5") * ttc * (ttc > 560 ? 1 : 0)
                       + coefs.get(type.name() + "-time") * tourTrainTime(o, d);
            }
            else {
                throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                                                + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
            }
        }
        catch (NullPointerException ex) {
            // uCar not found
            sLog.debug("NullPointerException: uTrain not found. Args: p.getPid: " + p.getPid() + ", o: " + o + ", d: " + d + ", TripType: " + type.name() + ". ");
            return Double.NEGATIVE_INFINITY;
        }
    }

    double uAir(Person2010 p, int o, int d, TripType type, int quarter) throws InvalidValueException {
        if(p.getIncLevel() < 1 || p.getIncLevel() > 3) {
            throw new InvalidValueException("Invalid incLevel: " + p.getIncLevel()
                                            + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
        }
        try {
            if(type == TripType.BUSINESS) {
                double tac = tourAirCost(o, d, quarter);
                if(tac == Double.POSITIVE_INFINITY) {
                    return Double.NEGATIVE_INFINITY;
                }
                return coefs.get(type.name() + "-ascAir")
                       + coefs.get(type.name() + "-cost1") * tac * (tac <= 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost2") * tac * (tac <= 332 && tac > 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost3") * tac * (tac <= 476 && tac > 332 ? 1 : 0)
                       + coefs.get(type.name() + "-cost4") * tac * (tac <= 620 && tac > 476 ? 1 : 0)
                       + coefs.get(type.name() + "-cost5") * tac * (tac > 620 ? 1 : 0)
                       + coefs.get(type.name() + "-time") * tourAirTime(o, d);
            }
            else if(type == TripType.PLEASURE) {
                double tac = tourAirCost(o, d, quarter);
                if(tac == Double.POSITIVE_INFINITY) {
                    return Double.NEGATIVE_INFINITY;
                }
                return coefs.get(type.name() + "-ascAir")
                       + coefs.get(type.name() + "-cost1") * tac * (tac <= 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost2") * tac * (tac <= 312 && tac > 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost3") * tac * (tac <= 436 && tac > 312 ? 1 : 0)
                       + coefs.get(type.name() + "-cost4") * tac * (tac > 436 ? 1 : 0)
                       + coefs.get(type.name() + "-time") * tourAirTime(o, d);
            }
            else if(type == TripType.PERSONAL_BUSINESS) {
                double tac = tourAirCost(o, d, quarter);
                if(tac == Double.POSITIVE_INFINITY) {
                    return Double.NEGATIVE_INFINITY;
                }
                return coefs.get(type.name() + "-ascAir")
                       + coefs.get(type.name() + "-cost1") * tac * (tac <= 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost2") * tac * (tac <= 312 && tac > 188 ? 1 : 0)
                       + coefs.get(type.name() + "-cost3") * tac * (tac <= 436 && tac > 312 ? 1 : 0)
                       + coefs.get(type.name() + "-cost4") * tac * (tac <= 560 && tac > 436 ? 1 : 0)
                       + coefs.get(type.name() + "-cost5") * tac * (tac > 560 ? 1 : 0)
                       + coefs.get(type.name() + "-time") * tourAirTime(o, d);
            }
            else {
                throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                                                + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
            }
        }
        catch (NullPointerException ex) {
            // uCar not found
            sLog.debug("NullPointerException: uAir not found. Args: p.getPid: " + p.getPid() + ", o: " + o + ", d: " + d + ", TripType: " + type.name() + ". ", ex);
            return Double.NEGATIVE_INFINITY;
        }
    }

    //  Tour_AirCost= AirFare*2
    public double tourAirCost(int o, int d, int quarter) {
        if(quarter == INVALID_QUARTER) {
            return airMap.get(getKey(o, d))[1] * 2;
        }
        else {
            if(quarterAirMap.get(getKey(o, d)) == null) {
                if(airMap.get(getKey(o, d)) == null) {
                    return Double.POSITIVE_INFINITY;
                }
                else {
                    return airMap.get(getKey(o, d))[1] * 2;
                }
            }
            else {
                if (quarterAirMap.get(getKey(o, d))[quarter] == null) {
                    if (airMap.get(getKey(o,d))[1] == null) {
                        return Double.POSITIVE_INFINITY;
                    }
                    return airMap.get(getKey(o,d))[1] * 2;
                }
                return quarterAirMap.get(getKey(o, d))[quarter] * 2;
            }
        }
    }

    //  Tour_AirTime=AirTime*2
    public double tourAirTime(int o, int d) {
        if(airMap.get(getKey(o, d)) == null) {
            return Double.POSITIVE_INFINITY;
        }
        else {
            return airMap.get(getKey(o, d))[0] * 2;
        }
    }

    //  Tour_CarCost=(DrvCost/weight + LodgeCost)*2
    //  (Drvcost from ODSKIM_Car_Business.csv and ODSKIM_Car_PPB.csv)
    public double tourCarCost(int incLevel, int o, int d, TripType type) {
        double driveCost = 0.0;
        try {
            if(type == TripType.BUSINESS) {
                if(businessCarMap.get(getKey(o, d)) == null || businessCarMap.get(getKey(o, d))[1] == 99999999) {
                    return Double.POSITIVE_INFINITY;
                }
                else {
                    driveCost = businessCarMap.get(getKey(o, d))[1];
                }
            }
            else if(type == TripType.PLEASURE || type == TripType.PERSONAL_BUSINESS) {
                if(otherCarMap.get(getKey(o, d)) == null  || otherCarMap.get(getKey(o, d))[1] == 99999999) {
                    driveCost = Double.POSITIVE_INFINITY;
                }
                else {
                    driveCost = otherCarMap.get(getKey(o, d))[1];
                }
            }
            else {
                throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                                                + ". (Args: tripPurpose: " + type.name()
                                                + ", o: " + o + ", d: " + d + ", incLevel: "
                                                + incLevel + ")");
            }
            return (driveCost / weight(type) + lodgeCost(incLevel, o, d, type)) * 2;
        }
        catch (InvalidValueException e) {
            sLog.error(e.getLocalizedMessage(), e);
            System.exit(1);
        }
        return Double.POSITIVE_INFINITY;
    }

    public double tourCarTime(int o, int d, TripType type) {
        try {
            if(type == TripType.BUSINESS) {
                if(businessCarMap.get(getKey(o, d)) == null || businessCarMap.get(getKey(o, d))[0] == 99999999) {
                    return Double.POSITIVE_INFINITY;
                }
                else {
                    return (businessCarMap.get(getKey(o, d))[0] + stopNights(o, d, type) * 12) * 2;
                }
            }
            else if(type == TripType.PLEASURE || type == TripType.PERSONAL_BUSINESS) {
                if(otherCarMap.get(getKey(o, d)) == null || otherCarMap.get(getKey(o, d))[0] == 99999999) {
                    return Double.POSITIVE_INFINITY;
                }
                else {
                    return (otherCarMap.get(getKey(o, d))[0] + stopNights(o, d, type) * 11) * 2;
                }
            }
            else {
                throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                                                + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ")");
            }
        }
        catch (InvalidValueException e) {
            sLog.error(e.getLocalizedMessage(), e);
            System.exit(1);
        }
        return Double.POSITIVE_INFINITY;
    }

    //  Cost_2000*2  (Cost_2000 from OD_SKIM_Train.csv)
    public double tourTrainCost(int o, int d) {
        if(trainMap.get(getKey(o, d)) == null) {
            return Double.POSITIVE_INFINITY;
        }
        else {
            return trainMap.get(getKey(o, d))[1] * 2;
        }
    }

    //  Tour_TrainTime=Time*2        (Time from OD_SKIM_Train.csv)
    public double tourTrainTime(int o, int d) {
        if(trainMap.get(getKey(o, d)) == null) {
            return Double.POSITIVE_INFINITY;
        }
        else {
            return trainMap.get(getKey(o, d))[0] * 2;
        }
    }

    //  LodgeCost = Unit_Lodge_Cost * Stop_nigt
    double lodgeCost(int incLevel, int o, int d, TripType type) throws InvalidValueException {
        return (double) unitLodgeCost(incLevel, type) * stopNights(o, d, type);
    }

    // Stop_nigt from ODSKIM_Car_Business.csv and ODSKIM_Car_PPB.csv
    double stopNights(int o, int d, TripType type) throws InvalidValueException {
        // o => column C, d => column D, stopNight => column H
        try {
            if(type == TripType.BUSINESS) {
                if (businessCarMap.get(getKey(o, d))[2] == 99999999) {
                    return Double.POSITIVE_INFINITY;
                }
                return businessCarMap.get(getKey(o, d))[2];
            }
            else if(type == TripType.PLEASURE || type == TripType.PERSONAL_BUSINESS) {
                if (otherCarMap.get(getKey(o, d))[2] == 99999999) {
                    return Double.POSITIVE_INFINITY;
                }
                return otherCarMap.get(getKey(o, d))[2];
            }
        }
        catch (NullPointerException e) {
            sLog.error(e.getLocalizedMessage() + " -- o: " + o + ", d: " + d, e);
            System.exit(1);
        }
        throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                                        + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ")");
    }

    int unitLodgeCost(int incLevel, TripType type) throws InvalidValueException {
        if(type == TripType.BUSINESS) {
            if(incLevel == 1) {
                return 100;
            }
            else if(incLevel == 2) {
                return 129;
            }
            else if(incLevel == 3) {
                return 157;
            }
            else {
                throw new InvalidValueException("Invalid incLevel: " + incLevel
                                                + ". (Args: incLevel = " + incLevel
                                                + ", tripPurpose: " + type + ")");
            }
        }
        else if(type == TripType.PLEASURE || type == TripType.PERSONAL_BUSINESS) {
            if(incLevel == 1) {
                return 43;
            }
            else if(incLevel == 2) {
                return 72;
            }
            else if(incLevel == 3) {
                return 100;
            }
            else {
                throw new InvalidValueException("Invalid incLevel: " + incLevel
                                                + ". (Args: incLevel = " + incLevel
                                                + ", tripPurpose: " + type.name() + ")");
            }
        }
        else {

            throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                                            + ". (Args: incLevel = " + incLevel
                                            + ", tripPurpose: " + type.name() + ")");

        }
    }

    public double weight(TripType type) throws InvalidValueException {
        if(type == TripType.BUSINESS) {
            return 1.0;
        }
        else if(type == TripType.PLEASURE || type == TripType.PERSONAL_BUSINESS) {
            return 2.0;
        }
        else {
            throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                                            + ". (Args: tripPurpose: " + type.name() + ")");
        }
    }

    private String getKey(int o, int d) {
        return Integer.toString(o) + "-" + Integer.toString(d);
    }
//    /*
//     * converts double x to Apcomplex and perform ApcomplexMath.exp
//     */
//    public double exp(double x) {
//        Apfloat apf = new Apfloat(String.valueOf(x), 30);
//        // ApfloatMath.exp("-4.6595999534040004e7").doubleValue() is 0.0
//        // So we have to get the value in String format then convert it to double
//        
//        return ApfloatMath.exp(apf).doubleValue();
//    }
//    
//    /*
//     * Converts double x to Apcomplex and perform ApcomplexMath.log
//     */
//    public double log(double x) {
//        Apfloat apf = new Apfloat(String.valueOf(x), 30);
//        return ApfloatMath.log(apf).doubleValue();
//    }
}
