package umd.lu.thesis.destchoice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.FileUtils;

/**
 * not used, not the final version
 *
 * @author Bo Sun
 */
public class Pleasure_2015_DestChoice {

    private final static int bufferSize = 10485760;

    private static ArrayList<Integer> idArr = new ArrayList<>();

    private static HashMap<Integer, Double> msaEmpTable = new HashMap<>();

    private static HashMap<Integer, Double> msaHhTable = new HashMap<>();

    private static HashMap<Integer, Integer> msaIdTable = new HashMap<>();

    private static HashMap<Integer, Integer> idMsaTable = new HashMap<>();

    private static HashMap<String, Double> odAirFare = new HashMap<>();

    private static HashMap<String, Double> odAirTime = new HashMap<>();

    private static HashMap<String, Double> odCarTime = new HashMap<>();

    private static HashMap<String, Double> odDriveCost = new HashMap<>();

    private static HashMap<String, Double> odStopNight = new HashMap<>();

    private static HashMap<String, Double> odTrainTime = new HashMap<>();

    private static HashMap<String, Double> odTrainCost = new HashMap<>();

    private final static double lgsCoefTime = -0.0812;

    private final static int lgsWeight = 2;

    /**
     * 2014 values
     *
     * private final static double coeffL = -0.00773;
     *
     * private final static double coeffM = -0.00826;
     *
     * private final static double coeffH = -0.00616;
     *
     *
     */
    private final static double Coef_cost1 = -0.00947;
    private final static double Coef_cost2 = -0.00434;
    private final static double Coef_cost3 = -0.000900;
    private final static double Coef_cost4 = -0.000335;
    private final static double Coef_Time = -0.0590;
    private final static double ASC_Air = -2.95;
    private final static double ASC_Train = -3.56;

    private static Random rand = new Random();

    public static void main(String[] args) throws IOException {
        loadALT();

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
                if (!line.startsWith("Person_Char_woIntel_HHPer")) {
                    bw.write(processLine(line));
                }
            }

            bw.flush();
        }
    }

    private static String prepareHeader() {
        // the very beginning five columns
        String header = "HouseholdId\tPersonid\tTripId\tO_MSA/NMSA\tINC\tDest\tEmp\tHHs\tLgs\tChoice\n";
        return header;
    }

    private static void loadALT() {
        try {
            try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("alt.file.path"), bufferSize)) {
                String altLine = br.readLine();
                // split 'altLine' into IDs by tab '\t'
                String[] split = altLine.split("\t");
                // add every one of the split sub string to alt array.
                for (String elem : split) {
                    idArr.add(new Integer(elem));
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    // find the value of the specific column from the given line
    // column starts from 1.
    private static String getColumnValue(int column, String line) {
        String[] split = line.split("\t");
        return split[column - 1];
    }

    private static String processLine(String line) {
        String header = "";

        // column B "HouseholdId"
        header += getColumnValue(2, line) + "\t";
        // column C "Personid"
        header += getColumnValue(3, line) + "\t";
        // column D "TripId"
        header += getColumnValue(4, line) + "\t";
        // column K "O_MSA/NMSA" (#11), this value will be used to look up the
        // 'fixed' IDs later.
        header += getColumnValue(11, line) + "\t";
        Integer oMsa = new Integer(getColumnValue(11, line));
        // column BT "INC" (#54)
        header += getColumnValue(54, line) + "\t";
        // column Q "D_MSA/NMSA" (#17)
        Integer dMsa = new Integer(getColumnValue(17, line));
        // column BE "LowInc" (#57)
        Integer lowInc = new Integer(getColumnValue(57, line));
        // column BF "MedInc" (#58)
        Integer midInc = new Integer(getColumnValue(58, line));
        // column BG "HighInc" (#59)
        Integer highInc = new Integer(getColumnValue(59, line));

        // get the fixed IDs
        Integer oFixedID = msaIdTable.get(oMsa);
        Integer dFixedID = msaIdTable.get(dMsa);
        // random select other 9 IDs (this is actually the indexes of array id[]        
        int[] randomIDs = getRandomIDs(9, oFixedID, dFixedID);

        // parameter "line" is parsed into 10 new lines:
        // First 9 lines with random picked ID
        String tenLines = "";
        for (int id : randomIDs) {
            // column A, B, C, D, E in output file
            tenLines += header;
            // column F in output file
            tenLines += Integer.toString(id) + "\t";

            Integer msa = idMsaTable.get(id);
            // column G in output file
            tenLines += msaEmpTable.get(msa).toString() + "\t";
            // column H in output file
            tenLines += msaHhTable.get(msa).toString() + "\t";
            // column I in output file
            tenLines += getLgsValue(oFixedID, id, getColumnValue(55, line), lowInc, midInc, highInc).toString() + "\t";
            // column J in output file
            tenLines += "0\t\n";
        }

        // The 10th line with choice value = 1.
        // column A, B, C, D, E in output file
        tenLines += header;
        // column F in output file
        tenLines += Integer.toString(dFixedID) + "\t";
        // column G in output file
        tenLines += msaEmpTable.get(dMsa).toString() + "\t";
        // column H in output file
        tenLines += msaHhTable.get(dMsa).toString() + "\t";
        // column I in output file
        tenLines += getLgsValue(oFixedID, dFixedID, getColumnValue(55, line), lowInc, midInc, highInc).toString() + "\t";
        // column J in output file
        tenLines += "1\t\n";

        // with the random IDs, we can now finish up the string 'processed'
//         processed += appendToLine(randomIDs, oFixedID, dFixedID, getColumnValue(72, line));
        return tenLines;
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

        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("airfare.file.path"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("O")) {
                    String[] split = line.split("\t");
                    odAirFare.put(split[0] + "-" + split[1], Double.parseDouble(split[2]));
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
    }

    private static int[] getRandomIDs(int num, Integer oFixedID, Integer dFixedID) {
        int[] randomIDs = new int[num];

        while (num > 0) {
            int r = rand.nextInt(idArr.size());
            int randID = idArr.get(r).intValue();
            if (randID != oFixedID.intValue() && randID != dFixedID.intValue() && !containsID(new Integer(randID), randomIDs)) {
                randomIDs[num - 1] = randID;
                num--;
            }
        }

        return randomIDs;
    }

    private static boolean containsID(Integer id, int[] randomIDs) {
        for (int i : randomIDs) {
            if (i == id.intValue()) {
                return true;
            }
        }
        return false;
    }

    private static Double getLgsValue(Integer o, Integer d, String i, Integer lowInc, Integer midInc, Integer highInc) {
        // constuct o-d pair key
        String key = o.toString() + "-" + d.toString();

        // get tourAirCost for FORMULA-1
        Double tourAirCost = 0.0;
        if (odAirFare.containsKey(key)) {
            tourAirCost = odAirFare.get(key);
        }
//        System.out.println("tourAirCost:\t" + tourAirCost);

        // get tourAirTime for FORMULA-1
        Double tourAirTime = odAirTime.get(key) * 2.0;
//        System.out.println("tourAirTime:\t" + tourAirTime);

        // get inc for FORMULA-1
        Double inc = Double.parseDouble(i);

        Double uAir = null;
        if (tourAirCost != 0.0 && tourAirTime != 0.0) {
            /**
             * 2014 formula: uAir = (coeffL * lowInc + coeffM * midInc + coeffH
             * * highInc) * tourAirCost + lgsCoefTime * tourAirTime;
             */
            // airplcost1 = 1 if Tour_AirCost <= 188 else airplcost1 = 0
            // airplcost2 = 1 if 188 < Tour_AirCost <= 312 else airplcost2 = 0
            // airplcost3 = 1 if 312 < Tour_AirCost <= 436 else airplcost3 = 0
            // airplcost4 = 1 if 436 < Tour_AirCost  else airplcost4 = 0
            uAir = ASC_Air
                    + Coef_cost1 * tourAirCost * (tourAirCost <= 188 ? 1 : 0)
                    + Coef_cost2 * tourAirCost * (tourAirCost <= 312 && tourAirCost > 188 ? 1 : 0)
                    + Coef_cost3 * tourAirCost * (tourAirCost <= 436 && tourAirCost > 312 ? 1 : 0)
                    + Coef_cost4 * tourAirCost * (tourAirCost > 436 ? 1 : 0)
                    + Coef_Time * tourAirTime;
        }
//        System.out.println("uAir:\t" + uAir);

        // get stopNight for FORMULA-2
        Double stopNight = odStopNight.get(key);
        // get unitLodgeCost for FORMULA-2-1
        // unitLodgeCost = 70  if     0 < inc <= 30000
        // unitLodgeCost = 90  if 30000 < inc <= 70000
        // unitLodgeCost = 110 if 70000 < inc 
        Double unitLodgeCost;
        if (inc.doubleValue() <= 30000.0) {
            unitLodgeCost = 30.0;
        } else if (inc.doubleValue() > 30000.0 && inc.doubleValue() <= 70000.0) {
            unitLodgeCost = 50.0;
        } else {
            unitLodgeCost = 70.0;
        }

        // FORMULA-2: lodgeCost = unitLodgeCost * stopNight
        Double lodgeCost = unitLodgeCost * stopNight;

        // get tourCarCost for FORMULA-3
        Double driveCost = odDriveCost.get(key);
        Double tourCarCost = (driveCost / lgsWeight + lodgeCost) * 2.0;

        // get tourCarTime for FORMULA-3
        Double tourCarTime = odCarTime.get(key) * 2.0;

        /**
         * 2014 formula: Double uCar = (coeffL * lowInc + coeffM * midInc +
         * coeffH * highInc) * tourCarCost + lgsCoefTime * tourCarTime;
         */
        // cplcost1 = 1 if Tour_CarCost <= 188 else cplcost1 = 0
        // cplcost2 = 1 if 188 < Tour_CarCost <= 312 else cplcost2 = 0
        // cplcost3 = 1 if 312 < Tour_CarCost <= 436 else cplcost3 = 0
        // cplcost4 = 1 if 436 < Tour_CarCost else cplcost4 = 0
        Double uCar = Coef_cost1 * tourCarCost * (tourCarCost <= 188 ? 1 : 0)
                + Coef_cost2 * tourCarCost * (tourCarCost <= 312 && tourCarCost > 188 ? 1 : 0)
                + Coef_cost3 * tourCarCost * (tourCarCost <= 436 && tourCarCost > 312 ? 1 : 0)
                + Coef_cost4 * tourCarCost * (tourCarCost > 436 ? 1 : 0)
                + Coef_Time * tourCarTime;

        // FORMULA-4: uTrain = coefCost * tourTrainCost + coefTime * tourTrainTime
        Double tourTrainCost = odTrainCost.get(key);
        Double tourTrainTime = odTrainTime.get(key);

        Double uTrain = null;
        if (tourTrainCost != null && tourTrainTime != null) {
            /**
             * 2014 formula: uTrain = (coeffL * lowInc + coeffM * midInc +
             * coeffH * highInc) * tourTrainCost * 2.0 + lgsCoefTime *
             * tourTrainTime * 2.0;
             */
            // tplcost1 = 1 if Tour_TrainCost <= 188 else tplcost1 = 0
            // tplcost2 = 1 if 188 < Tour_TrainCost <= 312 else tplcost2 = 0
            // tplcost3 = 1 if 312 < Tour_TrainCost <= 436 else tplcost3 = 0
            // tplcost4 = 1 if 436 < Tour_TrainCost   else tplcost4 = 0
            uTrain = ASC_Train
                    + Coef_cost1 * tourTrainCost * (tourTrainCost <= 188 ? 1 : 0)
                    + Coef_cost2 * tourTrainCost * (tourTrainCost <= 312 && tourTrainCost > 188 ? 1 : 0)
                    + Coef_cost3 * tourTrainCost * (tourTrainCost <= 436 && tourTrainCost > 312 ? 1 : 0)
                    + Coef_cost4 * tourTrainCost * (tourTrainCost > 436 ? 1 : 0)
                    + Coef_Time * tourTrainTime;
        }
//        System.out.println("uTrain:\t" + uTrain);
        // FORMULA-4: lgs = log[exp(uAir)+exp(uCar)], if odAirFare.containsKey(key).
        // if not, lgs = log[exp(uCar)]

        Double lgs = -1.0;
        if ((!odAirFare.containsKey(key) || odAirTime.get(key).doubleValue() == 0.0) && uTrain == null) {
            lgs = java.lang.Math.log(java.lang.Math.exp(uCar));
        } else if ((!odAirFare.containsKey(key) || odAirTime.get(key).doubleValue() == 0.0) && uTrain != null) {
            lgs = java.lang.Math.log(java.lang.Math.exp(uTrain) + java.lang.Math.exp(uCar));
        } else if (uTrain == null) {
            lgs = java.lang.Math.log(java.lang.Math.exp(uAir) + java.lang.Math.exp(uCar));
        } else {
            lgs = java.lang.Math.log(java.lang.Math.exp(uAir) + java.lang.Math.exp(uCar) + java.lang.Math.exp(uTrain));
        }
//        System.out.println("lgs:\t" + lgs);
//        System.out.println("=======================================");

        return lgs;
    }
}
