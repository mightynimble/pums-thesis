/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.pums2010.math.Math;
import umd.lu.thesis.pums2010.objects.ModeChoice;
import umd.lu.thesis.pums2010.objects.Person2010;
import umd.lu.thesis.pums2010.objects.Quarter;
import umd.lu.thesis.pums2010.objects.TravelMode;
import umd.lu.thesis.pums2010.objects.Trip;
import umd.lu.thesis.simulation.app2000.objects.TripType;

/**
 *
 * @author Home
 */
public class NationalTravelDemand {

    private final static Logger sLog = LogManager.getLogger(NationalTravelDemand.class);

    private static final int INVALID_QUARTER = -1;

    private static final int bulkSize = 100000;

    private static final int dbMaxBatchSize = 10000;

    private static List<Trip> tripBuffer;

    private static PreparedStatement pstmt;

    private static int startRow;  /* inclusive */

    private static int endRow;    /* exclusive */

    private static int currentRow;

    private static final int NIL_INT = Integer.MIN_VALUE;

    private static Pums2010DAOImpl pumsDao;

    private Math math;

    private HashMap<String, HashMap<String, Integer>> results;

    private final HashMap<Integer, Integer[]> zoneIdMap;

    private final HashMap<Integer, Integer> altMsaMap;

    private UniformRealDistribution rand;

    private static int[][] toursByPurposeAndStopFrequencyIB;
    private static int[][] toursByPurposeAndStopFrequencyOB;
    private static int[][] toursByPurposeAndModeChoice;
    private static int[][] toursByModeChoiceAndDest;
    private static HashMap<Integer, Integer[]> sortedODDistMap;
    
    private static long[] tripStats;
    private static final int TRIP_STATS_COLUMNS = 27;

    public NationalTravelDemand(Pums2010DAOImpl dao) {
        pumsDao = dao;
        altMsaMap = new HashMap<>();
        zoneIdMap = initZoneId();
        math = new Math();
        results = new HashMap<>();
        rand = new UniformRealDistribution();
        toursByPurposeAndStopFrequencyIB = new int[TripType.itemCount - 1][5];
        toursByPurposeAndStopFrequencyOB = new int[TripType.itemCount - 1][5];
        toursByPurposeAndModeChoice = new int[ModeChoice.itemCount][TripType.itemCount - 1];
        toursByModeChoiceAndDest = new int[ModeChoice.itemCount][Math.alt];
        sortedODDistMap = math.sortODDist();
        pstmt = null;
        tripBuffer = new ArrayList<>();
        tripStats = new long[TRIP_STATS_COLUMNS];
    }

    public NationalTravelDemand() {
        altMsaMap = new HashMap<>();
        zoneIdMap = initZoneId();
        pumsDao = new Pums2010DAOImpl();
        math = new Math();
        results = new HashMap<>();
        rand = new UniformRealDistribution();
        toursByPurposeAndStopFrequencyIB = new int[TripType.itemCount - 1][5];
        toursByPurposeAndStopFrequencyOB = new int[TripType.itemCount - 1][5];
        toursByPurposeAndModeChoice = new int[ModeChoice.itemCount][TripType.itemCount - 1];
        toursByModeChoiceAndDest = new int[ModeChoice.itemCount][Math.alt];
        sortedODDistMap = math.sortODDist();
        pstmt = null;
        tripBuffer = new ArrayList<>();
        tripStats = new long[TRIP_STATS_COLUMNS];
    }

    public void run(int start, int end) {
        startRow = start;
        endRow = end;
        currentRow = start;
        sLog.info("NationalTravelDemand Simulation Started. Start Row: " + startRow + ", End Row: " + endRow + ", bulkSize: " + bulkSize);

        math.preCalculateLogsum();

        for (int m = 0; m < TravelMode.itemCount; m++) {
            for (int q = 0; q < Quarter.itemCount; q++) {
                for (int t = 0; t < TripType.itemCount; t++) {
                    results.put(TravelMode.values()[m] + "-"
                            + Quarter.values()[q] + "-"
                            + TripType.values()[t], new HashMap<String, Integer>());
                }
            }
        }

        int rowCount = pumsDao.getTotalRecordsByMaxId("PERSON_HOUSEHOLD_EXPANDED");
        sLog.info("Total rows: " + rowCount);

        currentRow = startRow;
        while (currentRow < endRow) {
            batchProcessRecord();
            sLog.info("Batch completed. Current row: " + currentRow);
        }
        sLog.info("Completed processing records. CurrentRow: " + currentRow);

        outputResults();

        sLog.info("NationalTravelDemand Simulation Stopped.");
    }

    private void batchProcessRecord() {

        List<Person2010> pList = pumsDao.getPerson2010(currentRow, currentRow + bulkSize);
        for (Person2010 p : pList) {
            currentRow = p.getPid();
            sLog.debug("PID: " + currentRow);
            if (currentRow >= endRow) {
                break;
            }

            int o = lookupAlt(p);
            if (o == -1) {
                sLog.error("  -ERROR- MSAPMSA " + p.getMsapmsa() + " can't be mapped to a zone. Skipped. (p.id=" + p.getPid() + ")");
                continue;
            }

            if (p.getAge() < 18) {
                continue;
            }
            
            /**
             * For each BUSINESS tour
             */
            sLog.debug("Simulate BUSINESS tours.");
            runSimulationAndPopulateResults(p, o, TripType.BUSINESS);

            /**
             * For each PB tour
             */
            sLog.debug("Simulate PERSONAL_BUSINESS tours.");
            runSimulationAndPopulateResults(p, o, TripType.PERSONAL_BUSINESS);

            /**
             * For each PLEASURE tour
             */
            sLog.debug("Simulate PLEASURE tours.");
            runSimulationAndPopulateResults(p, o, TripType.PLEASURE);
        }
    }

    private Integer findDestinationChoice(Person2010 p, int o, TripType tripType, int quarter) {
        sLog.debug("Find Dest Choice - p: " + p.getPid() + ", o: " + o
                + ", Trip Purpose:  " + tripType.name() + ", quarter: " + quarter);
        Map<Double, List<Integer>> pMap = new HashMap<>();
        List<Double> pList = new ArrayList<>();
        double uDExpSum = math.destUDExpSum(p, o, tripType, quarter);
        sLog.debug("    destUDExpSum: " + uDExpSum);
        for (int d = 1; d <= Math.alt; d++) {
            double pU = math.destUDExp(p, o, d, tripType, quarter) / uDExpSum;
//            sLog.debug("    destP[" + d + "]: " + pU);
            pList.add(pU);
            if (pMap.get(pU) != null) {
                List tmp = pMap.get(pU);
                tmp.add(d);
            } else {
                List<Integer> tmp = new ArrayList<>();
                tmp.add(d);
                pMap.put(pU, tmp);
            }
        }

        int dest = math.MonteCarloMethod(pList, pMap, rand.sample());

//        if (dest ==1) {
//            sLog.info("--- destChoice: dest == 1, o: " + o);
//        }
        // For statistical purpose. Note that dest is from 1 to 380. Hence the minus 1.
//        toursByDestination[dest - 1] ++;
        return dest;

    }

    private Integer findToY(Person2010 p, int o, int d, TripType type) {
        sLog.debug("Find Time of Year - p: " + p.getPid() + ", o: " + o
                + ", d: " + d + ", Trip Purpose:  " + type.name());
        Map<Double, List<Integer>> pMap = new HashMap<>();
        List<Double> pList = new ArrayList<>();
        if (type == TripType.BUSINESS || type == TripType.PLEASURE) {
            double uDExpSum = math.toyUDExpSum(p, o, d, type);
            sLog.debug("    destUDExpSum: " + uDExpSum);
            for (int q = 1; q <= 4; q++) {
                double pU = math.toyUDExp(p, o, d, type, q) / uDExpSum;
                sLog.debug("    toyP[" + q + "]: " + pU);
                pList.add(pU);
                if (pMap.get(pU) != null) {
                    List tmp = pMap.get(pU);
                    tmp.add(q);
                } else {
                    List tmp = new ArrayList<>();
                    tmp.add(q);
                    pMap.put(pU, tmp);
                }
            }
        } else {
            pList.add(0.228);
            List<Integer> tmp1 = new ArrayList<>();
            tmp1.add(1);
            pMap.put(0.228, tmp1);
            sLog.debug("    toyP[1]: " + 0.228);
            pList.add(0.297);
            List<Integer> tmp2 = new ArrayList<>();
            tmp2.add(2);
            pMap.put(0.297, tmp2);
            sLog.debug("    toyP[2]: " + 0.297);
            List<Integer> tmp3 = new ArrayList<>();
            tmp3.add(3);
            pList.add(0.278);
            pMap.put(0.278, tmp3);
            sLog.debug("    toyP[3]: " + 0.278);
            List<Integer> tmp4 = new ArrayList<>();
            tmp4.add(4);
            pList.add(0.197);
            pMap.put(0.197, tmp4);
            sLog.debug("    toyP[4]: " + 0.197);
        }

        // monte carlo method
        int toy = math.MonteCarloMethod(pList, pMap, rand.sample());

        // For statistical purpose.
//        toursByToY[toy] ++;
        return toy;
    }

    protected Integer findTourDuration(Person2010 p, int d, TripType tripType, int toy) {
        sLog.debug("Find Trip Duration - p: " + p.getPid() + ", d: " + d
                + ", Trip Purpose:  " + tripType.name() + ", toy: " + toy);

        Map<Double, List<Integer>> pMap = new HashMap<>();
        List<Double> pList = new ArrayList<>();

        if (tripType == TripType.PLEASURE) {
            for (int t = 1; t <= 31; t++) {
                double pSt = math.pStStatic[t - 1];
                pList.add(pSt);
                List tmp = new ArrayList<>();
                tmp.add(t);
                pMap.put(pSt, tmp);
            }

            Collections.sort(pList);
//            double r = rand.sample();
//            double tempSt = 0.0;
//            for (int t = 1; t <= 31; t++) {
//                if (tempSt < r && r <= tempSt + pList.get(t - 1)) {
//                    return t; 
//                }
//                tempSt += pList.get(t - 1);
//            }
//            
            return math.MonteCarloMethod(pList, pMap, rand.sample());
        } else {
            // calculate p
            for (int t = 1; t <= 31; t++) {
                double pSt = 1 - math.tdST(p, d, toy, t, tripType);
                pList.add(pSt);
                List tmp = new ArrayList<>();
                tmp.add(t);
                pMap.put(pSt, tmp);
            }

            Collections.sort(pList);
            double r = rand.sample();
            double tempSt = 0.0;
            for (int t = 1; t <= 31; t++) {
                if (tempSt < r && r <= pList.get(t - 1)) {
                    return t;
                }
                tempSt = pList.get(t - 1);
            }
        }

        return 31;

//        return math.MonteCarloMethod(pList, pMap, rand.sample());
    }

    private Integer findTravelPartySize(Person2010 p, int d, TripType type) {
        sLog.debug("Find Party Size - p: " + p.getPid() + ", d: " + d
                + ", Trip Purpose:  " + type.name());
        double tspU1Exp = math.tpsUtpExp(p, d, 1, type);
        sLog.debug("    tspU1Exp: " + tspU1Exp);
        double tspU2Exp = math.tpsUtpExp(p, d, 2, type);
        sLog.debug("    tspU2Exp: " + tspU2Exp);
        double tspU3Exp = math.tpsUtpExp(p, d, 3, type);
        sLog.debug("    tspU3Exp: " + tspU3Exp);
        double tspU4Exp = math.tpsUtpExp(p, d, 4, type);
        sLog.debug("    tspU4Exp: " + tspU4Exp);
        double expSum = tspU1Exp + tspU2Exp + tspU3Exp + tspU4Exp;
        sLog.debug("    sum: " + expSum);
        double p1 = tspU1Exp / expSum;
        sLog.debug("    p1: " + p1);
        double p2 = tspU2Exp / expSum;
        sLog.debug("    p2: " + p2);
        double p3 = tspU3Exp / expSum;
        sLog.debug("    p3: " + p3);
        double p4 = tspU4Exp / expSum;
        sLog.debug("    p4: " + p4);

        Map<Double, List<Integer>> pMap = new HashMap<>();
        List<Double> pList = new ArrayList<>();

        pList.add(p1);
        List<Integer> tmp1 = new ArrayList<>();
        tmp1.add(1);
        pMap.put(p1, tmp1);
        pList.add(p2);
        List<Integer> tmp2 = new ArrayList<>();
        tmp2.add(2);
        pMap.put(p2, tmp2);
        pList.add(p3);
        List<Integer> tmp3 = new ArrayList<>();
        tmp3.add(3);
        pMap.put(p3, tmp3);
        pList.add(p4);
        List<Integer> tmp4 = new ArrayList<>();
        tmp4.add(4);
        pMap.put(p4, tmp4);

        // monte carlo method
        return math.MonteCarloMethod(pList, pMap, rand.sample());
    }

    private ModeChoice findModeChoice(Person2010 p, int d, TripType type, int toy, int days) {
        sLog.debug("Find Mode Choice - p: " + p.getPid() + ", d: " + d
                + ", Trip Purpose:  " + type.name() + ", toy: " + toy);
        double uCarExp = math.mcUcarExp(p, type, d, lookupAlt(p), days, false);
        sLog.debug("    uCarExp: " + uCarExp);
        double uAirExp = math.mcUairExp(p, type, d, lookupAlt(p), toy);
        sLog.debug("    uAirExp: " + uAirExp);
        double uTrainExp = math.mcUtrainExp(p, type, d, lookupAlt(p), days, false);
        sLog.debug("    uTrainExp: " + uTrainExp);
        double sum = uCarExp + uAirExp + uTrainExp;
        sLog.debug("    sum: " + sum);

        if (sum == 0.0) {
//            sLog.warn(" Sum == 0.0! Remove time constraint and recalculating... (p: " + p.getPid() + ", d: " + d + ", type: " + type.name() + ", toy: " + toy + ", days: " + days);
            uCarExp = math.mcUcarExp(p, type, d, lookupAlt(p), days, true);
            uTrainExp = math.mcUtrainExp(p, type, d, lookupAlt(p), days, true);
            sum = uCarExp + uTrainExp;
        }

        double pCar, pAir, pTrain;
        pCar = uCarExp / sum;
        sLog.debug("    pCar: " + pCar);

        pAir = uAirExp / sum;
        sLog.debug("    pAir: " + pAir);

        pTrain = uTrainExp / sum;
        sLog.debug("    pTrain: " + pTrain);

        if (pAir == 0.0 && pCar == 0.0 && pTrain == 0.0) {
            sLog.error("    pCar, pTrain, and pAir ALL == 0. p: pid = " + p.getPid());
            System.exit(1);
        }

        Map<Double, List<Integer>> pMap = new HashMap<>();
        List<Double> pList = new ArrayList<>();
        pList.add(pCar);
        pList.add(pAir);
        pList.add(pTrain);
        List<Integer> tmp1 = new ArrayList<>();
        tmp1.add(ModeChoice.CAR.getValue());
        pMap.put(pCar, tmp1);
        List<Integer> tmp2 = new ArrayList<>();
        tmp2.add(ModeChoice.AIR.getValue());
        pMap.put(pAir, tmp2);
        List<Integer> tmp3 = new ArrayList<>();
        tmp3.add(ModeChoice.TRAIN.getValue());
        pMap.put(pTrain, tmp3);

        // monte carlo method
        int modeChoiceValue = math.MonteCarloMethod(pList, pMap, rand.sample());
        sLog.debug("    modeChoiceValue: " + modeChoiceValue);

        ModeChoice mc = (modeChoiceValue == 0 ? ModeChoice.CAR : (modeChoiceValue == 1 ? ModeChoice.AIR : ModeChoice.TRAIN));

        // For statistical purpose
//        toursByTravelMode[mc.getValue()] ++;
        return mc;
    }

    private Integer findStopFrequency(int o, int d, Integer toy, Integer td, Integer tps, ModeChoice mc, TripType type, boolean isOutBound) {
        sLog.debug("Find Stop Frequency - o: " + o + ", d: " + d
                + ", Trip Duration: " + td + ", Party size: " + tps
                + ", Mode: " + mc.name() + ", Trip Purpose:  " + type.name()
                + ", outbound?: " + isOutBound);
        List<Double> uExpList = new ArrayList<>();
        double sum = 0.0;

        int maxStops = 5;
        Integer[] oNeighbors = sortedODDistMap.get(o);
        for (int i = 0; i < oNeighbors.length; i++) {
            if (i < 5) {
                if (oNeighbors[i] == d) {
                    maxStops = i;
                    break;
                }
            } else {
                maxStops = 5;
                break;
            }
        }
        if (maxStops == 0) {
            return 0;
        }

        for (int i = 0; i < maxStops; i++) {
            double uExp = math.stopFreqUExp(o, d, td, tps, mc, type, toy, i, isOutBound);
            uExpList.add(uExp);
            sum += uExp;
        }
        sLog.debug("    stopFreqUSum: " + sum);

        List<Double> pList = new ArrayList<>();
        Map<Double, List<Integer>> pMap = new HashMap<>();
        for (int i = 0; i < maxStops; i++) {
            double p = uExpList.get(i) / sum;
            sLog.debug("    stopFreqP[" + i + "]: " + p);
            pList.add(p);
            List<Integer> tmp = new ArrayList<>();
            tmp.add(i);
            pMap.put(p, tmp);
        }

        int stops = math.MonteCarloMethod(pList, pMap, rand.sample());
        if (stops == -1) {
            return 0;
        }

        // For statistical purpose
        if (isOutBound) {
            toursByPurposeAndStopFrequencyOB[type.getValue()][stops]++;
        } else {
            toursByPurposeAndStopFrequencyIB[type.getValue()][stops]++;
        }
        return stops;
    }

    private List<TripType> findStopTypes(Integer stops, TripType tripType, Integer tps, ModeChoice mc, boolean isOutBound) {
        sLog.debug("Find Stop Types - Number of  stops: " + stops
                + ", Party size: " + tps + ", Mode: " + mc.name()
                + ", Trip Purpose:  " + tripType.name() + ", outbound?: " + isOutBound);
        List<TripType> stopTypes = new ArrayList<>();
        for (int s = 1; s <= stops; s++) {
            double uBExp = math.stopTypeUExp(s, TripType.BUSINESS, tps, mc, isOutBound);
            double uPExp = math.stopTypeUExp(s, TripType.PLEASURE, tps, mc, isOutBound);
            double uPBExp = math.stopTypeUExp(s, TripType.PERSONAL_BUSINESS, tps, mc, isOutBound);
            double sum = uBExp + uPExp + uPBExp;
            sLog.debug("    uSum: " + sum);
            double pB = uBExp / sum;
            sLog.debug("    pB: " + pB);
            double pP = uPExp / sum;
            sLog.debug("    pP: " + pP);
            double pPB = uPBExp / sum;
            sLog.debug("    pPB: " + pPB);

            Map<Double, List<Integer>> pMap = new HashMap<>();
            List<Double> pList = new ArrayList<>();
            pList.add(pB);
            pList.add(pP);
            pList.add(pPB);

            List<Integer> tmp1 = new ArrayList<>();
            tmp1.add(TripType.BUSINESS.getValue());
            pMap.put(pB, tmp1);
            List<Integer> tmp2 = new ArrayList<>();
            tmp2.add(TripType.PLEASURE.getValue());
            pMap.put(pP, tmp2);
            List<Integer> tmp3 = new ArrayList<>();
            tmp3.add(TripType.PERSONAL_BUSINESS.getValue());
            pMap.put(pPB, tmp3);

            int typeValue = math.MonteCarloMethod(pList, pMap, rand.sample());
            stopTypes.add((typeValue == TripType.BUSINESS.getValue() ? TripType.BUSINESS
                    : (typeValue == TripType.PLEASURE.getValue() ? TripType.PLEASURE
                            : TripType.PERSONAL_BUSINESS)));
        }

        // For statistical purpose
        for (TripType t : stopTypes) {
            toursByPurposeAndModeChoice[mc.getValue()][t.getValue()]++;
        }
        // Plus trip purpose of destination. (Trip purpose of origin is ignored since it is always HOME.)
        toursByPurposeAndModeChoice[mc.getValue()][tripType.getValue()]++;

        return stopTypes;
    }

    private Integer findStopLocation(Person2010 p, int so, int o, int d, ModeChoice mc, TripType type, int toy, int days, int numOfStops, boolean isOutBound, List<Integer> pickedStopLocations) {
        sLog.debug("Find Stop Location - p: " + p.getPid() + ", stop origin: " + so
                + ", o: " + o + ", d: " + d + ", Mode: " + mc.name()
                + ", Trip Purpose:  " + type.name() + ", toy: " + toy
                + ", outbound?: " + isOutBound);

        // 1. Find all candidates first.
        Integer[] allZonesSorted = sortedODDistMap.get(o);
        List<Integer> candidates = new ArrayList<>();
        HashMap<String, Double[]> trainMap = math.getTrainMap();
        HashMap<String, Double[]> airMap = math.getAirMap();
        HashMap<String, Double[]> carMap = math.getCarMap();

        // 1-1. Find zone IDs z such that dist(o, z) < dist(o, d)
        for (int i = 0; i < allZonesSorted.length; i++) {
            if (allZonesSorted[i] != d) {
                candidates.add(allZonesSorted[i]);
            } else {
                break;
            }
        }

        // 1-2. Among all these primary candidates, if tripType is TRAIN/AIR, 
        //      remove those which don't have a train station/airport
        if (mc == ModeChoice.TRAIN) {
            for (Iterator<Integer> iterator = candidates.iterator(); iterator.hasNext();) {
                Integer candidate = iterator.next();
                if (trainMap.get(o + "-" + candidate) == null) {
                    iterator.remove();
                }
            }
        }
        if (mc == ModeChoice.AIR) {
            for (Iterator<Integer> iterator = candidates.iterator(); iterator.hasNext();) {
                Integer candidate = iterator.next();
                if (airMap.get(o + "-" + candidate) == null) {
                    iterator.remove();
                }
            }
        }

        // 1-3. Start picking stop locations from candidates. If tripType is AIR,
        //      the dist from picked zone ID to all other zone IDs must larger
        //      than 100 miles. If no such zone ID exists, set number of stops
        //      to the number of picked zone IDs and ignore the unassigned 
        //      trip purpose.
        if (mc == ModeChoice.AIR) {
            for (Iterator<Integer> iterator = candidates.iterator(); iterator.hasNext();) {
                Integer candidate = iterator.next();
                if (carMap.get(o + "-" + candidate)[3] < 100.0) {
                    // no need to null-check o-candidate, see step 1-2.
                    iterator.remove();
                } else if (carMap.get(d + "-" + candidate) == null || carMap.get(d + "-" + candidate)[3] < 100.0) {
                    iterator.remove();
                }
                for (int picked : pickedStopLocations) {
                    if (iterator.hasNext()) {
                        candidate = iterator.next();
                        if (carMap.get(picked + "-" + candidate) == null || carMap.get(picked + "-" + candidate)[3] < 100.0) {
                            iterator.remove();
                        }
                    }
                }
            }
        }

        // 1-4. Further remove so, o, d, and already picked zone IDs from 
        //      candidates
        List<Integer> readyToRemove = new ArrayList<>();
        for (Iterator<Integer> iterator = candidates.iterator(); iterator.hasNext();) {
            Integer candidate = iterator.next();
            if (candidate == o || candidate == d || candidate == so) {
                iterator.remove();
            }
            for (Integer picked : pickedStopLocations) {
                if (candidate.intValue() == picked.intValue()) {
                    // store to-be-removed candidate in an array then remove
                    // all together to prevent an invalidStateException
                    readyToRemove.add(candidate);
                }
            }
        }
        if (readyToRemove.size() > 1) {
            candidates.removeAll(readyToRemove);
        }

        if (candidates.size() == 0) {
            return -1;
        }

        // 2. If there are candidates left, calculate uExp and stop zone ID
        Map<Double, List<Integer>> pMap = new HashMap<>();
        List<Double> pList = new ArrayList<>();
        double expSum = 0.0;
        List<Double> uExpList = new ArrayList<>();

        // 2-1. Calculate uExp.
        for (Integer candidate : candidates) {
            double uExp = math.stopLocUExp(p, so, o, d, candidate, mc, type, toy, days, numOfStops, isOutBound, pickedStopLocations);
            expSum += uExp;
            uExpList.add(uExp);
        }

        // 2-2. Calculate p
        for (int z = 1; z <= candidates.size(); z++) {
            double pSt = uExpList.get(z - 1) / expSum;
//            if (pSt == Double.NEGATIVE_INFINITY || pSt == Double.POSITIVE_INFINITY || pSt == Double.NaN) {
//                sLog.error("  ERROR: uExpList.get(" + (z - 1) + "): " + uExpList.get(z - 1) + ", expSum: " + expSum + ", pSt: " + pSt);
//            }
            if (pMap.get(pSt) != null) {
                List tmp = pMap.get(pSt);
                tmp.add(z - 1);
            } else {
                List tmp = new ArrayList<>();
                tmp.add(z - 1);
                pMap.put(pSt, tmp);
            }
            pList.add(pSt);
        }

        // 2-3. Monte Carlo simulation
        int indx = math.MonteCarloMethod(pList, pMap, rand.sample());
        int loc = candidates.get(indx);

        // 3. The end. Sanity check.
        //
        // By adding the dist condition in stopLocUExp(math.java), an error will
        // occur when the dist is already the smallest so that all pSt will be
        // 0.0. In this case, the loc is chosen randomly from 1-380 and o/d/so
        // could be picked up. A mechanism in stopFreq calculation has been 
        // implemented to try to fix this issue. However, if it fails, we need
        // the following code block to catch it and terminate the execution for
        // further debugging.
        if (loc == o || loc == d || loc == so) {
            sLog.error("ERROR: loc == " + loc + ", o = " + o + ", d = " + d + ", numOfStops: " + numOfStops
                    + ", Mode: " + mc.name() + ", Trip Purpose:  " + type.name()
                    + ", toy: " + toy + ", outbound?: " + isOutBound);
            int t = 0;
            for (int l : pickedStopLocations) {
                sLog.error("  stop loc: " + l);
            }
            if (allZonesSorted.length != 0) {
                for (int l : allZonesSorted) {
                    sLog.error("  possible location: " + l);
                    if (l == d) {
                        break;
                    }
                }
            } else {
                sLog.info("  --possible location length == 0");
            }

            for (double v : uExpList) {
                sLog.error("  exp(" + t + "): " + v);
                t++;
            }
            sLog.error(" -FATAL ERROR-  -FATAL ERROR-  -FATAL ERROR-  -FATAL ERROR-  -FATAL ERROR-  -FATAL ERROR-  -FATAL ERROR- ");
            System.exit(-1);
        }

        // For statistical purpose.
        toursByModeChoiceAndDest[mc.getValue()][loc - 1]++;

        return loc;
    }

    private HashMap<Integer, Integer[]> initZoneId() {
        sLog.info("Initialize zone id.");
        HashMap<Integer, Integer[]> zone = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.zoneid"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("MSA/NMSA")) {
                    Integer key = Integer.parseInt(ExcelUtils.getColumnValue(1, line));
                    Integer[] value = {Integer.parseInt(ExcelUtils.getColumnValue(2, line)), Integer.parseInt(ExcelUtils.getColumnValue(3, line))};
                    zone.put(key, value);
                    altMsaMap.put(Integer.parseInt(ExcelUtils.getColumnValue(2, line)), Integer.parseInt(ExcelUtils.getColumnValue(3, line)));
                }
            }
            br.close();
        } catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        return zone;
    }

    private int lookupAlt(Person2010 p) {
        int alt = -1;
        if (p.getMsapmsa() == 9999) {
            int msapmsa = Integer.parseInt(p.getSt() + "99");
            if (zoneIdMap.get(msapmsa) == null) {
                return -1;
            }
            p.setTmpMsapmsa(msapmsa);
            alt = zoneIdMap.get(msapmsa)[0];
        } else {
            if (zoneIdMap.get(p.getMsapmsa()) == null) {
                return -1;
            }
            alt = zoneIdMap.get(p.getMsapmsa())[0];
        }
        return alt;
    }

    private void runSimulationAndPopulateResults(Person2010 p, int origin, TripType type) {
        if (type == TripType.BUSINESS) {
            sLog.debug("Total BUSINESS tour: " + p.getrB());
            for (int tour = 0; tour < p.getrB(); tour++) {
//                toursByPurpose[type.getValue()] ++;
                sLog.debug("Tour #" + tour);
                /**
                 * Tour Level
                 */
                // 1. Destination Choice
                int dest = findDestinationChoice(p, origin, type, INVALID_QUARTER);
                sLog.debug("    Dest Choice: " + dest);
                // 2. Time of Year
                int toy = findToY(p, origin, dest, type);
                sLog.debug("    Time of Year: " + toy);
                // 3. Trip Duration
                int days = findTourDuration(p, dest, type, toy);
                sLog.debug("    Trip Duration: " + days);
                // 4. Travel Party Size
                int party = findTravelPartySize(p, dest, type);
                sLog.debug("    Party Size: " + party);
                // 5. Mode Choice
                ModeChoice mode = findModeChoice(p, dest, type, toy, days);
                sLog.debug("    Mode: " + mode.name());
                /**
                 * Stop Level
                 */
                // 6. Stop Frequency
                int obNumOfStops = findStopFrequency(origin, dest, toy, days, party, mode, type, true);
                int ibNumOfStops = findStopFrequency(origin, dest, toy, days, party, mode, type, false);
                sLog.debug("    Number of ob stops: " + obNumOfStops + ", Number of ib stops: " + ibNumOfStops);
                // 7. Stop Purpose/Type (exclude origin and dest)
                List<TripType> obStopPurposes = findStopTypes(obNumOfStops, type, party, mode, true);
                List<TripType> ibStopPurposes = findStopTypes(ibNumOfStops, type, party, mode, false);
                sLog.debug("    Number of ob stop purposes: " + obStopPurposes.size() + ", Number of ib stop purposes: " + ibStopPurposes.size());
                String debug = "    obStopPurposes: [";
                for (TripType t : obStopPurposes) {
                    debug += t + ", ";
                }
                debug += "], ibStopPuposes: [";
                for (TripType t : ibStopPurposes) {
                    debug += t + ", ";
                }
                debug += "]";
                sLog.debug(debug);
                //
                //
                // 8. Stop Location (exclude origin and dest)
                // Super complex...
                //
                //
                List<Integer> obStopLocations = new ArrayList<>();
                List<Integer> ibStopLocations = new ArrayList<>();
                int so = -1;
                for (int stopIndex = 0; stopIndex < obNumOfStops; stopIndex++) {
                    if (stopIndex == 0) {
                        // first stop, its stop origin is 'o'
                        so = origin;
                    }
                    int loc = findStopLocation(p, so, origin, dest, mode, type, toy, days, obNumOfStops, true, obStopLocations);
                    if (loc == -1) {
                        break;
                    }
                    sLog.debug("    loc: " + loc);
                    obStopLocations.add(loc);
                    so = loc;
                }
                for (int stopIndex = 0; stopIndex < ibNumOfStops; stopIndex++) {
                    if (stopIndex == 0) {
                        // first stop, its stop origin is 'd'
                        so = dest;
                    }
                    int loc = findStopLocation(p, so, dest, origin, mode, type, toy, days, ibNumOfStops, false, ibStopLocations);
                    if (loc == -1) {
                        break;
                    }
                    sLog.debug("    loc: " + loc);
                    ibStopLocations.add(loc);
                    so = loc;
                }
                sLog.debug("    Number of ob stop locations: " + obStopLocations.size() + ", Number of ib stop locations: " + ibStopLocations.size());
                debug = "    obStopLocations: [";
                for (int t : obStopLocations) {
                    debug += t + ", ";
                }
                debug += "], ibStopLocations: [";
                for (int t : ibStopLocations) {
                    debug += t + ", ";
                }
                debug += "]";
                sLog.debug(debug);
                writeTripsToResults(p, mode, toy, type, origin, dest, party, obStopPurposes, obStopLocations, ibStopPurposes, ibStopLocations);
            }
        } else if (type == TripType.PERSONAL_BUSINESS) {
            sLog.debug("Total PERSONAL_BUSINESS tour: " + p.getrPB());
            for (int tour = 0; tour < p.getrPB(); tour++) {
//                toursByPurpose[type.getValue()] ++;
                sLog.debug("Tour #" + tour);
                /**
                 * Tour Level
                 */
                // 1. Destination Choice
                int dest = findDestinationChoice(p, origin, type, INVALID_QUARTER);
                sLog.debug("    Dest Choice: " + dest);
                // 2. Time of Year
                int toy = findToY(p, origin, dest, type);
                sLog.debug("    Time of Year: " + toy);
                // 3. Trip Duration
                int days = findTourDuration(p, dest, type, toy);
                sLog.debug("    Trip Duration: " + days);
                // 4. Travel Party Size
                int party = findTravelPartySize(p, dest, type);
                sLog.debug("    Party Size: " + party);
                // 5. Mode Choice
                ModeChoice mode = findModeChoice(p, dest, type, toy, days);
                sLog.debug("    Mode: " + mode.name());
                /**
                 * Stop Level
                 */
                // 6. Stop Frequency
                int obNumOfStops = findStopFrequency(origin, dest, toy, days, party, mode, type, true);
                int ibNumOfStops = findStopFrequency(origin, dest, toy, days, party, mode, type, false);
                sLog.debug("    Number of ob stops: " + obNumOfStops + ", Number of ib stops: " + ibNumOfStops);
                // 7. Stop Purpose/Type (exclude origin and dest)
                List<TripType> obStopPurposes = findStopTypes(obNumOfStops, type, party, mode, true);
                List<TripType> ibStopPurposes = findStopTypes(ibNumOfStops, type, party, mode, false);
                sLog.debug("    Number of ob stop purposes: " + obStopPurposes.size() + ", Number of ib stop purposes: " + ibStopPurposes.size());
                String debug = "    obStopPurposes: [";
                for (TripType t : obStopPurposes) {
                    debug += t + ", ";
                }
                debug += "], ibStopPuposes: [";
                for (TripType t : ibStopPurposes) {
                    debug += t + ", ";
                }
                debug += "]";
                sLog.debug(debug);
                //
                //
                // 8. Stop Location (exclude origin and dest)
                // Super complex...
                //
                //
                List<Integer> obStopLocations = new ArrayList<>();
                List<Integer> ibStopLocations = new ArrayList<>();
                int so = -1;
                for (int stopIndex = 0; stopIndex < obNumOfStops; stopIndex++) {
                    if (stopIndex == 0) {
                        // first stop, its stop origin is 'o'
                        so = origin;
                    }
                    int loc = findStopLocation(p, so, origin, dest, mode, type, toy, days, obNumOfStops, true, obStopLocations);
                    if (loc == -1) {
                        break;
                    }
                    sLog.debug("    loc: " + loc);
                    obStopLocations.add(loc);
                    so = loc;
                }
                for (int stopIndex = 0; stopIndex < ibNumOfStops; stopIndex++) {
                    if (stopIndex == 0) {
                        // first stop, its stop origin is 'd'
                        so = dest;
                    }
                    int loc = findStopLocation(p, so, dest, origin, mode, type, toy, days, ibNumOfStops, false, ibStopLocations);
                    if (loc == -1) {
                        break;
                    }
                    sLog.debug("    loc: " + loc);
                    ibStopLocations.add(loc);
                    so = loc;
                }
                sLog.debug("    Number of ob stop locations: " + obStopLocations.size() + ", Number of ib stop locations: " + ibStopLocations.size());
                debug = "    obStopLocations: [";
                for (int t : obStopLocations) {
                    debug += t + ", ";
                }
                debug += "], ibStopLocations: [";
                for (int t : ibStopLocations) {
                    debug += t + ", ";
                }
                debug += "]";
                sLog.debug(debug);
                /**
                 * Output Result
                 */
                writeTripsToResults(p, mode, toy, type, origin, dest, party, obStopPurposes, obStopLocations, ibStopPurposes, ibStopLocations);
            }
        } else {
            // type == TripType.PLEASURE
            int tour = 0;
            sLog.debug("Total PLEASURE tour: " + p.getrP());
            while (tour < p.getrP()) {
//                toursByPurpose[type.getValue()] ++;
                sLog.debug("Tour #" + tour);
                // 1. Trip Duration
                int days = findTourDuration(p, NIL_INT, type, NIL_INT);
                sLog.debug("    Tour Duration: " + days);
                // 2. Simple Time of Year Model
                int toy = findToY(p, origin, NIL_INT, type);
                sLog.debug("    Time of Year (Simple): " + toy);
                // 3. Travel Party Size
                int party = findTravelPartySize(p, NIL_INT, type);
                sLog.debug("    Party Size: " + party);
                // 4. Destination Choice
                int dest = findDestinationChoice(p, origin, type, toy);
                sLog.debug("    Dest Choice: " + dest);
                // 5. Full Time of Year Model
                toy = findToY(p, origin, dest, type);
                sLog.debug("    Time of Year (Full): " + toy);
                // 6. Mode Choice
                ModeChoice mode = findModeChoice(p, dest, type, toy, days);
                sLog.debug("    Mode: " + mode.name());
                /**
                 * Now stop level
                 */
                // 7. stop frequency
                int obNumOfStops = findStopFrequency(origin, dest, toy, days, party, mode, type, true);
                int ibNumOfStops = findStopFrequency(origin, dest, toy, days, party, mode, type, false);
                sLog.debug("    Number of ob stops: " + obNumOfStops + ", Number of ib stops: " + ibNumOfStops);
                // 8. stop purpose
                List<TripType> obStopPurposes = findStopTypes(obNumOfStops, type, party, mode, true);
                List<TripType> ibStopPurposes = findStopTypes(ibNumOfStops, type, party, mode, false);
                sLog.debug("    Number of ob stop purposes: " + obStopPurposes.size() + ", Number of ib stop purposes: " + ibStopPurposes.size());
                String debug = "    obStopPurposes: [";
                for (TripType t : obStopPurposes) {
                    debug += t + ", ";
                }
                debug += "], ibStopPuposes: [";
                for (TripType t : ibStopPurposes) {
                    debug += t + ", ";
                }
                debug += "]";
                sLog.debug(debug);
                //
                //
                // 8. Stop Location (exclude origin and dest)
                // Super complex...
                //
                //
                List<Integer> obStopLocations = new ArrayList<>();
                List<Integer> ibStopLocations = new ArrayList<>();
                int so = -1;
                for (int stopIndex = 0; stopIndex < obNumOfStops; stopIndex++) {
                    if (stopIndex == 0) {
                        // first stop, its stop origin is 'o'
                        so = origin;
                    }
                    int loc = findStopLocation(p, so, origin, dest, mode, type, toy, days, obNumOfStops, true, obStopLocations);
                    if (loc == -1) {
                        break;
                    }
                    sLog.debug("    loc: " + loc);
                    obStopLocations.add(loc);
                    so = loc;
                }
                for (int stopIndex = 0; stopIndex < ibNumOfStops; stopIndex++) {
                    if (stopIndex == 0) {
                        // first stop, its stop origin is 'd'
                        so = dest;
                    }
                    int loc = findStopLocation(p, so, dest, origin, mode, type, toy, days, ibNumOfStops, false, ibStopLocations);
                    if (loc == -1) {
                        break;
                    }
                    sLog.debug("    loc: " + loc);
                    ibStopLocations.add(loc);
                    so = loc;
                }
                sLog.debug("    Number of ob stop locations: " + obStopLocations.size() + ", Number of ib stop locations: " + ibStopLocations.size());
                debug = "    obStopLocations: [";
                for (int t : obStopLocations) {
                    debug += t + ", ";
                }
                debug += "], ibStopLocations: [";
                for (int t : ibStopLocations) {
                    debug += t + ", ";
                }
                debug += "]";
                sLog.debug(debug);
                /**
                 * Output Result
                 */
                writeTripsToResults(p, mode, toy, type, origin, dest, party, obStopPurposes, obStopLocations, ibStopPurposes, ibStopLocations);
                tour++;
            }
        }
    }

    private void writeTripsToResults(Person2010 p, ModeChoice mode, int toy, TripType type, int origin, int dest, int party, List<TripType> obStopPurposes, List<Integer> obStopLocations, List<TripType> ibStopPurposes, List<Integer> ibStopLocations) {
        int pId = p.getPid();
        /*
         outbound
         */
        String key;
        String odPair;
        if (obStopLocations.isEmpty()) {
            key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + type;
            odPair = origin + "-" + dest;
            updateMatrixCellAndTripStatsArr(key, odPair, p, type, 0, toy, mode, party, origin, dest);
        } else if (obStopLocations.size() == 1) {
            key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + obStopPurposes.get(0);
            odPair = origin + "-" + obStopLocations.get(0);
            updateMatrixCellAndTripStatsArr(key, odPair, p, type, 0, toy, mode, party, origin, dest);
            key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + type;
            odPair = obStopLocations.get(0) + "-" + dest;
            updateMatrixCellAndTripStatsArr(key, odPair, p, type, 0, toy, mode, party, origin, dest);
        } else {
            // first trip: origin -> stop1 (i = 0). Type is stopLoc's type
            key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + obStopPurposes.get(0);
            odPair = origin + "-" + obStopLocations.get(0);
            updateMatrixCellAndTripStatsArr(key, odPair, p, type, 0, toy, mode, party, origin, dest);
            // last trip: stopN -> dest (i = N). Type is tour's type.
            key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + type;
            odPair = obStopLocations.get(obStopLocations.size() - 1) + "-" + dest;
            updateMatrixCellAndTripStatsArr(key, odPair, p, type, 0, toy, mode, party, origin, dest);
            // enroute, output (stopOrigin, stopLoc), type is stopLoc's type
            for (int i = 0; i < obStopLocations.size() - 1; i++) {
                key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + obStopPurposes.get(i + 1);
                odPair = obStopLocations.get(i) + "-" + obStopLocations.get(i + 1);
                updateMatrixCellAndTripStatsArr(key, odPair, p, type, 0, toy, mode, party, origin, dest);
            }
        }
        /*
         inbound
         */
        if (ibStopLocations.isEmpty()) {
            key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + TripType.HOME;
            odPair = dest + "-" + origin;
            updateMatrixCellAndTripStatsArr(key, odPair, p, type, 1, toy, mode, party, origin, dest);
        } else if (ibStopLocations.size() == 1) {
            key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + ibStopPurposes.get(0);
            odPair = dest + "-" + ibStopLocations.get(0);
            updateMatrixCellAndTripStatsArr(key, odPair, p, type, 1, toy, mode, party, origin, dest);
            key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + TripType.HOME;
            odPair = ibStopLocations.get(0) + "-" + origin;
            updateMatrixCellAndTripStatsArr(key, odPair, p, type, 1, toy, mode, party, origin, dest);
        } else {
            // first trip: dest -> stop1 (i = 0). Type is stopLoc's type
            key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + ibStopPurposes.get(0);
            odPair = dest + "-" + ibStopLocations.get(0);
            updateMatrixCellAndTripStatsArr(key, odPair, p, type, 1, toy, mode, party, origin, dest);
            // last trip: stopN -> origin (i = N). Type is HOME.
            key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + TripType.HOME;
            odPair = ibStopLocations.get(ibStopLocations.size() - 1) + "-" + origin;
            updateMatrixCellAndTripStatsArr(key, odPair, p, type, 1, toy, mode, party, origin, dest);
            // enroute, output (stopOrigin, stopLoc), type is stopLoc's type
            for (int i = 0; i < ibStopLocations.size() - 1; i++) {
                key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + ibStopPurposes.get(i + 1);
                odPair = ibStopLocations.get(i) + "-" + ibStopLocations.get(i + 1);
                updateMatrixCellAndTripStatsArr(key, odPair, p, type, 1, toy, mode, party, origin, dest);
            }
        }
    }

    private void outputResults() {
        sLog.info("Output results to files.");
        for (int mc = 0; mc < ModeChoice.itemCount; mc++) {
            for (int toy = 0; toy < 4; toy++) {
                for (int type = 0; type < TripType.itemCount; type++) {
                    String key = ModeChoice.values()[mc] + "-" + Quarter.values()[toy] + "-" + TripType.values()[type];
                    String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                    String fileName = key + "-" + timestamp + "-" + rand.sample() + ".txt";
                    File f = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + fileName);
                    try (FileWriter fw = new FileWriter(f); BufferedWriter bw = new BufferedWriter(fw)) {
                        if (f.exists()) {
                            f.delete();
                        } else {
                            f.createNewFile();
                        }

                        for (int i = 1; i <= Math.alt; i++) {
                            for (int j = 1; j <= Math.alt; j++) {
                                bw.write(results.get(key).get(j + "-" + i) + "\t");
                            }
                            bw.write("\n");
                        }
                        bw.flush();
                    } catch (IOException ex) {
                        sLog.error("Failed to write to file: " + ThesisProperties.getProperties("simulation.pums2010.output.dir"), ex);
                        System.exit(1);
                    }
                }
            }
        }
        sLog.info("Output trip stats to file.");
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String fileName = "trip.stats-" + timestamp + "-" + rand.sample() + ".txt";
        File f = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + fileName);
        try (FileWriter fw = new FileWriter(f); BufferedWriter bw = new BufferedWriter(fw)) {
            if (f.exists()) {
                f.delete();
            } else {
                f.createNewFile();
            }

            for (int i = 0; i < TRIP_STATS_COLUMNS; i++) {
                bw.write(tripStats[i] + "\t");
            }
            bw.write("\n");
            bw.flush();
        } catch (IOException ex) {
            sLog.error("Failed to write to file: " + ThesisProperties.getProperties("simulation.pums2010.output.dir"), ex);
            System.exit(1);
        }
        
        sLog.info("SKIP Output statistical info to files.");
//        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
//        String fileName = "tours.by.purpose.and.stop.frequency.inbound-" + timestamp + ".txt";
//        File f = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + fileName);
//        try (FileWriter fw = new FileWriter(f); BufferedWriter bw = new BufferedWriter(fw)) {
//            if (f.exists()) {
//                f.delete();
//            } else {
//                f.createNewFile();
//            }
//
//            for (int i = 0; i < TripType.itemCount - 1; i++) {
//                for (int j = 0; j < 5; j++) {
//                    bw.write(toursByPurposeAndStopFrequencyIB[i][j] + "\t");
//                }
//                bw.write("\n");
//            }
//            bw.flush();
//        } catch (IOException ex) {
//            sLog.error("Failed to write to file: " + ThesisProperties.getProperties("simulation.pums2010.output.dir"), ex);
//            System.exit(1);
//        }
//        fileName = "tours.by.purpose.and.stop.frequency.outbound-" + timestamp + ".txt";
//        f = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + fileName);
//        try (FileWriter fw = new FileWriter(f); BufferedWriter bw = new BufferedWriter(fw)) {
//            if (f.exists()) {
//                f.delete();
//            } else {
//                f.createNewFile();
//            }
//
//            for (int i = 0; i < TripType.itemCount - 1; i++) {
//                for (int j = 0; j < 5; j++) {
//                    bw.write(toursByPurposeAndStopFrequencyOB[i][j] + "\t");
//                }
//                bw.write("\n");
//            }
//            bw.flush();
//        } catch (IOException ex) {
//            sLog.error("Failed to write to file: " + ThesisProperties.getProperties("simulation.pums2010.output.dir"), ex);
//            System.exit(1);
//        }
//
//        fileName = "tours.by.purpose.and.mode.choice-" + timestamp + ".txt";
//        f = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + fileName);
//        try (FileWriter fw = new FileWriter(f); BufferedWriter bw = new BufferedWriter(fw)) {
//            if (f.exists()) {
//                f.delete();
//            } else {
//                f.createNewFile();
//            }
//
//            for (int i = 0; i < TripType.itemCount - 1; i++) {
//                for (int j = 0; j < ModeChoice.itemCount; j++) {
//                    bw.write(toursByPurposeAndModeChoice[i][j] + "\t");
//                }
//                bw.write("\n");
//            }
//            bw.flush();
//        } catch (IOException ex) {
//            sLog.error("Failed to write to file: " + ThesisProperties.getProperties("simulation.pums2010.output.dir"), ex);
//            System.exit(1);
//        }
//
//        fileName = "tours.by.mode.choice.and.dest-" + timestamp + ".txt";
//        f = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + fileName);
//        try (FileWriter fw = new FileWriter(f); BufferedWriter bw = new BufferedWriter(fw)) {
//            if (f.exists()) {
//                f.delete();
//            } else {
//                f.createNewFile();
//            }
//
//            for (int i = 0; i < ModeChoice.itemCount; i++) {
//                for (int j = 0; j < Math.alt; j++) {
//                    bw.write(toursByModeChoiceAndDest[i][j] + "\t");
//                }
//                bw.write("\n");
//            }
//            bw.flush();
//        } catch (IOException ex) {
//            sLog.error("Failed to write to file: " + ThesisProperties.getProperties("simulation.pums2010.output.dir"), ex);
//            System.exit(1);
//        }
    }

    private void updateMatrixCellAndTripStatsArr(String key, String odPair, Person2010 p, TripType type, int inbound, int toy, ModeChoice mode, int party, int origin, int dest) {
        int tripO = Integer.parseInt(odPair.split("-")[0]);
        int oMsa = altMsaMap.get(tripO);
        int tripD = Integer.parseInt(odPair.split("-")[1]);
        int dMsa = altMsaMap.get(tripD);
        Trip trip = new Trip(p.getPid(), type.name(), inbound, tripO, oMsa, tripD, dMsa, toy, mode.name(), party, math.getCarMap().get(tripO + "-" + tripD)[3], origin, dest);
        updateTripStats(trip, p);
//        tripBuffer.add(trip);
//        if (tripBuffer.size() == dbMaxBatchSize) {
//            saveTripsToDb();
//            tripBuffer.clear();
//        }
        
        Integer count = results.get(key).get(odPair);
        if (count == null) {
            results.get(key).put(odPair, 1);
        } else {
            results.get(key).put(odPair, count + 1);
        }
    }
    
    /*
     TRIP STATS COLUMNS:
    
     0  low_income + air
     1  low_income + car
     2  low_income + train
     3	Medium Income, air
     4	Medium Income, car
     5	Medium Income, train
     6	High Income, air
     7	High Income, car
     8	High Income, train
     9	Male, air
     10	Male, car
     11	Male, train
     12	Female,air
     13	Female,car
     14	Female,train
     15	Age 1, air
     16	Age 1, car
     17	Age 1, train
     18	Age 2, air
     19	Age 2, car
     20	Age 2, train
     21	Age 3,air 
     22	Age 3,car
     23	Age 3,train
     24	Low Income VMT
     25	Medium Income VMT
     26	High Income VMT
     */
    private void updateTripStats(Trip trip, Person2010 p) {
        if (p.getIncLevel() == 1 && trip.getMode().equals(TravelMode.AIR.name())) { tripStats[0] += 1;}
        if (p.getIncLevel() == 1 && trip.getMode().equals(TravelMode.CAR.name())) { tripStats[1] += 1;}
        if (p.getIncLevel() == 1 && trip.getMode().equals(TravelMode.TRAIN.name())) { tripStats[2] += 1;}
        if (p.getIncLevel() == 2 && trip.getMode().equals(TravelMode.AIR.name())) { tripStats[3] += 1;}
        if (p.getIncLevel() == 2 && trip.getMode().equals(TravelMode.CAR.name())) { tripStats[4] += 1;}
        if (p.getIncLevel() == 2 && trip.getMode().equals(TravelMode.TRAIN.name())) { tripStats[5] += 1;}
        if (p.getIncLevel() == 3 && trip.getMode().equals(TravelMode.AIR.name())) { tripStats[6] += 1;}
        if (p.getIncLevel() == 3 && trip.getMode().equals(TravelMode.CAR.name())) { tripStats[7] += 1;}
        if (p.getIncLevel() == 3 && trip.getMode().equals(TravelMode.TRAIN.name())) { tripStats[8] += 1;}
        
        if (p.getSex() == 1 && trip.getMode().equals(TravelMode.AIR.name())) { tripStats[9] += 1;}
        if (p.getSex() == 1 && trip.getMode().equals(TravelMode.CAR.name())) { tripStats[10] += 1;}
        if (p.getSex() == 1 && trip.getMode().equals(TravelMode.TRAIN.name())) { tripStats[11] += 1;}
        if (p.getSex() == 2 && trip.getMode().equals(TravelMode.AIR.name())) { tripStats[12] += 1;}
        if (p.getSex() == 2 && trip.getMode().equals(TravelMode.CAR.name())) { tripStats[13] += 1;}
        if (p.getSex() == 2 && trip.getMode().equals(TravelMode.TRAIN.name())) { tripStats[14] += 1;}
        
        if (p.getAge() >= 18 && p.getAge() <= 35 && trip.getMode().equals(TravelMode.AIR.name())) { tripStats[15] += 1;}
        if (p.getAge() >= 18 && p.getAge() <= 35 && trip.getMode().equals(TravelMode.CAR.name())) { tripStats[16] += 1;}
        if (p.getAge() >= 18 && p.getAge() <= 35 && trip.getMode().equals(TravelMode.TRAIN.name())) { tripStats[17] += 1;}
        if (p.getAge() > 35 && p.getAge() <= 60 && trip.getMode().equals(TravelMode.AIR.name())) { tripStats[18] += 1;}
        if (p.getAge() > 35 && p.getAge() <= 60 && trip.getMode().equals(TravelMode.CAR.name())) { tripStats[19] += 1;}
        if (p.getAge() > 35 && p.getAge() <= 60 && trip.getMode().equals(TravelMode.TRAIN.name())) { tripStats[20] += 1;}
        if (p.getAge() > 60 && trip.getMode().equals(TravelMode.AIR.name())) { tripStats[21] += 1;}
        if (p.getAge() > 60 && trip.getMode().equals(TravelMode.CAR.name())) { tripStats[22] += 1;}
        if (p.getAge() > 60 && trip.getMode().equals(TravelMode.TRAIN.name())) { tripStats[23] += 1;}
        
        if (p.getIncLevel() == 1) { tripStats[24] += trip.getDistance();}
        if (p.getIncLevel() == 2) { tripStats[25] += trip.getDistance();}
        if (p.getIncLevel() == 3) { tripStats[26] += trip.getDistance();}
    }

    private void saveTripsToDb() {
        Connection conn = pumsDao.getConnection();
        try {
            conn.setAutoCommit(false);
            pstmt = conn.prepareStatement(
                    "INSERT INTO " + ThesisProperties.getProperties("simulation.pums2010.db.table.ntd_output")
                    + " (" + ThesisProperties.getProperties("simulation.pums2010.db.table.ntd_output.columns") + ") "
                    + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
            for (Trip trip : tripBuffer) {
                pstmt.setInt(1, trip.getPersonId());
                pstmt.setString(2, trip.getType());
                pstmt.setInt(3, trip.getInbound());
                pstmt.setInt(4, trip.getTripO());
                pstmt.setInt(5, trip.getoMsa());
                pstmt.setInt(6, trip.getTripD());
                pstmt.setInt(7, trip.getdMsa());
                pstmt.setInt(8, trip.getToy());
                pstmt.setString(9, trip.getMode());
                pstmt.setInt(10, trip.getParty());
                pstmt.setDouble(11, trip.getDistance());
                pstmt.setInt(12, trip.getOrigin());
                pstmt.setInt(13, trip.getDest());
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            conn.commit();
            pstmt.clearBatch();

        } catch (BatchUpdateException ex) {
            sLog.error("----BatchUpdateException----", ex);
            sLog.error("SQLState:  " + ex.getSQLState());
            sLog.error("Message:  " + ex.getMessage());
            sLog.error("Vendor:  " + ex.getErrorCode());
            sLog.error("Update counts:  ");
            int[] updateCounts = ex.getUpdateCounts();

            for (int i = 0; i < updateCounts.length; i++) {
                sLog.error(updateCounts[i] + "   ");
            }
            sLog.error("Program terminated with exit code 1.");
            System.exit(1);
        } catch (SQLException ex) {
            sLog.error("Database error.", ex);
            sLog.error("Program terminated with exit code 1.");
            System.exit(1);
        }
    }
}
