package umd.lu.thesis.pums2010.objects;

public class Trip {

    private int personId;
    private String type;
    private int inbound;
    private int tripO;
    private int oMsa;
    private int tripD;
    private int dMsa;
    private int toy;
    private String mode;
    private int party;
    private double distance;
    private int origin;
    private int dest;

    public Trip(int personId, String type, int inbound, int tripO, int oMsa, int tripD, int dMsa, int toy, String mode, int party, double distance, int origin, int dest) {
        this.personId = personId;
        this.type = type;
        this.inbound = inbound;
        this.tripO = tripO;
        this.oMsa = oMsa;
        this.tripD = tripD;
        this.dMsa = dMsa;
        this.toy = toy;
        this.mode = mode;
        this.party = party;
        this.distance = distance;
        this.origin = origin;
        this.dest = dest;
    }

    /**
     * @return the personId
     */
    public int getPersonId() {
        return personId;
    }

    /**
     * @param personId the personId to set
     */
    public void setPersonId(int personId) {
        this.personId = personId;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the inbound
     */
    public int getInbound() {
        return inbound;
    }

    /**
     * @param inbound the inbound to set
     */
    public void setInbound(int inbound) {
        this.inbound = inbound;
    }

    /**
     * @return the tripO
     */
    public int getTripO() {
        return tripO;
    }

    /**
     * @param tripO the tripO to set
     */
    public void setTripO(int tripO) {
        this.tripO = tripO;
    }

    /**
     * @return the oMsa
     */
    public int getoMsa() {
        return oMsa;
    }

    /**
     * @param oMsa the oMsa to set
     */
    public void setoMsa(int oMsa) {
        this.oMsa = oMsa;
    }

    /**
     * @return the tripD
     */
    public int getTripD() {
        return tripD;
    }

    /**
     * @param tripD the tripD to set
     */
    public void setTripD(int tripD) {
        this.tripD = tripD;
    }

    /**
     * @return the dMsa
     */
    public int getdMsa() {
        return dMsa;
    }

    /**
     * @param dMsa the dMsa to set
     */
    public void setdMsa(int dMsa) {
        this.dMsa = dMsa;
    }

    /**
     * @return the toy
     */
    public int getToy() {
        return toy;
    }

    /**
     * @param toy the toy to set
     */
    public void setToy(int toy) {
        this.toy = toy;
    }

    /**
     * @return the mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * @return the party
     */
    public int getParty() {
        return party;
    }

    /**
     * @param party the party to set
     */
    public void setParty(int party) {
        this.party = party;
    }

    /**
     * @return the distance
     */
    public double getDistance() {
        return distance;
    }

    /**
     * @param distance the distance to set
     */
    public void setDistance(double distance) {
        this.distance = distance;
    }

    /**
     * @return the origin
     */
    public int getOrigin() {
        return origin;
    }

    /**
     * @param origin the origin to set
     */
    public void setOrigin(int origin) {
        this.origin = origin;
    }

    /**
     * @return the dest
     */
    public int getDest() {
        return dest;
    }

    /**
     * @param dest the dest to set
     */
    public void setDest(int dest) {
        this.dest = dest;
    }
}
