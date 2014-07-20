package umd.lu.thesis.simulation.app2000.objects;

/**
 *
 * @author lousia
 */
public class Person {
    private int pid;
    private String recType;
    private int serialNo;
    private int pNum;
    private int pWeight;
    private int sex;
    private int age;
    private int dumAge;
    private int incLevel;
    private int dumEmp;
    private int msa;
    private int hhType;
    private int state;
    private int persons;
    private double hhinc;
    private int randB;
    private int randP;
    private int randPB;
    private int msapmsa;
    private int dest;
    private int time;
    private int mode;

    /**
     * @return the pid
     */
    public int getPid() {
        return pid;
    }

    /**
     * @param pid the pid to set
     */
    public void setPid(int pid) {
        this.pid = pid;
    }

    /**
     * @return the recType
     */
    public String getRecType() {
        return recType;
    }

    /**
     * @param recType the recType to set
     */
    public void setRecType(String recType) {
        this.recType = recType;
    }

    /**
     * @return the serialNo
     */
    public int getSerialNo() {
        return serialNo;
    }

    /**
     * @param serialNo the serialNo to set
     */
    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
    }

    /**
     * @return the pNum
     */
    public int getpNum() {
        return pNum;
    }

    /**
     * @param pNum the pNum to set
     */
    public void setpNum(int pNum) {
        this.pNum = pNum;
    }

    /**
     * @return the pWeight
     */
    public int getpWeight() {
        return pWeight;
    }

    /**
     * @param pWeight the pWeight to set
     */
    public void setpWeight(int pWeight) {
        this.pWeight = pWeight;
    }

    /**
     * @return the sex
     */
    public int getSex() {
        return sex;
    }

    /**
     * @param sex the sex to set
     */
    public void setSex(int sex) {
        this.sex = sex;
    }

    /**
     * @return the age
     */
    public int getAge() {
        return age;
    }

    /**
     * @param age the age to set
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * @return the dumAge
     */
//    public int getDumAge() {
//        return dumAge;
//    }

    /**
     * @param dumAge the dumAge to set
     */
//    public void setDumAge(int dumAge) {
//        this.dumAge = dumAge;
//    }

    /**
     * @return the incLevel
     */
    public int getIncLevel() {
        return incLevel;
    }

    /**
     * @param incLevel the incLevel to set
     */
    public void setIncLevel(int incLevel) {
        this.incLevel = incLevel;
    }

    /**
     * @return the dumEmp
     */
    public int getDumEmp() {
        return dumEmp;
    }

    /**
     * @param dumEmp the dumEmp to set
     */
    public void setDumEmp(int dumEmp) {
        this.dumEmp = dumEmp;
    }

    /**
     * @return the msa
     */
    public int getMsa() {
        return msa;
    }

    /**
     * @param msa the msa to set
     */
    public void setMsa(int msa) {
        this.msa = msa;
    }

    /**
     * @return the hhType
     */
    public int getHhType() {
        return hhType;
    }

    /**
     * @param hhType the hhType to set
     */
    public void setHhType(int hhType) {
        this.hhType = hhType;
    }

    /**
     * @return the state
     */
    public int getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * @return the persons
     */
    public int getPersons() {
        return persons;
    }

    /**
     * @param persons the persons to set
     */
    public void setPersons(int persons) {
        this.persons = persons;
    }

    /**
     * @return the hhinc
     */
    public double getHhinc() {
        return hhinc;
    }

    /**
     * @param hhinc the hhinc to set
     */
    public void setHhinc(double hhinc) {
        this.hhinc = hhinc;
    }

    /**
     * @return the randB
     */
    public int getRandB() {
        return randB;
    }

    /**
     * @param randB the randB to set
     */
    public void setRandB(int randB) {
        this.randB = randB;
    }

    /**
     * @return the randP
     */
    public int getRandP() {
        return randP;
    }

    /**
     * @param randP the randP to set
     */
    public void setRandP(int randP) {
        this.randP = randP;
    }

    /**
     * @return the randPB
     */
    public int getRandPB() {
        return randPB;
    }

    /**
     * @param randPB the randPB to set
     */
    public void setRandPB(int randPB) {
        this.randPB = randPB;
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
    
    public Person clone() {
        Person p = new Person();
        p.setAge(age);
        p.setDest(dest);
        p.setDumEmp(dumEmp);
        p.setHhType(hhType);
        p.setHhinc(hhinc);
        p.setIncLevel(incLevel);
        p.setMode(mode);
        p.setMsa(msa);
        p.setMsapmsa(msapmsa);
        p.setPersons(persons);
        p.setPid(pid);
        p.setRandB(randB);
        p.setRandP(randP);
        p.setRandPB(randPB);
        p.setRecType(recType);
        p.setSerialNo(serialNo);
        p.setSex(sex);
        p.setState(state);
        p.setTime(time);
        p.setpNum(pNum);
        p.setpWeight(pWeight);
     
        return p;
    }
    
}
