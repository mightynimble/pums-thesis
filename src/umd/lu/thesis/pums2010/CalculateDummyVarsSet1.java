package umd.lu.thesis.pums2010;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Home
 */
class CalculateDummyVarsSet1 {

    private Pums2010DAOImpl dao;

    private int totalRows;

    private org.apache.log4j.Logger log = org.apache.log4j.LogManager.getLogger(CalculateRandValues.class);
    
    private int[] dummyValueSet;
    
    private static final int batchSize = 50000;
    
    private static final String insertSqlPrefix = "INSERT INTO ID_EXPAND1 (ID, EMPLOYMENT, UNEMPLOYMENT,"
                                + " STUDENT, MALE, HHCHD, HHOCHD, SINGLE, NON_FAM, LOWINCOME, "
                                + "MEDINCOME, HIGHINCOME) VALUES ";

    public CalculateDummyVarsSet1(int totalRows) {
        this.totalRows = totalRows;
        this.dao = new Pums2010DAOImpl();
        dummyValueSet = new int[11];
    }

    public void run() {
        log.info("Started setting dummy value columns (EMPLOYMENT, UNEMPLOYMENT,"
                 + " STUDENT, MALE, HHCHD, HHOCHD, SINGLE, NON_FAM, LOWINCOME, "
                 + "MEDINCOME, HIGHINCOME)");
        String insertSql = insertSqlPrefix;
        StringBuilder sb = new StringBuilder(insertSql);
        for (int id = 1; id <= totalRows; id++) {
            if(id % 100000 == 1) {
                log.info("Progress: " + id + " out of " + totalRows);
            }

            try {
                Statement st = dao.getConnection().createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM PERSON_HOUSEHOLD_EXPANDED WHERE ID = " + id);
                Statement insertStmt = dao.getConnection().createStatement();
                while (rs.next()) {
                    dummyValueSet[0] = rs.getInt("EMP_STATUS") == 1 ? 1 : 0;
                    dummyValueSet[1] = rs.getInt("EMP_STATUS") == 2 ? 1 : 0;
                    dummyValueSet[2] = rs.getInt("EMP_STATUS") == 3 ? 1 : 0;
                    
                    dummyValueSet[3] = rs.getInt("SEX") == 1 ? 1 : 0;
                    
                    dummyValueSet[4] = rs.getInt("HHTYPE") == 2 ? 1 : 0;
                    dummyValueSet[5] = rs.getInt("HHTYPE") == 1 ? 1 : 0;
                    dummyValueSet[6] = rs.getInt("HHTYPE") == 3 ? 1 : 0;
                    dummyValueSet[7] = rs.getInt("HHTYPE") == 4 ? 1 : 0;
                    
                    dummyValueSet[8] = rs.getInt("INC_LVL") == 1 ? 1 : 0;
                    dummyValueSet[9] = rs.getInt("INC_LVL") == 2 ? 1 : 0;
                    dummyValueSet[10] = rs.getInt("INC_LVL") == 3 ? 1 : 0;
                    
                    prepareInsertElement(sb, rs.getInt("ID"), dummyValueSet);
                }
                if(id % batchSize == 0) {
                    // execute insert
                    insertStmt.executeUpdate(sb.toString());

                    // reset stringBuilder
                    sb.setLength(0);
                    insertSql = insertSqlPrefix;
                    sb.append(insertSql);
                }
                if (!sb.toString().equals(insertSqlPrefix)) {
                insertStmt.executeUpdate(sb.toString().substring(0, sb.length() - 1));
            }
            }
            catch (SQLException ex) {
                log.error("Error: " + ex.getLocalizedMessage(), ex);
                System.exit(1);
            }
        }
        log.info("Completed. ");

    }
    
    

    private void prepareInsertElement(StringBuilder sb, int id, int[] dummyValueSet) {
        String sql = "(" + id + ","
                     + dummyValueSet[0] + ","
                     + dummyValueSet[1] + ","
                     + dummyValueSet[2] + ","
                     + dummyValueSet[3] + ","
                     + dummyValueSet[4] + ","
                     + dummyValueSet[5] + ","
                     + dummyValueSet[6] + ","
                     + dummyValueSet[7] + ","
                     + dummyValueSet[8] + ","
                     + dummyValueSet[9] + ","
                     + dummyValueSet[10] + "),";
        sb.append(sql);
    }
}
