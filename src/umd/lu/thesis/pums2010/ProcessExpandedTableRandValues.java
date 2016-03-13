/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Home
 */
public class ProcessExpandedTableRandValues {

    private static int totalRows = 0;

    private static Pums2010DAOImpl pumsDao;

    public static void main(String[] args) throws Exception {

        org.apache.log4j.Logger log = org.apache.log4j.LogManager.getLogger(ProcessExpandedTableMSAPMSA.class);
        
        log.info("Started processing expanded table 'PERSON_HOUSEHOLD_EXPANDED'.");
        pumsDao = new Pums2010DAOImpl();
        log.info("Started - Querying for total number of rows...");
        totalRows = pumsDao.getTotalRecordsByMaxId("PERSON_HOUSEHOLD_EXPANDED");
        log.info("Completed. " + totalRows + " rows returned.");
        
        // calculate randBusiness, randPerson, and randBP columns
        (new CalculateRandValues(totalRows)).run();
        
        log.info("Completed processing expanded table 'PERSON_HOUSEHOLD_EXPANDED'.");
    }
}
