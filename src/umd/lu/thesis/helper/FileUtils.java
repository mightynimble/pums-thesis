package umd.lu.thesis.helper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import umd.lu.thesis.common.ThesisProperties;

/**
 *
 * @author Bo Sun
 */
public class FileUtils {

    public static BufferedReader openFileToRead(String inFile, int bufferSize) {
        BufferedReader br = null;
        try {
            if(bufferSize > 0) {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(inFile)), bufferSize);
            }
            else {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(inFile)), 10485760);
            }
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        return br;
    }

    public static BufferedWriter openFileToWrite(String outFile, int bufferSize) {
        BufferedWriter bw = null;
        try (FileWriter fw = new FileWriter(outFile)) {
            if(bufferSize > 0) {
                bw = new BufferedWriter(fw, bufferSize);
            }
            else {
                bw = new BufferedWriter(fw, 10485760);
            }
        }
        catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return bw;
    }
}
