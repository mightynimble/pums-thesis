/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.split;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import umd.lu.thesis.common.ThesisProperties;

/**
 *
 * @author lousia
 */
public class ProcessSplitFiles {

    private static final String rootPath = "/Users/lousia/Desktop/";
    private static final String prefix = "split_file_";
    private static final String outputFile = "/Users/lousia/Desktop/result.txt";
    private static final double size = 70.0;

    private final static Logger sLog = LogManager.getLogger(ProcessSplitFiles.class);

    public static void main(String[] args) throws Exception {

        List<String> files = findFiles();

        List<String> resultSet = new ArrayList<>();

        int fileCount = 1;
        for (String f : files) {
            double oneActual = 0;
            double twoActual = 0;
            double threeActual = 0;
            double onePredict = 0;
            double twoPredict = 0;
            double threePredict = 0;
            try (FileInputStream fstream = new FileInputStream(f); BufferedReader br = new BufferedReader(new InputStreamReader(fstream));) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    String[] splits = line.split("\t");
                    String colB = splits[2];
                    String colC = splits[3];
                    if (colB.startsWith("1")) {
                        oneActual++;
                    } else if (colB.startsWith("2")) {
                        twoActual++;
                    } else if (colB.startsWith("3")) {
                        threeActual++;
                    }
                    if (colC.startsWith("1")) {
                        onePredict++;
                    } else if (colC.startsWith("2")) {
                        twoPredict++;
                    } else if (colC.startsWith("3")) {
                        threePredict++;
                    }
                }
                resultSet.add(fileCount + "\t"
                        + oneActual / size + "\t" + onePredict / size + "\t"
                        + twoActual / size + "\t" + twoPredict / size + "\t"
                        + threeActual / size + "\t" + threePredict / size + "\n"
                );
            } catch (FileNotFoundException ex) {
                sLog.info("File not found: " + f);
            } catch (IOException ex) {
                sLog.error(ex.getLocalizedMessage(), ex);
                System.exit(1);
            }
            fileCount ++;
        }

        try (FileWriter fw = new FileWriter(outputFile); BufferedWriter bw = new BufferedWriter(fw)) {
            for (String line : resultSet) {
                bw.write(line);
                bw.flush();
            }
            
        } catch (IOException ex) {
            sLog.error("Failed to write to file: " + outputFile, ex);
            System.exit(1);
        }
    }

    private static List<String> findFiles() {
        File[] files = new File(rootPath).listFiles();
        List<String> qualifiedFiles = new ArrayList<>();
        for (File f : files) {
            if (f.getAbsolutePath().startsWith(rootPath + prefix)) {
                qualifiedFiles.add(f.getAbsolutePath());
            }
        }
        return qualifiedFiles;
    }
}
