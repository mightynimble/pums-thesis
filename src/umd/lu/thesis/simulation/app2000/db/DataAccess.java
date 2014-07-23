/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.simulation.app2000.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.*;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.simulation.app2000.objects.Person;
import umd.lu.thesis.simulation.app2000.objects.SimResult;

/**
 *
 * @author lousia
 */
public class DataAccess {
    
    private final static Logger sLog = LogManager.getLogger(DataAccess.class);

    private Connection connect = null;

    private static final String LEGACY_TABLE_BASE_NAME = "HH_Per_";
    private static final String TABLE_BASE_NAME = "sim_app2000_";

    public DataAccess() {
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
    }

    public Connection getConnection() {
        return connect;
    }

    private int getTableRowNumber(int id, boolean isLegacy) {
        String tableName = null;
        if (isLegacy) {
            tableName = LEGACY_TABLE_BASE_NAME + (id < 10 ? "0" + id : id);
        } else {
            tableName = TABLE_BASE_NAME + (id < 10 ? "0" + id : id);
        }

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

    public int getLegacyTableRowNumber(int id) {
        return getTableRowNumber(id, true);
    }

    public int getTableRowNumber(int id) {
        return getTableRowNumber(id, false);
    }

    public List<SimResult> bulkFetchSimTable(int tableId, int startId, int endId) {
        List<SimResult> results = new ArrayList<>();
        try {
            Statement statement = connect.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT msapmsa, dest, time, mode FROM "
                    + ThesisProperties.getProperties("simulation.app2000.db")
                    + "." + TABLE_BASE_NAME + (tableId < 10 ? "0" + tableId : tableId)
                    + " WHERE id >= " + startId + " AND id <= " + endId);

            while (resultSet.next()) {
                SimResult r = new SimResult(resultSet.getInt("msapmsa"), resultSet.getInt("dest"), resultSet.getInt("time"), resultSet.getInt("mode"));
                results.add(r);
            }
        } catch (SQLException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        return results;
    }
    
    public List<Person> bulkFetch(int tableId, int startId, int endId) {
        List<Person> persons = new ArrayList<>();
        try {
            Statement statement = connect.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM "
                    + ThesisProperties.getProperties("simulation.app2000.db")
                    + "." + LEGACY_TABLE_BASE_NAME + (tableId < 10 ? "0" + tableId : tableId)
                    + " WHERE id >= " + startId + " AND id <= " + endId);

            while (resultSet.next()) {
                Person p = getPersonFromResultSetEntry(resultSet);
                persons.add(p);
            }
        } catch (SQLException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        return persons;
    }

    private Person getPersonFromResultSetEntry(ResultSet rs) {

        Person p = new Person();
        try {
            p.setPid(rs.getInt("ID"));
            p.setRecType(rs.getString("Rectype"));
            p.setSerialNo(rs.getInt("Serialno"));
            p.setpNum(rs.getInt("PNUM"));
            p.setpWeight(rs.getInt("PWEIGHT"));
            p.setSex(rs.getInt("SEX"));
            p.setAge(rs.getInt("AGE"));
//            p.setDumAge(rs.getInt("DUMAGE"));
            p.setIncLevel(rs.getInt("INCLEVEL"));
            p.setDumEmp(rs.getInt("DUMEMP"));
            p.setMsa(rs.getInt("MSA"));
            p.setHhType(rs.getInt("HHType"));
            p.setState(rs.getInt("State"));
            p.setPersons(rs.getInt("PERSONS"));
            p.setHhinc(rs.getDouble("hhinc"));
            p.setRandB(rs.getInt("randBusiness"));
            p.setRandP(rs.getInt("randPerson"));
            p.setRandPB(rs.getInt("randPB"));
            p.setMsapmsa(rs.getInt("msapmsa"));
        } catch (SQLException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        return p;
    }

    public int save(Person p) {
        String insertSql = "INSERT INTO `" + TABLE_BASE_NAME
                + (p.getState() < 10 ? "0" + p.getState() : p.getState()) + "` "
                + "(`pid`, `Rectype`, `Serialno`, `PNUM`, `PWEIGHT`, `SEX`, `AGE`,"
                + "`INCLEVEL`, `DUMEMP`, `MSA`, `HHType`, `State`, "
                + "`PERSONS`, `hhinc`, `randBusiness`, `randPerson`, `randPB`, "
                + "`msapmsa`, `dest`, `time`, `mode`) VALUES (?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pStmt = connect.prepareStatement(insertSql);

            pStmt.setInt(1, p.getPid());
            pStmt.setString(2, p.getRecType());
            pStmt.setInt(3, p.getSerialNo());
            pStmt.setInt(4, p.getpNum());
            pStmt.setInt(5, p.getpWeight());
            pStmt.setInt(6, p.getSex());
            pStmt.setInt(7, p.getAge());
//            pStmt.setInt(8, p.getDumAge());
            pStmt.setInt(8, p.getIncLevel());
            pStmt.setInt(9, p.getDumEmp());
            pStmt.setInt(10, p.getMsa());
            pStmt.setInt(11, p.getHhType());
            pStmt.setInt(12, p.getState());
            pStmt.setInt(13, p.getPersons());
            pStmt.setDouble(14, p.getHhinc());
            pStmt.setInt(15, p.getRandB());
            pStmt.setInt(16, p.getRandP());
            pStmt.setInt(17, p.getRandPB());
            pStmt.setInt(18, p.getMsapmsa());
            pStmt.setInt(19, p.getDest());
            pStmt.setInt(20, p.getTime());
            pStmt.setInt(21, p.getMode());

            sLog.debug("pStmt: " + pStmt.toString());
            return pStmt.executeUpdate();
        } catch (SQLException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        return 0;
    }

}
