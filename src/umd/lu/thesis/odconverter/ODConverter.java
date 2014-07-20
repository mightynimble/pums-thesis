package umd.lu.thesis.odconverter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 *
 * @author Bo Sun
 */
public class ODConverter {

    private static final String inFile = "C:\\Users\\Home\\Desktop\\4th Quarter Air Fare OD Table.csv";

    private static final String outFile = "C:\\Users\\Home\\Desktop\\4_out.csv";

    /**
     * OD Converter Entrance
     * 
     * Convert csv file like this:
     *    | O1 | O2 | O3  ...
     * ---+----+----+-------
     * D1 | 2  | 4  |...
     * ---+----+----+-------
     * D2 |    |    |
     * ---+----+----+-------
     * 
     * To:
     * D1 | O1 | 2
     * ---+----+---
     * D1 | O2 | 4
     * ---+----+---
     * ... ... ...
     * 
     * @param args
     */
    public static void main(String[] args) throws IOException {
        int totalOs = -1;
        int lineNum = 0;
        String[] headers = null;

        ArrayList<ODPair> outPairs = new ArrayList<>();

        // open file
        try (FileInputStream fstream = new FileInputStream(inFile)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                lineNum++;
                String[] tokens = line.split(",");
                if(tokens[0].length() == 0) {
                    // This is the header
                    headers = tokens.clone();
                    totalOs = tokens.length - 1;
                }
                else {
                    // Data line. tokens[0] will be the D
                    // Loop to find the O
                    for (int i = 1; i < tokens.length; i++) {
                        if(tokens[i].length() != 0) {
                            ODPair p = new ODPair();
                            p.setD(tokens[0]);
                            p.setO(headers[i]);
                            p.setValue(tokens[i]);
                            outPairs.add(p);
                            System.out.println(p.getD() + "\t" + p.getO() + "\t" + p.getValue() + "\r\n");
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        FileWriter fstream = new FileWriter(outFile);
        try (BufferedWriter out = new BufferedWriter(fstream)) {
            for (ODPair p : outPairs) {
                out.write(p.getD() + "," + p.getO() + "," + p.getValue() + "\r\n");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
