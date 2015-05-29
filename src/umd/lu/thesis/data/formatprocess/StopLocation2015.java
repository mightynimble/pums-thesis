/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.data.formatprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.exceptions.InvalidValueException;
import umd.lu.thesis.exceptions.ValueNotFoundException;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.helper.FileUtils;

/**
 *
 * @author lousia
 */
public class StopLocation2015 {

    private final static int bufferSize = 10485760;

    private static ArrayList<Integer> idArr = new ArrayList<>();

    private static HashMap<Integer, Double> msaEmpTable = new HashMap<>();

    private static HashMap<Integer, Double> msaHhTable = new HashMap<>();

    private static HashMap<Integer, Integer> msaIdTable = new HashMap<>();

    private static HashMap<Integer, Integer> idMsaTable = new HashMap<>();

    private static HashMap<String, Double> odAirFareByQuarter = new HashMap<>();

    private static HashMap<String, Double> odAirTime = new HashMap<>();

    private static HashMap<String, Double> odAirFare = new HashMap<>();

    private static HashMap<String, Double> odCarTime = new HashMap<>();

    private static HashMap<String, Double> odDriveCost = new HashMap<>();

    private static HashMap<String, Double> odStopNight = new HashMap<>();

    private static HashMap<String, Double> odTrainTime = new HashMap<>();

    private static HashMap<String, Double> odTrainCost = new HashMap<>();

    private final static int lgsWeight = 1;

    public static void main(String[] args) throws IOException, InvalidValueException {

        loadHashTables();

        try (FileWriter fw = new FileWriter(ThesisProperties.getProperties("output.file.path"))) {
            BufferedWriter bw = new BufferedWriter(fw, bufferSize);

            // write header to output file
            bw.write(prepareHeader());

            // prepare reader to read input file
            BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("data.file.path"), bufferSize);

            // for each line in input file...
            String line = null;
            while ((line = br.readLine()) != null) {
                // skip header then process each line, write the processed line to 
                // output file.
                if (!line.startsWith("id")) {
                    try {
                        bw.write(processLine(line));
                    } catch (ValueNotFoundException ex) {
                        System.out.println("ValueNotFoundException: key: " + ex.getLocalizedMessage() + ", line: " + line);
                        for (int i = 0; i < 9; i++) {
                            br.readLine();
                        }
                    }
                }
            }

            bw.flush();
        }
    }

    private static String prepareHeader() {
        // the very beginning five columns
        String header = "id\tcid\toriginal_stopsto_householdid\toriginal_stopsto_personid\toriginal_stopsto_tripid\toriginal_stopsto_persontripid\toriginal_stopsto_pertrips\toriginal_stopsto_persontripweigh\tstopsto\tstopsno\tstpregion\tstpstate\tstpst\tstpmsa\tstpnite\tstplodgn\tstpreasn\tstprpse\tprestop\to_msa\td_msa\ttripchar_householdid\ttripchar_personid\ttripchar_tripid\ttripchar_persontripid\ttripchar_pertrips\ttripchar_persontripweight\tall_distance\thhtype\thouseholdsize\thouseholdpersons\tinc\tinc_c\tlowinc\tmedinc\thighinc\tage1\temploystatus\ttrparty\ttrprtyhousehold\ttrprtyad\ttrprtych\treturnquarter\tdepartquater\tniteaway\tnitedest\tnite_way\tturtrvlmode\temployment\thh\tstp_id\tprestp_id\td_id\tdetourdist\tprestpd_dist\tpurpose\ttime\tcost\tchoice\tnonmsa\tvot\tgeneralcost\n";
        return header;
    }

    private static String processLine(String line) throws InvalidValueException, ValueNotFoundException {
        String outLine = line + "\t";
        double vot = getVOT(line);
        outLine += vot + "\t";
        outLine += getGeneralCost(line, vot) + "\n";

        return outLine;
    }

    private static double getVOT(String line) throws InvalidValueException, ValueNotFoundException {
        String key = msaIdTable.get(Integer.parseInt(ExcelUtils.getColumnValue(20, line))) + "-" + msaIdTable.get(Integer.parseInt(ExcelUtils.getColumnValue(21, line)));
        String purpose = ExcelUtils.getColumnValue(56, line);
        String mode = ExcelUtils.getColumnValue(48, line);
        String quarter = ExcelUtils.getColumnValue(43, line);
        String inc = ExcelUtils.getColumnValue(32, line);

        if (mode.equals("1")) { // car
            return calcFromTourCarCost(key, inc, purpose);
        } else if (mode.equals("2")) { // air
            return calcFromTourAirCost(key, quarter, purpose);
        } else if (mode.equals("4")) { // train
            return calcFromTourTrainCost(key, purpose);
        }
        throw new InvalidValueException("Invalid mode value detected. Line: " + line);
    }

    private static void loadHashTables() throws IOException {
        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("dictionary.file.path"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("MSA")) {
                    // split[0] - MSA
                    // split[1] - ID
                    // split[2] - ZoneID
                    // split[3] - Employ
                    // split[4] - HHs   
                    String[] split = line.split("\t");

                    // MSA-Empl
                    msaEmpTable.put(Integer.parseInt(split[0]), Double.parseDouble(split[3]));
                    // MSA-HHs
                    msaHhTable.put(Integer.parseInt(split[0]), Double.parseDouble(split[4]));
                    // MSA-ID
                    msaIdTable.put(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
                    // ID-MSA
                    idMsaTable.put(Integer.parseInt(split[1]), Integer.parseInt(split[0]));
                }
            }
        }

        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("air.fare.by.quarter"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("Quarter")) {
                    String[] split = line.split("\t");

                    odAirFareByQuarter.put(split[1] + "-" + split[2] + "-" + split[0], Double.parseDouble(split[3]));
                }
            }
        }

        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("others.file.path"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("O")) {
                    String[] split = line.split("\t");
                    odAirTime.put(split[0] + "-" + split[1], Double.parseDouble(split[2]));
                    odCarTime.put(split[0] + "-" + split[1], Double.parseDouble(split[3]));
                    odDriveCost.put(split[0] + "-" + split[1], Double.parseDouble(split[4]));
                    odStopNight.put(split[0] + "-" + split[1], Double.parseDouble(split[5]));
                }
            }
        }
        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("train.file.path"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("SA")) {
                    String[] split = line.split("\t");
                    odTrainCost.put(split[0] + "-" + split[1], Double.parseDouble(split[3]));
                    odTrainTime.put(split[0] + "-" + split[1], Double.parseDouble(split[4]));
                }
            }
        }

        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("airfare.file.path"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("O")) {
                    String[] split = line.split("\t");
                    odAirFare.put(split[0] + "-" + split[1], Double.parseDouble(split[2]));
                }
            }
        }
    }

    private static double calcFromTourCarCost(String key, String inc, String purpose) throws InvalidValueException {
        double tourCarCost = tourCarCost(key, Double.parseDouble(inc));
        return compareAndSetVOT(tourCarCost, purpose);
    }

    private static double tourCarCost(String key, double inc) throws InvalidValueException {
        Double stopNight = odStopNight.get(key);
        if (stopNight == null) {
            throw new InvalidValueException("Invalid stopNight detected. Key: " + key);
        }
        Double unitLodgeCost;
        if (inc <= 30000.0) {
            unitLodgeCost = 70.0;
        } else if (inc > 30000.0 && inc <= 70000.0) {
            unitLodgeCost = 90.0;
        } else {
            unitLodgeCost = 110.0;
        }
        double lodgeCost = unitLodgeCost * stopNight;

        return (odDriveCost.get(key) / lgsWeight + lodgeCost) * 2.0;
    }

    private static double calcFromTourAirCost(String key, String quarter, String purpose) throws InvalidValueException, ValueNotFoundException {
        Double airCost = odAirFareByQuarter.get(key + "-" + quarter);
        try {
            if (airCost == null) {
                airCost = odAirFare.get(key) / 2;
            }
        } catch (NullPointerException ex) {
            System.out.println("NPE - key: " + key);
            throw new ValueNotFoundException(key);
        }

        return compareAndSetVOT(airCost * 2.0, purpose);
    }

    private static double calcFromTourTrainCost(String key, String purpose) throws InvalidValueException, ValueNotFoundException {
        if (odTrainCost.get(key) == null) {
            throw new ValueNotFoundException(key);
        }
        return compareAndSetVOT(odTrainCost.get(key) * 2.0, purpose);
    }

    private static double compareAndSetVOT(double tourXCost, String purpose) throws InvalidValueException {
        if (purpose.equals("Business")) {
            if (tourXCost <= 188) {
                return 1.095384615;
            }
            if (tourXCost <= 332 && tourXCost > 188) {
                return 3.811563169;
            }
            if (tourXCost <= 476 && tourXCost > 332) {
                return 5.377643505;
            }
            if (tourXCost <= 620 && tourXCost > 476) {
                return 9.621621622;
            }
            if (tourXCost > 620) {
                return 12.8057554;
            }
        } else if (purpose.equals("Pleasure")) {
            if (tourXCost <= 188) {
                return 6.230200634;
            }
            if (tourXCost <= 312 && tourXCost > 188) {
                return 13.59447005;
            }
            if (tourXCost <= 436 && tourXCost > 312) {
                return 65.55555556;
            }
            if (tourXCost > 436) {
                return 176.119403;
            }
        } else if (purpose.equals("PB")) {
            if (tourXCost <= 188) {
                return 2.582677165;
            }
            if (tourXCost <= 312 && tourXCost > 188) {
                return 5.754385965;
            }
            if (tourXCost <= 436 && tourXCost > 312) {
                return 8.282828283;
            }
            if (tourXCost <= 560 && tourXCost > 436) {
                return 11.88405797;
            }
            if (tourXCost > 560) {
                return 30.37037037;
            }
        }
        throw new InvalidValueException("Invalid purpose detected. Purpose: " + purpose);
    }

    private static String getGeneralCost(String line, double vot) {
        return String.valueOf(Double.parseDouble(ExcelUtils.getColumnValue(57, line)) * vot + Double.parseDouble(ExcelUtils.getColumnValue(58, line)));
    }

}
