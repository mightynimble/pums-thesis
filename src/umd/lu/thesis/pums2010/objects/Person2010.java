/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010.objects;

/**
 *
 * @author Home
 */
public class Person2010 extends umd.lu.thesis.simulation.app2000.objects.Person {

    private int pid;

    private int serialNo;

    private int pwgtp;

    private int age;

    private int pincp;

    private int sch;

    private int sex;

    private int esr;

    private int puma;

    private int st;

    private int np;

    private int hht;

    private int hincp;

    private int hupaoc;

    private int huparc;

    private int sumpinc;

    private double htinc;

    private int rage;

    private int incLevel;

    private int empStatus;

    private int hhType;

    private int msapmsa;

    private int rB;

    private int rP;

    private int rPB;
    
    private int dumEmp;
    
    /**
     * There is no corresponding zone id for msapmsa 9999. A 'state + 99' technique
     * is used to change the msapmsa in this case. The updated msapmsa is stored
     * in this temporary field.
     */
    private int tmpMsapmsa;

    /**
     * @return the id
     */
    public int getPid() {
        return pid;
    }

    /**
     * @param id the id to set
     */
    public void setPid(int pid) {
        this.pid = pid;
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
     * @return the pwgtp
     */
    public int getPwgtp() {
        return pwgtp;
    }

    /**
     * @param pwgtp the pwgtp to set
     */
    public void setPwgtp(int pwgtp) {
        this.pwgtp = pwgtp;
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
     * @return the pincp
     */
    public int getPincp() {
        return pincp;
    }

    /**
     * @param pincp the pincp to set
     */
    public void setPincp(int pincp) {
        this.pincp = pincp;
    }

    /**
     * @return the sch
     */
    public int getSch() {
        return sch;
    }

    /**
     * @param sch the sch to set
     */
    public void setSch(int sch) {
        this.sch = sch;
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
     * @return the esr
     */
    public int getEsr() {
        return esr;
    }

    /**
     * @param esr the esr to set
     */
    public void setEsr(int esr) {
        this.esr = esr;
    }

    /**
     * @return the puma
     */
    public int getPuma() {
        return puma;
    }

    /**
     * @param puma the puma to set
     */
    public void setPuma(int puma) {
        this.puma = puma;
    }

    /**
     * @return the st
     */
    public int getSt() {
        return st;
    }

    /**
     * @param st the st to set
     */
    public void setSt(int st) {
        this.st = st;
    }

    /**
     * @return the np
     */
    public int getNp() {
        return np;
    }

    /**
     * @param np the np to set
     */
    public void setNp(int np) {
        this.np = np;
    }

    /**
     * @return the hht
     */
    public int getHht() {
        return hht;
    }

    /**
     * @param hht the hht to set
     */
    public void setHht(int hht) {
        this.hht = hht;
    }

    /**
     * @return the hincp
     */
    public int getHincp() {
        return hincp;
    }

    /**
     * @param hincp the hincp to set
     */
    public void setHincp(int hincp) {
        this.hincp = hincp;
    }

    /**
     * @return the hupaoc
     */
    public int getHupaoc() {
        return hupaoc;
    }

    /**
     * @param hupaoc the hupaoc to set
     */
    public void setHupaoc(int hupaoc) {
        this.hupaoc = hupaoc;
    }

    /**
     * @return the huparc
     */
    public int getHuparc() {
        return huparc;
    }

    /**
     * @param huparc the huparc to set
     */
    public void setHuparc(int huparc) {
        this.huparc = huparc;
    }

    /**
     * @return the sumpinc
     */
    public int getSumpinc() {
        return sumpinc;
    }

    /**
     * @param sumpinc the sumpinc to set
     */
    public void setSumpinc(int sumpinc) {
        this.sumpinc = sumpinc;
    }

    /**
     * @return the htinc
     */
    public double getHtinc() {
        return htinc;
    }

    /**
     * @param htinc the htinc to set
     */
    public void setHtinc(int htinc) {
        this.htinc = htinc;
    }

    /**
     * @return the rage
     */
    public int getRage() {
        return rage;
    }

    /**
     * @param rage the rage to set
     */
    public void setRage(int rage) {
        this.rage = rage;
    }

    /**
     * @return the incLvl
     */
    public int getIncLevel() {
        return incLevel;
    }

    /**
     * @param incLvl the incLvl to set
     */
    public void setIncLevel(int incLvl) {
        this.incLevel = incLvl;
    }

    /**
     * @return the empStatus
     */
    public int getEmpStatus() {
        return empStatus;
    }

    /**
     * @param empStatus the empStatus to set
     */
    public void setEmpStatus(int empStatus) {
        this.empStatus = empStatus;
    }

    /**
     * @return the hhtype
     */
    public int getHhType() {
        return hhType;
    }

    /**
     * @param hhtype the hhtype to set
     */
    public void setHhType(int hhtype) {
        this.hhType = hhtype;
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
     * @return the rB
     */
    public int getrB() {
        return rB;
    }

    /**
     * @param rB the rB to set
     */
    public void setrB(int rB) {
        this.rB = rB;
    }

    /**
     * @return the rP
     */
    public int getrP() {
        return rP;
    }

    /**
     * @param rP the rP to set
     */
    public void setrP(int rP) {
        this.rP = rP;
    }

    /**
     * @return the rPB
     */
    public int getrPB() {
        return rPB;
    }

    /**
     * @param rPB the rPB to set
     */
    public void setrPB(int rPB) {
        this.rPB = rPB;
    }
    
    public int getDumEmp() {
        return dumEmp;
    }
    
    public void setDumEmp(int dumEmp) {
        this.dumEmp = dumEmp;
    }
    
    public int getTmpMsapmsa() {
        return tmpMsapmsa;
    }
    
    public void setTmpMsapmsa(int tmpMsapmsa) {
        this.tmpMsapmsa = tmpMsapmsa;
    }
}
