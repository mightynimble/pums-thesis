/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import umd.lu.thesis.common.ThesisBase;

/**
 *
 * @author Home
 */
public class NationalTravelDemandExec extends ThesisBase{
    private final static Logger sLog = LogManager.getLogger(NationalTravelDemandExec.class);
    
    public static void main(String[] args) throws Exception {
//        int start = Integer.parseInt(args[0]);
//        int end = Integer.parseInt(args[1]);
        int start = 73030;
        int end = 73031;
        try {
            ThesisBase tb = new ThesisBase();
            NationalTravelDemand runner = new NationalTravelDemand();
            runner.run(start, end);
        }
        catch (Exception e) {
            sLog.error("error: ", e);
        }
    }
}
