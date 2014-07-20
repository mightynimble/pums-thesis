package umd.lu.thesis.data.formatprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.FileUtils;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author Home
 */
public class PreProcessStopsFile {
    
    /*
     * program specific settings
     */
    private final static int bufferSize = 10485760;
    private final static int maxQueueSize = 10;
    
    /*
     * data file specific settings
     */
    private final static String headerStarts = "cid";
    private final static int a = 1;
    private final static int h = 8;
    private final static int i = 9;
    private final static int m = 13;
    private final static int q = 17;
    private final static int maxStops = 4;
    private final static ArrayList<String> invalidStopPurposeList = new ArrayList<>(Arrays.asList("0", "#N/A"));
    
    public static void main(String[] args) throws Exception {
        // Initialize queue
        BlockingQueue<String> queue = new ArrayBlockingQueue<String>(maxQueueSize);
        
        try (FileWriter fw = new FileWriter(ThesisProperties.getProperties("transition.file.path"))) {
            BufferedWriter bw = new BufferedWriter(fw, bufferSize);
            
            // prepare reader to read input file
            BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("data.file.path"), bufferSize);
            
            // Read headers
            String line = br.readLine();
            if (line != null && line.startsWith(headerStarts)) {
                bw.write(line + "\n");
            }
            
            int lineCount = 0;
            // Now data...
            boolean keepReading = true;
            ArrayList<String> block;
            while (keepReading && queue.size() < maxQueueSize) {
                while (keepReading && queue.size() < maxQueueSize) {
                    line = br.readLine();
                    lineCount++;
                    if (lineCount > 13912) {
                        System.out.println("Line: " + lineCount);
                    }
                    if(line != null) {
                        queue.add(line);
                    }
                    else {
                        // End of file
                        block = processQueue(queue);
                        for (String processedLine : block) {
                            bw.write(processedLine + "\n");
                        }
                        keepReading = false;
                    }   
                }
                if (keepReading) {
                    block = processQueue(queue);
                    for (String processedLine : block) {
                        bw.write(processedLine + "\n");
                    }
                }
            }
//            block = processQueue(queue);
//            for (String processedLine : block) {
//                bw.write(processedLine + "\n");
//            }
            bw.flush();
            bw.close();
        }
    }
    
    private static ArrayList<String> processQueue(BlockingQueue<String> queue) throws Exception {
        
        ArrayList<String> cidGroup = getCidGroup(queue);
        if (cidGroup != null) {
            ArrayList<String> block = processCidGroup(cidGroup);
            return block;
        }
        return null;
    }
    
    private static ArrayList<String> getCidGroup(BlockingQueue<String> queue) throws Exception {
        ArrayList<String> cidGroup = new ArrayList<>();
                
        String firstLine = queue.poll();
        String firstCid = getColumnValue(a, firstLine);
        cidGroup.add(firstLine);
        
        String peekedCid = getColumnValue(a, queue.peek());
        while (queue.peek() != null) {
            if(firstCid.equals(peekedCid)) {
                cidGroup.add(queue.poll());
                if (queue.peek() != null) {
                    peekedCid = getColumnValue(a, queue.peek());
                }
                else {
                    peekedCid = null;
                }
            }
            else {
                return cidGroup;
            }
        }
        
        return cidGroup;
    }
    
    private static ArrayList<String> processCidGroup(ArrayList<String> cidGroup) throws Exception {
        ArrayList<String> block = new ArrayList<>();
        int stopCount = 0;
        for (String line : cidGroup) {
            String currentStopPurpose = getColumnValue(q, line);
            if(invalidStopPurposeList.contains(currentStopPurpose)) {
                // Invalid stop purpose, skip.
                continue;
            }
            else {
                stopCount++;
            }
        }
        int stopIndex = 1;
        for (String line : cidGroup) {
            String currentStopPurpose = getColumnValue(q, line);
            if (invalidStopPurposeList.contains(currentStopPurpose)) {
                // Invalid stop purpose, skip.
                continue;
            }
            else {
                // Valid
                String processedLine = setColumnValue(q, line, currentStopPurpose);
                processedLine = setColumnValue(i, processedLine, Integer.toString(stopIndex));
                processedLine = setColumnValue(h, processedLine, Integer.toString(stopCount));
                block.add(processedLine);
                stopIndex++;
            }
        }
        
        return block;
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
            return line.substring(begin, end).trim();
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
            String suffix = line.substring(end);
            return prefix + value + suffix;
        }
        else {
            throw new Exception("Can't locate column " + column + " in line: " + line); 
        }
    }
}


