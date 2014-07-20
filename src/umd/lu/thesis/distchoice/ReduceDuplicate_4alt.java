package umd.lu.thesis.distchoice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.FileUtils;

public class ReduceDuplicate_4alt {

    private final static int bufferSize = 10485760;

    private final static int groupSize = 4;

    private static int currentLineNumber = 1;
    
    private final static int idColumn = 1;
    
    private final static int lgsColumn = 13;

    public static void main(String[] args) throws IOException {
        try (FileWriter fw = new FileWriter(ThesisProperties.getProperties("output.file.path"))) {
            BufferedWriter bw = new BufferedWriter(fw, bufferSize);

            // prepare reader to read input file
            BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("data.file.path"), bufferSize);
            // discard header
            br.readLine();
            currentLineNumber++;

            while (true) {
                bw.write(groupRead(br, bw));
            }
        }
    }

    private static String groupRead(BufferedReader br, BufferedWriter bw) throws IOException {
        // read 7 lines into HashTable
        HashSet<String> block = new HashSet<>();

        // prepare concatenated 7-line string
        String concatenated = "";

        for (int i = 0; i < groupSize; i++) {
            String line = br.readLine();
            currentLineNumber++;
            if(line != null) {
                String idLgsPair = getColumnValue(idColumn, line) + " - " + getColumnValue(lgsColumn, line);
                block.add(idLgsPair);
                concatenated += line + "\n";
            }
            else if(line == null && i == 0) {
                System.out.println("Finished. currentLineNumber = " + currentLineNumber);
                bw.flush();
                bw.close();
                br.close();
                System.exit(0);
            }
            else {
                throw new IOException("Premature end of file.");
            }
        }
        if(block.size() != 1) {
            return concatenated;
        }
        return "";
    }

    // find the value of the specific column from the given line
    // column starts from 1.
    private static String getColumnValue(int column, String line) {
        String[] split = line.split("\t");
        return split[column - 1];
    }
}
