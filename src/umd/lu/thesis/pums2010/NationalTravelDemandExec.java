/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010;

/**
 *
 * @author Home
 */
public class NationalTravelDemandExec {
    
    public static void main(String[] args) throws Exception {
        int start = Integer.parseInt(args[0]);
        int end = Integer.parseInt(args[1]);
//        int start = 73030;
//        int end = 73031;
        NationalTravelDemand runner = new NationalTravelDemand();
        runner.run(start, end);
    }
}
