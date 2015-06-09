/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.helper.FileUtils;

/**
 *
 * @author Home
 */
public class CalculateMSAPMSA {

    private Pums2010DAOImpl dao;

    private final static int ioBufferSize = 10485760;

    // HashMap:    <stpuma , (puma, msapmsa)      >
    private HashMap<Integer, List<MsapmsaPercentagePair>> pumaMsaPerpopContainer = new HashMap<>();

    org.apache.log4j.Logger log = org.apache.log4j.LogManager.getLogger(CalculateMSAPMSA.class);
    
    // can't process all states due to memory issues
    private int cutoffState = 5;

    public CalculateMSAPMSA() {
    }

    public void run() {
        dao = new Pums2010DAOImpl();
        log.info("Started to load file 'PUMA_MSA_PerPop.txt'.");
        initPumaMsaPerpopContainer();
        log.info("Completed loading file. " + pumaMsaPerpopContainer.size() + " records parsed.");

        log.info("Started to calculate MSAPMSA.");
        List<Integer> states = getStates();
        for (Integer st : states) {
            if(st > cutoffState) {
                List<Integer> pumas = getPumas(st);
                for (Integer p : pumas) {
                    List<Integer> ids = getIdsbyStAndPuma(st, p);
                    List<MsapmsaPercentagePair> mpList = pumaMsaPerpopContainer.get(constructStPuma(st, p));
                    Random r = new Random();
                    List<Integer> selectedIds = new ArrayList<>();

                    for (int i = 0; i < mpList.size(); i++) {
                        selectedIds.clear();
                        MsapmsaPercentagePair mp = mpList.get(i);
                        double percentage = mp.getPercentage();
                        int msapmsa = mp.getMsapmsa();
                        int size = ids.size();
                        if(i == mpList.size() - 1) {
                            // last one in the list
                            setMsapmsa(ids, msapmsa);
                        }
                        else {
                            // not last one
                            int assignable = (int) (size * percentage);
                            for (int assgn = 0; assgn < assignable; assgn++) {
                                size = ids.size();
                                int randomId = r.nextInt(size);
                                selectedIds.add(ids.get(randomId));
                                ids.remove(randomId);
                            }
                            setMsapmsa(selectedIds, msapmsa);
                        }
                    }
                }
            }
        }
        log.info("Completed.");
    }

    private List<Integer> getStates() {
        log.info("Started querying all ST types.");
        String stmtString = "select st from person_household_expanded group by st";
        List<Integer> states = new ArrayList<>();
        try {
            Statement st = dao.getConnection().createStatement();
            ResultSet rs = st.executeQuery(stmtString);
            while (rs.next()) {
                states.add(rs.getInt(1));
            }
        }
        catch (SQLException ex) {
            log.error("Error: " + ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        log.info("Completed. " + states.size() + " returned.");
        return states;
    }

    private List<Integer> getPumas(int state) {
        log.info("Started querying all PUMA types by state " + state + ".");
        String stmtString = "SELECT PUMA FROM PERSON_HOUSEHOLD_EXPANDED WHERE ST=" + state + " GROUP BY PUMA";
        List<Integer> pumas = new ArrayList<>();
        try {
            Statement st = dao.getConnection().createStatement();
            ResultSet rs = st.executeQuery(stmtString);
            while (rs.next()) {
                pumas.add(rs.getInt(1));
            }
        }
        catch (SQLException ex) {
            log.error("Error: " + ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        log.info("Completed. " + pumas.size() + " returned.");
        return pumas;
    }

    private void initPumaMsaPerpopContainer() {
        // prepare reader to read input file
        BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("pums.puma.msa.perpop"), ioBufferSize);

        String line = null;
        try {
            while ((line = br.readLine()) != null) {
                // skip header then process each line
                if(!line.startsWith("statefips")) {
                    MsapmsaPercentagePair mpp = new MsapmsaPercentagePair();
                    mpp.setMsapmsa(Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.e, line)));
                    mpp.setPercentage(Double.parseDouble(ExcelUtils.getColumnValue(ExcelUtils.h, line)));
                    // first, look up by the key 
                    Integer key = Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.i, line));
                    List<MsapmsaPercentagePair> values = pumaMsaPerpopContainer.get(key);
                    // not found, create the List and put the value in
                    if(values == null) {
                        values = new ArrayList<>();
                    }
                    values.add(mpp);
                    pumaMsaPerpopContainer.put(key, values);
                }
            }
        }
        catch (IOException ex) {
            log.error("Error: " + ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
    }

    private Integer constructStPuma(int st, int puma) {
        return Integer.parseInt(st + String.format("%05d", puma));
    }

    private List<Integer> getIdsbyStAndPuma(Integer state, Integer puma) {
        log.info("Start querying for IDs on ST = " + state + " and PUMA = " + puma);
        List<Integer> ids = new ArrayList<>();
        String stmtString = "SELECT ID FROM PERSON_HOUSEHOLD_EXPANDED WHERE ST=" + state + " AND PUMA=" + puma;
        try {
            Statement st = dao.getConnection().createStatement();
            ResultSet rs = st.executeQuery(stmtString);
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        catch (SQLException ex) {
            log.error("Error: " + ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        log.info("Completed. " + ids.size() + " returned.");
        return ids;
    }

    private int setMsapmsa(List<Integer> ids, int msapmsa) {
        log.info("Start updating MSAPMSA (value: " + msapmsa + ") for " + ids.size() + " IDs");
        int rows = -1;
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE PERSON_HOUSEHOLD_EXPANDED SET MSAPMSA = ").append(msapmsa).append(" WHERE ID IN (");
        for (int i = 0; i < ids.size(); i++) {
            if(i == ids.size() - 1) {
                sb.append(ids.get(i));
            }
            else {
                sb.append(ids.get(i) + ",");
            }
        }
        sb.append(");");
        try {
            Statement stmt = dao.getConnection().createStatement();
            rows = stmt.executeUpdate(sb.toString());

        }
        catch (SQLException ex) {
            log.error("Error: " + ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        log.info("Completed. " + rows + " returned");
        return rows;
    }
}
