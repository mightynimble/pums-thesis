package umd.lu.thesis.pums;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.core.MultivaluedHashMap;
import umd.lu.thesis.exceptions.InvalidValueException;
import umd.lu.thesis.pums.dao.PumsDAOImpl;
import org.apache.commons.math3.distribution.NormalDistribution;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.pums.objects.NormalDistributionParams;
import umd.lu.thesis.pums.utils.HhPerUtils;

/**
 *
 * @author lousia
 */
public class OriginalRandom {

    private static double mean;

    private static double sd;

    private static PumsDAOImpl pumsDao;

    private static HashMap<String, Integer> statePumaDbLineCount;

    private static HashMap<String, Integer> statePumaMsaPMsa;

    private static MultivaluedHashMap<String, Integer[]> statePumaPerPop;

    private static HashMap<String, Double[]> tripRateBusiness;

    private static HashMap<String, Double[]> tripRatePleasure;

    private static HashMap<String, Double[]> tripRatePb;

    private static List<String> batchItems;

    private static final int SELECT_BATCH = 10000;

//    private static final int[] TABLE_IDS = {1, 2, 4, 5, 6, 8, 9, 10, 11, 12, 13, 15, 16,
//        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34,
//        35, 36, 37, 38, 39, 40, 41, 42, 44, 45, 46, 47, 48, 49, 50, 51, 53, 54, 55, 56};
    private static final int[] TABLE_IDS = {40, 41, 42, 44, 45, 46, 47, 48, 49, 50, 51, 53, 54, 55, 56};

    private static final int injectStartId = 0;

    private static final String tableHeader = "HH_Per_";

    public static void main(String[] args) throws Exception {
        initializeParams();

        double startTime = 0.0;
        double stopTime = 0.0;
        for (int id : TABLE_IDS) {
            System.out.println("Start to process table: " + getTableName(id) + " column.");
            startTime = System.nanoTime()/1000000000.0;
            
            processTable(id);
            
            stopTime = System.nanoTime()/1000000000.0;
            System.out.println(" -- speed: " + (int)(stopTime - startTime) + " sec, memory: " + (Runtime.getRuntime().totalMemory()/1024/1024) + "MB");
        }
    }
    
    private static void processTable(int id) throws InvalidValueException, SQLException {
        batchItems.clear();
        
        initPumaStateMaps(id);
        
        int totalLines = 0;
        int startPk = 0;
        if (injectStartId != 0) {
            startPk = injectStartId;
        }
        int stopPk = startPk + SELECT_BATCH;

        int processedRows = SELECT_BATCH;
        
        while (processedRows == SELECT_BATCH) {
            if (totalLines % 50000 <= totalLines) {
                System.out.println("current line: " + totalLines);
            }
            
            processedRows = batchProcess(id, startPk, stopPk);
            
            totalLines += processedRows;
            startPk += processedRows;
            stopPk = startPk + SELECT_BATCH;
        }
        
        processMsaPMsaColumn(id);
    }
    
    private static int batchProcess(int tableId, int start, int end) throws InvalidValueException {
        String statementString = "SELECT * FROM " + getTableName(tableId) + " WHERE ID > " + start + " AND ID <= " + end;
        int count = 0;
        try {
            Statement st = pumsDao.getConnection().createStatement();
            ResultSet rs = st.executeQuery(statementString);

            while (rs.next()) {
                count++;
                String updateSql = constructUpdateStatement(rs, tableId);
                batchItems.add(updateSql);
            }
            System.out.println(" -- executing batchItems: " + batchItems.size());
            pumsDao.batchExecution(batchItems);
            batchItems.clear();
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
//        System.gc();
        return count;
    }
    
    private static String constructUpdateStatement(ResultSet rs, int tableId) throws SQLException, InvalidValueException {
        Double randBusiness = getRandomSample(rs, "business");
        Double randPleasure = getRandomSample(rs, "pleasure");
        Double randPb = getRandomSample(rs, "pb");
        
        return "UPDATE " + getTableName(tableId) + " SET "
                + "  INCLEVEL = " + HhPerUtils.getIncLevel(rs)
                + ", MSA = " + HhPerUtils.getMsa(rs)
                + ", HHType = " + HhPerUtils.getHhType(rs)
                + ", randbusiness = " + randBusiness
                + ", randperson = " + randPleasure
                + ", randpb = " + randPb
                + " WHERE id = " + rs.getInt("ID") + ";";
                
    }
    
    private static double getRandomSample(ResultSet rs, String type) {
        try {
            int msa = HhPerUtils.getMsa(rs);
            int incLevel = HhPerUtils.getIncLevel(rs);
            int sex = HhPerUtils.getSex(rs);
            int dumEmp = HhPerUtils.getDumEmp(rs);

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
    
    private static NormalDistributionParams getNormalDistributionParams(int msa, int incLevel, int sex, int dumEmp, String type) throws InvalidValueException {
        NormalDistributionParams params = new NormalDistributionParams();
        String key = msa + "-" + incLevel + "-" + sex + "-" + dumEmp;
        if (type.equalsIgnoreCase("business")) {
            params.setMean(tripRateBusiness.get(key)[0]);
            params.setSd(tripRateBusiness.get(key)[1]);
        } else if (type.equalsIgnoreCase("pleasure")) {
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

    private static void processMsaPMsaColumn(int id) throws InvalidValueException, SQLException {
        batchItems.clear();

        for (String key : statePumaPerPop.keySet()) {
            int state = Integer.parseInt(key.split("-")[0]);
            if (state != id) {
                continue;
            }
            int puma5 = Integer.parseInt(key.split("-")[1]);
            if (statePumaPerPop.get(key).size() == 1) {
                int probability = statePumaPerPop.getFirst(key)[0];
                int msaPMsa = statePumaPerPop.getFirst(key)[1];
                Integer tmp = statePumaMsaPMsa.get(key);
                if (tmp == null) {
                    throw new InvalidValueException("Invalid key: " + key + " in statePumaMsaPMsa<>");
                }
                int msaPMsa5 = tmp;
                if (msaPMsa5 == 9999) {
                    batchItems.add("UPDATE " + getTableName(state)
                            + " SET msapmsa = " + state + "99 WHERE State = "
                            + state + " AND PUMA5 = " + puma5 + ";");
                } else {
                    batchItems.add("UPDATE " + getTableName(state)
                            + " SET msapmsa = " + msaPMsa + " WHERE State = "
                            + state + " AND PUMA5 = " + puma5 + ";");
                }
            } else {
                int rows = statePumaDbLineCount.get(key);
                for (Integer[] values : statePumaPerPop.get(key)) {
                    int probability = values[0];
                    int msaPMsa = values[1];
                    int msaPMsa5 = statePumaMsaPMsa.get(key);
                    int updateRows = Math.round((float)probability / 100 * rows);
                    if (msaPMsa5 == 9999) {
                        batchItems.add("UPDATE " + getTableName(state)
                                + " SET msapmsa = " + state + "99 WHERE State = "
                                + state + " AND PUMA5 = " + puma5 + " AND msapmsa is null LIMIT " + updateRows + ";");
                    } else if (msaPMsa5 == 9998 || msaPMsa5 == 9997) {
                        if (msaPMsa == 9999) {
                            batchItems.add("UPDATE " + getTableName(state)
                                    + " SET msapmsa = " + state + "99 WHERE State = "
                                    + state + " AND PUMA5 = " + puma5 + " AND msapmsa is null LIMIT " + updateRows + ";");
                        } else {
                            batchItems.add("UPDATE " + getTableName(state)
                                    + " SET msapmsa = " + msaPMsa + " WHERE State = "
                                    + state + " AND PUMA5 = " + puma5 + " AND msapmsa is null LIMIT " + updateRows + ";");
                        }
                    } else {
                        batchItems.add("UPDATE " + getTableName(state)
                                + " SET msapmsa = " + msaPMsa + " WHERE State = "
                                + state + " AND PUMA5 = " + puma5 + " AND msapmsa is null LIMIT " + updateRows + ";");
                    }
                }
            }
        }

        pumsDao.batchExecution(batchItems);
    }

    private static void initPumaStateMaps(int id) {
        statePumaDbLineCount.clear();
        statePumaMsaPMsa.clear();
        
        try {
            String st = "SELECT STATE, PUMA5, COUNT(*), MSAPMSA5 FROM "
                    + getTableName(id) + " GROUP BY STATE, PUMA5;";
            ResultSet rs = pumsDao.executeQuery(st);
            while (rs.next()) {
                int state = rs.getInt(1);
                int puma5 = rs.getInt(2);
                int count = rs.getInt(3);
                int msaPMsa5 = rs.getInt(4);
                statePumaDbLineCount.put(Integer.toString(state) + "-"
                        + Integer.toString(puma5), count);
                statePumaMsaPMsa.put(Integer.toString(state) + "-"
                        + Integer.toString(puma5), msaPMsa5);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static void initializeParams() {
        pumsDao = new PumsDAOImpl();
        batchItems = new ArrayList<>();
        statePumaDbLineCount = new HashMap<>();
        statePumaMsaPMsa = new HashMap<>();
        statePumaPerPop = new MultivaluedHashMap<>();
        tripRateBusiness = new HashMap<>();
        tripRatePleasure = new HashMap<>();
        tripRatePb = new HashMap<>();

        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("pums.puma.msa.perpop"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("statefips")) {
                    String key = ExcelUtils.getColumnValue(1, line) + "-"
                            + ExcelUtils.getColumnValue(3, line);
                    Integer probability = Math.round(Float.parseFloat(ExcelUtils.getColumnValue(8, line)) * 100);
                    Integer msaPMsa = Integer.parseInt(ExcelUtils.getColumnValue(5, line));

                    statePumaPerPop.add(key, new Integer[]{probability, msaPMsa});
                }
            }
            br.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

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

    private static String getTableName(int id) {
        return tableHeader + (id < 10 ? "0" + id : id);
    }

}
