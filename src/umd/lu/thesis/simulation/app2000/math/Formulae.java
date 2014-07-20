package umd.lu.thesis.simulation.app2000.math;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.exceptions.InvalidValueException;
import umd.lu.thesis.helper.ExcelUtils;
import java.lang.Math.*;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import umd.lu.thesis.simulation.app2000.objects.Person;
import umd.lu.thesis.simulation.app2000.objects.TripType;
import org.apache.log4j.*;

/**
 *
 * @author lousia
 */
public class Formulae {

    private final static Logger sLog = LogManager.getLogger(Formulae.class);

    private static final HashMap<String, Double[]> otherCarMap = initOtherCarMap();
    private static final HashMap<String, Double[]> businessCarMap = initBusinessCarMap();
    private static final HashMap<String, Double[]> airMap = initAirMap();
    private static final HashMap<String, Double[]> trainMap = initTrainMap();
    private static final HashMap<String, Double[]> quarterAirMap = initQuarterAirMap();

    // <msa, {emp, hh}>
    private static final HashMap<Integer, Double[]> msaEmpMap = initMsaEmpMap();

    private static final HashMap<Integer, Integer> zoneMsaMap = initZoneMsaMap();

    // {coef_time, coef_cost1, coef_cost2, coef_cost3}
    private static final double[] coefBusiness = {-0.0569, -0.00615, -0.00388, -0.00125};
    private static final double[] coefPleasure = {-0.0812, -0.00773, -0.00826, -0.00616};
    private static final double[] coefPb = {-0.0899, -0.00779, -0.00733, -0.00621};

    public static final int alt = 378;
    
    private static final int INVALID_QUARTER = -1;

    //  U_d(i) = Coef_lgs * logsum(i) + Coef_Dist * Dist(i) + Coef_MSA* MSA(i) + Coef_lnemp * lnEmp(i)
    //     + Coef_lnhh * lnhh(i) + Coef_lV * Dum_LV1 + Coef_Orlando * Dum_Orlando(i)
    //     + Coef_Sandigo * Dum_Sandigo(i) + Coef_Sanfran * Dum * Dum_Sanfran(i)
    double uD(Person p, int o, int d, TripType type, int quarter) throws InvalidValueException {
        if (o == d) {
            throw new InvalidValueException("Invalid O-D value. O and D should not be equal. (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
        }

        if (type == TripType.BUSINESS) {
            double uCar = uCar(p, o, d, type);
            double uTrain = uTrain(p, o, d, type);
            double uAir = uAir(p, o, d, type);

            double coefLgs = 1.171;
            double coefDist = -0.0001;
            double coefMsa = -0.8;
            double coefLnEmp = 3.066;
            double coefLnHh = -2.314;
            double coefLV = 2.333;
            double coefOrlando = 1.894;
            double coefSandigo = 1.351;
            double coefSanfran = 1.215;

            double uD = coefLgs * logsum(o, d, type, uCar, uAir, uTrain, INVALID_QUARTER)
                    + coefDist * businessCarMap.get(getKey(o, d))[3]
                    + coefMsa * zoneMsaMap.get(d)
                    + coefLnEmp * log(msaEmpMap.get(d)[0])
                    + coefLnHh * log(msaEmpMap.get(d)[1])
                    + coefLV * (d == 201 ? 1 : 0)
                    + coefOrlando * (d == 270 ? 1 : 0)
                    + coefSandigo * (d == 313 ? 1 : 0)
                    + coefSanfran * (d == 314 ? 1 : 0);
//            sLog.info("ud: uD: " + uD + ", pid: " + p.getPid() + ", o: " + o + ", d: " + d);
            return uD;

        } else if (type == TripType.PLEASURE) {
            double uCar = uCar(p, o, d, type);
            double uTrain = uTrain(p, o, d, type);
            double uAir = uAirQuarter(p, o, d, type, quarter);

            double coefLgs = 0.756;
            double coefDist = -0.0002;
            double coefMsa = -1.44;
            double coefLnEmp = 2.52;
            double coefLnHh = -1.895;
            double coefFL = 2.027;
            double coefLV = 2.938;
            double coefSanfran = 1.273;
            double coefSandigo = 1.318;

            return coefLgs * logsum(o, d, type, uCar, uAir, uTrain, INVALID_QUARTER)
                    + coefDist * otherCarMap.get(getKey(o, d))[3]
                    + coefMsa * zoneMsaMap.get(d)
                    + coefLnEmp * log(msaEmpMap.get(d)[0])
                    + coefLnHh * log(msaEmpMap.get(d)[1])
                    + coefFL * (isFlDummy(d) ? 1 : 0)
                    + coefLV * (d == 201 ? 1 : 0)
                    + coefSandigo * (d == 313 ? 1 : 0)
                    + coefSanfran * (d == 314 ? 1 : 0);
        } else if (type == TripType.PERSONAL_BUSINESS) {
            double uCar = uCar(p, o, d, type);
            double uTrain = uTrain(p, o, d, type);
            double uAir = uAir(p, o, d, type);

            double coefLgs = 0.763;
            double coefDist = -0.0004;
            double coefMsa = -0.935;
            double coefLnEmp = 1.942;
            double coefLnHh = -1.261;

            return coefLgs * logsum(o, d, type, uCar, uAir, uTrain, INVALID_QUARTER)
                    + coefDist * otherCarMap.get(getKey(o, d))[3]
                    + coefMsa * zoneMsaMap.get(d)
                    + coefLnEmp * log(msaEmpMap.get(d)[0])
                    + coefLnHh * log(msaEmpMap.get(d)[1]);
        } else {
            throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                    + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
        }
    }

    public double pCar(Person p, int o, int d, TripType type, int quarter) throws InvalidValueException {
        return exp(uCar(p, o, d, type)) / (exp(uCar(p, o, d, type)) + exp(uAirQuarter(p, o, d, type, quarter)) + exp(uTrain(p, o, d, type)));
    }

    public double pAirQuarter(Person p, int o, int d, TripType type, int quarter) throws InvalidValueException {
        return exp(uAirQuarter(p, o, d, type, quarter)) / (exp(uCar(p, o, d, type)) + exp(uAirQuarter(p, o, d, type, quarter)) + exp(uTrain(p, o, d, type)));
    }

    public double pTrain(Person p, int o, int d, TripType type, int quarter) throws InvalidValueException {
        return exp(uTrain(p, o, d, type)) / (exp(uCar(p, o, d, type)) + exp(uAirQuarter(p, o, d, type, quarter)) + exp(uTrain(p, o, d, type)));
    }

    public double pD(Person p, int o, int d, TripType type, int quarter, double sum) throws InvalidValueException {
        double numerator = exp(uD(p, o, d, type, quarter));
        return numerator / sum;
    }

    public double timePx(Person p, TripType type, int quarter) throws InvalidValueException {
        double coefInc1 = 8.67e-06;
        double coefInc2 = 4.02e-06;
        double coefInc3 = 8.09e-07;
        double coefEmp1 = 0.308;
        double coefEmp2 = 0.458;
        double coefEmp3 = 0.236;
        double coefUnEmp1 = 0.062;
        double coefUnEmp2 = 0.374;
        double coefUnEmp3 = 0.019;
        double coefSig1 = 0.137;
        double coefSig2 = 0.28;
        double coefSig3 = 0.124;
        double coefAge1 = 0.010;
        double coefAge2 = 0.010;
        double coefAge3 = 0.006;
        double coefHhoChd1 = 0.312;
        double coefHhoChd2 = 0.468;
        double coefHhoChd3 = 0.42;
        double coefHhChd1 = 0.539;
        double coefHhChd2 = 0.842;
        double coefHhChd3 = 0.825;
        double asc1 = 0.119;
        double asc2 = -0.39;
        double asc3 = -0.005;

        if (type == TripType.PLEASURE) {
            double u1 = asc1 + coefInc1 * p.getHhinc() + coefEmp1 * (p.getDumEmp() == 1 ? 1 : 0)
                    + coefUnEmp1 * (p.getDumEmp() == 2 ? 1 : 0) + coefSig1 * (p.getHhType() == 3 ? 1 : 0)
                    + coefAge1 * p.getAge() + coefHhChd1 * (p.getHhType() == 2 ? 1 : 0)
                    + coefHhoChd1 * (p.getHhType() == 1 ? 1 : 0);
            double u2 = asc2 + coefInc2 * p.getHhinc() + coefEmp2 * (p.getDumEmp() == 1 ? 1 : 0)
                    + coefUnEmp2 * (p.getDumEmp() == 2 ? 1 : 0) + coefSig2 * (p.getHhType() == 3 ? 1 : 0)
                    + coefAge2 * p.getAge() + coefHhChd2 * (p.getHhType() == 2 ? 1 : 0)
                    + coefHhoChd2 * (p.getHhType() == 1 ? 1 : 0);
            double u3 = asc3 + coefInc3 * p.getHhinc() + coefEmp3 * (p.getDumEmp() == 1 ? 1 : 0)
                    + coefUnEmp3 * (p.getDumEmp() == 2 ? 1 : 0) + coefSig3 * (p.getHhType() == 3 ? 1 : 0)
                    + coefAge3 * p.getAge() + coefHhChd3 * (p.getHhType() == 2 ? 1 : 0)
                    + coefHhoChd3 * (p.getHhType() == 1 ? 1 : 0);
            double u4 = 0;

            if (quarter == 1) {
                return exp(u1) / (exp(u1) + exp(u2) + exp(u3) + exp(u4));
            } else if (quarter == 2) {
                return exp(u2) / (exp(u1) + exp(u2) + exp(u3) + exp(u4));
            } else if (quarter == 3) {
                return exp(u3) / (exp(u1) + exp(u2) + exp(u3) + exp(u4));
            } else {
                return exp(u4) / (exp(u1) + exp(u2) + exp(u3) + exp(u4));
            }
        } else {
            throw new InvalidValueException("Invalid tripType. Args: quarter: " + quarter + ", tripTpye: " + type.name() + ", Person: PID=" + p.getPid());
        }
    }

    public double timePx(int quarter, int o, int d, TripType type, Person p) throws InvalidValueException {
        if (type == TripType.BUSINESS || type == TripType.PERSONAL_BUSINESS) {
            double[] logsums = new double[5];
            double uCar = uCar(p, o, d, type);
            double uTrain = uTrain(p, o, d, type);
            for (int q = 1; q <= 4; q++) {
                double uAir = uAirQuarter(p, o, d, type, q);
                logsums[q] = logsum(o, d, type, uCar, uAir, uTrain, quarter);
            }
            double u1 = 0;
            double u2 = 0;
            double u3 = 0;
            double u4 = 0;

            if (type == TripType.BUSINESS) {
                double coefLgs = 0.062;
                double coefInc1 = 9.67e-06;
                double coefInc2 = 2.27e-06;
                double coefInc3 = 1.54e-06;
                double coefEmp1 = 0.519;
                double coefEmp2 = 0.251;
                double coefEmp3 = -0.205;
                double coefMale1 = 0.051;
                double coefMale2 = -0.252;
                double coefMale3 = -0.055;
                double coefAge1 = 0.02;
                double coefAge2 = 0.02;
                double coefAge3 = 0.009;
                double asc1 = 0.509;
                double asc2 = 0.443;
                double asc3 = 0.49;

                u1 = asc1 + coefLgs * logsums[1] + coefInc1 * p.getHhinc()
                        + coefEmp1 * (p.getDumEmp() == 1 ? 1 : 0)
                        + coefMale1 * (p.getSex() == 1 ? 1 : 0)
                        + coefAge1 * p.getAge();
                u2 = asc2 + coefLgs * logsums[2] + coefInc2 * p.getHhinc()
                        + coefEmp2 * (p.getDumEmp() == 1 ? 1 : 0)
                        + coefMale2 * (p.getSex() == 1 ? 1 : 0)
                        + coefAge2 * p.getAge();
                u3 = asc3 + coefLgs * logsums[3] + coefInc3 * p.getHhinc()
                        + coefEmp3 * (p.getDumEmp() == 1 ? 1 : 0)
                        + coefMale3 * (p.getSex() == 1 ? 1 : 0)
                        + coefAge3 * p.getAge();
                u4 = coefLgs * logsums[4];
            } else if (type == TripType.PERSONAL_BUSINESS) {
                double coefLgs = 0.01;
                double coefInc1 = 1.0e-05;
                double coefInc2 = 5.64e-06;
                double coefInc3 = -6.65e-07;
                double coefEmp1 = 0.174;
                double coefEmp2 = 0.322;
                double coefEmp3 = 0.327;
                double coefMale1 = -0.083;
                double coefMale2 = -0.21;
                double coefMale3 = -0.110;
                double coefAge1 = 0.011;
                double coefAge2 = 0.016;
                double coefAge3 = 0.006;
                double coefHhChd1 = -0.01;
                double coefHhChd2 = 0.068;
                double coefHhChd3 = -0.249;
                double asc1 = 0.568;
                double asc2 = 0.224;
                double asc3 = 0.524;

                u1 = asc1 + coefLgs * logsums[1] + coefInc1 * p.getHhinc()
                        + coefEmp1 * (p.getDumEmp() == 1 ? 1 : 0)
                        + coefMale1 * (p.getSex() == 1 ? 1 : 0)
                        + coefAge1 * p.getAge()
                        + coefHhChd1 * (p.getHhType() == 2 ? 1 : 0);
                u2 = asc2 + coefLgs * logsums[2] + coefInc2 * p.getHhinc()
                        + coefEmp2 * (p.getDumEmp() == 1 ? 1 : 0)
                        + coefMale2 * (p.getSex() == 1 ? 1 : 0)
                        + coefAge2 * p.getAge()
                        + coefHhChd2 * (p.getHhType() == 2 ? 1 : 0);
                u3 = asc3 + coefLgs * logsums[3] + coefInc3 * p.getHhinc()
                        + coefEmp3 * (p.getDumEmp() == 1 ? 1 : 0)
                        + coefMale3 * (p.getSex() == 1 ? 1 : 0)
                        + coefAge3 * p.getAge()
                        + coefHhChd3 * (p.getHhType() == 2 ? 1 : 0);
                u4 = coefLgs * logsums[4];

            }

            if (quarter == 1) {
                return exp(u1) / (exp(u1) + exp(u2) + exp(u3) + exp(u4));
            } else if (quarter == 2) {
                return exp(u2) / (exp(u1) + exp(u2) + exp(u3) + exp(u4));
            } else if (quarter == 3) {
                return exp(u3) / (exp(u1) + exp(u2) + exp(u3) + exp(u4));
            } else {
                return exp(u4) / (exp(u1) + exp(u2) + exp(u3) + exp(u4));
            }
        } else {
            throw new InvalidValueException("Invalid tripType. Args: quarter: " + quarter + ", o: " + o + ", d: " + d + ", tripTpye: " + type.name() + ", Person: PID=" + p.getPid());
        }
    }

    double logsum(int o, int d, TripType type, double uCar, double uAir, double uTrain, int quarter) throws InvalidValueException {
        if (type == TripType.BUSINESS) {
            if ((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) == null : quarterAirMap.get(getKey(o, d)) == null)
                    && trainMap.get(getKey(o, d)) != null
                    && businessCarMap.get(getKey(o, d)) != null) {
//                sLog.info("=== uCar: " + uCar + ", uTrain: " + uTrain);
                return log(exp(uCar) + exp(uTrain));
            } else if ((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) != null : quarterAirMap.get(getKey(o, d)) != null)
                    && trainMap.get(getKey(o, d)) == null
                    && businessCarMap.get(getKey(o, d)) != null) {
//                sLog.info("=== uCar: " + uCar + ", uAir: " + uAir);
                return log(exp(uCar) + exp(uAir));
            } else if ((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) == null : quarterAirMap.get(getKey(o, d)) == null)
                    && trainMap.get(getKey(o, d)) == null
                    && businessCarMap.get(getKey(o, d)) != null) {
//                sLog.info("=== uCar: " + uCar);
                return log(exp(uCar));
            } else if ((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) != null : quarterAirMap.get(getKey(o, d)) != null)
                    && trainMap.get(getKey(o, d)) != null
                    && businessCarMap.get(getKey(o, d)) != null) {
//                sLog.info("=== uCar: " + uCar + ", uTrain: " + uTrain + ", uAir: " + uAir);
                return log(exp(uCar) + exp(uTrain) + exp(uAir));
            } else {
                throw new InvalidValueException("Cannot find car time/cost/stopnights/etc. businessCarMap.get(Integer.toString(o) + \"-\" + Integer.toString(d) returns NULL. (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", uCar: " + uCar + ", uAir: " + uAir + ", uTrain: " + uTrain + ")");
            }
        } else if (type == TripType.PERSONAL_BUSINESS || type == TripType.PLEASURE) {
            if ((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) == null : quarterAirMap.get(getKey(o, d)) == null)
                    && trainMap.get(getKey(o, d)) != null
                    && otherCarMap.get(getKey(o, d)) != null) {
                return log(exp(uCar) + exp(uTrain));
            } else if ((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) != null : quarterAirMap.get(getKey(o, d)) != null)
                    && trainMap.get(getKey(o, d)) == null
                    && otherCarMap.get(getKey(o, d)) != null) {
                return log(exp(uCar) + exp(uAir));
            } else if ((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) == null : quarterAirMap.get(getKey(o, d)) == null)
                    && trainMap.get(getKey(o, d)) == null
                    && otherCarMap.get(getKey(o, d)) != null) {
                return log(exp(uCar));
            } else if ((quarter == INVALID_QUARTER ? airMap.get(getKey(o, d)) != null : quarterAirMap.get(getKey(o, d)) != null)
                    && trainMap.get(getKey(o, d)) != null
                    && otherCarMap.get(getKey(o, d)) != null) {
                return log(exp(uCar) + exp(uTrain) + exp(uAir));
            } else {
                throw new InvalidValueException("Cannot find car time/cost/stopnights/etc. otherCarMap.get(Integer.toString(o) + \"-\" + Integer.toString(d) returns NULL. (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", uCar: " + uCar + ", uAir: " + uAir + ", uTrain: " + uTrain + ")");
            }
        } else {
            throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                    + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ")");
        }
    }

    //  Coef_Cost1*LowIncome*Tour_CarCost + Coef_Cost2*MedIncome*Tour_CarCost 
    //      + Coef_Cost3*highIncome*Tour_CarCost + Coef_Time*Tour_Cartime
    double uCar(Person p, int o, int d, TripType type) throws InvalidValueException {
        if (p.getIncLevel() < 1 || p.getIncLevel() > 3) {
            throw new InvalidValueException("Invalid incLevel: " + p.getIncLevel()
                    + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
        }
        try {
            if (type == TripType.BUSINESS) {
                return coefBusiness[p.getIncLevel()] * tourCarCost(p.getIncLevel(), o, d, type)
                        + coefBusiness[0] * tourCarTime(o, d, type);
            } else if (type == TripType.PLEASURE) {
                return coefPleasure[p.getIncLevel()] * tourCarCost(p.getIncLevel(), o, d, type)
                        + coefPleasure[0] * tourCarTime(o, d, type);
            } else if (type == TripType.PERSONAL_BUSINESS) {
                return coefPb[p.getIncLevel()] * tourCarCost(p.getIncLevel(), o, d, type)
                        + coefPb[0] * tourCarTime(o, d, type);
            } else {
                throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                        + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
            }
        } catch (NullPointerException ex) {
            // uCar not found
            sLog.debug("NullPointerException: uCar not found. Args: p.getPid: " + p.getPid() + ", o: " + o + ", d: " + d + ", TripType: " + type.name() + ". ");
            return Double.NEGATIVE_INFINITY;
        }
    }

    //  U_Train = Coef_Cost1*LowIncome*Tour_TrainCost + Coef_Cost2*MedIncome*Tour_TrainCost 
    //      + Coef_Cost3*highIncome*Tour_TrainCost + Coef_Time*Tour_Traintime
    double uTrain(Person p, int o, int d, TripType type) throws InvalidValueException {
        if (p.getIncLevel() < 1 || p.getIncLevel() > 3) {
            throw new InvalidValueException("Invalid incLevel: " + p.getIncLevel()
                    + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
        }
        try {
            if (type == TripType.BUSINESS) {
                return coefBusiness[p.getIncLevel()] * tourTrainCost(o, d)
                        + coefBusiness[0] * tourTrainTime(o, d);
            } else if (type == TripType.PLEASURE) {
                return coefPleasure[p.getIncLevel()] * tourTrainCost(o, d)
                        + coefPleasure[0] * tourTrainTime(o, d);
            } else if (type == TripType.PERSONAL_BUSINESS) {
                return coefPb[p.getIncLevel()] * tourTrainCost(o, d)
                        + coefPb[0] * tourTrainTime(o, d);
            } else {
                throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                        + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
            }
        } catch (NullPointerException ex) {
            // uCar not found
            sLog.debug("NullPointerException: uTrain not found. Args: p.getPid: " + p.getPid() + ", o: " + o + ", d: " + d + ", TripType: " + type.name() + ". ");
            return Double.NEGATIVE_INFINITY;
        }
    }

    //  U_Air = Coef_Cost1*LowIncome*Tour_AirCost + Coef_Cost2*MedIncome*Tour_AirCost 
    //    + Coef_Cost3*highIncome*Tour_AirCost + Coef_Time*Tour_Airtime
    double uAir(Person p, int o, int d, TripType type) throws InvalidValueException {
        if (p.getIncLevel() < 1 || p.getIncLevel() > 3) {
            throw new InvalidValueException("Invalid incLevel: " + p.getIncLevel()
                    + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
        }
        try {
            if (type == TripType.BUSINESS) {
                return coefBusiness[p.getIncLevel()] * tourAirCost(o, d)
                        + coefBusiness[0] * tourAirTime(o, d);
            } else if (type == TripType.PLEASURE) {
                return coefPleasure[p.getIncLevel()] * tourAirCost(o, d)
                        + coefPleasure[0] * tourAirTime(o, d);
            } else if (type == TripType.PERSONAL_BUSINESS) {
                return coefPb[p.getIncLevel()] * tourAirCost(o, d)
                        + coefPb[0] * tourAirTime(o, d);
            } else {
                throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                        + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
            }
        } catch (NullPointerException ex) {
            // uCar not found
            sLog.debug("NullPointerException: uAir not found. Args: p.getPid: " + p.getPid() + ", o: " + o + ", d: " + d + ", TripType: " + type.name() + ". ");
            return Double.NEGATIVE_INFINITY;
        }
    }

    double uAirQuarter(Person p, int o, int d, TripType type, int quarter) throws InvalidValueException {
        if (p.getIncLevel() < 1 || p.getIncLevel() > 3) {
            throw new InvalidValueException("Invalid incLevel: " + p.getIncLevel()
                    + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
        }
        try {
            if (type == TripType.BUSINESS) {
                return coefBusiness[p.getIncLevel()] * tourAirCostQuarter(o, d, quarter)
                        + coefBusiness[0] * tourAirTime(o, d);
            } else if (type == TripType.PLEASURE) {
                return coefPleasure[p.getIncLevel()] * tourAirCostQuarter(o, d, quarter)
                        + coefPleasure[0] * tourAirTime(o, d);
            } else if (type == TripType.PERSONAL_BUSINESS) {
                return coefPb[p.getIncLevel()] * tourAirCostQuarter(o, d, quarter)
                        + coefPb[0] * tourAirTime(o, d);
            } else {
                throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                        + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + p.getIncLevel() + ")");
            }
        } catch (NullPointerException ex) {
            // uCar not found
            sLog.debug("NullPointerException: uAirQuarter not found. Args: p.getPid: " + p.getPid() + ", o: " + o + ", d: " + d + ", TripType: " + type.name() + ". ");
            return Double.NEGATIVE_INFINITY;
        }
    }

    //  Tour_AirCost= AirFare*2
    double tourAirCost(int o, int d) {
        return airMap.get(getKey(o, d))[1] * 2;
    }

    double tourAirCostQuarter(int o, int d, int quarter) {
        return quarterAirMap.get(getKey(o, d))[quarter] * 2;
    }

    //  Tour_AirTime=AirTime*2
    double tourAirTime(int o, int d) {
        return airMap.get(getKey(o, d))[0] * 2;
    }

    //  Tour_CarCost=(DrvCost/weight + LodgeCost)*2
    //  (Drvcost from ODSKIM_Car_Business.csv and ODSKIM_Car_PPB.csv)
    double tourCarCost(int incLevel, int o, int d, TripType type) throws InvalidValueException {
        double driveCost = 0.0;
        int weight = 0;
        if (type == TripType.BUSINESS) {
            driveCost = businessCarMap.get(getKey(o, d))[1];
        } else if (type == TripType.PLEASURE || type == TripType.PERSONAL_BUSINESS) {
            driveCost = otherCarMap.get(getKey(o, d))[1];
        } else {
            throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                    + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", incLevel: " + incLevel + ")");
        }
        return (driveCost / weight(type) + lodgeCost(incLevel, o, d, type)) * 2;
    }

    double tourCarTime(int o, int d, TripType type) throws InvalidValueException {
        if (type == TripType.BUSINESS) {
            return businessCarMap.get(getKey(o, d))[0] * 2;
        } else if (type == TripType.PLEASURE || type == TripType.PERSONAL_BUSINESS) {
            return otherCarMap.get(getKey(o, d))[0] * 2;
        } else {
            throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                    + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ")");
        }
    }

    //  Cost_2000*2  (Cost_2000 from OD_SKIM_Train.csv)
    double tourTrainCost(int o, int d) {
        return trainMap.get(getKey(o, d))[1] * 2;
    }

    //  Tour_TrainTime=Time*2        (Time from OD_SKIM_Train.csv)
    double tourTrainTime(int o, int d) {
        return trainMap.get(getKey(o, d))[0] * 2;
    }

    //  LodgeCost = Unit_Lodge_Cost * Stop_nigt
    double lodgeCost(int incLevel, int o, int d, TripType type) throws InvalidValueException {
        return (double) unitLodgeCost(incLevel, type) * stopNights(o, d, type);
    }

    // Stop_nigt from ODSKIM_Car_Business.csv and ODSKIM_Car_PPB.csv
    double stopNights(int o, int d, TripType type) throws InvalidValueException {
        // o => column C, d => column D, stopNight => column H
        if (type == TripType.BUSINESS) {
            return businessCarMap.get(getKey(o, d))[2];
        } else if (type == TripType.PLEASURE || type == TripType.PERSONAL_BUSINESS) {
            return otherCarMap.get(getKey(o, d))[2];
        } else {
            throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                    + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ")");
        }
    }

    int unitLodgeCost(int incLevel, TripType type) throws InvalidValueException {
        if (type == TripType.BUSINESS) {
            if (incLevel == 1) {
                return 70;
            } else if (incLevel == 2) {
                return 90;
            } else if (incLevel == 3) {
                return 110;
            } else {
                throw new InvalidValueException("Invalid incLevel: " + incLevel
                        + ". (Args: incLevel = " + incLevel + ", tripPurpose: " + type + ")");
            }
        } else if (type == TripType.PLEASURE || type == TripType.PERSONAL_BUSINESS) {
            if (incLevel == 1) {
                return 30;
            } else if (incLevel == 2) {
                return 50;
            } else if (incLevel == 3) {
                return 70;
            } else {
                throw new InvalidValueException("Invalid incLevel: " + incLevel
                        + ". (Args: incLevel = " + incLevel + ", tripPurpose: " + type.name() + ")");
            }
        } else {

            throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                    + ". (Args: incLevel = " + incLevel + ", tripPurpose: " + type.name() + ")");

        }
    }

    double weight(TripType type) throws InvalidValueException {
        if (type == TripType.BUSINESS) {
            return 1.0;
        } else if (type == TripType.PLEASURE || type == TripType.PERSONAL_BUSINESS) {
            return 2.0;
        } else {
            throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                    + ". (Args: tripPurpose: " + type.name() + ")");
        }
    }

    private static HashMap<String, Double[]> initOtherCarMap() {
        sLog.info("----Initialize otherCarMap.");
        HashMap<String, Double[]> ocm = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.odskim_car_other"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("MSA_A")) {
                    String key = ExcelUtils.getColumnValue(3, line) + "-" + ExcelUtils.getColumnValue(4, line);
                    String carTime = ExcelUtils.getColumnValue(6, line);
                    String driveCost = ExcelUtils.getColumnValue(7, line);
                    String stopNights = ExcelUtils.getColumnValue(8, line);
                    String dist = ExcelUtils.getColumnValue(5, line);
                    Double[] value = {
                        Double.parseDouble(carTime),
                        Double.parseDouble(driveCost),
                        Double.parseDouble(stopNights),
                        Double.parseDouble(dist)
                    };
                    ocm.put(key, value);
                }
            }
            br.close();
        } catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        } catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return ocm;
    }

    private static HashMap<String, Double[]> initBusinessCarMap() {
        sLog.info("----Initialize businessCarMap.");
        HashMap<String, Double[]> bcm = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.odskim_car_business"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("MSA_A")) {
                    String key = ExcelUtils.getColumnValue(3, line) + "-" + ExcelUtils.getColumnValue(4, line);
                    String carTime = ExcelUtils.getColumnValue(6, line);
                    String driveCost = ExcelUtils.getColumnValue(7, line);
                    String stopNights = ExcelUtils.getColumnValue(8, line);
                    String dist = ExcelUtils.getColumnValue(5, line);
                    Double[] value = {
                        Double.parseDouble(carTime),
                        Double.parseDouble(driveCost),
                        Double.parseDouble(stopNights),
                        Double.parseDouble(dist)
                    };
                    bcm.put(key, value);
                }
            }
            br.close();
        } catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        } catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return bcm;
    }

    private static HashMap<String, Double[]> initAirMap() {
        sLog.info("----Initialize airMap.");
        HashMap<String, Double[]> am = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.air_skim_avg"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("MSA_A")) {
                    String key = ExcelUtils.getColumnValue(3, line) + "-" + ExcelUtils.getColumnValue(4, line);
                    String airTime = ExcelUtils.getColumnValue(7, line);
                    String airCost = ExcelUtils.getColumnValue(8, line);
                    Double[] value = {
                        Double.parseDouble(airTime),
                        Double.parseDouble(airCost)
                    };
                    am.put(key, value);
                }
            }
            br.close();
        } catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        } catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return am;
    }

    private static HashMap<String, Double[]> initTrainMap() {
        sLog.info("----Initialize trainMap.");
        HashMap<String, Double[]> tm = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.odskim_train"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("A_MSA")) {
                    String key = ExcelUtils.getColumnValue(3, line) + "-" + ExcelUtils.getColumnValue(4, line);
                    String trainTime = ExcelUtils.getColumnValue(5, line);
                    String trainCost = ExcelUtils.getColumnValue(6, line);
                    Double[] value = {
                        Double.parseDouble(trainTime),
                        Double.parseDouble(trainCost)
                    };
                    tm.put(key, value);
                }
            }
            br.close();
        } catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        } catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return tm;
    }

    private static HashMap<Integer, Integer> initZoneMsaMap() {
        sLog.info("----Initialize zoneMsaMap.");
        HashMap<Integer, Integer> zoneMsa = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.zone_msa"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("alt")) {
                    Integer key = Integer.parseInt(ExcelUtils.getColumnValue(1, line));
                    Integer value = Integer.parseInt(ExcelUtils.getColumnValue(2, line));
                    zoneMsa.put(key, value);
                }
            }
            br.close();
        } catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        } catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return zoneMsa;
    }

    private static HashMap<Integer, Double[]> initMsaEmpMap() {
        sLog.info("----Initialize msaEmpMap.");
        HashMap<Integer, Double[]> mem = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.msa_emp"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("MSA_2000")) {
                    Integer key = Integer.parseInt(ExcelUtils.getColumnValue(2, line));
                    Double emp = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    Double hh = Double.parseDouble(ExcelUtils.getColumnValue(5, line));
                    Double[] value = {emp, hh};
                    mem.put(key, value);
                }
            }
            br.close();
        } catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        } catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return mem;
    }

    private static HashMap<String, Double[]> initQuarterAirMap() {
        sLog.info("----Initialize quarterAirMap.");
        HashMap<String, Double[]> qam = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.msafare_1"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("quarter")) {
                    String key = ExcelUtils.getColumnValue(2, line) + "-" + ExcelUtils.getColumnValue(3, line);
                    Double fare = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    Double[] value = new Double[5];
                    value[1] = fare;
                    qam.put(key, value);
                }
            }
            br.close();
        } catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        } catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }

        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.msafare_2"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("quarter")) {
                    String key = ExcelUtils.getColumnValue(2, line) + "-" + ExcelUtils.getColumnValue(3, line);
                    Double[] tmp = new Double[5];
                    tmp[2] = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    qam.put(key, tmp);
                }
            }
            br.close();
        } catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        } catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }

        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.msafare_3"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("quarter")) {
                    String key = ExcelUtils.getColumnValue(2, line) + "-" + ExcelUtils.getColumnValue(3, line);
                    Double[] tmp = new Double[5];
                    tmp[3] = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    qam.put(key, tmp);
                }
            }
            br.close();
        } catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        } catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }

        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.msafare_4"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("quarter")) {
                    String key = ExcelUtils.getColumnValue(2, line) + "-" + ExcelUtils.getColumnValue(3, line);
                    Double[] tmp = new Double[5];
                    tmp[4] = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    qam.put(key, tmp);
                }
            }
            br.close();
        } catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        } catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }

        return qam;
    }

    private String getKey(int o, int d) {
//        if (o <= d) {
        return Integer.toString(o) + "-" + Integer.toString(d);
//        } else {
//            return Integer.toString(d) + "-" + Integer.toString(o);
//        }
    }

    private boolean isFlDummy(int d) {
        if (d == 55 || d == 90 || d == 124 || d == 126 || d == 171 || d == 194
                || d == 229 || d == 270 || d == 274 || d == 321 || d == 343
                || d == 344 || d == 365) {
            return true;
        }
        return false;
    }

    public double expSum(Person p, int o, TripType type, int quarter) throws InvalidValueException {
        double sum = 0.0;
        for (int i = 1; i <= alt; i++) {
            if (o != i) {
                sum += exp(uD(p, o, i, type, quarter));
            }
        }
        return sum;
    }
}
