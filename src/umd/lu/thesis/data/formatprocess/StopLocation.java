package umd.lu.thesis.data.formatprocess;

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
 *
 * @author Home
 */
public class StopLocation {
    /*
     * program specific settings
     */

    private final static int bufferSize = 10485760;

    private final static String headerStarts = "cid";
    
    private static Random rand = new Random();

//    private static ArrayList<Integer> randomIDs = new ArrayList<>();

    private final static int expandedLines = 9;

    private final static int m = 13;

    private final static int r = 18;

    private final static int t = 20;

    private final static int u = 21;

    private final static int af = 32;

    private final static int av = 48;

    private final static int aw = 49;

    private final static String business = "business";

    private final static String personalBusiness = "pb";

    private final static String pleasure = "pleasure";

    /*
     * data file specific settings
     */
    private static ArrayList<Integer> idArr = new ArrayList<>();

    private static HashMap<Integer, Integer> msaIdTable = new HashMap<>();

    private static HashMap<Integer, Integer> idMsaTable = new HashMap<>();

    private static HashMap<String, Double> odAirFare = new HashMap<>();

    private static HashMap<String, Double> odAirTime = new HashMap<>();

    private static HashMap<String, Double> odCarTime = new HashMap<>();

    private static HashMap<String, Double> odDriveCost = new HashMap<>();

    private static HashMap<String, Double> odStopNight = new HashMap<>();

    private static HashMap<String, Double> odTrainTime = new HashMap<>();

    private static HashMap<String, Double> odTrainCost = new HashMap<>();

    public static void main(String[] args) throws Exception {
        loadALT();

        loadHashTables();

        try (FileWriter fw = new FileWriter(ThesisProperties.getProperties("output.file.path"))) {
            BufferedWriter bw = new BufferedWriter(fw, bufferSize);

            // prepare reader to read input file
            BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("data.file.path"), bufferSize);

            // for each line in input file...
            String line = null;
            int count = 0;
            while ((line = br.readLine()) != null) {
                count++;
                if(count % 100 == 0) {
                    System.out.println("Line: " + count);
                }
                // skip header then process each line, write the processed line to 
                // output file.
                if(!line.startsWith(headerStarts)) {
                    int rId = msaIdTable.get(Integer.parseInt(getColumnValue(r, line)));
                    int mId = msaIdTable.get(Integer.parseInt(getColumnValue(m, line)));
                    int uId = msaIdTable.get(Integer.parseInt(getColumnValue(u, line)));
                    if (getColumnValue(av, line).equals("2")
                            && odAirFare.get(rId + "-" + mId) != null
                            && odAirFare.get(mId + "-" + uId) != null
                            && odAirFare.get(rId + "-" + uId) != null
                            || !getColumnValue(av, line).equals("2")) {
                        bw.write(processLine(line));
                    }
                    else {
                        System.out.println("NULL VALUE: r: " + rId + ", m: " + mId + ", u: " + uId);
                        System.out.println("----------: r-m: " + odAirFare.get(rId + "-" + mId)
                                + ", m-u: " + odAirFare.get(mId + "-" + uId)
                                + ", r-u: " + odAirFare.get(rId + "-" + uId));
                    }
                }
                else {
                    bw.write(line + "\tTime\tCost\tChoice\n");
                }
            }

            bw.flush();
            bw.close();
        }
    }

    private static String processLine(String line) throws Exception {
        String lines = "";

        int[] randomIDs = getRandomIDs(getColumnValue(m, line), getColumnValue(r, line), getColumnValue(u, line), getColumnValue(t, line), Integer.parseInt(getColumnValue(av, line)));

        for (int i = 0; i <= expandedLines; i++) {
            if(i == 0) {
                // First line, copy everything, add "time" and "cost" columns in the end
                lines += line + "\t" + calculateTimeColumn(line, null) + "\t" + calculateCostColumn(line, null) + "\t1\n";
            }
            else {
                // Expanded line, find random ID, then add the two columns
                lines += setColumnValue(m, line, Integer.toString(idMsaTable.get(randomIDs[i - 1]))) + "\t" + calculateTimeColumn(line, randomIDs[i - 1]) + "\t" + calculateCostColumn(line, randomIDs[i - 1]) + "\t0\n";
            }
        }
        
        // if there are "Null" values, delete these 10 lines
        if (lines.contains("Null")) {
            lines = "";
        }

        return lines;
    }

    private static int[] getRandomIDs(String excluded1, String excluded2, String excluded3, String excluded4, int avValue) {
        Integer id1 = msaIdTable.get(Integer.parseInt(excluded1));
        Integer id2 = msaIdTable.get(Integer.parseInt(excluded2));
        Integer id3 = msaIdTable.get(Integer.parseInt(excluded3));
        Integer id4 = msaIdTable.get(Integer.parseInt(excluded4));
        int num = expandedLines;
        int[] randomIDs = new int[num];
        
        System.out.println("m: " + id1 + ", r: " + id2 + ", u: " + id3 + ", t: " + id4);

        while (num > 0) {
            int r = rand.nextInt(idArr.size());
            int randID = idArr.get(r).intValue();
            if(randID != id1 && randID != id2 && randID != id3 && randID != id4 && !containsID(new Integer(randID), randomIDs)
                    && (avValue == 2 && odAirFare.get(id2 + "-" + randID) != null && odAirFare.get(randID + "-" + id2) != null && odAirFare.get(randID + "-" + id3) != null || avValue != 2)) {
                randomIDs[num - 1] = randID;
                num--;
            }
        }

        return randomIDs;
    }

    private static boolean containsID(Integer id, int[] randomIDs) {
        for (int i : randomIDs) {
            if(i == id.intValue()) {
                return true;
            }
        }
        return false;
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
        }
        catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private static void loadHashTables() throws IOException {
        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("dictionary.file.path"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("MSA")) {
                    // split[0] - MSA
                    // split[1] - ID
                    // split[2] - ZoneID
                    // split[3] - Employ
                    // split[4] - HHs   
                    String[] split = line.split("\t");

                    // MSA-ID
                    msaIdTable.put(Integer.parseInt(split[0].trim()), Integer.parseInt(split[1].trim()));
                    // ID-MSA
                    idMsaTable.put(Integer.parseInt(split[1].trim()), Integer.parseInt(split[0].trim()));
                }
            }
        }

        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("airfare.file.path"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("O")) {
                    String[] split = line.split("\t");
                    odAirFare.put(split[0].trim() + "-" + split[1].trim(), Double.parseDouble(split[2].trim()));
                }
            }
        }

        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("others.file.path"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("O")) {
                    String[] split = line.split("\t");
                    // Column C
                    odAirTime.put(split[0].trim() + "-" + split[1].trim(), Double.parseDouble(split[2].trim()));
                    // Column D
                    odCarTime.put(split[0].trim() + "-" + split[1].trim(), Double.parseDouble(split[3].trim()));
                    // Column E
                    odDriveCost.put(split[0].trim() + "-" + split[1].trim(), Double.parseDouble(split[4].trim()));
                    // Column F
                    odStopNight.put(split[0].trim() + "-" + split[1].trim(), Double.parseDouble(split[5].trim()));
                }
            }
        }
        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("train.file.path"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("SA")) {
                    String[] split = line.split("\t");
                    // Column D
                    odTrainCost.put(split[0].trim() + "-" + split[1].trim(), Double.parseDouble(split[3].trim()));
                    // Column E
                    odTrainTime.put(split[0].trim() + "-" + split[1].trim(), Double.parseDouble(split[4].trim()));
                }
            }
        }
    }

    private static String getColumnValue(int column, String line) throws Exception {
        int pos = 0;
        int c = 1;
        while (c < column) {
            pos = line.indexOf("\t", pos + 1);
            c++;
        }
        int begin = 0;
        int end = 0;
        if(pos != -1) {
            begin = pos;
            end = line.indexOf("\t", pos + 1);
            return end == -1 ? line.substring(begin).trim() : line.substring(begin, end).trim();
        }
        else {
            throw new Exception("Can't locate column " + column + " in line: " + line);
        }
    }

    private static String setColumnValue(int column, String line, String value) throws Exception {
        int pos = 0;
        int c = 1;
        while (c < column) {
            pos = line.indexOf("\t", pos + 1);
            c ++;
        }
        int begin = 0;
        int end = 0;
        if(pos != -1) {
            begin = pos;
            end = line.indexOf("\t", pos + 1);
            String prefix = line.substring(0, begin + 1);
            String suffix = end == -1 ? "" : line.substring(end);
            return prefix + value + suffix;
        }
        else {
            throw new Exception("Can't locate column " + column + " in line: " + line); 
        }
    }

    private static String calculateTimeColumn(String line, Integer id) throws Exception {
        try {
        Double m_r = 0.0;
        Double m_u = 0.0;
        Double r_u = 0.0;
        int mId = msaIdTable.get(Integer.parseInt(getColumnValue(m, line)));
        int rId = msaIdTable.get(Integer.parseInt(getColumnValue(r, line)));
        int uId = msaIdTable.get(Integer.parseInt(getColumnValue(u, line)));
        int avValue = Integer.parseInt(getColumnValue(av, line));

        if(id != null) {
            // Expanded line, get random M column value
            m_r = getPairTime(id, rId, avValue);
            m_u = getPairTime(id, uId, avValue);
        }
        else {
            // Original line
            m_r = getPairTime(mId, rId, avValue);
            m_u = getPairTime(mId, uId, avValue);
        }
        r_u = getPairTime(rId, uId, avValue);
        
        if (m_r == null) {
            m_r = mId == rId ? 0.0 : null;
        }
        if (m_u == null) {
            m_u = mId == uId ? 0.0 : null;
        }
        if (r_u == null) {
            r_u = rId == uId ? 0.0 : null;
        }

        if(m_r == null || m_u == null || r_u == null) {
            return "Null(m_r = "
                   + (m_r == null ? "Null" : Double.toString(m_r)) + ", m_u = "
                   + (m_u == null ? "Null" : Double.toString(m_u)) + ", r_u = "
                   + (r_u == null ? "Null" : Double.toString(r_u)) + ", rId = "
                   + rId + ", mId = " + mId + ", uId = " + uId + ")";
        }
        else {
            return Double.toString(m_r + m_u - r_u);
        }
        }
        catch (java.lang.NullPointerException e){
            System.out.println("mValue: " + getColumnValue(m, line) + ", Line: " + line);
            throw e;
        }
    }

    private static String calculateCostColumn(String line, Integer id) throws Exception {
        Double r_m = 0.0;
        Double m_u = 0.0;
        Double r_u = 0.0;
        Double income = 0.0;
        int mId = msaIdTable.get(Integer.parseInt(getColumnValue(m, line)));
        int rId = msaIdTable.get(Integer.parseInt(getColumnValue(r, line)));
        int uId = msaIdTable.get(Integer.parseInt(getColumnValue(u, line)));
        int avValue = Integer.parseInt(getColumnValue(av, line));
        int afValue = Integer.parseInt(getColumnValue(af, line));
        String awValue = getColumnValue(aw, line);

        if(id != null) {
            // Expanded line, get random M column value
            r_m = getPairCost(rId, id, avValue, afValue, awValue);
            m_u = getPairCost(id, uId, avValue, afValue, awValue);
        }
        else {
            // Original line
            r_m = getPairCost(rId, mId, avValue, afValue, awValue);
            m_u = getPairCost(mId, uId, avValue, afValue, awValue);
        }
        r_u = getPairCost(rId, uId, avValue, afValue, awValue);
               
        if (r_m == null) {
            r_m = mId == rId ? 0.0 : null;
        }
        if (m_u == null) {
            m_u = mId == uId ? 0.0 : null;
        }
        if (r_u == null) {
            r_u = rId == uId ? 0.0 : null;
        }
        
        if(r_m == null || m_u == null || r_u == null) {
            return "Null(m_r = "
                   + (r_m == null ? "Null" : Double.toString(r_m)) + ", m_u = "
                   + (m_u == null ? "Null" : Double.toString(m_u)) + ", r_u = "
                   + (r_u == null ? "Null" : Double.toString(r_u)) + ", rId = "
                   + rId + ", mId = " + mId + ", uId = " + uId + ")";
        }
        else {
            return Double.toString(r_m + m_u - r_u);
        }
    }

    private static Double getPairTime(int id1, int id2, int av) {
        String key = Integer.toString(id1) + "-" + Integer.toString(id2);
        if(av == 1) {
            return odCarTime.get(key) + odStopNight.get(key) * 12;
        }
        else if(av == 2) {
            return odAirTime.get(key);
        }
        else {
            return odTrainTime.get(key);
        }
    }

    private static Double getPairCost(Integer id1, int id2, int av, int af, String aw) {
        String key = Integer.toString(id1) + "-" + Integer.toString(id2);
        Double incParam = getIncomeParam(af, aw);
        if(av == 1) {
            if(aw.equalsIgnoreCase(business)) {
                return odDriveCost.get(key) + odStopNight.get(key) * incParam;
            }
            else {
                return odDriveCost.get(key) / 2 + odStopNight.get(key) * incParam;
            }
        }
        else if(av == 2) {
            if (odAirFare.get(key) == null) {
                System.out.println("Key: " + key);
            }
            return odAirFare.get(key) / 2;
        }
        else {
            return odTrainCost.get(key);
        }
    }

    private static Double getIncomeParam(int af, String aw) {
        if(aw.equalsIgnoreCase(business)) {
            if(af <= 30000) {
                return 70.0;
            }
            if(af <= 70000 && af > 30000) {
                return 90.0;
            }
            if(af > 70000) {
                return 110.0;
            }
        }
        else {
            if(af <= 30000) {
                return 30.0;
            }
            if(af <= 70000 && af > 30000) {
                return 50.0;
            }
            if(af > 70000) {
                return 70.0;
            }
        }
        return null;
    }
}
