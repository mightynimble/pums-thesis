package umd.lu.thesis.simulation.app2000;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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

    private static final String TABLE_BASE_NAME = "sim_app2000_";

    private static final int[] TABLE_IDS = {1, 2, 4, 5, 6, 8, 9, 10, 11, 12, 13, 15, 16,
        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34,
        35, 36, 37, 38, 39, 40, 41, 42, 44, 45, 46, 47, 48, 49, 50, 51, 53, 54, 55, 56};
//    private static final int[] TABLE_IDS = {1, 2};

    // key: o-d-time-mode, value: integer
    private HashMap<String, Integer> results = new HashMap<>();

    private static final int bulkFetchSize = 50000;

    private Connection connect = null;

    public static void main(String[] args) throws Exception {
        System.out.println("Start generating reports.");

        (new Report()).runner();

        System.out.println("-DONE-");
    }

    private void runner() {
        String url = "jdbc:mysql://" + ThesisProperties.getProperties("db.host") + ":3306/";
        String db = ThesisProperties.getProperties("simulation.app2000.db");
        String user = ThesisProperties.getProperties("db.username");
        String password = ThesisProperties.getProperties("db.password");
        String driver = "com.mysql.jdbc.Driver";

        try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName(driver);
            connect = DriverManager.getConnection(url + db, user, password);
            connect.setAutoCommit(true);
        } catch (ClassNotFoundException | SQLException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }

        initZoneId();

        for (int id : TABLE_IDS) {
            System.out.println("Process table: " + id);
            processTable(id);
            System.out.println("Finished processing table: " + id + ", results.size() = " + results.size() + ", memory: " + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB");
        }

        outputResults();
    }

    private void processTable(int id) {
        int rowCount = getTableRowNumber(id);
        int start = 1;
        int end = start + bulkFetchSize;
        try {
            while (start <= rowCount) {
                List<SimResult> simRs = new ArrayList<>();
                Statement statement = connect.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT msapmsa, dest, time, mode FROM "
                        + ThesisProperties.getProperties("simulation.app2000.db")
                        + "." + TABLE_BASE_NAME + (id < 10 ? "0" + id : id)
                        + " WHERE id >= " + start + " AND id <= " + end);

                while (resultSet.next()) {
                    SimResult r = new SimResult(resultSet.getInt("msapmsa"), resultSet.getInt("dest"), resultSet.getInt("time"), resultSet.getInt("mode"));
                    simRs.add(r);
                }

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

                simRs.clear();
            }

            // dump results
            try (FileWriter fw = new FileWriter(ThesisProperties.getProperties("simulation.app2000.output_basename") + id + ".txt"); BufferedWriter bw = new BufferedWriter(fw)) {
                for (String key : results.keySet()) {
                    bw.write(key + "\t" + results.get(key) + "\n");
                }
            } catch (IOException ex) {
                sLog.error(ex.getLocalizedMessage(), ex);
                System.exit(1);
            }
        } catch (SQLException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }

        results.clear();
    }

    private int getTableRowNumber(int id) {
        String tableName = TABLE_BASE_NAME + (id < 10 ? "0" + id : id);

        int count = -1;
        try {
            Statement statement = connect.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM "
                    + ThesisProperties.getProperties("simulation.app2000.db") + "." + tableName);

            while (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        return count;
    }

    private void outputResults() {

        loadResultsFromDumpFiles();

        for (int time = 1; time <= 4; time++) {
            String timeCat = "t" + time;
            for (int mode = 1; mode <= 3; mode++) {
                String modeCat = "m" + mode;
                String table = timeCat + modeCat;
                try (FileWriter fw = new FileWriter(ThesisProperties.getProperties("simulation.app2000.output_basename") + table + ".txt"); BufferedWriter bw = new BufferedWriter(fw)) {
                    System.out.println("Generating table: " + ThesisProperties.getProperties("simulation.app2000.output_basename") + table + ".txt");
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

    private void loadResultsFromDumpFiles() {
        results.clear();

        for (int id : TABLE_IDS) {
            try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.app2000.output_basename") + id + ".txt");BufferedReader br = new BufferedReader(new InputStreamReader(fstream))) {
                String line = "";
                while((line = br.readLine()) != null) {
                    line = line.trim();
                    String[] split = line.split("\t");
                    Integer value = results.get(split[0]);
                    if (value == null) {
                        results.put(split[0], Integer.parseInt(split[1]));
                    }
                    else {
                        results.put(split[0], value + Integer.parseInt(split[1]));
                    }
                }
            } catch (IOException ex) {
                sLog.error(ex.getLocalizedMessage(), ex);
                System.exit(1);
            }
        }
    }
}
