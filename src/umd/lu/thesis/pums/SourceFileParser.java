package umd.lu.thesis.pums;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import umd.lu.thesis.common.ThesisConstants;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.FileUtils;
import umd.lu.thesis.pums.objects.ColumnPosition;
import umd.lu.thesis.exceptions.*;

/**
 *
 * @author Home
 */
public class SourceFileParser {

    private final static List<ColumnPosition> pColPositions = new ArrayList<>();

    private final static List<ColumnPosition> hColPositions = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        initColumnPositions();

        File inputFolder = new File(ThesisProperties.getProperties("pums.input.folder"));
        File outputFolder = new File(ThesisProperties.getProperties("pums.output.folder"));
        for (File inFile : inputFolder.listFiles()) {
            if (inFile.getName().toLowerCase().endsWith(".txt")) {
                System.out.println("++++ Processing File: " + inFile.getName());
                String outFileName = outputFolder.getAbsolutePath() + "/" + inFile.getName().substring(0, inFile.getName().length() - 3) + "output";
                try {
                    FileWriter fw_h = new FileWriter(outFileName + ".h.txt");
                    BufferedWriter bw_h = new BufferedWriter(fw_h, ThesisConstants.bufferSize);
                    FileWriter fw_p = new FileWriter(outFileName + ".p.txt");
                    BufferedWriter bw_p = new BufferedWriter(fw_p, ThesisConstants.bufferSize);

                    // prepare reader to read input file
                    BufferedReader br = FileUtils.openFileToRead(inFile.getAbsolutePath(), ThesisConstants.bufferSize);
                    String line = null;
                    int lineCount = 0;
                    while ((line = br.readLine()) != null) {
                        if (lineCount++ % 10000 == 0) {
                            System.out.println("     Processed: " + lineCount + " lines.");
                        }
                        if (line.startsWith("H")) {
                            bw_h.write(processLine(line) + "\n");
                        } else {
                            bw_p.write(processLine(line) + "\n");
                        }
                    }

                    bw_h.flush();
                    bw_p.flush();
                } catch (IOException | LineFormatException e) {
                    System.out.println(e);
                    System.exit(1);
                }
            }
        }
    }

    private static void initColumnPositions() throws IOException {
        // Init P
        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("p.column.position.input"), ThesisConstants.bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("RT")) {
                    ColumnPosition cp = new ColumnPosition(line);
                    pColPositions.add(cp);
                }
            }
        }
        // Init H
        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("h.column.position.input"), ThesisConstants.bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("RT")) {
                    ColumnPosition cp = new ColumnPosition(line);
                    hColPositions.add(cp);
                }
            }
        }
    }

    private static String processLine(String line) throws LineFormatException {
        String processedLine = "";
        if (line.startsWith("P")) {
            for (ColumnPosition elem : pColPositions) {
                processedLine += line.substring(elem.getStart() - 1, elem.getEnd()) + "\t";
            }
        } else if (line.startsWith("H")) {
            for (ColumnPosition elem : hColPositions) {
                processedLine += line.substring(elem.getStart() - 1, elem.getEnd()) + "\t";
            }
        } else {
            throw new LineFormatException("Line should starts with either 'P' or 'H'.");
        }
        return processedLine;
    }
}
