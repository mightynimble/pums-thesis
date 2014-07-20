package umd.lu.thesis.pums.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import umd.lu.thesis.exceptions.InvalidValueException;

/**
 *
 * @author lousia
 */
public class HhPerUtils {

    public static int getIncLevel(ResultSet rs) throws SQLException, InvalidValueException {
        int incLevel = 3;

        int hhInc = rs.getInt("hhinc");
        if (hhInc < 30000) {
            incLevel = 1;
        } else if (hhInc >= 30000 && hhInc < 75000) {
            incLevel = 2;
        } else if (hhInc >= 75000) {
            incLevel = 3;
        } else {
            throw new InvalidValueException("Invalid hhInc value: " + hhInc + ", ID: " + rs.getInt("ID"));
        }

        return incLevel;
    }

    public static int getMsa(ResultSet rs) throws SQLException {
        int msa = 1;

        int msaPMsa5 = rs.getInt("MSAPMSA5");
        if (msaPMsa5 == 9999) {
            msa = 2;
        }

        return msa;
    }

    public static int getHhType(ResultSet rs) {
        int hhType = 1;
        try {
            int hht = rs.getInt("HHT");
            int parc = rs.getInt("PARC");
            int paoc = rs.getInt("PAOC");
            if ((hht == 1 || hht == 2 || hht == 3) && (parc == 4 || paoc == 4)) {
                hhType = 1;
            } else if ((hht == 1 || hht == 2 || hht == 3)
                    && ((parc >= 1 && parc <= 3) || (paoc >= 1 && paoc <= 3))) {
                hhType = 2;
            } else if (hht == 4 || hht == 6) {
                hhType = 3;
            } else if (hht == 5 || hht == 7) {
                hhType = 4;
            } else if (hht == 0 && parc == 0 && paoc == 0) {
                hhType = 0;
            } else {
                throw new InvalidValueException("Invalid hht, parc, or paoc value(s): hht = "
                        + hht + ", parc = " + parc + ", paoc = " + paoc + ", ID: " + rs.getInt("ID"));
            }
        } catch (SQLException | InvalidValueException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        return hhType;
    }

    public static int getSex(ResultSet rs) throws SQLException, InvalidValueException {
        int sex = 1;

        sex = rs.getInt("SEX");
        if (sex < 1 || sex > 2) {
            throw new InvalidValueException("Invalid SEX value: " + sex + ", ID: " + rs.getInt("ID"));
        }

        return sex;
    }

    public static int getDumEmp(ResultSet rs) throws SQLException, InvalidValueException {
        int dumEmp = 1;

        dumEmp = rs.getInt("DUMEMP");
        if (dumEmp < 1 || dumEmp > 3) {
            throw new InvalidValueException("Invalid DUMEMP value: " + dumEmp + ", ID: " + rs.getInt("ID"));
        }

        return dumEmp;
    }
    
    public static int getDumAge(ResultSet rs) throws SQLException, InvalidValueException {
        int dumAge = 1;
        int age = rs.getInt("AGE");
        if (age >= 19 && age <= 35) {
            dumAge = 1;
        }
        else if (age >= 36 && age <= 55 ){
            dumAge = 2;
        }
        else if (age >=56) {
            dumAge = 3;
        }
        else throw new InvalidValueException("Invalid AGE value: " + age + ", ID: " + rs.getInt("ID"));
        return dumAge;
    }

    public static int getPWeight(ResultSet rs) throws SQLException, InvalidValueException {
        int pWeight = 1;

        pWeight = rs.getInt("PWEIGHT");
        if (pWeight < 1) {
            throw new InvalidValueException("Invalid PWEIGHT value: " + pWeight + ", ID: " + rs.getInt("ID"));
        }

        return pWeight;
    }

    public static int getState(ResultSet rs) throws SQLException {
        return rs.getInt("State");
    }

    public static int getPuma5(ResultSet rs) throws SQLException {
        return rs.getInt("PUMA5");
    }

    public static int getMsaPMsa5(ResultSet rs) throws SQLException {
        return rs.getInt("MSAPMSA5");
    }
}
