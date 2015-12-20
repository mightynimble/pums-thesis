/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.pums2010.objects.Person2010;
import umd.lu.thesis.simulation.app2000.objects.TripType;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import java.lang.*;
/**
 *
 * @author Media
 */
public class DurPredictStDev {

    private final static Logger sLog = LogManager.getLogger(DurationPrediction.class);

    private final static int runCounts = 10000;

    private final static int obsCount = 30;

    public DurPredictStDev() {
    }

    public static void main(String[] args) throws Exception {
        DurationPrediction dp = new DurationPrediction();

        File f = new File(ThesisProperties.getProperties("simulation.pums2010.duration.prediction.output"));
        String line;
        Person2010 p = new Person2010();
        int d = -1;
        int toy = -1;
        // init predictionDiffs for all runs; this is the final result.
        HashMap<Integer, double[]> predictionDiffs = new HashMap<>();
        for (int i = 0; i < obsCount; i++) {
            double[] tmpList = new double[runCounts];
            predictionDiffs.put(i, tmpList);
        }
        for (int count = 0; count < runCounts; count++) {
            System.out.println("  " + count + "/" + runCounts);
            // init observations and predictions for each run/file
            HashMap<Integer, Double> observations = new HashMap<>();
            HashMap<Integer, Double> predictions = new HashMap<>();
            for (int i = 0; i < obsCount; i++) {
                observations.put(i, 0.0);
                predictions.put(i, 0.0);
            }
            try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.duration.prediction.input"));
                    BufferedReader br = new BufferedReader(new InputStreamReader(fstream));) {
                while ((line = br.readLine()) != null) {
                    if (!line.toLowerCase().startsWith("id")) {
                        // Columns needed: EG(137), BO(67), BR(70), BW(75), CP(94), CZ(104), CG(85)
                        d = Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.eg, line));
                        p.setHhType(Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.bo, line)));
                        p.setNp(Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.br, line)));
                        p.setIncLevel(Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.bw, line)));
                        p.setEmpStatus(Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.cp, line)));
                        toy = Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.cz, line));
                        p.setAge(Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.cg, line)));

                        Integer durPrediction = dp.findTourDuration(p, d, TripType.BUSINESS, toy);
                        Integer durObservation = Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.db, line)) > 29 ? 29 : Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.db, line));
                        observations.put(durObservation, observations.get(durObservation) + 1);
                        // durPrediction is 1 to 30. Hence the minus 1.
                        predictions.put(durPrediction - 1, predictions.get(durPrediction - 1) + 1);
                    }
                }
                // calculate diffs:
                for (int i = 0; i < obsCount; i++) {
                    predictionDiffs.get(i)[count] = Math.abs(observations.get(i) - predictions.get(i));
                }
                br.close();
            } catch (Exception ex) {
                System.out.println("---------------------" + d);
                sLog.error(ex.getLocalizedMessage(), ex);
                System.exit(1);
            }
        }

        // output results
        try (FileWriter fw = new FileWriter(f);
                BufferedWriter bw = new BufferedWriter(fw)) {
            for (int i = 0; i < obsCount; i++) {
                StandardDeviation stDevInstance = new StandardDeviation();
                double stDev = stDevInstance.evaluate(predictionDiffs.get(i));
                Mean avgInstance = new Mean();
                double avg = avgInstance.evaluate(predictionDiffs.get(i));
                bw.write(i + "\t" + avg + "\t"+ stDev + "\n");
            }

        } catch (Exception ex) {
            System.out.println("---------------------" + d);
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
    }
}
