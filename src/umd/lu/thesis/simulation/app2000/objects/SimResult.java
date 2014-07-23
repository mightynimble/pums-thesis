package umd.lu.thesis.simulation.app2000.objects;

/**
 *
 * @author Yijing Lu
 */
public class SimResult {

    private int msapmsa;
    private int d;
    private int time;
    private int mode;
    
    public SimResult(int msapmsa, int d, int time, int mode) {
        msapmsa = msapmsa;
        d = d;
        time = time;
        mode = mode;
    }

    /**
     * @return the o
     */
    public int getMsapmsa() {
        return msapmsa;
    }

    /**
     * @param o the o to set
     */
    public void setMsapmsa(int o) {
        this.msapmsa = o;
    }

    /**
     * @return the d
     */
    public int getD() {
        return d;
    }

    /**
     * @param d the d to set
     */
    public void setD(int d) {
        this.d = d;
    }

    /**
     * @return the time
     */
    public int getTime() {
        return time;
    }

    /**
     * @param time the time to set
     */
    public void setTime(int time) {
        this.time = time;
    }

    /**
     * @return the mode
     */
    public int getMode() {
        return mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(int mode) {
        this.mode = mode;
    }
}
