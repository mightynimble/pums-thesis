package umd.lu.thesis.helper;

import umd.lu.thesis.common.ThesisProperties;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.*;
import umd.lu.thesis.common.ThesisConstants;
import umd.lu.thesis.objects.Airfare;

/**
 *
 * @author Bo Sun
 */
public class DatabaseUtils {

    private static String dbUrl = null;

    private static Connection conn = null;

    private Statement statement = null;

    private PreparedStatement preparedStatement = null;

    private ResultSet resultSet = null;

    private String dbUser = null;

    private String dbPassword = null;

    private String dbHost = null;

    private String dbDatabase = null;

    public DatabaseUtils() {
        dbUser = ThesisProperties.getProperties(ThesisConstants.DB_USER);

        dbPassword = ThesisProperties.getProperties(ThesisConstants.DB_PASSWORD);

        dbHost = ThesisProperties.getProperties(ThesisConstants.DB_HOST);

        dbDatabase = ThesisProperties.getProperties(ThesisConstants.DB_DATABASE);

        dbUrl = "jdbc:mysql://" + dbHost + "/" + dbDatabase + "?user=" + dbUser + "&password=" + dbPassword;

        conn = getDatabaseConnection();
    }

    public Connection getDatabaseConnection() {
        try {
            if(conn != null) {
                return conn;
            }
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(dbUrl);
            return conn;
        }
        catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void insertAirfareRecord(Airfare rec) {
        try {
            preparedStatement = conn.prepareStatement("insert into thesis.airfare "
                                                      + "(ItinID,MktID,MktCoupons,Year,Quarter,"
                                                      + "OriginAirportID,OriginAirportSeqID,OriginCityMarketID,Origin,"
                                                      + "OriginCountry,OriginStateFips,OriginState,OriginStateName,OriginWac,"
                                                      + "DestAirportID,DestAirportSeqID,DestCityMarketID,Dest,DestCountry,"
                                                      + "DestStateFips,DestState,DestStateName,DestWac,AirportGroup,"
                                                      + "WacGroup,TkCarrierChange,TkCarrierGroup,OpCarrierChange,OpCarrierGroup,"
                                                      + "RPCarrier,TkCarrier,OpCarrier,BulkFare,Passengers,"
                                                      + "MktFare,MktDistance,MktDistanceGroup,MktMilesFlown,NonStopMiles,"
                                                      + "ItinGeoType,MktGeoType)"
                                                      + " values "
                                                      + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                                                      + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");

            preparedStatement.setLong(1, rec.ItinID);
            preparedStatement.setLong(2, rec.MktID);
            preparedStatement.setInt(3, rec.MktCoupons);
            preparedStatement.setInt(4, rec.Year);
            preparedStatement.setInt(5, rec.Quarter);
            preparedStatement.setInt(6, rec.OriginAirportID);
            preparedStatement.setInt(7, rec.OriginAirportSeqID);
            preparedStatement.setInt(8, rec.OriginCityMarketID);
            preparedStatement.setString(9, rec.Origin);
            preparedStatement.setString(10, rec.OriginCountry);
            preparedStatement.setString(11, rec.OriginStateFips);
            preparedStatement.setString(12, rec.OriginState);
            preparedStatement.setString(13, rec.OriginStateName);
            preparedStatement.setInt(14, rec.OriginWac);
            preparedStatement.setInt(15, rec.DestAirportID);
            preparedStatement.setInt(16, rec.DestAirportSeqID);
            preparedStatement.setInt(17, rec.DestCityMarketID);
            preparedStatement.setString(18, rec.Dest);
            preparedStatement.setString(19, rec.DestCountry);
            preparedStatement.setString(20, rec.DestStateFips);
            preparedStatement.setString(21, rec.DestState);
            preparedStatement.setString(22, rec.DestStateName);
            preparedStatement.setInt(23, rec.DestWac);
            preparedStatement.setString(24, rec.AirportGroup);
            preparedStatement.setString(25, rec.WacGroup);
            preparedStatement.setFloat(26, rec.TkCarrierChange);
            preparedStatement.setString(27, rec.TkCarrierGroup);
            preparedStatement.setFloat(28, rec.OpCarrierChange);
            preparedStatement.setString(29, rec.OpCarrierGroup);
            preparedStatement.setString(30, rec.RPCarrier);
            preparedStatement.setString(31, rec.TkCarrier);
            preparedStatement.setString(32, rec.OpCarrier);
            preparedStatement.setString(33, rec.BulkFare);
            preparedStatement.setFloat(34, rec.Passengers);
            preparedStatement.setFloat(35, rec.MktFare);
            preparedStatement.setFloat(36, rec.MktDistance);
            preparedStatement.setInt(37, rec.MktDistanceGroup);
            preparedStatement.setFloat(38, rec.MktMilesFlown);
            preparedStatement.setFloat(39, rec.NonStopMiles);
            preparedStatement.setInt(40, rec.ItinGeoType);
            preparedStatement.setInt(41, rec.MktGeoType);

            preparedStatement.execute();
        }
        catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
