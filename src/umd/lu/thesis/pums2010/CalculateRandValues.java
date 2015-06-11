/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.math3.distribution.NormalDistribution;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.exceptions.InvalidValueException;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.pums.objects.NormalDistributionParams;
import umd.lu.thesis.pums.utils.HhPerUtils;

/**
 *
 * @author Home
 */
class CalculateRandValues {

    private Pums2010DAOImpl dao;

    private int totalRows;

    private org.apache.log4j.Logger log = org.apache.log4j.LogManager.getLogger(CalculateRandValues.class);

    private static HashMap<String, Double[]> tripRateBusiness;

    private static HashMap<String, Double[]> tripRatePleasure;

    private static HashMap<String, Double[]> tripRatePb;

    private static final int batchSize = 500;

    private static final int cutoffId = 1300000;

    public CalculateRandValues(int totalRows) {
        this.totalRows = totalRows;
        dao = new Pums2010DAOImpl();
        tripRateBusiness = new HashMap<>();
        tripRatePleasure = new HashMap<>();
        tripRatePb = new HashMap<>();

        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("pums.business.triprate"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("Group")) {
                    String key = ExcelUtils.getColumnValue(2, line) + "-"
                            + ExcelUtils.getColumnValue(3, line) + "-"
                            + ExcelUtils.getColumnValue(4, line) + "-"
                            + ExcelUtils.getColumnValue(5, line);
                    Double[] value = {Double.parseDouble(ExcelUtils.getColumnValue(6, line)),
                        Double.parseDouble(ExcelUtils.getColumnValue(7, line))
                    };

                    tripRateBusiness.put(key, value);
                }
            }
            br.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("pums.pleasure.triprate"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("Group")) {
                    String key = ExcelUtils.getColumnValue(2, line) + "-"
                            + ExcelUtils.getColumnValue(3, line) + "-"
                            + ExcelUtils.getColumnValue(4, line) + "-"
                            + ExcelUtils.getColumnValue(5, line);
                    Double[] value = {Double.parseDouble(ExcelUtils.getColumnValue(6, line)),
                        Double.parseDouble(ExcelUtils.getColumnValue(7, line))
                    };

                    tripRatePleasure.put(key, value);
                }
            }
            br.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("pums.pb.triprate"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("Group")) {
                    String key = ExcelUtils.getColumnValue(2, line) + "-"
                            + ExcelUtils.getColumnValue(3, line) + "-"
                            + ExcelUtils.getColumnValue(4, line) + "-"
                            + ExcelUtils.getColumnValue(5, line);
                    Double[] value = {Double.parseDouble(ExcelUtils.getColumnValue(6, line)),
                        Double.parseDouble(ExcelUtils.getColumnValue(7, line))
                    };

                    tripRatePb.put(key, value);
                }
            }
            br.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public void run() {
        log.info("Started setting rand value columns (R_BUSINESS, R_PERSON, R_PB)");
        String insertSql = "INSERT INTO ID_RANDS (ID, R_BUSINESS, R_PERSON, R_PB) VALUES ";
//        StringBuilder sb = new StringBuilder(insertSql);
        try {
            Statement st = dao.getConnection().createStatement();
            Statement insertStmt = dao.getConnection().createStatement();
            for (int id = cutoffId; id <= totalRows; id++) {
                if (id % 100000 == 1) {
                    log.info("Progress: " + id + " out of " + totalRows);
                }

                ResultSet rs = st.executeQuery("SELECT * FROM PERSON_HOUSEHOLD_EXPANDED WHERE ID = " + id);
                while (rs.next()) {
                    Double randBusiness = getRandomSample(rs, "business");
                    Double randPerson = getRandomSample(rs, "person");
                    Double randPB = getRandomSample(rs, "pb");
                    insertSql = prepareInsertElement(insertSql, rs.getInt("ID"), randBusiness, randPerson, randPB);
                }
                if (id % batchSize == 0) {
                    // execute insert
//                    sb.deleteCharAt(sb.lastIndexOf(","));
                    insertStmt.executeUpdate(insertSql.substring(0, insertSql.length() - 1));
                    insertSql = "INSERT INTO ID_RANDS (ID, R_BUSINESS, R_PERSON, R_PB) VALUES ";
//                    sb = new StringBuilder(insertSql);
                }

            }
            
            if (!insertSql.equals("INSERT INTO ID_RANDS (ID, R_BUSINESS, R_PERSON, R_PB) VALUES ")) {
                insertStmt.executeUpdate(insertSql.substring(0, insertSql.length() - 1));
            }
        } catch (SQLException ex) {
//                log.error("SQL: " + sb.toString());
            log.error("Error: " + ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        log.info("Completed. ");

    }

    private String prepareInsertElement(String sql, int id, Double randBusiness, Double randPerson, Double randPB) {
        sql += "(" + id + "," + randBusiness + "," + randPerson + "," + randPB + "),";
//        sb.append(sql);
        return sql;
    }

    private double getRandomSample(ResultSet rs, String type) {
        try {
            int msa = rs.getInt("MSAPMSA") == 9999 ? 2 : 1;
            int incLevel = rs.getInt("INC_LVL");
            int sex = rs.getInt("SEX");
            int dumEmp = rs.getInt("EMP_STATUS");

            NormalDistributionParams ndParams = getNormalDistributionParams(msa, incLevel, sex, dumEmp, type);
            if (ndParams.getSd() == 0.0) {
                return ndParams.getMean();
            } else {
                NormalDistribution nd = new NormalDistribution(ndParams.getMean(), ndParams.getSd());
                double tmpSample = nd.sample();
                return tmpSample < 0.0 ? 0.0 : tmpSample;
            }
        } catch (SQLException | InvalidValueException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        return Double.NEGATIVE_INFINITY;
    }

    private NormalDistributionParams getNormalDistributionParams(int msa, int incLevel, int sex, int dumEmp, String type) throws InvalidValueException {
        NormalDistributionParams params = new NormalDistributionParams();
        String key = msa + "-" + incLevel + "-" + sex + "-" + dumEmp;
        if (type.equalsIgnoreCase("business")) {
            params.setMean(tripRateBusiness.get(key)[0]);
            params.setSd(tripRateBusiness.get(key)[1]);
        } else if (type.equalsIgnoreCase("person")) {
            params.setMean(tripRatePleasure.get(key)[0]);
            params.setSd(tripRatePleasure.get(key)[1]);
        } else if (type.equalsIgnoreCase("pb")) {
            params.setMean(tripRatePb.get(key)[0]);
            params.setSd(tripRatePb.get(key)[1]);
        } else {
            throw new InvalidValueException("Invalid type value: " + type);
        }
        return params;
    }
}
