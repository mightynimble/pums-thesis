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
import static java.lang.Math.abs;
import static java.lang.Math.abs;
import static java.lang.Math.abs;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import umd.lu.thesis.simulation.app2000.objects.TripType;

/**
 *
 * @author Home
 */
public class NationalTravelDemand {

    private final static Logger sLog = LogManager.getLogger(NationalTravelDemand.class);

    private static final int INVALID_QUARTER = -1;

    private static final int bulkSize = 2000;

    private static int startRow;  /* inclusive */

    private static int endRow;    /* exclusive */

    private static int currentRow;

    private static final int NIL_INT = Integer.MIN_VALUE;

    private static Pums2010DAOImpl pumsDao;

    private Math math;

    private HashMap<String, HashMap<String, Integer>> results;

    private final HashMap<Integer, Integer[]> zoneIdMap;

    private UniformRealDistribution rand;
    
    private static int[][] toursByPurposeAndStopFrequencyIB;
    private static int[][] toursByPurposeAndStopFrequencyOB;
    private static int[][] toursByPurposeAndModeChoice;
    private static int[][] toursByModeChoiceAndDest;
    
    public NationalTravelDemand(Pums2010DAOImpl dao) {
        pumsDao = dao;
        zoneIdMap = initZoneId();
        math = new Math();
        results = new HashMap<>();
        rand = new UniformRealDistribution();
        toursByPurposeAndStopFrequencyIB = new int[TripType.itemCount - 1][5];
        toursByPurposeAndStopFrequencyOB = new int[TripType.itemCount - 1][5];
        toursByPurposeAndModeChoice = new int[ModeChoice.itemCount][TripType.itemCount - 1];
        toursByModeChoiceAndDest = new int[ModeChoice.itemCount][Math.alt];
    }

    public NationalTravelDemand() {
        zoneIdMap = initZoneId();
        pumsDao = new Pums2010DAOImpl();
        math = new Math();
        results = new HashMap<>();
        rand = new UniformRealDistribution();
        toursByPurposeAndStopFrequencyIB = new int[TripType.itemCount - 1][5];
        toursByPurposeAndStopFrequencyOB = new int[TripType.itemCount - 1][5];
        toursByPurposeAndModeChoice = new int[ModeChoice.itemCount][TripType.itemCount - 1];
        toursByModeChoiceAndDest = new int[ModeChoice.itemCount][Math.alt];
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
            if (currentRow == endRow) {
                break;
            }

            int o = lookupAlt(p);
            if (o == -1) {
                sLog.error("  -ERROR- MSAPMSA " + p.getMsapmsa() + " can't be mapped to a zone. Skipped. (p.id=" + p.getPid() + ")");
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
        // Note that S(T) gets smaller when T increase. So the loop can break
        // early.
        double prevAbs = Double.POSITIVE_INFINITY;
        double abs = Double.POSITIVE_INFINITY;
        for (int t = 1; t <= 30; t++) {
            sLog.debug("    prevAbs: " + prevAbs + ", newAbs: " + abs);
            abs = abs(math.tdST(p, d, toy, t, tripType) - 0.5);
            if (abs < prevAbs) {
                prevAbs = abs;
                continue;
            } else {
                return t - 1;
            }
        }
        return 30;
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
        for (int i = 0; i < 5; i++) {
            double uExp = math.stopFreqUExp(o, d, td, tps, mc, type, toy, i, isOutBound);
            uExpList.add(uExp);
            sum += uExp;
        }
        sLog.debug("    stopFreqUSum: " + sum);

        List<Double> pList = new ArrayList<>();
        Map<Double, List<Integer>> pMap = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            double p = uExpList.get(i) / sum;
            sLog.debug("    stopFreqP[" + i + "]: " + p);
            pList.add(p);
            List<Integer> tmp = new ArrayList<>();
            tmp.add(i);
            pMap.put(p, tmp);
        }

        int stops = math.MonteCarloMethod(pList, pMap, rand.sample());
        // For statistical purpose
        if(isOutBound) {
            toursByPurposeAndStopFrequencyOB[type.getValue()][stops]++;
        }
        else {
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
            toursByPurposeAndModeChoice[mc.getValue()][t.getValue()] ++;
        }
        // Plus trip purpose of destination. (Trip purpose of origin is ignored since it is always HOME.)
        toursByPurposeAndModeChoice[mc.getValue()][tripType.getValue()] ++;
                
        return stopTypes;
    }

    private Integer findStopLocation(Person2010 p, int so, int o, int d, ModeChoice mc, TripType type, int toy, int days, int numOfStops, boolean isOutBound, List<Integer> stopLocations) {
        sLog.debug("Find Stop Location - p: " + p.getPid() + ", stop origin: " + so
                + ", o: " + o + ", d: " + d + ", Mode: " + mc.name()
                + ", Trip Purpose:  " + type.name() + ", toy: " + toy
                + ", outbound?: " + isOutBound);
        Map<Double, List<Integer>> pMap = new HashMap<>();
        List<Double> pList = new ArrayList<>();
        List<Double> uExpList = new ArrayList<>();
        double expSum = 0.0;
        // cache uExp in a List
        for (int z = 1; z <= Math.alt; z++) {
            double uExp = math.stopLocUExp(p, so, o, d, z, mc, type, toy, days, numOfStops, isOutBound, stopLocations);
            expSum += uExp;
            uExpList.add(uExp);
        }
        // calculate p
        for (int z = 1; z <= Math.alt; z++) {
            double pSt = uExpList.get(z - 1) / expSum;
            if (pSt == Double.NEGATIVE_INFINITY || pSt == Double.POSITIVE_INFINITY || pSt == Double.NaN) {
                sLog.debug("    pSt[" + (z - 1) + "]: " + pSt);
            }
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

        int loc = math.MonteCarloMethod(pList, pMap, rand.sample()) + 1;
        
        // For statistical purpose.
        toursByModeChoiceAndDest[mc.getValue()][loc - 1] ++;
        
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
            for (int tour = 0; tour < p.getrP(); tour++) {
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
                // 8. Stop Location (exclude origin and dest)
                List<Integer> obStopLocations = new ArrayList<>();
                List<Integer> ibStopLocations = new ArrayList<>();
                int so = -1;
                for (int stopIndex = 0; stopIndex < obNumOfStops; stopIndex++) {
                    if (stopIndex == 0) {
                        // first stop, its stop origin is 'o'
                        so = origin;
                    }
                    int loc = findStopLocation(p, so, origin, dest, mode, type, toy, days, obNumOfStops, true, obStopLocations);
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
                String key;
                String odPair;
                // outbound
                sLog.debug("  *. Populate od result matrices.");
                int i = 0;
                for (int stopLoc : obStopLocations) {
                    if (i == 0) {
                        // first stop, output (origin, stopLoc), type is stopLoc's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + obStopPurposes.get(i);
                        odPair = origin + "-" + stopLoc;
                    } else if (i == obStopLocations.size() - 1) {
                        // last stop, output (stopLoc, dest), type is tour's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + type;
                        odPair = stopLoc + "-" + dest;
                    } else {
                        // enroute, output (stopOrigin, stopLoc), type is stopLoc's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + obStopPurposes.get(i);
                        odPair = obStopLocations.get(i - 1) + "-" + stopLoc;
                    }
                    updateMatrixCell(key, odPair);
                    i++;
                }
                // inbound
                sLog.debug("  *. Populate id result matrices.");
                i = 0;
                for (int stopLoc : ibStopLocations) {
                    if (i == 0) {
                        // first stop, output (dest, stopLoc), type is stopLoc's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + ibStopPurposes.get(i);
                        odPair = dest + "-" + stopLoc;
                    } else if (i == ibStopLocations.size() - 1) {
                        // last stop, output (stopLoc, origin), type is HOME
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + TripType.HOME;
                        odPair = stopLoc + "-" + origin;
                    } else {
                        // enroute, output (stopOrigin, stopLoc), type is stopLoc's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + ibStopPurposes.get(i);
                        odPair = ibStopLocations.get(i - 1) + "-" + stopLoc;
                    }
                    updateMatrixCell(key, odPair);
                    i++;
                }
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
                // 8. Stop Location (exclude origin and dest)
                List<Integer> obStopLocations = new ArrayList<>();
                List<Integer> ibStopLocations = new ArrayList<>();
                int so = -1;
                for (int stopIndex = 0; stopIndex < obNumOfStops; stopIndex++) {
                    if (stopIndex == 0) {
                        // first stop, its stop origin is 'o'
                        so = origin;
                    }
                    int loc = findStopLocation(p, so, origin, dest, mode, type, toy, days, obNumOfStops, true, obStopLocations);
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
                String key;
                String odPair;
                // outbound
                sLog.debug("  *. Populate od result matrices.");
                int i = 0;
                for (int stopLoc : obStopLocations) {
                    if (i == 0) {
                        // first stop, output (origin, stopLoc), type is stopLoc's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + obStopPurposes.get(i);
                        odPair = origin + "-" + stopLoc;
                    } else if (i == obStopLocations.size() - 1) {
                        // last stop, output (stopLoc, dest), type is tour's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + type;
                        odPair = stopLoc + "-" + dest;
                    } else {
                        // enroute, output (stopOrigin, stopLoc), type is stopLoc's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + obStopPurposes.get(i);
                        odPair = obStopLocations.get(i - 1) + "-" + stopLoc;
                    }
                    updateMatrixCell(key, odPair);
                    i++;
                }
                // inbound
                sLog.debug("  *. Populate id result matrices.");
                i = 0;
                for (int stopLoc : ibStopLocations) {
                    if (i == 0) {
                        // first stop, output (dest, stopLoc), type is stopLoc's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + ibStopPurposes.get(i);
                        odPair = dest + "-" + stopLoc;
                    } else if (i == ibStopLocations.size() - 1) {
                        // last stop, output (stopLoc, origin), type is HOME
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + TripType.HOME;
                        odPair = stopLoc + "-" + origin;
                    } else {
                        // enroute, output (stopOrigin, stopLoc), type is stopLoc's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + ibStopPurposes.get(i);
                        odPair = ibStopLocations.get(i - 1) + "-" + stopLoc;
                    }
                    updateMatrixCell(key, odPair);
                    i++;
                }
            }
        } else {
            // type == TripType.PLEASURE
            int tour = 0;
            sLog.debug("Total PLEASURE tour: " + p.getrPB());
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
                // 9. Stop Location
                List<Integer> obStopLocations = new ArrayList<>();
                List<Integer> ibStopLocations = new ArrayList<>();
                int so = -1;
                for (int stopIndex = 0; stopIndex < obNumOfStops; stopIndex++) {
                    if (stopIndex == 0) {
                        // first stop, its stop origin is 'o'
                        so = origin;
                    }
                    int loc = findStopLocation(p, so, origin, dest, mode, type, toy, days, obNumOfStops, true, obStopLocations);
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
                String key;
                String odPair;
                // outbound
                sLog.debug("  *. Populate od result matrices.");
                int i = 0;
                for (int stopLoc : obStopLocations) {
                    if (i == 0) {
                        // first stop, output (origin, stopLoc), type is stopLoc's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + obStopPurposes.get(i);
                        odPair = origin + "-" + stopLoc;
                    } else if (i == obStopLocations.size() - 1) {
                        // last stop, output (stopLoc, dest), type is tour's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + type;
                        odPair = stopLoc + "-" + dest;
                    } else {
                        // enroute, output (stopOrigin, stopLoc), type is stopLoc's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + obStopPurposes.get(i);
                        odPair = obStopLocations.get(i - 1) + "-" + stopLoc;
                    }
                    updateMatrixCell(key, odPair);
                    i++;
                }
                // inbound
                sLog.debug("  *. Populate id result matrices.");
                i = 0;
                for (int stopLoc : ibStopLocations) {
                    if (i == 0) {
                        // first stop, output (dest, stopLoc), type is stopLoc's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + ibStopPurposes.get(i);
                        odPair = dest + "-" + stopLoc;
                    } else if (i == ibStopLocations.size() - 1) {
                        // last stop, output (stopLoc, origin), type is HOME
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + TripType.HOME;
                        odPair = stopLoc + "-" + origin;
                    } else {
                        // enroute, output (stopOrigin, stopLoc), type is stopLoc's type
                        key = mode.name() + "-" + Quarter.values()[toy - 1] + "-" + ibStopPurposes.get(i);
                        odPair = ibStopLocations.get(i - 1) + "-" + stopLoc;
                    }
                    updateMatrixCell(key, odPair);
                    i++;
                }
                tour++;
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
                    String fileName = key + "-" + timestamp + ".txt";
                    File f = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + fileName);
                    try (FileWriter fw = new FileWriter(f); BufferedWriter bw = new BufferedWriter(fw)) {
                        if (f.exists()) {
                            f.delete();
                        } else {
                            f.createNewFile();
                        }

                        for (int i = 0; i < Math.alt; i++) {
                            for (int j = 0; j < Math.alt; j++) {
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
        sLog.info("Output statistical info to files.");
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String fileName = "tours.by.purpose.and.stop.frequency.inbound-" + timestamp + ".txt";
        File f = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + fileName);
        try (FileWriter fw = new FileWriter(f); BufferedWriter bw = new BufferedWriter(fw)) {
            if(f.exists()) {
                f.delete();
            }
            else {
                f.createNewFile();
            }

            for (int i = 0; i < TripType.itemCount - 1; i++) {
                for (int j = 0; j < 5; j++) {
                    bw.write(toursByPurposeAndStopFrequencyIB[i][j] + "\t");
                }
                bw.write("\n");
            }
            bw.flush();
        }
        catch (IOException ex) {
            sLog.error("Failed to write to file: " + ThesisProperties.getProperties("simulation.pums2010.output.dir"), ex);
            System.exit(1);
        }
        fileName = "tours.by.purpose.and.stop.frequency.outbound-" + timestamp + ".txt";
        f = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + fileName);
        try (FileWriter fw = new FileWriter(f); BufferedWriter bw = new BufferedWriter(fw)) {
            if(f.exists()) {
                f.delete();
            }
            else {
                f.createNewFile();
            }

            for (int i = 0; i < TripType.itemCount - 1; i++) {
                for (int j = 0; j < 5; j++) {
                    bw.write(toursByPurposeAndStopFrequencyOB[i][j] + "\t");
                }
                bw.write("\n");
            }
            bw.flush();
        }
        catch (IOException ex) {
            sLog.error("Failed to write to file: " + ThesisProperties.getProperties("simulation.pums2010.output.dir"), ex);
            System.exit(1);
        }
        
        fileName = "tours.by.purpose.and.mode.choice-" + timestamp + ".txt";
        f = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + fileName);
        try (FileWriter fw = new FileWriter(f); BufferedWriter bw = new BufferedWriter(fw)) {
            if(f.exists()) {
                f.delete();
            }
            else {
                f.createNewFile();
            }

            for (int i = 0; i < TripType.itemCount - 1; i++) {
                for (int j = 0; j < ModeChoice.itemCount; j++) {
                    bw.write(toursByPurposeAndModeChoice[i][j] + "\t");
                }
                bw.write("\n");
            }
            bw.flush();
        }
        catch (IOException ex) {
            sLog.error("Failed to write to file: " + ThesisProperties.getProperties("simulation.pums2010.output.dir"), ex);
            System.exit(1);
        }
        
        fileName = "tours.by.mode.choice.and.dest-" + timestamp + ".txt";
        f = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + fileName);
        try (FileWriter fw = new FileWriter(f); BufferedWriter bw = new BufferedWriter(fw)) {
            if(f.exists()) {
                f.delete();
            }
            else {
                f.createNewFile();
            }

            for (int i = 0; i < ModeChoice.itemCount; i++) {
                for (int j = 0; j < Math.alt; j++) {
                    bw.write(toursByModeChoiceAndDest[i][j] + "\t");
                }
                bw.write("\n");
            }
            bw.flush();
        }
        catch (IOException ex) {
            sLog.error("Failed to write to file: " + ThesisProperties.getProperties("simulation.pums2010.output.dir"), ex);
            System.exit(1);
        }
    }

    private void updateMatrixCell(String key, String odPair) {
        Integer count = results.get(key).get(odPair);
        if (count == null) {
            results.get(key).put(odPair, 1);
        } else {
            results.get(key).put(odPair, count + 1);
        }
    }
}
