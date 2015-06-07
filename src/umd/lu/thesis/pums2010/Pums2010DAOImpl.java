/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.dao.DbThesisDAO;

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
}
