package umd.lu.thesis.simulation.app2000;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.exceptions.InvalidValueException;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.simulation.app2000.objects.Person;
import umd.lu.thesis.simulation.app2000.db.DataAccess;
import umd.lu.thesis.simulation.app2000.math.Formulae;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import umd.lu.thesis.simulation.app2000.objects.TripType;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

/**
 *
 * @author lousia
 */
public class App2000 {

    private final static Logger sLog = LogManager.getLogger(App2000.class);

    private static final int[] TABLE_IDS = {1, 2, 4, 5, 6, 8, 9, 10, 11, 12, 13, 15, 16,
        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34,
        35, 36, 37, 38, 39, 40, 41, 42, 44, 45, 46, 47, 48, 49, 50, 51, 53, 54, 55, 56};
//    private static final int[] TABLE_IDS = {2};
//    private static final int[] TABLE_IDS = {12, 13};

    // key: o-d-time-mode, value: integer
//    private static HashMap<String, Integer> results;
//    private static HashMap<String, Integer> partialRs;
    private static final String LEGACY_TABLE_BASE_NAME = "HH_Per_";

    private DataAccess dao = null;

    private Formulae f = null;

    private static final int dbFetchSize = 5000;

    private HashMap<Integer, Integer> hmZoneId = null;

    private static int startRow;
    private static final int bulkSize = 100000;
    private static int tableId;

    public App2000() {
        dao = new DataAccess();
        hmZoneId = new HashMap<>();
        f = new Formulae();
    }

    public void run() throws Exception {

        initialization();

        sLog.info("Initialize processing range. startRow: " + startRow + ", batch size: " + bulkSize + ", current table id: " + tableId);
        int totalRows = dao.getLegacyTableRowNumber(tableId);
        boolean endOfTable = true;
        sLog.info("Total rows in legacy table " + LEGACY_TABLE_BASE_NAME + tableId + ": " + totalRows);
        if (startRow + bulkSize > totalRows) {
            processRows(startRow, totalRows, tableId, endOfTable);
            recordProgress(1, getNextTableId(tableId));
        } else {
            processRows(startRow, startRow + bulkSize - 1, tableId, endOfTable);
            recordProgress(startRow + bulkSize, tableId);
        }

        sLog.info("-- DONE --");

    }

    private void recordProgress(int start, int table) {
        sLog.info("Record progress to file.");
        File f = new File(ThesisProperties.getProperties("simulation.app2000.progress"));
        if (f.exists()) {
            f.delete();
        }

        try (FileWriter fw = new FileWriter(f); BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(Integer.toString(start) + "\t" + Integer.toString(table));
            bw.flush();
        } catch (IOException ex) {
            sLog.error("Failed to write to file: " + ThesisProperties.getProperties("simulation.app2000.progress"), ex);
            System.exit(1);
        }
    }

    private void loadProgress() {
        sLog.info("Load progress from file.");
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.progress")); BufferedReader br = new BufferedReader(new InputStreamReader(fstream))) {
            String[] progress = br.readLine().trim().split("\t");
            startRow = Integer.parseInt(progress[0]);
            tableId = Integer.parseInt(progress[1]);
            sLog.info("Progress from last run: startRow: " + startRow + ", tableID: " + tableId);
        } catch (IOException ex) {
            sLog.error("Failed to read file: " + ThesisProperties.getProperties("simulation.app2000.progress"), ex);
            System.exit(1);
        }
    }

    private int getNextTableId(int currentTableId) throws InvalidValueException {
        for (int i = 0; i < TABLE_IDS.length; i++) {
            if (TABLE_IDS[i] == currentTableId) {
                return TABLE_IDS[i + 1];
            }
        }
        throw new InvalidValueException("Invalid table ID: " + currentTableId);
    }

    private int processRows(int start, int end, int tableId, boolean endOfTable) {
        int expandedRows = 0;
        double startTime = System.nanoTime() / 1000000000.0;
        List<Person> persons = dao.bulkFetch(tableId, start, end);
        int counter = 0;
        for (Person p : persons) {
            Set<Person> expanded = expandPerson(p);
            expandedRows += expanded.size();
            populateExpandedSet(expanded);
            counter ++;
            if (counter % 10000 == 0) {
                sLog.info(counter + " processed.");
            }
        }
        double stopTime = System.nanoTime() / 1000000000.0;
        sLog.info("Expanded rows from " + start + " to " + end + " in talbe " + tableId + " into " + expandedRows + " new rows.");
        sLog.info("++++ Total time: " + (int) (stopTime - startTime) + " sec");
        sLog.info("++++ Total rows generated: " + expandedRows + "");
        sLog.info("++++ Speed: " + (int) (expandedRows / (stopTime - startTime)) + " rows/sec");
        sLog.info("++++ Memory: " + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB");
        
        if (endOfTable) {
            sLog.info("==== Finished process table: " + tableId);
        }

        return expandedRows;
    }

//    private int processTable(int id, int rowCount) {
//
//        int pointer = 1;
//        int expandedRows = 0;
//        while (pointer <= rowCount) {
//            double startTime = System.nanoTime() / 1000000000.0;
//            List<Person> bulkPerson = dao.bulkFetch(id, pointer, dbFetchSize);
//            for (Person p : bulkPerson) {
//                Set<Person> expanded = expandPerson(p);
//                expandedRows += expanded.size();
//                populateExpandedSet(expanded);
//            }
//            double stopTime = System.nanoTime() / 1000000000.0;
//            sLog.info(" - row " + pointer + " ~ " + (pointer + dbFetchSize) + ". Time: " + (int) (stopTime - startTime) + " sec");
//            pointer += dbFetchSize;
//
//        }
//        return expandedRows;
//
//    }
    private Set<Person> expandPerson(Person p) {
        Set<Person> expanded = new HashSet<>();
        try {
            int randB = p.getRandB();
            int randP = p.getRandP();
            int randPB = p.getRandPB();
            p.setRandB(0);
            p.setRandP(0);
            p.setRandPB(0);
            for (int i = 1; i <= randB; i++) {
                p.setRandB(i);
                expanded.add(p.clone());
            }
            p.setRandB(0);
            for (int i = 1; i <= randP; i++) {
                p.setRandP(i);
                expanded.add(p.clone());
            }

            p.setRandP(0);
            for (int i = 1; i <= randPB; i++) {
                p.setRandPB(i);
                expanded.add(p.clone());
            }

        } catch (Exception ex) {
            sLog.error("Error: person: pid: " + p.getPid());
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        return expanded;
    }

    private void populateExpandedSet(Set<Person> expanded) {
        UniformRealDistribution rand = new UniformRealDistribution();
        try {
            for (Person p : expanded) {
                int o = hmZoneId.get(p.getMsapmsa());
//                sLog.info("=== o:" + o + ", pid: " + p.getPid());
                List<Double> pSet = new ArrayList<>();
                double pSum = 0.0;
                double expSum = f.expSum(p, o, (p.getRandB() != 0 ? TripType.BUSINESS : TripType.PERSONAL_BUSINESS), -1);

                if (p.getRandB() != 0 || p.getRandPB() != 0) {
                    for (int d = 1; d <= Formulae.alt; d++) {
                        if (o != d) {
                            double prb = f.pD(p, o, d, (p.getRandB() != 0 ? TripType.BUSINESS : TripType.PERSONAL_BUSINESS), -1, expSum);
//                            sLog.info("=== d: " + d + ", prb: " + prb);
                            pSum += prb;
                            pSet.add(prb);
                        } else {
                            pSet.add(0.0);
                        }
                    }
                    int dest = pickDest(pSet, rand.sample(), p);
                    int time = pickTime(o, dest, rand.sample(), p);
                    int mode = pickMode(rand.sample(), o, dest, time, (p.getRandB() != 0 ? TripType.BUSINESS : TripType.PERSONAL_BUSINESS), p);

                    p.setDest(dest);
                    p.setTime(time);
                    p.setMode(mode);
                } else if (p.getRandP() != 0) {
                    int time = pickTime(p, rand.sample());
                    for (int d = 1; d <= Formulae.alt; d++) {
                        if (o != d) {
                            double prb = f.pD(p, o, d, (p.getRandB() != 0 ? TripType.BUSINESS : TripType.PERSONAL_BUSINESS), time, expSum);
                            pSum += prb;
                            pSet.add(prb);
                        } else {
                            pSet.add(0.0);
                        }
                    }
                    int dest = pickDest(pSet, rand.sample(), p);
                    int mode = pickMode(rand.sample(), o, dest, time, TripType.PLEASURE, p);

                    p.setDest(dest);
                    p.setTime(time);
                    p.setMode(mode);
                } else if (p.getRandB() == 0 && p.getRandP() == 0 && p.getRandPB() == 0) {
                    sLog.error("Invalid randB/randP/randPB: pid: " + p.getPid());
                    System.exit(1);
                }

                dao.save(p);
            }
        } catch (InvalidValueException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
    }

    private void initialization() {
        initZoneId();
        loadProgress();

//        initResultsTables();
    }

//    private void initResultsTables() {
//        sLog.info("----Initialize results tables.");
//        results = new HashMap<>();
//        partialRs = new HashMap<>();
//    }
    private void initZoneId() {
        sLog.info("----Initialize zone id.");
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.zoneid"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("MSA/NMSA")) {
                    Integer key = Integer.parseInt(ExcelUtils.getColumnValue(1, line));
                    Integer value = Integer.parseInt(ExcelUtils.getColumnValue(2, line));
                    hmZoneId.put(key, value);
                }
            }
            br.close();
        } catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
    }

    private int pickDest(List<Double> pSet, double rand, Person p) throws InvalidValueException {
//        sLog.info("=== pickDest: rand: " + rand);
        double sum1 = 0.0;
        double sum2 = 0.0;
        for (int i = 0; i <= Formulae.alt; i++) {
//            sLog.info("- i: " + i);
            if (i == 0) {
                sum1 = 0.0;
            } else {
                sum1 += pSet.get(i - 1);
            }
            sum2 = sum1 + pSet.get(i);
            if (sum1 <= rand && rand < sum2) {
//                sLog.info("  - sum1: " + sum1 + ", sum2: " + sum2);
                return i + 1;
            }
        }
//        sLog.info("=== END of pickDest");
        throw new InvalidValueException("Cannot find 'dest'. (Person PID: " + p.getPid() + ", rand:  " + rand + ")");
    }

    private int pickMode(double r, int o, int d, int quarter, TripType tripType, Person p) throws InvalidValueException {
//        sLog.info("=== pickMode: rand: " + r);
        double pCar = f.pCar(p, o, d, tripType, quarter);
        double pAir = f.pAirQuarter(p, o, d, tripType, quarter);
//        double pTrain = f.pTrain(incLevel, o, d, tripType, quarter);
        if (0 <= r && r < pCar) {
            return 1;
        }
        if (pCar <= r && r < pCar + pAir) {
            return 2;
        }
        if (pCar + pAir <= r && r < 1) {
            return 3;
        }
        throw new InvalidValueException("Cannot find 'mode'. (Person PID: " + p.getPid() + ", rand: " + r + ", o: " + o + ", d: " + d + ", tripPurpose: " + tripType.name() + ")");
    }

    private int pickTime(int o, int d, double r, Person p) throws InvalidValueException {
//        sLog.info("=== pickTime (B and PB): rand: " + r);
        double p1 = 0;
        double p2 = 0;
        double p3 = 0;
        if (p.getRandB() != 0) {
            p1 = f.timePx(1, o, d, TripType.BUSINESS, p);
            p2 = f.timePx(2, o, d, TripType.BUSINESS, p);
            p3 = f.timePx(3, o, d, TripType.BUSINESS, p);
//            double p4 = f.timePx(4, o, d, TripType.BUSINESS, p);
        } else if (p.getRandPB() != 0) {
            p1 = f.timePx(1, o, d, TripType.PERSONAL_BUSINESS, p);
            p2 = f.timePx(2, o, d, TripType.PERSONAL_BUSINESS, p);
            p3 = f.timePx(3, o, d, TripType.PERSONAL_BUSINESS, p);
        } else {
            throw new InvalidValueException("Invalid tripType. Args: rand: " + r + ", o: " + o + ", d: " + d + ", Person: PID=" + p.getPid());
        }

        if (0 <= r && r < p1) {
            return 1;
        } else if (p1 <= r && r < p1 + p2) {
            return 2;
        } else if (p1 + p2 <= r && r < p1 + p2 + p3) {
            return 3;
        } else if (p1 + p2 + p3 <= r && r < 1) {
            return 4;
        } else {
            throw new InvalidValueException("Cannot find 'time'. (Args: rand: " + r + ", o: " + o + ", d: " + d + ", Person: PID=" + p.getPid());
        }
    }

    private int pickTime(Person p, double r) throws InvalidValueException {
//        sLog.info("=== pickTime (P): rand: " + r);
        if (p.getRandP() == 0) {
            throw new InvalidValueException("Invalid tripType. Args: Person: PID=" + p.getPid());
        }
        double p1 = f.timePx(p, TripType.PLEASURE, 1);
        double p2 = f.timePx(p, TripType.PLEASURE, 2);
        double p3 = f.timePx(p, TripType.PLEASURE, 3);
//        double p4 = f.timePx(p, TripType.PLEASURE, 4);

        if (0 <= r && r < p1) {
            return 1;
        } else if (p1 <= r && r < p1 + p2) {
            return 2;
        } else if (p1 + p2 <= r && r < p1 + p2 + p3) {
            return 3;
        } else if (p1 + p2 + p3 <= r && r < 1) {
            return 4;
        } else {
            throw new InvalidValueException("Cannot find 'time'. (Args: : \"rand: " + r + ", Person: PID=" + p.getPid());
        }
    }

//    private void updateResultTable(Person p) {
//        String key = hmZoneId.get(p.getMsapmsa()) + "-" + p.getDest() + "-" + p.getTime() + "-" + p.getMode();
//        if (results.get(key) == null) {
//            results.put(key, 1);
//            partialRs.put(key, 1);
//        } else {
//            Integer value = results.get(key);
//            results.put(key, value + 1);
//            partialRs.put(key, value + 1);
//        }
//    }
//    private void outputResults() {
//        for (int time = 1; time <= 4; time++) {
//            String timeCat = "t" + time;
//            for (int mode = 1; mode <= 3; mode++) {
//                String modeCat = "m" + mode;
//                String table = timeCat + modeCat;
//                try (FileWriter fw = new FileWriter(ThesisProperties.getProperties("simulation.app2000.output_basename") + table + ".txt"); BufferedWriter bw = new BufferedWriter(fw)) {
//                    sLog.info("Generating table: " + ThesisProperties.getProperties("simulation.app2000.output_basename") + table + ".txt");
//                    String header = "O\\D\t";
//                    for (int i = 1; i <= 378; i++) {
//                        header += i + "\t";
//                    }
//                    header += "\n";
//                    bw.write(header);
//
//                    String line = "";
//                    for (int o = 1; o <= 378; o++) {
//                        line = o + "\t";
//                        for (int d = 1; d <= 378; d++) {
//                            Integer odPair = results.get(o + "-" + d + "-" + time + "-" + mode);
//                            if (odPair == null) {
//                                line += "0\t";
//                            } else {
//                                line += odPair.toString() + "\t";
//                            }
//                        }
//                        line += "\n";
//                        bw.write(line);
//                    }
//                } catch (IOException ex) {
//                    sLog.error(ex.getLocalizedMessage(), ex);
//                    System.exit(1);
//                }
//            }
//        }
//    }
//    private void dumpResults(int id) {
//        try (FileWriter fw = new FileWriter(ThesisProperties.getProperties("simulation.app2000.dumpfile_basename") + id + ".txt"); BufferedWriter bw = new BufferedWriter(fw)) {
//            for (String key : partialRs.keySet()) {
//                bw.write(key + "\t" + partialRs.get(key) + "\n");
//            }
//            bw.flush();
//        } catch (IOException ex) {
//            sLog.error(ex.getLocalizedMessage(), ex);
//            System.exit(1);
//        }
//    }
//    private boolean loadDumpFile(int id) {
//        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.dumpfile_basename") + id + ".txt"); BufferedReader br = new BufferedReader(new InputStreamReader(fstream));) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                line = line.trim();
//                String[] splits = line.split("\t");
//                results.put(splits[0], Integer.parseInt(splits[1]));
//                partialRs.put(splits[0], Integer.parseInt(splits[1]));
//            }
//            return true;
//        } catch (FileNotFoundException ex) {
//            sLog.info("File not found: " + ThesisProperties.getProperties("simulation.app2000.dumpfile_basename") + id + ".txt");
//        } catch (IOException ex) {
//            sLog.error(ex.getLocalizedMessage(), ex);
//            System.exit(1);
//        }
//        return false;
//    }
}
