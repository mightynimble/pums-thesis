package umd.lu.thesis.pums.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.dao.DbThesisDAO;

/**
 *
 * @author lousia
 */
public class PumsDAOImpl implements DbThesisDAO {

    private Connection connect = null;

    private String url;

    private String driver;

    private String db;

    private String user;

    private String password;

    public PumsDAOImpl() {
        url = "jdbc:mysql://localhost:3306/";
        db = ThesisProperties.getProperties("pums.db");
        user = ThesisProperties.getProperties("db.username");
        password = ThesisProperties.getProperties("db.password");
        driver = "com.mysql.jdbc.Driver";

        try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName(driver);
            connect = DriverManager.getConnection(url + db, user, password);
            connect.setAutoCommit(true);
        } catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public void createPersonTable(String tableName) {

        String sqlCreateTable = "CREATE TABLE `" + tableName + "` (\n"
                + "  `ID` int(11) NOT NULL AUTO_INCREMENT,\n"
                + "  `Rectype` varchar(5) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,\n"
                + "  `Serialno` int(11) DEFAULT NULL,\n"
                + "  `PNUM` int(11) DEFAULT NULL,\n"
                + "  `PWEIGHT` int(11) DEFAULT NULL,\n"
                + "  `SEX` int(11) DEFAULT NULL,\n"
                + "  `AGE` int(11) DEFAULT NULL,\n"
                + "  `ENROLL` int(11) DEFAULT NULL,\n"
                + "  `ESR` int(11) DEFAULT NULL,\n"
                + "  `INCWS` int(11) DEFAULT NULL,\n"
                + "  `INCTOT` int(11) DEFAULT NULL,\n"
                + "  `DUMAGE` int(11) DEFAULT NULL,\n"
                + "  `INCLEVEL` int(11) DEFAULT NULL,\n"
                + "  `DUMEMP` int(11) DEFAULT NULL,\n"
                + "  `MSA` int(11) DEFAULT NULL,\n"
                + "  `HHType` int(11) DEFAULT NULL,\n"
                + "  PRIMARY KEY (`ID`),\n"
                + "  KEY `serialno_indx` (`Serialno`)\n"
                + ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;";
        createTable(sqlCreateTable);
    }

    public void createHouseholdTable(String tableName) {

        String sqlCreateTable = "CREATE TABLE `" + tableName + "` (\n"
                + "  `ID` int(11) NOT NULL AUTO_INCREMENT,\n"
                + "  `Rectype` varchar(5) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,\n"
                + "  `Serialno` int(11) DEFAULT NULL,\n"
                + "  `State` int(11) DEFAULT NULL,\n"
                + "  `Region` int(11) DEFAULT NULL,\n"
                + "  `Division` int(11) DEFAULT NULL,\n"
                + "  `PUMA5` int(11) DEFAULT NULL,\n"
                + "  `PUMA1` int(11) DEFAULT NULL,\n"
                + "  `MSAPMSA5` int(11) DEFAULT NULL,\n"
                + "  `PERSONS` int(11) DEFAULT NULL,\n"
                + "  `HHT` int(11) DEFAULT NULL,\n"
                + "  `PAOC` int(11) DEFAULT NULL,\n"
                + "  `PARC` int(11) DEFAULT NULL,\n"
                + "  PRIMARY KEY (`ID`),\n"
                + "  KEY `serialno_h_indx` (`Serialno`)\n"
                + ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;";
        createTable(sqlCreateTable);
    }

    public void createTable(String sql) {
        try {
            Statement stmt = connect.createStatement();
            stmt.execute(sql);
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public Connection getConnection() {
        return connect;
    }

    public int batchExecution(List<String> statements) throws SQLException {
        Statement stmt = connect.createStatement();
        for (String st : statements) {
            stmt.addBatch(st);
        }

        int[] rowsAffected = stmt.executeBatch();
//        connect.commit();
        int rtn = 0;
        for (int r : rowsAffected) {
            rtn += r;
        }

        return rtn;
    }

    public void destroy() {
        try {
            connect.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public ResultSet executeQuery(String sql) {
        try {
            Statement st = connect.createStatement();
            return st.executeQuery(sql);
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public void executeUpdate(String sql) {
        try {
            Statement st = connect.createStatement();
            int c = st.executeUpdate(sql);
            c = c+1;
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public void batchUpdate(List<String> batchItems) {
        for (String sql : batchItems) {
            executeUpdate(sql);
        }
    }

}
