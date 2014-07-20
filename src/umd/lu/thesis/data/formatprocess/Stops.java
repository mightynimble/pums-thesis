package umd.lu.thesis.data.formatprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.FileUtils;

/**
 *
 * @author Home
 */
public class Stops {

    private final static int bufferSize = 10485760;

    private final static int columnStopsTo = 7;

    private final static int h = 8;

    private final static int i = 9;

    private final static int j = 10;

    private final static int k = 11;

    private final static int l = 12;

    private final static int m = 13;

    private final static int n = 14;

    private final static int o = 15;

    private final static int p = 16;

    private final static int q = 17;

    private final static int r = 18;

    private final static int s = 19;

    private final static int t = 20;

    private final static int u = 21;

    private final static int v = 22;

    private final static int w = 23;

    private final static int x = 24;

    private final static int y = 25;

    private final static int z = 26;

    private final static int aa = 27;

    private final static int ab = 28;

    private final static int ac = 29;

    private final static int ad = 30;

    private final static int ae = 31;

    private final static int af = 32;

    private final static int ag = 33;

    private final static int ah = 34;

    private final static int ai = 35;

    public static void main(String[] args) throws IOException {
        try (FileWriter fw = new FileWriter(ThesisProperties.getProperties("output.file.path"))) {
            BufferedWriter bw = new BufferedWriter(fw, bufferSize);

            // prepare reader to read input file
            BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("data.file.path"), bufferSize);

            // for each line in input file...
            String line = null;
            while ((line = br.readLine()) != null) {
                // skip header then process each line, write the processed line to 
                // output file.
                if(!line.startsWith("HouseholdId")) {
                    bw.write(processLine(line));
                }
                else {
                    bw.write(prepareHeader());
                }
            }
            bw.flush();
        }
    }

    private static String prepareHeader() {
        String header = "HouseholdId\tPersonid\tTripId\tPersonTripId\tPerTrips\tPersonTripWeight\tStopsTo\tStopsNo\tValue1\tValue2\tValue3\tValue4\tValue5\tValue6\tValue7\t\n";
        return header;
    }

    /**
     * Every "line" will turn into 7 new lines.
     * 
     * @param in input line
     * @return 7 output lines (in a string)
     */
    private static String processLine(String in) {
        String processedLine = "";
        int stopsTo = Integer.parseInt(getColumnValue(columnStopsTo, in));

        for (int i = 1; i <= stopsTo; i++) {
            processedLine += writeFixedColumns(in);
            processedLine += i + "\t";
            processedLine += getColumnValues(h + 7 * (i - 1), n + 7 * (i - 1), in);
            processedLine += "\n";
        }

        return processedLine;
    }

    private static String writeFixedColumns(String in) {
        String fixed = "";
        // A - HouseholdId
        fixed += getColumnValue(1, in) + "\t";
        // B - Personid
        fixed += getColumnValue(2, in) + "\t";
        // C - TripId
        fixed += getColumnValue(3, in) + "\t";
        // D - PersonTripId
        fixed += getColumnValue(4, in) + "\t";
        // E - PerTrips
        fixed += getColumnValue(5, in) + "\t";
        // F - PersonTripWeight
        fixed += getColumnValue(6, in) + "\t";
        // F - StopsTo
        fixed += getColumnValue(7, in) + "\t";

        return fixed;
    }

    /**
     * find the value of the specific column from the given line column starts 
     * from 1.
     * @param column
     * @param in
     * @return 
     */
    private static String getColumnValue(int column, String in) {
        String[] split = in.split("\t");
        return split[column - 1];
    }

    private static String getColumnValues(int startColumn, int endColumn, String in) {
        String rtn = "";
        for (int i = startColumn; i <= endColumn; i++) {
            rtn += getColumnValue(i, in) + "\t";
        }
        return rtn;
    }
}
