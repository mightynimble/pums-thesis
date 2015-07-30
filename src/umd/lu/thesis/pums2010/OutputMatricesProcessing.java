/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.pums2010.math.Math;
import umd.lu.thesis.pums2010.objects.ModeChoice;
import umd.lu.thesis.pums2010.objects.Quarter;
import umd.lu.thesis.pums2010.objects.TravelMode;
import umd.lu.thesis.simulation.app2000.objects.TripType;

/**
 *
 * @author Home
 */
public class OutputMatricesProcessing {

    private final static Logger sLog = LogManager.getLogger(OutputMatricesProcessing.class);

    private static HashMap<String, HashMap<String, Integer>> results;

    public static void main(String[] args) throws Exception {
        sLog.info("Start to process output files from NationalTravelDemand.");
        sLog.info("  Initializing results map...");
        results = new HashMap<>();
        HashMap<String, Integer> subset = new HashMap<>();
        for (int m = 0; m < TravelMode.itemCount; m++) {
            for (int q = 0; q < Quarter.itemCount; q++) {
                for (int t = 0; t < TripType.itemCount; t++) {
                    results.put(TravelMode.values()[m] + "-"
                                + Quarter.values()[q] + "-"
                                + TripType.values()[t], new HashMap<String, Integer>());
                }
            }
        }
        sLog.info("  Done.");

        File outputDir = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir"));
        sLog.info("  Output file location: " + outputDir.getAbsolutePath());
        for (File f : outputDir.listFiles()) {
            String[] splits = f.getName().split("-");
            String key = splits[0] + "-" + splits[1] + "-" + splits[2];
            sLog.info("    Parse file: " + f.getName() + ", key: " + key);
            try (FileReader fr = new FileReader(f); BufferedReader br = new BufferedReader(fr)) {
                String line;
                int row = 1;
                String subKey;
                int val;
                while ((line = br.readLine()) != null) {
                    for (int col = 1; col <= Math.alt; col++) {
                        val = ExcelUtils.getColumnValue(col, line).equalsIgnoreCase("null")
                              ? 0 : Integer.parseInt(ExcelUtils.getColumnValue(col, line));
                        subKey = col + "-" + row;
                        if(results.get(key).get(subKey) == null || results.get(key).get(subKey) == 0) {
                            results.get(key).put(subKey, val);
                        }
                        else {
                            results.get(key).put(subKey, results.get(key).get(subKey) + 1);
                        }
                    }
                    row++;
                }
            }
            catch (IOException e) {
                sLog.error("    Error: " + e.getLocalizedMessage(), e);
                System.exit(1);
            }
        }

        sLog.info("  Write results to file.");


        Map<String, List<Integer>> subtotalRowMap = new HashMap<>();
        Map<String, List<Integer>> subtotalColMap = new HashMap<>();
        for (int i = 0; i <= Math.alt; i++) {
        }

        int totalTrips = 0;
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String totalTripsFilename = timestamp + "-statistic-total-trips.txt";
        File totalTripsFile = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + totalTripsFilename);
        String subtotalColFilename = timestamp + "-statistic-subtotal-col.txt";
        File subtotalColFile = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + subtotalColFilename);
        String subtotalRowFilename = timestamp + "-statistic-subtotal-row.txt";
        File subtotalRowFile = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + subtotalRowFilename);
        for (int mc = 0; mc < ModeChoice.itemCount; mc++) {
            for (int toy = 0; toy < 4; toy++) {
                for (int type = 0; type < TripType.itemCount; type++) {
                    totalTrips = 0;
                    String key = ModeChoice.values()[mc] + "-" + Quarter.values()[toy] + "-" + TripType.values()[type];

                    if(subtotalRowMap.get(key) == null) {
                        List<Integer> tmpList = new ArrayList<>();
                        for (int tmp = 0; tmp <= Math.alt; tmp++) {
                            tmpList.add(0);
                        }
                        subtotalRowMap.put(key, tmpList);

                    }
                    if(subtotalColMap.get(key) == null) {
                        List<Integer> tmpList = new ArrayList<>();
                        for (int tmp = 0; tmp <= Math.alt; tmp++) {
                            tmpList.add(0);
                        }
                        subtotalColMap.put(key, tmpList);
                    }

                    String fileName = timestamp + "-final-" + key + ".txt";
                    File f = new File(ThesisProperties.getProperties("simulation.pums2010.output.dir") + fileName);
                    try (FileWriter fw = new FileWriter(f);
                         BufferedWriter bw = new BufferedWriter(fw);
                         FileWriter totalFw = new FileWriter(totalTripsFile, true);
                         BufferedWriter totalBw = new BufferedWriter(totalFw);) {

                        // matrix
                        for (int row = 1; row <= Math.alt; row++) {
                            for (int col = 1; col <= Math.alt; col++) {
                                bw.write(results.get(key).get(col + "-" + row) + "\t");
                                subtotalRowMap.get(key).set(row, subtotalRowMap.get(key).get(row) + results.get(key).get(col + "-" + row));
                                subtotalColMap.get(key).set(col, subtotalColMap.get(key).get(col) + results.get(key).get(col + "-" + row));
                                totalTrips += results.get(key).get(col + "-" + row);
                            }
                            bw.write("\n");
                        }
                        bw.flush();

                        // total trips
                        totalBw.write(key + "\t" + totalTrips + "\n");
                        totalBw.flush();

                    }
                    catch (IOException ex) {
                        sLog.error("Failed to write to file: " + ThesisProperties.getProperties("simulation.pums2010.output.dir"), ex);
                        System.exit(1);
                    }
                }
            }
        }

        // subtotal cols and rows
        String[] colContent = new String[Math.alt + 1];
        for (int tmp = 0; tmp < colContent.length; tmp++) {
            colContent[tmp] = "";
        }
        String[] rowContent = new String[Math.alt + 1];
        for (int tmp = 0; tmp < rowContent.length; tmp++) {
            rowContent[tmp] = "";
        }
        for (int mc = 0; mc < ModeChoice.itemCount; mc++) {
            for (int toy = 0; toy < 4; toy++) {
                for (int type = 0; type < TripType.itemCount; type++) {
                    String key = ModeChoice.values()[mc] + "-" + Quarter.values()[toy] + "-" + TripType.values()[type];
                    for (int lineNum = 0; lineNum <= Math.alt; lineNum++) {
                        if(lineNum == 0) {
                            colContent[lineNum] += key + "\t";
                            rowContent[lineNum] += key + "\t";
                        }
                        else {
                            colContent[lineNum] += subtotalColMap.get(key).get(lineNum) + "\t";
                            rowContent[lineNum] += subtotalRowMap.get(key).get(lineNum) + "\t";
                        }
                    }
                }
            }
        }
        try (FileWriter subtotalColFw = new FileWriter(subtotalColFile, true);
             BufferedWriter subtotalColBw = new BufferedWriter(subtotalColFw);
             FileWriter subtotalRowFw = new FileWriter(subtotalRowFile, true);
             BufferedWriter subtotalRowBw = new BufferedWriter(subtotalRowFw);) {
            for (String content : colContent) {
                subtotalColBw.write(content + "\n");
            }
            subtotalColBw.flush();
            for (String content : rowContent) {
                subtotalRowBw.write(content + "\n");
            }
            subtotalRowBw.flush();
        }
        catch (IOException ex) {
            sLog.error("Failed to write to file: " + ThesisProperties.getProperties("simulation.pums2010.output.dir"), ex);
            System.exit(1);
        }
        sLog.info("Completed processing output files from NationalTravelDemand.");
    }
}
