package umd.lu.thesis.simulation.app2000;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.simulation.app2000.db.DataAccess;
import umd.lu.thesis.simulation.app2000.objects.SimResult;

/**
 *
 * @author Yijing Lu
 */
public class Report {

    private final static Logger sLog = LogManager.getLogger(App2000.class);

    private static HashMap<Integer, Integer> hmZoneId = new HashMap<>();

    private static final int[] TABLE_IDS = {1, 2, 4, 5, 6, 8, 9, 10, 11, 12, 13, 15, 16,
        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34,
        35, 36, 37, 38, 39, 40, 41, 42, 44, 45, 46, 47, 48, 49, 50, 51, 53, 54, 55, 56};

    // key: o-d-time-mode, value: integer
    private static HashMap<String, Integer> results = new HashMap<>();

    private static final int bulkFetchSize = 50000;

    private static DataAccess dao = new DataAccess();

    public static void main(String[] args) throws Exception {
        sLog.info("Start generating reports.");

        for (int id : TABLE_IDS) {
            sLog.info("Process table: " + id);
            processTable(id);
            sLog.info("Finished processing table: " + id);
        }
        
        outputResults();

        sLog.info("-DONE-");
    }

    private static void processTable(int id) {
        int rowCount = dao.getTableRowNumber(id);
        int start = 1;
        int end = start + bulkFetchSize;

        while (start <= rowCount) {
            List<SimResult> simRs = dao.bulkFetchSimTable(id, start, end);
            start = end + 1;
            end = end + bulkFetchSize;

            for (SimResult r : simRs) {
                String key = hmZoneId.get(r.getMsapmsa()) + "-" + r.getD() + "-"
                        + r.getTime() + "-" + r.getMode();
                Integer value = results.get(key);
                if (value == null) {
                    results.put(key, 1);
                } else {
                    results.put(key, value + 1);
                }
            }
        }
    }

    private static void outputResults() {
        for (int time = 1; time <= 4; time++) {
            String timeCat = "t" + time;
            for (int mode = 1; mode <= 3; mode++) {
                String modeCat = "m" + mode;
                String table = timeCat + modeCat;
                try (FileWriter fw = new FileWriter(ThesisProperties.getProperties("simulation.app2000.output_basename") + table + ".txt"); BufferedWriter bw = new BufferedWriter(fw)) {
                    sLog.info("Generating table: " + ThesisProperties.getProperties("simulation.app2000.output_basename") + table + ".txt");
                    String header = "O\\D\t";
                    for (int i = 1; i <= 378; i++) {
                        header += i + "\t";
                    }
                    header += "\n";
                    bw.write(header);

                    String line = "";
                    for (int o = 1; o <= 378; o++) {
                        line = o + "\t";
                        for (int d = 1; d <= 378; d++) {
                            Integer odPair = results.get(o + "-" + d + "-" + time + "-" + mode);
                            if (odPair == null) {
                                line += "0\t";
                            } else {
                                line += odPair.toString() + "\t";
                            }
                        }
                        line += "\n";
                        bw.write(line);
                    }
                } catch (IOException ex) {
                    sLog.error(ex.getLocalizedMessage(), ex);
                    System.exit(1);
                }
            }
        }
    }

    private void initZoneId() {
        sLog.info("----Initialize zone id.");
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.zoneid"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("MSA/NMSA")) {
                    Integer key = Integer.parseInt(ExcelUtils.getColumnValue(1, line));
                    Integer value = Integer.parseInt(ExcelUtils.getColumnValue(2, line));
                    hmZoneId.put(key, value);
                }
            }
            br.close();
        } catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
    }
}
