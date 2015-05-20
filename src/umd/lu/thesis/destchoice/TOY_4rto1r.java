package umd.lu.thesis.destchoice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.helper.FileUtils;

/**
 *
 * @author lousia
 */
public class TOY_4rto1r {

    private final static int bufferSize = 10485760;

    public static void main(String[] args) throws IOException {
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
                    String line2 = br.readLine();
                    String line3 = br.readLine();
                    String line4 = br.readLine();
                    bw.write(processLine(line, line2, line3, line4));
                }
            }

            bw.flush();
        }
    }

    private static String prepareHeader() {
        // the very beginning five columns
        String header = "id\tuniqueid\tPerson_Char_woIntel_HHPer\tHouseholdId\tPersonid\tTripId\tO_MSA/NMSA\tD_MSA/NMSA\tINC\tLowInc\tMedInc\tHighInc\tTOY\tHHType\tAge1\tEmployStatus\tSex\tLGS1\tLGS2\tLGS3\tLGS4\tChoice\n";
        return header;
    }
    
    private static String processLine(String line, String line2, String line3, String line4) {
        // copy first 17 columns
        String outLine = "";
        for (int i = 1; i < 18; i ++) {
            outLine += ExcelUtils.getColumnValue(i, line) + "\t";
        }
        
        // LGS column
        outLine += ExcelUtils.getColumnValue(18, line) + "\t";
        outLine += ExcelUtils.getColumnValue(18, line2) + "\t";
        outLine += ExcelUtils.getColumnValue(18, line3) + "\t";
        outLine += ExcelUtils.getColumnValue(18, line4) + "\t";
        
        // flag column
        if (ExcelUtils.getColumnValue(19, line).equals("1")) {
            outLine += ExcelUtils.getColumnValue(20, line) + "\n";
        }
        else if (ExcelUtils.getColumnValue(19, line2).equals("1")) {
            outLine += ExcelUtils.getColumnValue(20, line2) + "\n";
        }
        else if (ExcelUtils.getColumnValue(19, line3).equals("1")) {
            outLine += ExcelUtils.getColumnValue(20, line3) + "\n";
        }
        else if (ExcelUtils.getColumnValue(19, line4).equals("1")) {
            outLine += ExcelUtils.getColumnValue(20, line4) + "\n";
        }
        
        return outLine;
    }
}
