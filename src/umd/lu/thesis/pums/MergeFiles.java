package umd.lu.thesis.pums;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import umd.lu.thesis.common.ThesisConstants;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.helper.FileUtils;

/**
 *
 * @author lousia
 */
public class MergeFiles {

    private static final int[] pColumns = {1, 2, 3, 6, 12, 14, 34, 102, 144, 160};
    private static final int[] hColumns = {1, 2, 4, 5, 6, 7, 8, 10, 21, 91, 98, 99};

    public static void main(String[] args) throws Exception {
        long hCount = 0;
        long pCount = 0;

        // open output files to write
        try {
            FileWriter fw_p = new FileWriter(ThesisProperties.getProperties("merged.p.file.path"));
            BufferedWriter bw_p = new BufferedWriter(fw_p, ThesisConstants.bufferSize);
            FileWriter fw_h = new FileWriter(ThesisProperties.getProperties("merged.h.file.path"));
            BufferedWriter bw_h = new BufferedWriter(fw_h, ThesisConstants.bufferSize);
            File pumsOutputFolder = new File(ThesisProperties.getProperties("pums.output.folder"));

            for (File file : pumsOutputFolder.listFiles()) {
                System.out.println("File - " + file.getName());

                if (file.getName().contains(".output.h.txt")) {
                    // H file, simply merge together.
                    BufferedReader br_h = new BufferedReader(new FileReader(file), ThesisConstants.bufferSize);
                    String line = "";
                    int subTotal = 0;
                    while ((line = br_h.readLine()) != null) {
                        bw_h.write(generateNewLine(line, hColumns));
                        subTotal += 1;
                        if (subTotal % 100000 == 1) {
                            System.out.println("     - line " + subTotal);
                        }
                        hCount += 1;
                    }
                    System.out.println("     - SubTotal: " + subTotal);
                    bw_h.flush();
                } else if (file.getName().contains(".output.p.txt")) {
                    // P file, expand based on column 6.
                    BufferedReader br_p = new BufferedReader(new FileReader(file), ThesisConstants.bufferSize);
                    String line = "";
                    int subTotal = 0;
                    while ((line = br_p.readLine()) != null) {
                        int columnSix = Integer.parseInt(ExcelUtils.getColumnValue(6, line));

                        for (int i = 0; i < columnSix; i++) {
                            bw_p.write(generateNewLine(line, pColumns));
                            subTotal += 1;
                            if (subTotal % 500000 == 1) {
                                System.out.println("     - line " + subTotal);
                            }
                            pCount += 1;
                        }
                    }
                    System.out.println("     - SubTotal: " + subTotal);
                    bw_p.flush();
                }
            }
            bw_h.close();
            bw_p.close();
            System.out.println("+++++++++++ H Files Total: " + hCount);
            System.out.println("+++++++++++ P Files Total: " + pCount);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private static String generateNewLine(String line, int[] columns) throws Exception {
        String newLine = "";
        for (int i = 0; i < columns.length; i++) {
            newLine += ExcelUtils.getColumnValue(columns[i], line);
            if (i == columns.length - 1) {
                newLine += "\t0\t0\t0\t0\t0\n";
            } else {
                newLine += "\t";
            }
        }
        return newLine;
    }
}
