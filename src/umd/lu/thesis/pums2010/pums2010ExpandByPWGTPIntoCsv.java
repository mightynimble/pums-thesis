package umd.lu.thesis.pums2010;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.helper.FileUtils;

/**
 * Expand rows in person_lj_household table, put them into multiple CSV files,
 * then import the files into person_household_expanded table.
 * 
 * @author Home
 */
public class pums2010ExpandByPWGTPIntoCsv {

    private static Pums2010DAOImpl pumsDao;

    private final static int ioBufferSize = 10485760;

    private static int inTotal;

    public static void main(String[] args) throws Exception {

        // get total rows from db.
        pumsDao = new Pums2010DAOImpl();
        System.out.println("pums2010ExpandByPWGTP started.\n");
        System.out.print("Querying for total records in 'PERSON_LJ_HOUSEHOLD' table... ");
        inTotal = pumsDao.getTotalRecords("PERSON_LJ_HOUSEHOLD");
        System.out.println("Done. (" + inTotal + " records)");

        expandRecordsIntoCSV();

        System.out.println("Completed.");
    }

    private static void expandRecordsIntoCSV() throws IOException, SQLException {
        Statement st = pumsDao.getConnection().createStatement();
        try (FileWriter fw = new FileWriter(ThesisProperties.getProperties("output.file.path"))) {
            BufferedWriter bw = new BufferedWriter(fw, ioBufferSize);


            for (int i = 1; i < inTotal + 1; i++) {
                String stmtString = "SELECT * FROM PERSON_LJ_HOUSEHOLD WHERE ID = " + i;
                ResultSet rs = st.executeQuery(stmtString);
                String dbRow = "";
                if(rs.next()) {
                    int expandInto = rs.getInt("PWGTP");
                    dbRow = generateBaseRowFromResultSet(rs);
                    for (int r = 0; r < expandInto; r++) {
                        bw.write(dbRow);
                    }
                }
                else {
                    break;
                }
                if (i % 10000 == 1) {
                    System.out.println("Progress: " + i + " out of " + inTotal);
                }
            }

            bw.flush();
        }
    }

    private static String generateBaseRowFromResultSet(ResultSet rs) throws SQLException {
        return (rs.getString("SERIALNO") == null ? "\\N" : rs.getInt("SERIALNO")) + ","
               + (rs.getString("PWGTP") == null ? "\\N" : rs.getInt("PWGTP")) + ","
               + (rs.getString("AGEP") == null ? "\\N" : rs.getInt("AGEP")) + ","
               + (rs.getString("PINCP") == null ? "\\N" : rs.getInt("PINCP")) + ","
               + (rs.getString("SCH") == null ? "\\N" : rs.getInt("SCH")) + ","
               + (rs.getString("SEX") == null ? "\\N" : rs.getInt("SEX")) + ","
               + (rs.getString("ESR") == null ? "\\N" : rs.getInt("ESR")) + ","
               + (rs.getString("PUMA") == null ? "\\N" : rs.getInt("PUMA")) + ","
               + (rs.getString("ST") == null ? "\\N" : rs.getInt("ST")) + ","
               + (rs.getString("NP") == null ? "\\N" : rs.getInt("NP")) + ","
               + (rs.getString("HHT") == null ? "\\N" : rs.getInt("HHT")) + ","
               + (rs.getString("HINCP") == null ? "\\N" : rs.getInt("HINCP")) + ","
               + (rs.getString("HUPAOC") == null ? "\\N" : rs.getInt("HUPAOC")) + ","
               + (rs.getString("HUPARC") == null ? "\\N" : rs.getInt("HUPARC")) + ","
               + (rs.getString("SUMPINC") == null ? "\\N" : rs.getInt("SUMPINC")) + ","
               + (rs.getString("HTINC") == null ? "\\N" : rs.getInt("HTINC")) + ","
               + (rs.getString("RAGE") == null ? "\\N" : rs.getInt("RAGE")) + ","
               + (rs.getString("INC_LVL") == null ? "\\N" : rs.getInt("INC_LVL")) + ","
               + (rs.getString("EMP_STATUS") == null ? "\\N" : rs.getInt("EMP_STATUS")) + ","
               + (rs.getString("HHTYPE") == null ? "\\N" : rs.getInt("HHTYPE")) + ",\\N,\\N,\\N,\\N\n";
        // Leave places for MSAPMSA, R_B, R_P, and R_PB columns
    }
}
