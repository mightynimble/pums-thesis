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

public class Business_2015_DestChoice {

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

//    private final static double lgsCoefTime = -0.0569;
    private final static int lgsWeight = 1;
//    private final static double coeffL = -0.00615;
//    private final static double coeffM = -0.00388;
//    private final static double coeffH = -0.00125;
    private final static double Coef_cost1 = -0.0325;
    private final static double Coef_cost2 = -0.00934;
    private final static double Coef_cost3 = -0.00662;
    private final static double Coef_cost4 = -0.00370;
    private final static double Coef_cost5 = -0.00278;
    private final static double Coef_Time = -0.0356;
    private final static double ASC_Air = -0.440;
    private final static double ASC_Train = -2.93;

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
        // column L "O_MSA/NMSA" (#12), this value will be used to look up the
        // 'fixed' IDs later.
        header += getColumnValue(12, line) + "\t";
        Integer oMsa = new Integer(getColumnValue(12, line));
        // column BT "INC" (#72)
        header += getColumnValue(72, line) + "\t";
        // column R "D_MSA/NMSA" (#18)
        Integer dMsa = new Integer(getColumnValue(18, line));
        // column BV "LowInc" (#74)
        Integer lowInc = new Integer(getColumnValue(74, line));
        // column BW "MedInc" (#75)
        Integer midInc = new Integer(getColumnValue(75, line));
        // column BX "HighInc" (#76)
        Integer highInc = new Integer(getColumnValue(76, line));

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
            tenLines += getLgsValue(oFixedID, id, getColumnValue(72, line), lowInc, midInc, highInc).toString() + "\t";
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
        tenLines += getLgsValue(oFixedID, dFixedID, getColumnValue(72, line), lowInc, midInc, highInc).toString() + "\t";
        // column J in output file
        tenLines += "1\t\n";

        // with the random IDs, we can now finish up the string 'processed'
//         tenLines += appendToLine(randomIDs, oFixedID, dFixedID, getColumnValue(72, line));
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

    private static String appendToLine(int[] randomIDs, Integer oFixedID, Integer dFixedID, String inc, Integer lowInc, Integer midInc, Integer highInc) {
        String tail = "";
        String av = "";

        for (Integer id : idArr) {
            if (containsID(id, randomIDs) || id == oFixedID.intValue()) {
                Integer msa = idMsaTable.get(id);
                tail += msaEmpTable.get(msa).toString() + "\t";
                tail += msaHhTable.get(msa).toString() + "\t";
                // TODO: Lgs value
                tail += getLgsValue(oFixedID, id, inc, lowInc, midInc, highInc).toString() + "\t";
                av += "1\t";
            } else {
                tail += "0\t0\t0\t";
                av += "0\t";
            }
        }
        tail += av;

        tail += dFixedID.toString();

        return tail + "\n";
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
//        System.out.println("o-d: " + o + "-" + d);

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

        // FORMULA-1: U_Air = Constant + Coef_Cost*Tour_AirCost + Coef_Time*Tour_AirTime + Coef_Inc*INC/100000
//        Double uAir = lgsConstant + lgsCoefCost * tourAirCost.doubleValue() + lgsCoefTime * tourAirTime.doubleValue() + lgsCoefInc * inc / 100000.0;
        // FORMULA-1: U_Air = CoeffL*LowInc*Tour_AirCost + CoeffM * MedInc * Tour_AirCost + CoeffH*HigInc*Tour_AirCost + Coeff_Time * Tour_AirTime
        Double uAir = null;
        if (tourAirCost != 0.0 && tourAirTime != 0.0) {
            // 2014 fomula
//            uAir = (coeffL * lowInc + coeffM * midInc + coeffH * highInc) * tourAirCost + lgsCoefTime * tourAirTime;
            // airbcost1 = 1 if Tour_AirCost <= 188 else airbcost1 = 0
            // airbcost2 = 1 if 188 < Tour_AirCost <= 332 else airbcost2 = 0
            // airbcost3 = 1 if 332 < Tour_AirCost <= 476 else airbcost3 = 0
            // airbcost4 = 1 if 476 < Tour_AirCost <= 620 else airbcost4 = 0
            // airbcost5 = 1 if   620 < Tour_AirCost      else airbcost5 = 0
            uAir = ASC_Air
                    + Coef_cost1 * tourAirCost * (tourAirCost <= 188 ? 1 : 0)
                    + Coef_cost2 * tourAirCost * (tourAirCost <= 332 && tourAirCost > 188 ? 1 : 0)
                    + Coef_cost3 * tourAirCost * (tourAirCost <= 476 && tourAirCost > 332 ? 1 : 0)
                    + Coef_cost4 * tourAirCost * (tourAirCost <= 620 && tourAirCost > 476 ? 1 : 0)
                    + Coef_cost5 * tourAirCost * (tourAirCost > 620 ? 1 : 0)
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
            unitLodgeCost = 70.0;
        } else if (inc.doubleValue() > 30000.0 && inc.doubleValue() <= 70000.0) {
            unitLodgeCost = 90.0;
        } else {
            unitLodgeCost = 110.0;
        }

        // FORMULA-2: lodgeCost = unitLodgeCost * stopNight
        Double lodgeCost = unitLodgeCost * stopNight;

        // get tourCarCost for FORMULA-3
        Double driveCost = odDriveCost.get(key);
        Double tourCarCost = (driveCost / lgsWeight + lodgeCost) * 2.0;
//        System.out.println("tourCarCost:\t" + tourCarCost);

        // get tourCarTime for FORMULA-3
        Double tourCarTime = odCarTime.get(key) * 2.0;
//        System.out.println("tourCarTime:\t" + tourCarTime);

        // FORMULA-3: uCar = coefCost * tourCarCost + coefTime * tourCarTime
//        Double uCar = lgsCoefCost * tourCarCost + lgsCoefTime * tourCarTime;
//        // FORMULA-3: U_Car = CoeffL*LowInc*tourCarCost + CoeffM * MedInc *tourCarCost + CoeffH*HigInc*tourCarCost + CoeffTime * tourCarTime
//(2014)        Double uCar = (coeffL * lowInc + coeffM * midInc + coeffH * highInc) * tourCarCost + lgsCoefTime * tourCarTime;
        // cbcost1 = 1 if Tour_CarCost <= 188 else cbcost1 = 0
        // cbcost2 = 1 if 188 < Tour_CarCost <= 332 else cbcost2 = 0
        // cbcost3 = 1 if 332 < Tour_CarCost <= 476 else cbcost3 = 0
        // cbcost4 = 1 if 476 < Tour_CarCost <= 620 else cbcost4 = 0
        // cbcost5 = 1 if   620 < Tour_CarCost      else cbcost5 = 0 
        Double uCar = Coef_cost1 * tourCarCost * (tourCarCost <= 188 ? 1 : 0)
                + Coef_cost2 * tourCarCost * (tourCarCost <= 332 && tourCarCost > 188 ? 1 : 0)
                + Coef_cost3 * tourCarCost * (tourCarCost <= 476 && tourCarCost > 332 ? 1 : 0)
                + Coef_cost4 * tourCarCost * (tourCarCost <= 620 && tourCarCost > 476 ? 1 : 0)
                + Coef_cost5 * tourCarCost * (tourCarCost > 620 ? 1 : 0)
                + Coef_Time * tourCarTime;
//        System.out.println("uCar:\t" + uCar);

        // FORMULA-4: uTrain = coefCost * tourTrainCost + coefTime * tourTrainTime
        Double tourTrainCost = odTrainCost.get(key);
        Double tourTrainTime = odTrainTime.get(key);

        Double uTrain = null;
        if (tourTrainCost != null && tourTrainTime != null) {
            tourTrainCost *= 2;
            tourTrainTime *= 2;
// (2014)            uTrain = (coeffL * lowInc + coeffM * midInc + coeffH * highInc) * tourTrainCost * 2.0 + lgsCoefTime * tourTrainTime * 2.0;
            // tbcost1 = 1 if Tour_TrainCost <= 188 else tbcost1 = 0
            // tbcost2 = 1 if 188 < Tour_TrainCost <= 332 else tbcost2 = 0
            // tbcost3 = 1 if 332 < Tour_TrainCost <= 476 else tbcost3 = 0
            // tbcost4 = 1 if 476 < Tour_TrainCost <= 620 else tbcost4 = 0
            // tbcost5 = 1 if   620 < Tour_TrainCost      else tbcost5 = 0 
            uTrain = ASC_Train
                    + Coef_cost1 * tourTrainCost * (tourTrainCost <= 188 ? 1 : 0)
                    + Coef_cost2 * tourTrainCost * (tourTrainCost <= 332 && tourTrainCost > 188 ? 1 : 0)
                    + Coef_cost3 * tourTrainCost * (tourTrainCost <= 476 && tourTrainCost > 332 ? 1 : 0)
                    + Coef_cost4 * tourTrainCost * (tourTrainCost <= 620 && tourTrainCost > 476 ? 1 : 0)
                    + Coef_cost5 * tourTrainCost * (tourTrainCost > 620 ? 1 : 0)
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
