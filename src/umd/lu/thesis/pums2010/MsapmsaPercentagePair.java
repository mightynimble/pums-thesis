/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010;

/**
 *
 * @author Home
 */
class MsapmsaPercentagePair {
    private int msapmsa;
    private double percentage;
                      
    public MsapmsaPercentagePair(int msapmsa, double percentage) {
        msapmsa = msapmsa;
        percentage = percentage;
    }

    public MsapmsaPercentagePair() {
        
    }

    /**
     * @return the msapmsa
     */
    public int getMsapmsa() {
        return msapmsa;
    }

    /**
     * @param msapmsa the msapmsa to set
     */
    public void setMsapmsa(int msapmsa) {
        this.msapmsa = msapmsa;
    }

    /**
     * @return the percentage
     */
    public double getPercentage() {
        return percentage;
    }

    /**
     * @param percentage the percentage to set
     */
    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
