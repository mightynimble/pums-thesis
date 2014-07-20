package umd.lu.thesis.pums;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import umd.lu.thesis.common.ThesisConstants;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.pums.dao.PumsDAOImpl;

/**
 *
 * @author lousia
 */
public class ImportDataFilesToDb {

    private static final int[] pColumns = {1, 2, 3, 6, 12, 14, 34, 102, 144, 160};
    private static final int[] hColumns = {1, 2, 4, 5, 6, 7, 8, 10, 21, 91, 98, 99};
    private static final int maxBatch = 2000;

    private static PumsDAOImpl pumsDao;

    public static void main(String[] args) throws Exception {
        long hCount = 0;
        long pCount = 0;

        // open output files to write
        try {
            pumsDao = new PumsDAOImpl();

            File pumsOutputFolder = new File(ThesisProperties.getProperties("pums.output.folder"));

            for (File file : pumsOutputFolder.listFiles()) {
                System.out.println("File - " + file.getName());
                List<String> batchItems = new ArrayList<>();

                String fileName = file.getName();
                if (fileName.contains(".output.h.txt")) {
                    String tableName = "H_" + fileName.substring(0, fileName.length() - ".output.h.txt".length());
                    pumsDao.createHouseholdTable(tableName);
                    batchItems.clear();
                    // H file
                    BufferedReader br_h = new BufferedReader(new FileReader(file), ThesisConstants.bufferSize);
                    String line = "";
                    int subTotal = 0;
                    while ((line = br_h.readLine()) != null) {
                        if (batchItems.size() == maxBatch) {
                            // batch full, execute
                            pumsDao.batchExecution(batchItems);
                            // clear up
                            batchItems.clear();
                        }
                        String newLine = constructInsertStatement(line, hColumns, "H", tableName);
                        if (!newLine.isEmpty()) {
                            batchItems.add(newLine);
                        }
                        subTotal += 1;
                        if (subTotal % 100000 == 1) {
                            System.out.println("     - line " + subTotal);
                        }
                        hCount += 1;
                    }
                    // execute the rest inserts in batchItems
                    pumsDao.batchExecution(batchItems);
                    // clear up
                    batchItems.clear();

                    System.out.println("     - SubTotal: " + subTotal);
                    br_h.close();
                } else if (file.getName().contains(".output.p.txt")) {
                    String tableName = "P_" + fileName.substring(0, fileName.length() - ".output.p.txt".length());
                    pumsDao.createPersonTable(tableName);
                    batchItems.clear();
                    // P file
                    BufferedReader br_p = new BufferedReader(new FileReader(file), ThesisConstants.bufferSize);
                    String line = "";
                    int subTotal = 0;
                    while ((line = br_p.readLine()) != null) {
                        if (batchItems.size() == maxBatch) {
                            // batch full, execute
                            pumsDao.batchExecution(batchItems);
                            // clear up
                            batchItems.clear();
                        }
                        String newLine = constructInsertStatement(line, pColumns, "P", tableName);
                        if (!newLine.isEmpty()) {
                            batchItems.add(newLine);
                        }
                        subTotal += 1;
                        if (subTotal % 100000 == 1) {
                            System.out.println("     - line " + subTotal);
                        }
                        pCount += 1;
                    }
                    // execute the rest inserts in batchItems
                    pumsDao.batchExecution(batchItems);
                    // clear up
                    batchItems.clear();

                    System.out.println("     - SubTotal: " + subTotal);
                    br_p.close();
                }
            }

            pumsDao.destroy();
        } catch (IOException e) {
            System.out.println(e);
        }

        System.out.println("+++++++++++ H Files Total: " + hCount);
        System.out.println("+++++++++++ P Files Total: " + pCount);
    }

    private static String constructInsertStatement(String line, int[] columns, String type, String tableName) throws Exception {
        String statement = "INSERT INTO " + tableName
                + " VALUES (DEFAULT, '" + type.toUpperCase() + "', ";

        int age = 0;
        int enroll = 0;
        int esr = 0;
        for (int i = 1; i < columns.length; i++) {
            String elem = ExcelUtils.getColumnValue(columns[i], line);
            if (i == 9 && type.equalsIgnoreCase("P")) {
                // 10th column in P file: incTot
                if (elem.contains("-") || elem.isEmpty()) {
                    elem = "0";
                }
            }
            if (i == 8 && type.equalsIgnoreCase("P")) {
                // 9th column in P file: incWs
                if (elem.contains("175000+")) {
                    elem = "180000";
                }
                if (elem.isEmpty()) {
                    elem = "0";
                }
            }
            if (i == 5) {
                // 6th column in P file: age
                // skip this line if age <= 18
                age = Integer.parseInt(elem);
                if (age <= 18) {
                    return "";
                }
            }
            if (i == 6) {
                // 7th column in P file: enroll
                enroll = Integer.parseInt(elem);
            }
            if (i == 7) {
                // 8th column in P file: esr
                esr = Integer.parseInt(elem);
            }

            Integer value = Integer.parseInt(elem);
            statement += value.toString() + ",";
        }

        if (type.equalsIgnoreCase("P")) {
            // dumAge
            statement += getDumAge(age) + ",";
            // incLevel
            statement += "NULL,";
            // dumEmp
            statement += getDumEmp(enroll, esr) + ",";
            // msa
            statement += "NULL,";
            // hht
            statement += "NULL);";
        } else {
            // replace the last ',' to ')'
            if (statement.endsWith(",")) {
                statement = statement.substring(0, statement.length() - 1) + ");";
            }
        }

        return statement;
    }

    private static String getDumAge(int age) {
        if (age <= 35) {
            return "1";
        } else if (age > 35 && age <= 55) {
            return "2";
        } else {
            return "3";
        }
    }

    private static String getDumEmp(int enroll, int esr) {
        if (enroll == 2 || enroll == 3) {
            return "3";
        }
        if (esr == 1 || esr == 2 || esr == 4 || esr == 5) {
            return "1";
        }
        return "2";
    }
}
