package umd.lu.thesis.airfare;

import java.sql.Statement;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import umd.lu.thesis.common.ThesisConstants;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.DatabaseUtils;
import umd.lu.thesis.helper.FileUtils;
import umd.lu.thesis.objects.Airfare;

/**
 *
 * @author Bo Sun
 */
public class Main {

    private final static int bufferSize = 10485760;

    private static DatabaseUtils dbUtil = new DatabaseUtils();

    public static void main(String[] args) throws IOException {
        try {
            BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("data.file.path"), bufferSize);
            FileWriter fw = new FileWriter(ThesisProperties.getProperties("output.file.path"));
            BufferedWriter bw = new BufferedWriter(fw, bufferSize);

            // Read data file into database
            String line = null;
            while ((line = br.readLine()) != null) {
                if(line.startsWith("\"" + ThesisConstants.COL_ItinID + "\"")) {
                    continue;
                }
                dbUtil.insertAirfareRecord(processLine(line));
            }

            //
            Statement stmt = dbUtil.getDatabaseConnection().createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                                                                            java.sql.ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(Integer.MIN_VALUE);
            ResultSet rs = stmt.executeQuery("SELECT OriginAirportSeqID, DestAirportSeqID, avg(MktFare) "
                    + "FROM thesis.airfare where OriginCountry = DestCountry "
                    + "group by OriginAirportSeqID, DestAirportSeqID;");
            String outLine = null;
            while (rs.next()) {
                outLine = rs.getString(ThesisConstants.COL_OriginAirportSeqID) + ","
                          + rs.getString(ThesisConstants.COL_DestAirportSeqID) + ","
                          + rs.getString(3) + "\r\n";
                bw.write(outLine);
            }
            bw.flush();
            bw.close();

        }
        catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static Airfare processLine(String line) {
        Airfare rec = new Airfare();

        String[] m = line.split(",");
        if(m.length > 1) {
            rec.ItinID = Long.parseLong(m[0]);
            rec.MktID = Long.parseLong(m[1]);
            rec.MktCoupons = Integer.parseInt(m[2]);
            rec.Year = Integer.parseInt(m[3]);
            rec.Quarter = Integer.parseInt(m[4]);
            rec.OriginAirportID = Integer.parseInt(m[5]);
            rec.OriginAirportSeqID = Integer.parseInt(m[6]);
            rec.OriginCityMarketID = Integer.parseInt(m[7]);
            rec.Origin = m[8];
            rec.OriginCountry = m[9];
            rec.OriginStateFips = m[10];
            rec.OriginState = m[11];
            rec.OriginStateName = m[12];
            rec.OriginWac = Integer.parseInt(m[13]);
            rec.DestAirportID = Integer.parseInt(m[14]);
            rec.DestAirportSeqID = Integer.parseInt(m[15]);
            rec.DestCityMarketID = Integer.parseInt(m[16]);
            rec.Dest = m[17];
            rec.DestCountry = m[18];
            rec.DestStateFips = m[19];
            rec.DestState = m[20];
            rec.DestStateName = m[21];
            rec.DestWac = Integer.parseInt(m[22]);
            rec.AirportGroup = m[23];
            rec.WacGroup = m[24];
            rec.TkCarrierChange = Float.parseFloat(m[25]);
            rec.TkCarrierGroup = m[26];
            rec.OpCarrierChange = Float.parseFloat(m[27]);
            rec.OpCarrierGroup = m[28];
            rec.RPCarrier = m[29];
            rec.TkCarrier = m[30];
            rec.OpCarrier = m[31];
            rec.BulkFare = m[32];
            rec.Passengers = Float.parseFloat(m[33]);
            rec.MktFare = Float.parseFloat(m[34]);
            rec.MktDistance = Float.parseFloat(m[35]);
            rec.MktDistanceGroup = Integer.parseInt(m[36]);
            rec.MktMilesFlown = Float.parseFloat(m[37]);
            rec.NonStopMiles = Float.parseFloat(m[38]);
            rec.ItinGeoType = Integer.parseInt(m[39]);
            rec.MktGeoType = Integer.parseInt(m[40]);
        }

        return rec;
    }
}
