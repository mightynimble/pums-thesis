/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010;

import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.dao.DbThesisDAO;
import umd.lu.thesis.pums2010.objects.Person2010;

/**
 *
 * @author Home
 */
public class Pums2010DAOImpl implements DbThesisDAO {

    private Connection connect = null;

    private String url;

    private String driver;

    private String db;

    private String user;

    private String password;

    public Pums2010DAOImpl() {
        url = "jdbc:mysql://" + ThesisProperties.getProperties("db.host") + ":" + ThesisProperties.getProperties("db.port") + "/";
        db = ThesisProperties.getProperties("pums.db");
        user = ThesisProperties.getProperties("db.username");
        password = ThesisProperties.getProperties("db.password");
        driver = "com.mysql.jdbc.Driver";

        try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName(driver);
            connect = DriverManager.getConnection(url + db, user, password);
            connect.setAutoCommit(true);
        }
        catch (com.mysql.jdbc.exceptions.jdbc4.CommunicationsException ex) {
            System.err.println("Not using database. Ignore this error and continue...");
        }
        catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public Connection getConnection() {
        return connect;
    }

    public int getTotalRecords(String table) {
        String stmtString = "SELECT COUNT(*) FROM " + table;
        try {
            Statement st = connect.createStatement();
            ResultSet rs = st.executeQuery(stmtString);

            while (rs.next()) {
                return rs.getInt(1);
            }
        }
        catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        return -1;
    }

    public int getTotalRecordsByMaxId(String table) {
        String stmtString = "SELECT id FROM " + table + " ORDER BY id DESC LIMIT 0, 1";
        try {
            Statement st = connect.createStatement();
            ResultSet rs = st.executeQuery(stmtString);

            while (rs.next()) {
                return rs.getInt(1);
            }
        }
        catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        return -1;
    }

    List<Person2010> getPerson2010(int startId, int endId) {
        String sql = "SELECT ID_RANDS.ID,"
                     + "`person_household_expanded`.`SERIALNO`,"
                     + "`person_household_expanded`.`PWGTP`,"
                     + "`person_household_expanded`.`AGEP`,"
                     + "`person_household_expanded`.`PINCP`,"
                     + "`person_household_expanded`.`SCH`,"
                     + "`person_household_expanded`.`SEX`,"
                     + "`person_household_expanded`.`ESR`,"
                     + "`person_household_expanded`.`PUMA`,"
                     + "`person_household_expanded`.`ST`,"
                     + "`person_household_expanded`.`NP`,"
                     + "`person_household_expanded`.`HHT`,"
                     + "`person_household_expanded`.`HINCP`,"
                     + "`person_household_expanded`.`HUPAOC`,"
                     + "`person_household_expanded`.`HUPARC`,"
                     + "`person_household_expanded`.`SUMPINC`,"
                     + "`person_household_expanded`.`HTINC`,"
                     + "`person_household_expanded`.`RAGE`,"
                     + "`person_household_expanded`.`INC_LVL`,"
                     + "`person_household_expanded`.`EMP_STATUS`,"
                     + "`person_household_expanded`.`HHTYPE`,"
                     + "`person_household_expanded`.`MSAPMSA`,"
                     + "`ID_RANDS`.`R_BUSINESS`,"
                     + "`ID_RANDS`.`R_PERSON`,"
                     + "`ID_RANDS`.`R_PB`"
                     + "FROM PERSON_HOUSEHOLD_EXPANDED "
                     + "LEFT JOIN ID_RANDS ON PERSON_HOUSEHOLD_EXPANDED.ID = ID_RANDS.ID "
                     + "WHERE ID_RANDS.ID >= " + startId + " AND ID_RANDS.ID < " + endId;

        List<Person2010> pList = new ArrayList<>();
        try {
            Statement st = connect.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Person2010 p = new Person2010();
                p.setPid(rs.getInt("ID"));
                p.setSerialNo(rs.getInt("SERIALNO"));
                p.setPwgtp(rs.getInt("PWGTP"));
                p.setAge(rs.getInt("AGEP"));
                p.setPincp(rs.getInt("PINCP"));
                p.setSch(rs.getInt("SCH"));
                p.setSex(rs.getInt("SEX"));
                p.setEsr(rs.getInt("ESR"));
                p.setPuma(rs.getInt("PUMA"));
                p.setSt(rs.getInt("ST"));
                p.setNp(rs.getInt("NP"));
                p.setHht(rs.getInt("HHT"));
                p.setHincp(rs.getInt("HINCP"));
                p.setHupaoc(rs.getInt("HUPAOC"));
                p.setHuparc(rs.getInt("HUPARC"));
                p.setSumpinc(rs.getInt("SUMPINC"));
                p.setHtinc(rs.getInt("HTINC"));
                p.setRage(rs.getInt("RAGE"));
                p.setIncLevel(rs.getInt("INC_LVL"));
                p.setEmpStatus(rs.getInt("EMP_STATUS"));
                p.setHhType(rs.getInt("HHTYPE"));
                p.setMsapmsa(rs.getInt("MSAPMSA"));
                p.setrB((int) (rs.getDouble("R_BUSINESS") + 0.5));
                p.setrP((int) (rs.getDouble("R_PERSON") + 0.5));
                p.setrPB((int) (rs.getDouble("R_PB") + 0.5));

                pList.add(p);
            }
        }
        catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        return pList;
    }
}
