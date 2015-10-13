/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.pums2010.math;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.exceptions.InvalidValueException;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.pums2010.math.Math;
import umd.lu.thesis.pums2010.objects.ModeChoice;
import umd.lu.thesis.pums2010.objects.Person2010;
import umd.lu.thesis.simulation.app2000.objects.TripType;

/**
 *
 * @author Home
 */
public class Math /* extends umd.lu.thesis.simulation.app2000.math.Formulae */ {

    private static final int INVALID_QUARTER = -1;

    private final HashMap<String, Double[]> otherCarMap;

    private final HashMap<String, Double[]> businessCarMap;

    private final HashMap<String, Double[]> airMap;

    private final HashMap<String, Double[]> trainMap;

    private final HashMap<String, Double[]> quarterAirMap;

    // <msapmsa, [zoneId, dumMsa]>
    private final HashMap<Integer, Integer[]> zonIdMap;

    private final HashMap<Integer, Integer> idMsaMap;

    // <o/d, [emp, hh]>    
    private final HashMap<Integer, Double[]> msaEmpMap;

    private static final Logger sLog = LogManager.getLogger(Math.class);

    public static final int alt = 380;

    /**
     * Used to temporarily store the sum of exp of uDs to prevent duplicate
     * calculation.
     */
//    private LinkedHashMap<String, Double> UD_EXP_SUM_BUFFER;
//    private static final int UD_EXP_SUM_BUFFER_SIZE = 10000;
    /**
     * Used to store all pre-calculated logsum.
     * @see LogSum.java - the value of logsum can be calculated from
     *  - o [1-380]
     *  - d [1-380]
     *  - trip purpose [business, pb, pleasure]
     *  - quarter [-1, 1, 2, 3, 4]
     */
    private HashMap<String, Double> logsumCacheQ0;

    private HashMap<String, Double> logsumCacheQ1;

    private HashMap<String, Double> logsumCacheQ2;

    private HashMap<String, Double> logsumCacheQ3;

    private HashMap<String, Double> logsumCacheQ4;

    private static final Map<String, Double> destCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put(TripType.BUSINESS.name() + "-lgs", 0.9989822);
        aMap.put(TripType.BUSINESS.name() + "-dist", -0.0027291);
        aMap.put(TripType.BUSINESS.name() + "-sqDist", 0.0009215);
        aMap.put(TripType.BUSINESS.name() + "-trDist", -0.0000112);
        aMap.put(TripType.BUSINESS.name() + "-msa", -0.6954229);
        aMap.put(TripType.BUSINESS.name() + "-emp", 0.002333);
        aMap.put(TripType.BUSINESS.name() + "-hh", -0.0026529);
        aMap.put(TripType.BUSINESS.name() + "-lv", -2.178784);

        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-lgs", 0.6089201);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-dist", -0.0036128);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-sqDist", 0.0007052);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-msa", -0.9645431);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-emp", 0.0013741);
        aMap.put(TripType.PERSONAL_BUSINESS.name() + "-hh", -0.0011477);

        aMap.put(TripType.PLEASURE.name() + "-lgs", 0.4242957);
        aMap.put(TripType.PLEASURE.name() + "-dist", -0.0033921);
        aMap.put(TripType.PLEASURE.name() + "-sqDist", 0.0006737);
        aMap.put(TripType.PLEASURE.name() + "-msa", -1.255553);
        aMap.put(TripType.PLEASURE.name() + "-emp", 0.0016221);
        aMap.put(TripType.PLEASURE.name() + "-hh", -0.0016747);
        aMap.put(TripType.PLEASURE.name() + "-fl", 1.974002);
        aMap.put(TripType.PLEASURE.name() + "-lv", -1.821725);

        destCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> toyBusinessCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("Coef_Lgs", 0.05);
        aMap.put("Coef_Inc_1", 7.63e-06);
        aMap.put("Coef_Inc_2", 4.40e-07);
        aMap.put("Coef_Inc_3", 1.40e-06);
        aMap.put("Coef_emp_1", 0.4816896);
        aMap.put("Coef_emp_2", 0.1557063);
        aMap.put("Coef_emp_3", -0.0948035);
        aMap.put("Coef_hhchd_1", 0.4217099);
        aMap.put("Coef_hhchd_2", 0.3083643);
        aMap.put("Coef_hhchd_3", 0.0781339);
        aMap.put("Coef_Age_1", 0.0224959);
        aMap.put("Coef_Age_2", 0.0203469);
        aMap.put("Coef_Age_3", 0.0126859);
        aMap.put("Coef_School_1", 0.3201603);
        aMap.put("Coef_School_2", 0.0748973);
        aMap.put("Coef_School_3", 0.8341451);
        aMap.put("Asc_1", -1.469728);
        aMap.put("Asc_2", -0.7446439);
        aMap.put("Asc_3", -0.3704078);

        toyBusinessCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> toySimplePleasureCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("Coef_Inc_1", 7.96e-06);
        aMap.put("Coef_Inc_2", 3.41e-06);
        aMap.put("Coef_Inc_3", 2.54e-07);
        aMap.put("Coef_emp_1", 0.1535123);
        aMap.put("Coef_emp_2", 0.3906334);
        aMap.put("Coef_emp_3", 0.123274);
        aMap.put("Coef_unemp_1", -0.0554525);
        aMap.put("Coef_unemp_2", 0.333431);
        aMap.put("Coef_unemp_3", -0.1062914);
        aMap.put("Coef_sig_1", 0.1179802);
        aMap.put("Coef_sig_2", 0.3194204);
        aMap.put("Coef_sig_3", 0.157738);
        aMap.put("Coef_Age_1", 0.0093923);
        aMap.put("Coef_Age_2", 0.0091673);
        aMap.put("Coef_Age_3", 0.006373);
        aMap.put("Coef_hhochd_1", 0.3306901);
        aMap.put("Coef_hhochd_2", 0.5035874);
        aMap.put("Coef_hhochd_3", 0.4754719);
        aMap.put("Coef_hhchd_1", 0.5747832);
        aMap.put("Coef_hhchd_2", 0.926389);
        aMap.put("Coef_hhchd_3", 0.8699875);
        aMap.put("Asc_1", -1.228896);
        aMap.put("Asc_2", -1.285858);
        aMap.put("Asc_3", -0.5417412);


        toySimplePleasureCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> toyFullPleasureCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("Coef_lgs", 0.1);
        aMap.put("Coef_Inc_1", 8.98e-06);
        aMap.put("Coef_Inc_2", 4.40e-06);
        aMap.put("Coef_Inc_3", 1.69e-06);
        aMap.put("Coef_emp_1", 0.197132);
        aMap.put("Coef_emp_2", 0.0443198);
        aMap.put("Coef_emp_3", 0.2129413);
        aMap.put("Coef_School_1", 0.0153994);
        aMap.put("Coef_School_2", -0.3909301);
        aMap.put("Coef_School_3", 0.0499155);
        aMap.put("Coef_Age_1", 0.0104505);
        aMap.put("Coef_Age_2", 0.0108332);
        aMap.put("Coef_Age_3", 0.0077513);
        aMap.put("Coef_hhchd_1", 0.337076);
        aMap.put("Coef_hhchd_2", 0.5335793);
        aMap.put("Coef_hhchd_3", 0.5243304);
        aMap.put("Asc_1", -1.120773);
        aMap.put("Asc_2", -0.6538967);
        aMap.put("Asc_3", -0.4036146);

        toyFullPleasureCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> tdBusinessCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("p_MSA", -0.152);
        aMap.put("p_Cwc", 0.115);
        aMap.put("p_sf", -0.056);
        aMap.put("p_nfh", -0.158);
        aMap.put("p_size", 0.015);
        aMap.put("p_medinc", -0.015);
        aMap.put("p_higinc", -0.128);
        aMap.put("p_unemp", -0.147);
        aMap.put("p_student", -0.043);
        aMap.put("p_quart2", -0.246);
        aMap.put("p_quart3", -0.328);
        aMap.put("p_quart4", -0.140);
        aMap.put("p_age", 0.004);
        aMap.put("p_harf", -0.408);
        aMap.put("cons", -0.751);

        tdBusinessCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> tdPleasureCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("p_cwc", 0.054);
        aMap.put("p_sf", -0.088);
        aMap.put("p_nfh", -0.145);
        aMap.put("p_size", 0.011);
        aMap.put("p_medinc", -0.051);
        aMap.put("p_higinc", -0.219);
        aMap.put("p_unemp", -0.334);
        aMap.put("p_student", -0.278);
        aMap.put("p_age", -0.006);
        aMap.put("p_harf", -0.150);
        aMap.put("cons", -0.731);

        tdPleasureCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> tdPBCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("p_MSA", -0.081);
        aMap.put("p_Cwc", 0.153);
        aMap.put("p_sf", -0.199);
        aMap.put("p_nfh", 0.142);
        aMap.put("p_size", 0.020);
        aMap.put("p_medinc", -0.083);
        aMap.put("p_higinc", -0.298);
        aMap.put("p_unemp", -0.225);
        aMap.put("p_student", -0.753);
        aMap.put("p_quart2", -0.064);
        aMap.put("p_quart3", -0.130);
        aMap.put("p_quart4", -0.065);
        aMap.put("p_age", 0.004);
        aMap.put("p_harf", -0.572);
        aMap.put("cons", -0.517);

        tdPBCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> tpsBusinessCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("tp2_msa_b", -0.2132348);
        aMap.put("tp2_sf_b", -1.194334);
        aMap.put("tp2_cwc_b", -0.4745523);
        aMap.put("tp2_size_b", 0.0031476);
        aMap.put("tp2_linc_b", 0.6468207);
        aMap.put("tp2_minc_b", 0.3557382);
        aMap.put("tp2_age_b", 0.021452);
        aMap.put("tp2_femal_b", 0.9040727);
        aMap.put("tp2_cons_b", -1.253271);
        aMap.put("tp3_msa_b", -0.3436734);
        aMap.put("tp3_sf_b", -0.8973103);
        aMap.put("tp3_cwc_b", 0.0709871);
        aMap.put("tp3_size_b", -0.0597594);
        aMap.put("tp3_linc_b", 1.067521);
        aMap.put("tp3_minc_b", 0.6042227);
        aMap.put("tp3_age_b", 0.008456);
        aMap.put("tp3_femal_b", 0.9682016);
        aMap.put("tp3_cons_b", -2.087201);
        aMap.put("tp4_msa_b", -0.3218568);
        aMap.put("tp4_sf_b", -0.0830076);
        aMap.put("tp4_cwc_b", 0.022077);
        aMap.put("tp4_size_b", 0.3053654);
        aMap.put("tp4_linc_b", 1.11747);
        aMap.put("tp4_minc_b", 0.6137806);
        aMap.put("tp4_age_b", -0.0072482);
        aMap.put("tp4_femal_b", 1.048865);
        aMap.put("tp4_cons_b", -2.39346);

        tpsBusinessCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> tpsPleasureCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("tp2_sf_p", -2.519033);
        aMap.put("tp2_cwc_p", 0.1026147);
        aMap.put("tp2_size_p", 0.2545038);
        aMap.put("tp2_linc_p", 0.1770234);
        aMap.put("tp2_minc_p", 0.1897853);
        aMap.put("tp2_age_p", 0.022891);
        aMap.put("tp2_femal_p", 0.0219075);
        aMap.put("tp2_cons_p", 1.00429);
        aMap.put("tp3_sf_p", -1.731458);
        aMap.put("tp3_cwc_p", 1.613627);
        aMap.put("tp3_size_p", -0.1888752);
        aMap.put("tp3_linc_p", 0.2370588);
        aMap.put("tp3_minc_p", 0.2797856);
        aMap.put("tp3_age_p", 0.012797);
        aMap.put("tp3_femal_p", 0.0899659);
        aMap.put("tp3_cons_p", -0.1912607);
        aMap.put("tp4_sf_p", -0.9936722);
        aMap.put("tp4_cwc_p", 1.397388);
        aMap.put("tp4_size_p", 0.3770754);
        aMap.put("tp4_linc_p", 0.4807467);
        aMap.put("tp4_minc_p", 0.4158904);
        aMap.put("tp4_age_p", 0.0156395);
        aMap.put("tp4_femal_p", 0.1027345);
        aMap.put("tp4_cons_p", -1.631986);

        tpsPleasureCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> tpsPBCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("tp2_msa_pb", -0.0591476);
        aMap.put("tp2_sf_pb", -2.150063);
        aMap.put("tp2_cwc_pb", -0.0114238);
        aMap.put("tp2_size_pb", -0.1766115);
        aMap.put("tp2_linc_pb", 0.338039);
        aMap.put("tp2_minc_pb", 0.2527234);
        aMap.put("tp2_age_pb", 0.021437);
        aMap.put("tp2_femal_pb", 0.2995287);
        aMap.put("tp2_cons_pb", 0.3434581);
        aMap.put("tp3_msa_pb", 0.0472045);
        aMap.put("tp3_sf_pb", -1.372581);
        aMap.put("tp3_cwc_pb", 0.5580346);
        aMap.put("tp3_size_pb", -0.0115575);
        aMap.put("tp3_linc_pb", 0.6703549);
        aMap.put("tp3_minc_pb", 0.3156061);
        aMap.put("tp3_age_pb", 0.0113649);
        aMap.put("tp3_femal_pb", 0.4295393);
        aMap.put("tp3_cons_pb", -0.9980764);
        aMap.put("tp4_msa_pb", -0.1821842);
        aMap.put("tp4_sf_pb", -0.8483076);
        aMap.put("tp4_cwc_pb", 0.8420289);
        aMap.put("tp4_size_pb", 0.2700369);
        aMap.put("tp4_linc_pb", 0.7382278);
        aMap.put("tp4_minc_pb", 0.4141297);
        aMap.put("tp4_age_pb", 0.0084153);
        aMap.put("tp4_femal_pb", 0.45848);
        aMap.put("tp4_cons_pb", -1.606166);

        tpsPBCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> mcCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();

        aMap.put("Coef_" + TripType.BUSINESS.name() + "_cost1", -0.0325);
        aMap.put("Coef_" + TripType.BUSINESS.name() + "_cost2", -0.00934);
        aMap.put("Coef_" + TripType.BUSINESS.name() + "_cost3", -0.00662);
        aMap.put("Coef_" + TripType.BUSINESS.name() + "_cost4", -0.00370);
        aMap.put("Coef_" + TripType.BUSINESS.name() + "_cost5", -0.00278);
        aMap.put("Coef_" + TripType.BUSINESS.name() + "_Time", -0.0356);
        aMap.put("ASC_" + TripType.BUSINESS.name() + "_Air", -0.440);
        aMap.put("ASC_" + TripType.BUSINESS.name() + "_Train", -2.93);

        aMap.put("Coef_" + TripType.PLEASURE.name() + "_cost1", -0.00947);
        aMap.put("Coef_" + TripType.PLEASURE.name() + "_cost2", -0.00434);
        aMap.put("Coef_" + TripType.PLEASURE.name() + "_cost3", -0.000900);
        aMap.put("Coef_" + TripType.PLEASURE.name() + "_cost4", -0.000335);
        aMap.put("Coef_" + TripType.PLEASURE.name() + "_Time", -0.0590);
        aMap.put("ASC_" + TripType.PLEASURE.name() + "_Air", -2.95);
        aMap.put("ASC_" + TripType.PLEASURE.name() + "_Train", -3.56);

        aMap.put("Coef_" + TripType.PERSONAL_BUSINESS + "_cost1", -0.0127);
        aMap.put("Coef_" + TripType.PERSONAL_BUSINESS + "_cost2", -0.00570);
        aMap.put("Coef_" + TripType.PERSONAL_BUSINESS + "_cost3", -0.00396);
        aMap.put("Coef_" + TripType.PERSONAL_BUSINESS + "_cost4", -0.00276);
        aMap.put("Coef_" + TripType.PERSONAL_BUSINESS + "_cost5", -0.00108);
        aMap.put("Coef_" + TripType.PERSONAL_BUSINESS + "_Time", -0.0328);
        aMap.put("ASC_" + TripType.PERSONAL_BUSINESS + "_Air", -1.49);
        aMap.put("ASC_" + TripType.PERSONAL_BUSINESS + "_Train", -3.75);

        mcCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> stopFreqCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();
        aMap.put("pi_dist1", 0.0015247);
        aMap.put("pi_dura1", 0.023012);
        aMap.put("pi_party1", 0.0603076);
        aMap.put("pi_car1", 1.516187);
        aMap.put("pi_busi1", 0.3918097);
        aMap.put("pi_plea1", 0.9623766);
        aMap.put("pi_quart2_1", -0.597106);
        aMap.put("pi_quart3_1", -0.1543705);
        aMap.put("pi_quart4_1", -0.2364871);
        aMap.put("consi1", -9.568104);
        aMap.put("pi_dist2", 0.0004792);
        aMap.put("pi_dura2", 0.0161129);
        aMap.put("pi_party2", -0.0064394);
        aMap.put("pi_car2", 2.208714);
        aMap.put("pi_busi2", 0.1327181);
        aMap.put("pi_plea2", 0.1237595);
        aMap.put("pi_quart2_2", 0.0828193);
        aMap.put("pi_quart3_2", 0.0831211);
        aMap.put("pi_quart4_2", -0.4385773);
        aMap.put("consi2", -6.267259);
        aMap.put("pi_dist3", 0.000203);
        aMap.put("pi_dura3", 0.0144204);
        aMap.put("pi_party3", -0.0404676);
        aMap.put("pi_car3", -3.05455);
        aMap.put("pi_busi3", 0.415034);
        aMap.put("pi_plea3", 0.190623);
        aMap.put("pi_quart2_3", 1.396496);
        aMap.put("pi_quart3_3", 1.251489);
        aMap.put("pi_quart4_3", 1.555322);
        aMap.put("consi3", -3.374192);
        aMap.put("pi_dist4", 0.0007112);
        aMap.put("pi_dura4", 0.0295693);
        aMap.put("pi_party4", -0.0134412);
        aMap.put("pi_car4", -1.319811);
        aMap.put("pi_busi4", 0.5741574);
        aMap.put("pi_plea4", 0.4442947);
        aMap.put("pi_quart2_4", 0.4472538);
        aMap.put("pi_quart3_4", 0.4163974);
        aMap.put("pi_quart4_4", 0.4659111);
        aMap.put("consi4", -6.182341);
        aMap.put("po_dist1", 0.0012815);
        aMap.put("po_dura1", 0.0238628);
        aMap.put("po_party1", -0.006822);
        aMap.put("po_car1", 0.9088002);
        aMap.put("po_busi1", -0.4902532);
        aMap.put("po_plea1", 0.7258997);
        aMap.put("po_quart2_1", -0.4103674);
        aMap.put("po_quart3_1", -0.0937313);
        aMap.put("po_quart4_1", -0.9647268);
        aMap.put("conso1", -8.382998);
        aMap.put("po_dist2", 0.0004824);
        aMap.put("po_dura2", 0.0182069);
        aMap.put("po_party2", -0.0095017);
        aMap.put("po_car2", 3.423379);
        aMap.put("po_busi2", 0.2188791);
        aMap.put("po_plea2", 0.4077914);
        aMap.put("po_quart2_2", 0.2187904);
        aMap.put("po_quart3_2", 0.0145411);
        aMap.put("po_quart4_2", -0.0265966);
        aMap.put("conso2", -7.644667);
        aMap.put("po_dist3", 0.0007946);
        aMap.put("po_dura3", 0.0205631);
        aMap.put("po_party3", 0.0162793);
        aMap.put("po_car3", 0.2476782);
        aMap.put("po_busi3", 0.660808);
        aMap.put("po_plea3", 0.6134739);
        aMap.put("po_quart2_3", 0.0553138);
        aMap.put("po_quart3_3", 0.1239214);
        aMap.put("po_quart4_3", -0.45224);
        aMap.put("conso3", -6.40614);
        aMap.put("po_dist4", 0.0011343);
        aMap.put("po_dura4", 0.02196);
        aMap.put("po_party4", 0.0141095);
        aMap.put("po_car4", 1.137262);
        aMap.put("po_busi4", 0.2281426);
        aMap.put("po_plea4", 0.4712899);
        aMap.put("po_quart2_4", 0.1696806);
        aMap.put("po_quart3_4", 0.6319622);
        aMap.put("po_quart4_4", -0.4364023);
        aMap.put("conso4", -8.120939);
        stopFreqCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> stopTypeCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();
        aMap.put("pi_sec_b", -0.0835463);
        aMap.put("pi_thir_b", -0.0835076);
        aMap.put("pi_fou_b", 0.6456524);
        aMap.put("pi_pt_b", -1.68357);
        aMap.put("pi_pbt_b", -1.262809);
        aMap.put("pi_party_b", 0.0);
        aMap.put("pi_car_b", 0.7504028);
        aMap.put("pi_air_b", 0.7986318);
        aMap.put("conis_b", -1.883052);
        aMap.put("pi_sec_pb", -0.0177027);
        aMap.put("pi_thir_pb", -0.0045039);
        aMap.put("pi_fou_pb", 1.686615);
        aMap.put("pi_pt_pb", 1.295783);
        aMap.put("pi_pbt_pb", 2.978684);
        aMap.put("pi_party_pb", 0.0);
        aMap.put("pi_car_pb", 0.6189764);
        aMap.put("pi_air_pb", -0.3557332);
        aMap.put("conis_pb", -5.816549);
        aMap.put("po_sec_b", -0.4766282);
        aMap.put("po_thir_b", -0.4796777);
        aMap.put("po_fou_b", -0.4246014);
        aMap.put("po_pt_b", -3.827447);
        aMap.put("po_pbt_b", -2.646279);
        aMap.put("po_party_b", 0.0);
        aMap.put("po_car_b", -0.2727584);
        aMap.put("po_air_b", 1.052367);
        aMap.put("conos_b", 0.8188961);
        aMap.put("po_sec_pb", -0.0699327);
        aMap.put("po_thir_pb", -0.1648464);
        aMap.put("po_fou_pb", 0.0024075);
        aMap.put("po_pt_pb", 0.5222961);
        aMap.put("po_pbt_pb", 3.078477);
        aMap.put("po_party_pb", 0.0);
        aMap.put("po_car_pb", -0.1631058);
        aMap.put("po_air_pb", 0.5699912);
        aMap.put("conos_pb", -4.019405);
        stopTypeCoefs = Collections.unmodifiableMap(aMap);
    }

    private static final Map<String, Double> stopLocCoefs;

    static {
        Map<String, Double> aMap = new HashMap<>();
        aMap.put("po_det_dist", -0.0081102);
        aMap.put("po_gtc_s", -0.1029191);
        aMap.put("po_gtc_m", -0.0001649);
        aMap.put("po_gtc_l", 0.0015895);
        aMap.put("po_gtc_lc", -0.0015207);
        aMap.put("po_emp", 0.0017037);
        aMap.put("po_hh", -0.0015843);
        aMap.put("po_nmsa", 1.82495);
        aMap.put("pi_det_dist", -0.0080677);
        aMap.put("pi_gtc_s", -0.0360756);
        aMap.put("pi_gtc_m", -0.0007487);
        aMap.put("pi_gtc_mc", 0.0010112);
        aMap.put("pi_gtc_l", -0.0000916);
        aMap.put("pi_emp", 0.000631);
        aMap.put("pi_hh", -0.0002232);
        aMap.put("pi_nmsa", 1.780490);
        stopLocCoefs = Collections.unmodifiableMap(aMap);
    }

    private LogSum logsum;

    public Math() {
        idMsaMap = new HashMap<>();
        zonIdMap = initZoneIdMapAndIdMsaMap();
        otherCarMap = initOtherCarMap();
        businessCarMap = initBusinessCarMap();
        airMap = initAirMap();
        trainMap = initTrainMap();
        quarterAirMap = initQuarterAirMap();
        msaEmpMap = initMsaEmpMap();

        logsum = new LogSum(trainMap, airMap, quarterAirMap, businessCarMap, otherCarMap);
//        UD_EXP_SUM_BUFFER = new LinkedHashMap<>();

        logsumCacheQ0 = new HashMap<>();
        logsumCacheQ1 = new HashMap<>();
        logsumCacheQ2 = new HashMap<>();
        logsumCacheQ3 = new HashMap<>();
        logsumCacheQ4 = new HashMap<>();
    }

    public double destUD(Person2010 p, int o, int d, TripType type, int quarter) throws InvalidValueException {
        double dist = o == d ? Double.NEGATIVE_INFINITY : businessCarMap.get(getKey(o, d))[3];
        if(dist >= 50) {
            if(type == TripType.BUSINESS) {
                return destCoefs.get(type.name() + "-lgs") * getLogsum(p, o, d, type, quarter)
                       + destCoefs.get(type.name() + "-dist") * dist
                       + destCoefs.get(type.name() + "-sqDist") * dist * dist * 0.001
                       + destCoefs.get(type.name() + "-trDist") * dist * dist * dist * 0.00001
                       + destCoefs.get(type.name() + "-msa") * zonIdMap.get(idMsaMap.get(d))[1]
                       + destCoefs.get(type.name() + "-emp") * msaEmpMap.get(d)[0]
                       + destCoefs.get(type.name() + "-hh") * msaEmpMap.get(d)[1]
                       + destCoefs.get(type.name() + "-lv") * (d == 201 ? 1 : 0);
            }
            else if(type == TripType.PLEASURE) {
                return destCoefs.get(type.name() + "-lgs") * getLogsum(p, o, d, type, quarter)
                       + destCoefs.get(type.name() + "-dist") * dist
                       + destCoefs.get(type.name() + "-sqDist") * dist * dist * 0.001
                       + destCoefs.get(type.name() + "-msa") * zonIdMap.get(idMsaMap.get(d))[1]
                       + destCoefs.get(type.name() + "-emp") * msaEmpMap.get(d)[0]
                       + destCoefs.get(type.name() + "-hh") * msaEmpMap.get(d)[1]
                       + destCoefs.get(type.name() + "-fl")
                         * (d == 55 || d == 90 || d == 124 || d == 126 || d == 171
                            || d == 194 || d == 229 || d == 270 || d == 274
                            || d == 321 || d == 343 || d == 344 || d == 365 ? 1 : 0)
                       + destCoefs.get(type.name() + "-lv") * (d == 201 ? 1 : 0);
            }
            else if(type == TripType.PERSONAL_BUSINESS) {
                return destCoefs.get(type.name() + "-lgs") * getLogsum(p, o, d, type, quarter)
                       + destCoefs.get(type.name() + "-dist") * dist
                       + destCoefs.get(type.name() + "-sqDist") * dist * dist * 0.001
                       + destCoefs.get(type.name() + "-msa") * zonIdMap.get(idMsaMap.get(d))[1]
                       + destCoefs.get(type.name() + "-emp") * msaEmpMap.get(d)[0]
                       + destCoefs.get(type.name() + "-hh") * msaEmpMap.get(d)[1];
            }
        }
        else {
            return Double.NEGATIVE_INFINITY;
        }
        throw new InvalidValueException("Invalid tripPurpose: " + type.name()
                                        + ". (Args: tripPurpose: " + type.name() + ", o: " + o + ", d: " + d + ", person.ID: " + p.getPid() + ")");
    }

    // sum[(e^(u_d1 ) +e^(u_d2 )+....+ e^(u_dn )]
    public double destUDExpSum(Person2010 p, int o, TripType type, int quarter) {
        double sum = 0.0;
        try {
            for (int d = 1; d <= Math.alt; d++) {
                sum += exp(destUD(p, o, d, type, quarter));
            }
        }
        catch (InvalidValueException e) {
            sLog.error(e.getLocalizedMessage(), e);
            System.exit(1);
        }
        return sum;
    }

    public double toyUDExpSum(Person2010 p, int o, int d, TripType type) {
        return toyUDExp(p, o, d, type, 1) + toyUDExp(p, o, d, type, 2) + toyUDExp(p, o, d, type, 3) + toyUDExp(p, o, d, type, 4);
    }

    public double destUDExp(Person2010 p, int o, int d, TripType type, int quarter) {
        try {
            return exp(destUD(p, o, d, type, quarter));
        }
        catch (InvalidValueException e) {
            sLog.error(e.getLocalizedMessage(), e);
            System.exit(1);
        }
        return 0.0;
    }

    public double toyUDExp(Person2010 p, int o, int d, TripType type, int quarter) {
        try {
            return exp(toyUD(p, o, d, type, quarter));
        }
        catch (InvalidValueException e) {
            sLog.error(e.getLocalizedMessage(), e);
            System.exit(1);
        }
        return 0.0;
    }

    public double toyUD(Person2010 p, int o, int d, TripType type, int quarter) throws InvalidValueException {
        if(type == TripType.BUSINESS) {
            if(quarter == 1) {
                return toyBusinessCoefs.get("Asc_1")
                       + toyBusinessCoefs.get("Coef_Lgs") * getLogsum(p, o, d, type, 1)
                       + toyBusinessCoefs.get("Coef_Inc_1") * p.getHtinc()
                       + toyBusinessCoefs.get("Coef_emp_1") * (p.getEmpStatus() == 1 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_hhchd_1") * (p.getHhType() == 2 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_School_1") * (p.getEmpStatus() == 3 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_Age_1") * p.getAge();

            }
            else if(quarter == 2) {
                return toyBusinessCoefs.get("Asc_2")
                       + toyBusinessCoefs.get("Coef_Lgs") * getLogsum(p, o, d, type, 2)
                       + toyBusinessCoefs.get("Coef_Inc_2") * p.getHtinc()
                       + toyBusinessCoefs.get("Coef_emp_2") * (p.getEmpStatus() == 1 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_hhchd_2") * (p.getHhType() == 2 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_School_2") * (p.getEmpStatus() == 3 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_Age_2") * p.getAge();
            }
            else if(quarter == 3) {
                return toyBusinessCoefs.get("Asc_3")
                       + toyBusinessCoefs.get("Coef_Lgs") * getLogsum(p, o, d, type, 3)
                       + toyBusinessCoefs.get("Coef_Inc_3") * p.getHtinc()
                       + toyBusinessCoefs.get("Coef_emp_3") * (p.getEmpStatus() == 1 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_hhchd_3") * (p.getHhType() == 2 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_School_3") * (p.getEmpStatus() == 3 ? 1 : 0)
                       + toyBusinessCoefs.get("Coef_Age_3") * p.getAge();
            }
            else {
                // quarter == 4
                return toyBusinessCoefs.get("Coef_Lgs") * getLogsum(p, o, d, type, 4);
            }
        }
        else if(type == TripType.PLEASURE) {
            if(d == Integer.MIN_VALUE) {
                if(quarter == 1) {
                    return toySimplePleasureCoefs.get("Asc_1")
                           + toySimplePleasureCoefs.get("Coef_Inc_1") * p.getHtinc()
                           + toySimplePleasureCoefs.get("Coef_emp_1") * (p.getEmpStatus() == 1 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_unemp_1") * (p.getEmpStatus() == 2 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_sig_1") * (p.getHhType() == 3 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_Age_1") * p.getAge()
                           + toySimplePleasureCoefs.get("Coef_hhchd_1") * (p.getHhType() == 2 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_hhochd_1") * (p.getHhType() == 1 ? 1 : 0);
                }
                else if(quarter == 2) {
                    return toySimplePleasureCoefs.get("Asc_2")
                           + toySimplePleasureCoefs.get("Coef_Inc_2") * p.getHtinc()
                           + toySimplePleasureCoefs.get("Coef_emp_2") * (p.getEmpStatus() == 1 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_unemp_2") * (p.getEmpStatus() == 2 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_sig_2") * (p.getHhType() == 3 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_Age_2") * p.getAge()
                           + toySimplePleasureCoefs.get("Coef_hhchd_2") * (p.getHhType() == 2 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_hhochd_2") * (p.getHhType() == 1 ? 1 : 0);
                }
                else if(quarter == 3) {
                    return toySimplePleasureCoefs.get("Asc_3")
                           + toySimplePleasureCoefs.get("Coef_Inc_3") * p.getHtinc()
                           + toySimplePleasureCoefs.get("Coef_emp_3") * (p.getEmpStatus() == 1 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_unemp_3") * (p.getEmpStatus() == 2 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_sig_3") * (p.getHhType() == 3 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_Age_3") * p.getAge()
                           + toySimplePleasureCoefs.get("Coef_hhchd_3") * (p.getHhType() == 2 ? 1 : 0)
                           + toySimplePleasureCoefs.get("Coef_hhochd_3") * (p.getHhType() == 1 ? 1 : 0);
                }
                else {
                    // quarter == 4
                    return 0.0;
                }
            }
            else {
                if(quarter == 1) {
                    return toyFullPleasureCoefs.get("Asc_1")
                           + toyFullPleasureCoefs.get("Coef_lgs") * logsum.calculateLogsum(p, o, d, type, quarter)
                           + toyFullPleasureCoefs.get("Coef_Inc_1") * p.getHtinc()
                           + toyFullPleasureCoefs.get("Coef_emp_1") * (p.getEmpStatus() == 1 ? 1 : 0)
                           + toyFullPleasureCoefs.get("Coef_School_1") * (p.getEmpStatus() == 3 ? 1 : 0)
                           + toyFullPleasureCoefs.get("Coef_Age_1") * p.getAge()
                           + toyFullPleasureCoefs.get("Coef_hhchd_1") * (p.getHhType() == 2 ? 1 : 0);
                }
                else if(quarter == 2) {
                    return toyFullPleasureCoefs.get("Asc_2")
                           + toyFullPleasureCoefs.get("Coef_lgs") * logsum.calculateLogsum(p, o, d, type, quarter)
                           + toyFullPleasureCoefs.get("Coef_Inc_2") * p.getHtinc()
                           + toyFullPleasureCoefs.get("Coef_emp_2") * (p.getEmpStatus() == 1 ? 1 : 0)
                           + toyFullPleasureCoefs.get("Coef_School_2") * (p.getEmpStatus() == 3 ? 1 : 0)
                           + toyFullPleasureCoefs.get("Coef_Age_2") * p.getAge()
                           + toyFullPleasureCoefs.get("Coef_hhchd_2") * (p.getHhType() == 2 ? 1 : 0);
                }
                else if(quarter == 3) {
                    return toyFullPleasureCoefs.get("Asc_3")
                           + toyFullPleasureCoefs.get("Coef_lgs") * logsum.calculateLogsum(p, o, d, type, quarter)
                           + toyFullPleasureCoefs.get("Coef_Inc_3") * p.getHtinc()
                           + toyFullPleasureCoefs.get("Coef_emp_3") * (p.getEmpStatus() == 1 ? 1 : 0)
                           + toyFullPleasureCoefs.get("Coef_School_3") * (p.getEmpStatus() == 3 ? 1 : 0)
                           + toyFullPleasureCoefs.get("Coef_Age_3") * p.getAge()
                           + toyFullPleasureCoefs.get("Coef_hhchd_3") * (p.getHhType() == 2 ? 1 : 0);
                }
                else {
                    // quarter == 4
                    return toyFullPleasureCoefs.get("Coef_lgs") * logsum.calculateLogsum(p, o, d, type, quarter);
                }
            }
        }
        throw new InvalidValueException("Invalid TripType found. Only [BUSINESS, PLEASURR] is accepted here. [PB] doesn't need to calculate uD.");
    }

    private double tdHT(Person2010 p, int d, int toy, int t, TripType type) {
        if(type == TripType.BUSINESS) {//???
            return tdBusinessCoefs.get("p_MSA") * zonIdMap.get(idMsaMap.get(d))[1]
                   + tdBusinessCoefs.get("p_Cwc") * (p.getHhType() == 2 ? 1 : 0)
                   + tdBusinessCoefs.get("p_sf") * (p.getHhType() == 3 ? 1 : 0)
                   + tdBusinessCoefs.get("p_nfh") * (p.getHhType() == 4 ? 1 : 0)
                   + tdBusinessCoefs.get("p_size") * p.getNp()
                   + tdBusinessCoefs.get("p_medinc") * (p.getIncLevel() == 2 ? 1 : 0)
                   + tdBusinessCoefs.get("p_higinc") * (p.getIncLevel() == 3 ? 1 : 0)
                   + tdBusinessCoefs.get("p_unemp") * (p.getEmpStatus() == 2 ? 1 : 0)
                   + tdBusinessCoefs.get("p_student") * (p.getEmpStatus() == 3 ? 1 : 0)
                   + tdBusinessCoefs.get("p_quart2") * (toy == 2 ? 1 : 0)
                   + tdBusinessCoefs.get("p_quart3") * (toy == 3 ? 1 : 0)
                   + tdBusinessCoefs.get("p_quart4") * (toy == 4 ? 1 : 0)
                   + tdBusinessCoefs.get("p_age") * (p.getAge())
                   + tdBusinessCoefs.get("p_harf") * log(t)
                   + tdBusinessCoefs.get("cons");
        }
        else if(type == TripType.PLEASURE) {
            return tdPleasureCoefs.get("p_cwc") * (p.getHhType() == 2 ? 1 : 0)
                   + tdPleasureCoefs.get("p_sf") * (p.getHhType() == 3 ? 1 : 0)
                   + tdPleasureCoefs.get("p_nfh") * (p.getHhType() == 4 ? 1 : 0)
                   + tdPleasureCoefs.get("p_size") * p.getNp()
                   + tdPleasureCoefs.get("p_medinc") * (p.getIncLevel() == 2 ? 1 : 0)
                   + tdPleasureCoefs.get("p_higinc") * (p.getIncLevel() == 3 ? 1 : 0)
                   + tdPleasureCoefs.get("p_unemp") * (p.getEmpStatus() == 2 ? 1 : 0)
                   + tdPleasureCoefs.get("p_student") * (p.getEmpStatus() == 3 ? 1 : 0)
                   + tdPleasureCoefs.get("p_age") * (p.getAge())
                   + tdPleasureCoefs.get("p_harf") * log(t)
                   + tdPleasureCoefs.get("cons");

        }
        else {
            // TripType.PERSONAL_BUSINESS
            return tdPBCoefs.get("p_MSA") * zonIdMap.get(idMsaMap.get(d))[1]
                   + tdPBCoefs.get("p_Cwc") * (p.getHhType() == 2 ? 1 : 0)
                   + tdPBCoefs.get("p_sf") * (p.getHhType() == 3 ? 1 : 0)
                   + tdPBCoefs.get("p_nfh") * (p.getHhType() == 4 ? 1 : 0)
                   + tdPBCoefs.get("p_size") * p.getNp()
                   + tdPBCoefs.get("p_medinc") * (p.getIncLevel() == 2 ? 1 : 0)
                   + tdPBCoefs.get("p_higinc") * (p.getIncLevel() == 3 ? 1 : 0)
                   + tdPBCoefs.get("p_unemp") * (p.getEmpStatus() == 2 ? 1 : 0)
                   + tdPBCoefs.get("p_student") * (p.getEmpStatus() == 3 ? 1 : 0)
                   + tdPBCoefs.get("p_quart2") * (toy == 2 ? 1 : 0)
                   + tdPBCoefs.get("p_quart3") * (toy == 3 ? 1 : 0)
                   + tdPBCoefs.get("p_quart4") * (toy == 4 ? 1 : 0)
                   + tdPBCoefs.get("p_age") * (p.getAge())
                   + tdPBCoefs.get("p_harf") * log(t)
                   + tdPBCoefs.get("cons");
        }
    }

    private double tdPT(Person2010 p, int d, int toy, int t, TripType type) {
        return 1 / (1 + exp(0 - tdHT(p, d, toy, t, type)));
    }

    public double tdST(Person2010 p, int d, int toy, int t, TripType type) {
        double sT = 1.0;
        for (int i = 1; i <= t; i++) {
            sT *= (1 - tdPT(p, d, toy, i, type));
        }
        return sT;
    }

    private double tpsBusinessUTP(Person2010 p, int d, int tps) {
        if(tps == 1) {
            return 0.0;
        }
        else {
            return tpsBusinessCoefs.get("tp" + tps + "_msa_b") * zonIdMap.get(idMsaMap.get(d))[1]
                   + tpsBusinessCoefs.get("tp" + tps + "_sf_b") * (p.getHhType() == 3 ? 1 : 0)
                   + tpsBusinessCoefs.get("tp" + tps + "_cwc_b") * (p.getHhType() == 2 ? 1 : 0)
                   + tpsBusinessCoefs.get("tp" + tps + "_size_b") * p.getNp()
                   + tpsBusinessCoefs.get("tp" + tps + "_linc_b") * (p.getIncLevel() == 1 ? 1 : 0)
                   + tpsBusinessCoefs.get("tp" + tps + "_minc_b") * (p.getIncLevel() == 2 ? 1 : 0)
                   + tpsBusinessCoefs.get("tp" + tps + "_age_b") * p.getAge()
                   + tpsBusinessCoefs.get("tp" + tps + "_femal_b") * (1 - (p.getSex() == 1 ? 1 : 0))
                   + tpsBusinessCoefs.get("tp" + tps + "_cons_b");
        }
    }

    private double tpsPleasureUTP(Person2010 p, int d, int tps) {
        if(tps == 1) {
            return 0.0;
        }
        else {
            return tpsPleasureCoefs.get("tp" + tps + "_sf_p") * (p.getHhType() == 3 ? 1 : 0)
                   + tpsPleasureCoefs.get("tp" + tps + "_cwc_p") * (p.getHhType() == 2 ? 1 : 0)
                   + tpsPleasureCoefs.get("tp" + tps + "_size_p") * p.getNp()
                   + tpsPleasureCoefs.get("tp" + tps + "_linc_p") * (p.getIncLevel() == 1 ? 1 : 0)
                   + tpsPleasureCoefs.get("tp" + tps + "_minc_p") * (p.getIncLevel() == 2 ? 1 : 0)
                   + tpsPleasureCoefs.get("tp" + tps + "_age_p") * p.getAge()
                   + tpsPleasureCoefs.get("tp" + tps + "_femal_p") * (1 - (p.getSex() == 1 ? 1 : 0))
                   + tpsPleasureCoefs.get("tp" + tps + "_cons_p");
        }
    }

    private double tpsPBUTP(Person2010 p, int d, int tps) {
        if(tps == 1) {
            return 0.0;
        }
        else {
            return tpsPBCoefs.get("tp" + tps + "_msa_pb") * zonIdMap.get(idMsaMap.get(d))[1]
                   + tpsPBCoefs.get("tp" + tps + "_sf_pb") * (p.getHhType() == 3 ? 1 : 0)
                   + tpsPBCoefs.get("tp" + tps + "_cwc_pb") * (p.getHhType() == 2 ? 1 : 0)
                   + tpsPBCoefs.get("tp" + tps + "_size_pb") * p.getNp()
                   + tpsPBCoefs.get("tp" + tps + "_linc_pb") * (p.getIncLevel() == 1 ? 1 : 0)
                   + tpsPBCoefs.get("tp" + tps + "_minc_pb") * (p.getIncLevel() == 2 ? 1 : 0)
                   + tpsPBCoefs.get("tp" + tps + "_age_pb") * p.getAge()
                   + tpsPBCoefs.get("tp" + tps + "_femal_pb") * (1 - (p.getSex() == 1 ? 1 : 0))
                   + tpsPBCoefs.get("tp" + tps + "_cons_pb");
        }
    }

    public double tpsUtpExp(Person2010 p, int d, int tps, TripType type) {
        if(type == TripType.BUSINESS) {
            return exp(tpsBusinessUTP(p, d, tps));
        }
        else if(type == TripType.PLEASURE) {
            return exp(tpsPleasureUTP(p, d, tps));
        }
        else {
            return exp(tpsPBUTP(p, d, tps));
        }
    }

    public double mcUcarExp(Person2010 p, TripType type, int d, int o, int days, boolean retry) {
        if(!retry && logsum.tourCarTime(o, d, type) > days * 24 / 2) {
            return 0.0;
        }
        double tourCarCost = logsum.tourCarCost(p.getIncLevel(), o, d, type);
        if(tourCarCost == Double.POSITIVE_INFINITY) {
            return 0.0;
        }
        if(type == TripType.BUSINESS) {
            return exp(mcCoefs.get("Coef_" + type.name() + "_cost1") * tourCarCost * (tourCarCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourCarCost * (tourCarCost > 188 && tourCarCost <= 332 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourCarCost * (tourCarCost > 332 && tourCarCost <= 476 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourCarCost * (tourCarCost > 476 && tourCarCost <= 620 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost5") * tourCarCost * (tourCarCost > 620 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourCarTime(o, d, type));
        }
        else if(type == TripType.PLEASURE) {
            return exp(mcCoefs.get("Coef_" + type.name() + "_cost1") * tourCarCost * (tourCarCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourCarCost * (tourCarCost > 188 && tourCarCost <= 312 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourCarCost * (tourCarCost > 312 && tourCarCost <= 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourCarCost * (tourCarCost > 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourCarTime(o, d, type));
        }
        else {
            // type == TripType.PERSONAL_BUSINESS
            return exp(mcCoefs.get("Coef_" + type.name() + "_cost1") * tourCarCost * (tourCarCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourCarCost * (tourCarCost > 188 && tourCarCost <= 312 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourCarCost * (tourCarCost > 312 && tourCarCost <= 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourCarCost * (tourCarCost > 436 && tourCarCost <= 560 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost5") * tourCarCost * (tourCarCost > 560 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourCarTime(o, d, type));
        }
    }

    public double mcUairExp(Person2010 p, TripType type, int d, int o, int toy) {
        double tourAirCost = logsum.tourAirCost(o, d, toy);
        if(tourAirCost == Double.POSITIVE_INFINITY) {
            return 0.0;
        }
        if(type == TripType.BUSINESS) {
            return exp(mcCoefs.get("ASC_" + type.name() + "_Air")
                       + mcCoefs.get("Coef_" + type.name() + "_cost1") * tourAirCost * (tourAirCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourAirCost * (tourAirCost > 188 && tourAirCost <= 332 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourAirCost * (tourAirCost > 332 && tourAirCost <= 476 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourAirCost * (tourAirCost > 476 && tourAirCost <= 620 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost5") * tourAirCost * (tourAirCost > 620 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourAirTime(o, d));
        }
        else if(type == TripType.PLEASURE) {
            return exp(mcCoefs.get("ASC_" + type.name() + "_Air")
                       + mcCoefs.get("Coef_" + type.name() + "_cost1") * tourAirCost * (tourAirCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourAirCost * (tourAirCost > 188 && tourAirCost <= 312 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourAirCost * (tourAirCost > 312 && tourAirCost <= 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourAirCost * (tourAirCost > 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourAirTime(o, d));
        }
        else {
            // type == TripType.PERSONAL_BUSINESS
            return exp(mcCoefs.get("ASC_" + type.name() + "_Air")
                       + mcCoefs.get("Coef_" + type.name() + "_cost1") * tourAirCost * (tourAirCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourAirCost * (tourAirCost > 188 && tourAirCost <= 312 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourAirCost * (tourAirCost > 312 && tourAirCost <= 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourAirCost * (tourAirCost > 436 && tourAirCost <= 560 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost5") * tourAirCost * (tourAirCost > 560 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourAirTime(o, d));
        }
    }

    public double mcUtrainExp(Person2010 p, TripType type, int d, int o, int days, boolean retry) {
        if(!retry && logsum.tourTrainTime(o, d) > days * 24 / 2) {
            return 0.0;
        }
        double tourTrainCost = logsum.tourTrainCost(o, d);
        if(tourTrainCost == Double.POSITIVE_INFINITY) {
            return 0.0;
        }
        if(type == TripType.BUSINESS) {
            return exp(mcCoefs.get("ASC_" + type.name() + "_Train")
                       + mcCoefs.get("Coef_" + type.name() + "_cost1") * tourTrainCost * (tourTrainCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourTrainCost * (tourTrainCost > 188 && tourTrainCost <= 332 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourTrainCost * (tourTrainCost > 332 && tourTrainCost <= 476 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourTrainCost * (tourTrainCost > 476 && tourTrainCost <= 620 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost5") * tourTrainCost * (tourTrainCost > 620 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourTrainTime(o, d));
        }
        else if(type == TripType.PLEASURE) {
            return exp(mcCoefs.get("ASC_" + type.name() + "_Train")
                       + mcCoefs.get("Coef_" + type.name() + "_cost1") * tourTrainCost * (tourTrainCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourTrainCost * (tourTrainCost > 188 && tourTrainCost <= 312 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourTrainCost * (tourTrainCost > 312 && tourTrainCost <= 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourTrainCost * (tourTrainCost > 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourTrainTime(o, d));
        }
        else {
            // type == TripType.PERSONAL_BUSINESS
            return exp(mcCoefs.get("ASC_" + type.name() + "_Train")
                       + mcCoefs.get("Coef_" + type.name() + "_cost1") * tourTrainCost * (tourTrainCost <= 188 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost2") * tourTrainCost * (tourTrainCost > 188 && tourTrainCost <= 312 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost3") * tourTrainCost * (tourTrainCost > 312 && tourTrainCost <= 436 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost4") * tourTrainCost * (tourTrainCost > 436 && tourTrainCost <= 560 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_cost5") * tourTrainCost * (tourTrainCost > 560 ? 1 : 0)
                       + mcCoefs.get("Coef_" + type.name() + "_Time") * logsum.tourTrainTime(o, d));
        }
    }

    public double stopFreqUExp(int o, int d, int td, int tps, ModeChoice mc, TripType type, int toy, int numOfStops, boolean isOutBound) {
        double dist = o == d ? 0 : businessCarMap.get(getKey(o, d))[3];
        if(numOfStops == 0) {
            return exp(0.0);
        }
        if (numOfStops >= 5) {
            sLog.error("-- ERROR -- numOfStops >= 5. numOfStops: " + numOfStops + ", o: " + o + ", d: " + d + ", td: " + td + ", tps: " + tps + ", mc: " + mc.name() + ", type: " + type.name() + ", toy: " + toy + ", outbound?: " + isOutBound);
            System.exit(-1);
        }
        return exp(stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_dist" + numOfStops) * dist
                   + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_dura" + numOfStops) * td
                   + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_party" + numOfStops) * tps
                   + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_car" + numOfStops) * (mc == ModeChoice.CAR ? 1 : 0)
                   + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_busi" + numOfStops) * (type == TripType.BUSINESS ? 1 : 0)
                   + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_plea" + numOfStops) * (type == TripType.PLEASURE ? 1 : 0)
                   + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_quart2_" + numOfStops) * (toy == 2 ? 1 : 0)
                   + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_quart3_" + numOfStops) * (toy == 3 ? 1 : 0)
                   + stopFreqCoefs.get("p" + (isOutBound ? "o" : "i") + "_quart4_" + numOfStops) * (toy == 4 ? 1 : 0)
                   + stopFreqCoefs.get("cons" + (isOutBound ? "o" : "i") + numOfStops));
    }

    public double stopTypeUExp(int numOfStop, TripType type, int tps, ModeChoice mc, boolean isOutBound) {
        if(type == TripType.PLEASURE) {
            return exp(0.0);
        }
        else {
            return exp(stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_sec_" + (type == TripType.BUSINESS ? "b" : "pb")) * (numOfStop == 2 ? 1 : 0)
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_thir_" + (type == TripType.BUSINESS ? "b" : "pb")) * (numOfStop == 3 ? 1 : 0)
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_fou_" + (type == TripType.BUSINESS ? "b" : "pb")) * (numOfStop == 4 ? 1 : 0)
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_pt_" + (type == TripType.BUSINESS ? "b" : "pb")) * (type == TripType.PLEASURE ? 1 : 0)
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_pbt_" + (type == TripType.BUSINESS ? "b" : "pb")) * (type == TripType.PERSONAL_BUSINESS ? 1 : 0)
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_party_" + (type == TripType.BUSINESS ? "b" : "pb")) * tps
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_car_" + (type == TripType.BUSINESS ? "b" : "pb")) * (mc == ModeChoice.CAR ? 1 : 0)
                   + stopTypeCoefs.get("p" + (isOutBound ? "o" : "i") + "_air_" + (type == TripType.BUSINESS ? "b" : "pb")) * (mc == ModeChoice.AIR ? 1 : 0)
                   + stopTypeCoefs.get("con" + (isOutBound ? "o" : "i") + "s_" + (type == TripType.BUSINESS ? "b" : "pb")));

        }
    }

    public double stopLocUExp(Person2010 p, int so, int o, int d, int s, ModeChoice mc, TripType type, int toy, int days, int numOfStops, boolean isOutBound, List<Integer> stopLocations) {
        if(s == so || s == d || s == o) {
            return 0.0;
        }
        
        for (int loc : stopLocations) {
            if (loc == s) {
                return 0.0;
            }
        }
        
//        double tripTime = 0.0;
//        if (mc == ModeChoice.CAR) {
//            tripTime = logsum.tourCarTime(so, s, type);
//            if (tripTime > days * 24 / 2 / 2 / (numOfStops + 1)) {
//                return 0.0;
//            }
//        }
//        else if (mc == ModeChoice.TRAIN) {
//            tripTime = logsum.tourTrainTime(so, s);
//            if (tripTime > days * 24 / 2 / 2 / (numOfStops + 1)) {
//                return 0.0;
//            }
//        }
//        else {
//            tripTime = logsum.tourAirTime(so, s);
//            if (tripTime > days * 24 / 2 / 2 / (numOfStops + 1)) {
//                return 0.0;
//            }
//        }
        
        
        double gtc = generailizedTravelCost(p, so, o, d, s, mc, type, toy);
//        sLog.debug("      gtc[" + s + "]: " + gtc);
        double dist = 0.0;
        try {
            dist = businessCarMap.get(getKey(so, d))[3];
        } catch (NullPointerException e) {
            String msg = e.getLocalizedMessage() + ". (p: " + p.getPid() + ", so: " + so + ", o: " + o + ", d: " + d + ", s: " + s + ", mc: " + mc.name() + ", type: " + type.name() + ", toy: " + toy + ", outBound: " + isOutBound + ", days: " + days + ", num of stops:  " + numOfStops + ") Stop Locations: ";
            for (int loc: stopLocations) {
                msg += loc + ", ";
            }
            sLog.error(msg);
        }
        if(gtc == Double.POSITIVE_INFINITY) {
            // meaning some key pair couldn't be found in train/car/air files.
            return 0.0;
        }
        double u = stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_det_dist") * detDist(so, d, s)
                   + stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_gtc_s") * (dist < 150 ? gtc : 0)
                   + stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_gtc_m") * (dist >= 150 && dist < 550 ? gtc : 0)
                   + stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_gtc_l") * (dist >= 550 ? gtc : 0)
                   + (isOutBound ? stopLocCoefs.get("po_gtc_lc") : stopLocCoefs.get("pi_gtc_mc"))
                     * (isOutBound ? (dist >= 550 ? gtc : 0) : (dist >= 150 && dist < 550 ? gtc : 0)) * (mc == ModeChoice.CAR ? 1 : 0)
                   + stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_emp") * msaEmpMap.get(s)[0]
                   + stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_hh") * msaEmpMap.get(s)[1]
                   + stopLocCoefs.get("p" + (isOutBound ? "o" : "i") + "_nmsa") * (1 - zonIdMap.get(idMsaMap.get(s))[1]);
        if (u == Double.NaN || u == Double.POSITIVE_INFINITY) {
            return 0.0;
        }
        return exp(u);
    }

    private double generailizedTravelCost(Person2010 p, int so, int o, int d, int s, ModeChoice mc, TripType type, int toy) {
        return detourTravelCost(p, so, d, s, mc, type, toy) + detourTravelTime(p, so, d, s, mc, type) * vot(p, o, d, mc, type, toy);
    }

    private double detourTravelCost(Person2010 p, int so, int d, int s, ModeChoice mc, TripType type, int toy) {
        double tmp1;
        double tmp2;
        double tmp3;
        if (mc == ModeChoice.CAR) {
            tmp1 = logsum.tourCarCost(p.getIncLevel(), so, s, type);
            tmp2 = logsum.tourCarCost(p.getIncLevel(), s, d, type);
            tmp3 = logsum.tourCarCost(p.getIncLevel(), so, d, type);

        } else if (mc == ModeChoice.AIR) {
            tmp1 = logsum.tourAirCost(so, s, toy);
            tmp2 = logsum.tourAirCost(s, d, toy);
            tmp3 = logsum.tourAirCost(so, d, toy);
        } else {
            // mc == ModeChoice.TRAIN
            tmp1 = logsum.tourTrainCost(so, s);
            tmp2 = logsum.tourTrainCost(s, d);
            tmp3 = logsum.tourTrainCost(so, d);
        }

        if (tmp1 == Double.POSITIVE_INFINITY || tmp2 == Double.POSITIVE_INFINITY || tmp3 == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        } else {
            return (tmp1 + tmp2 - tmp3) / 2;
        }
    }

    private double detourTravelTime(Person2010 p, int so, int d, int s, ModeChoice mc, TripType type) {
        double tmp1, tmp2, tmp3;
        if (mc == ModeChoice.CAR) {
            tmp1 = logsum.tourCarTime(so, s, type);
            tmp2 = logsum.tourCarTime(s, d, type);
            tmp3 = logsum.tourCarTime(so, d, type);
        } else if (mc == ModeChoice.AIR) {
            tmp1 = logsum.tourAirTime(so, s);
            tmp2 = logsum.tourAirTime(s, d);
            tmp3 = logsum.tourAirTime(s, d);
        } else {
            // mc == ModeChoice.TRAIN
            tmp1 = logsum.tourTrainTime(so, s);
            tmp2 = logsum.tourTrainTime(s, d);
            tmp3 = logsum.tourTrainTime(so, d);
        }
        if (tmp1 == Double.POSITIVE_INFINITY || tmp2 == Double.POSITIVE_INFINITY || tmp3 == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        } else {
            return (tmp1 + tmp2 - tmp3) / 2;
        }
    }

    private double vot(Person2010 p, int o, int d, ModeChoice mc, TripType type, int toy) {
        if(mc == ModeChoice.CAR) {
            double tcc = logsum.tourCarCost(p.getIncLevel(), o, d, type);
            return getVotValue(type, tcc);
        }
        else if(mc == ModeChoice.AIR) {
            double tac = logsum.tourAirCost(o, d, toy);
            return getVotValue(type, tac);
        }
        else {
            // mc == ModeChoice.TRAIN
            double ttc = logsum.tourTrainCost(o, d);
            return getVotValue(type, ttc);
        }

    }

    private double getVotValue(TripType type, double cost) {
        if(type == TripType.BUSINESS) {
            if(cost <= 188) {
                return 1.095384615;
            }
            if(cost > 188 && cost <= 332) {
                return 3.811563169;
            }
            if(cost > 332 && cost <= 476) {
                return 5.377643505;
            }
            if(cost > 476 && cost <= 620) {
                return 9.621621622;
            }
            if(cost > 620) {
                return 12.8057554;
            }
            return Double.POSITIVE_INFINITY;
        }
        if(type == TripType.PLEASURE) {
            if(cost <= 188) {
                return 6.230200634;
            }
            if(cost > 188 && cost <= 312) {
                return 13.59447005;
            }
            if(cost > 312 && cost <= 436) {
                return 65.55555556;
            }
            if(cost > 436) {
                return 176.119403;
            }
            return Double.POSITIVE_INFINITY;
        }
        if(type == TripType.PERSONAL_BUSINESS) {
            if(cost <= 188) {
                return 2.582677165;
            }
            if(cost > 188 && cost <= 312) {
                return 5.754385965;
            }
            if(cost > 312 && cost <= 436) {
                return 8.282828283;
            }
            if(cost > 436 && cost <= 560) {
                return 11.88405797;
            }
            if(cost > 560) {
                return 30.37037037;
            }
            return Double.POSITIVE_INFINITY;
        }
        return Double.POSITIVE_INFINITY;
    }

    private double detDist(int so, int d, int s) {
        if(businessCarMap.get(getKey(so, s))[3] < 50) {
            return Double.POSITIVE_INFINITY;
        }
        if(so == s) {
            return Double.POSITIVE_INFINITY;
        }
        return businessCarMap.get(getKey(so, s))[3] + businessCarMap.get(getKey(s, d))[3] - businessCarMap.get(getKey(so, d))[3];
    }

    public void preCalculateLogsum() {
        sLog.info("Started pre-calculate logsum.");
        Person2010 p = new Person2010();
        for (int o = 1; o <= Math.alt; o++) {
            sLog.debug("  " + o + " out of " + Math.alt + " done.");
            for (int d = 1; d <= Math.alt; d++) {
                if(o == d) {
                    continue;
                }
                try {
                    p.setIncLevel(1);
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, INVALID_QUARTER));
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, INVALID_QUARTER));
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, INVALID_QUARTER));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 1));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 1));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 1));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 2));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 2));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 2));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 3));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 3));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 3));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 4));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 4));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 4));

                    p.setIncLevel(2);
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, INVALID_QUARTER));
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, INVALID_QUARTER));
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, INVALID_QUARTER));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 1));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 1));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 1));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 2));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 2));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 2));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 3));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 3));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 3));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 4));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 4));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 4));

                    p.setIncLevel(3);
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, INVALID_QUARTER));
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, INVALID_QUARTER));
                    logsumCacheQ0.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, INVALID_QUARTER));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 1));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 1));
                    logsumCacheQ1.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 1));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 2));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 2));
                    logsumCacheQ2.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 2));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 3));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 3));
                    logsumCacheQ3.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 3));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.BUSINESS, 4));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PLEASURE.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PLEASURE, 4));
                    logsumCacheQ4.put(o + "-" + d + "-" + p.getIncLevel() + "-" + TripType.PERSONAL_BUSINESS.name(),
                                      logsum.calculateLogsum(p, o, d, TripType.PERSONAL_BUSINESS, 4));
                }
                catch (InvalidValueException e) {
                    sLog.error(e.getLocalizedMessage(), e);
                    System.exit(1);
                }
            }
        }
        sLog.info("Completed. quarter0 size: " + logsumCacheQ0.size()
                  + ", quarter1 size: " + logsumCacheQ1.size()
                  + ", quarter2 size: " + logsumCacheQ2.size()
                  + ", quarter3 size: " + logsumCacheQ3.size()
                  + ", quarter4 size: " + logsumCacheQ4.size());
    }

    private Double getLogsum(Person2010 p, int o, int d, TripType type, int quarter) {
        double logsum = Double.NEGATIVE_INFINITY;
        if(quarter == INVALID_QUARTER) {
            logsum = logsumCacheQ0.get(o + "-" + d + "-" + p.getIncLevel() + "-" + type.name());
        }
        else if(quarter == 1) {
            logsum = logsumCacheQ1.get(o + "-" + d + "-" + p.getIncLevel() + "-" + type.name());
        }
        else if(quarter == 2) {
            logsum = logsumCacheQ2.get(o + "-" + d + "-" + p.getIncLevel() + "-" + type.name());
        }
        else if(quarter == 3) {
            logsum = logsumCacheQ3.get(o + "-" + d + "-" + p.getIncLevel() + "-" + type.name());
        }
        else if(quarter == 4) {
            logsum = logsumCacheQ4.get(o + "-" + d + "-" + p.getIncLevel() + "-" + type.name());
        }
        return logsum;
    }

    public Integer MonteCarloMethod(List<Double> pList, Map<Double, List<Integer>> pMap, double smpl) {
        sLog.debug("    Monte carlo rand: " + smpl);
        Collections.sort(pList);
        double tmpSum = 0.0;
        int pickedIndex = - 1;
        for (int ptr = 0; ptr < pList.size(); ptr++) {
            double tmp = tmpSum;
            tmpSum += pList.get(ptr);
            if(smpl >= tmp && smpl < tmpSum) {
                pickedIndex = ptr;
                break;
            }
        }
        if(pickedIndex == -1) {
            pickedIndex = pList.size() - 1;
        }
        sLog.debug("        pickedIndex: " + pickedIndex);
        sLog.debug("        pList.get(pickedIndex): " + pList.get(pickedIndex));
        List<Integer> rtn = pMap.get(pList.get(pickedIndex));
//        if (rtn == null) {
//            sLog.error("        rtn is null. pMap dump: ");
//            for (Double key : pMap.keySet()) {
//                sLog.error("        pMap[" + key + "]: " + pMap.get(key));
//            }
//            System.exit(1);
//        }
        if (rtn.size() > 1) {
            int rand = (int) (System.currentTimeMillis() % rtn.size());
            return rtn.get(rand);
        }
        return rtn.get(0);
    }

    /**
     * File Loading Methods below
     * @return 
     */
    private HashMap<String, Double[]> initOtherCarMap() {
        sLog.info("Initialize otherCarMap.");
        HashMap<String, Double[]> ocm = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.odskim_car_other"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("MSA_A")) {
                    String key = ExcelUtils.getColumnValue(3, line) + "-" + ExcelUtils.getColumnValue(4, line);
                    String carTime = ExcelUtils.getColumnValue(6, line);
                    String driveCost = ExcelUtils.getColumnValue(7, line);
                    String stopNights = ExcelUtils.getColumnValue(8, line);
                    String dist = ExcelUtils.getColumnValue(5, line);
                    Double[] value = {
                        Double.parseDouble(carTime),
                        Double.parseDouble(driveCost),
                        Double.parseDouble(stopNights),
                        Double.parseDouble(dist)
                    };
                    ocm.put(key, value);
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return ocm;
    }

    private HashMap<String, Double[]> initBusinessCarMap() {
        sLog.info("Initialize businessCarMap.");
        HashMap<String, Double[]> bcm = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.odskim_car_business"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("MSA_A")) {
                    String key = ExcelUtils.getColumnValue(3, line) + "-" + ExcelUtils.getColumnValue(4, line);
                    String carTime = ExcelUtils.getColumnValue(6, line);
                    String driveCost = ExcelUtils.getColumnValue(7, line);
                    String stopNights = ExcelUtils.getColumnValue(8, line);
                    String dist = ExcelUtils.getColumnValue(5, line);
                    Double[] value = {
                        Double.parseDouble(carTime),
                        Double.parseDouble(driveCost),
                        Double.parseDouble(stopNights),
                        Double.parseDouble(dist)
                    };
                    bcm.put(key, value);
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return bcm;
    }

    private HashMap<String, Double[]> initAirMap() {
        sLog.info("Initialize airMap.");
        HashMap<String, Double[]> am = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.air_skim_avg"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("O_MSA")) {
                    String key = ExcelUtils.getColumnValue(3, line) + "-" + ExcelUtils.getColumnValue(4, line);
                    String airTime = ExcelUtils.getColumnValue(7, line);
                    String airCost = ExcelUtils.getColumnValue(8, line);
                    Double[] value = {
                        Double.parseDouble(airTime),
                        Double.parseDouble(airCost)
                    };
                    am.put(key, value);
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return am;
    }

    private HashMap<String, Double[]> initTrainMap() {
        sLog.info("Initialize trainMap.");
        HashMap<String, Double[]> tm = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.odskim_train"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("A_MSA")) {
                    String key = ExcelUtils.getColumnValue(3, line) + "-" + ExcelUtils.getColumnValue(4, line);
                    String trainTime = ExcelUtils.getColumnValue(5, line);
                    String trainCost = ExcelUtils.getColumnValue(6, line);
                    Double[] value = {
                        Double.parseDouble(trainTime),
                        Double.parseDouble(trainCost)
                    };
                    tm.put(key, value);
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return tm;
    }

    private HashMap<Integer, Double[]> initMsaEmpMap() {
        sLog.info("Initialize msaEmpMap.");
        HashMap<Integer, Double[]> mem = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.msa_emp"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("MSA")) {
                    Integer key = zonIdMap.get(Integer.parseInt(ExcelUtils.getColumnValue(1, line)))[0];
                    Double emp = Double.parseDouble(ExcelUtils.getColumnValue(3, line));
                    Double hh = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    Double[] value = {emp, hh};
                    mem.put(key, value);
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }
        return mem;
    }

    private HashMap<String, Double[]> initQuarterAirMap() {
        sLog.info("Initialize quarterAirMap.");
        HashMap<String, Double[]> qam = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.msafare_1"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("Quarter")) {
                    String key = ExcelUtils.getColumnValue(2, line)
                                 + "-"
                                 + ExcelUtils.getColumnValue(3, line);
                    Double fare = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    if(qam.get(key) != null) {
                        Double[] value = qam.get(key);
                        value[1] = fare;
                        qam.put(key, value);
                    }
                    else {
                        Double[] value = new Double[5];
                        value[1] = fare;
                        qam.put(key, value);
                    }
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }

        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.msafare_2"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("Quarter")) {
                    String key = ExcelUtils.getColumnValue(2, line)
                                 + "-"
                                 + ExcelUtils.getColumnValue(3, line);

                    Double fare = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    if(qam.get(key) != null) {
                        Double[] value = qam.get(key);
                        value[2] = fare;
                        qam.put(key, value);
                    }
                    else {
                        Double[] value = new Double[5];
                        value[2] = fare;
                        qam.put(key, value);
                    }
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }

        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.msafare_3"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("Quarter")) {
                    String key = ExcelUtils.getColumnValue(2, line)
                                 + "-"
                                 + ExcelUtils.getColumnValue(3, line);

                    Double fare = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    if(qam.get(key) != null) {
                        Double[] value = qam.get(key);
                        value[3] = fare;
                        qam.put(key, value);
                    }
                    else {
                        Double[] value = new Double[5];
                        value[3] = fare;
                        qam.put(key, value);
                    }
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }

        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.msafare_4"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("Quarter")) {
                    String key = ExcelUtils.getColumnValue(2, line)
                                 + "-"
                                 + ExcelUtils.getColumnValue(3, line);

                    Double fare = Double.parseDouble(ExcelUtils.getColumnValue(4, line));
                    if(qam.get(key) != null) {
                        Double[] value = qam.get(key);
                        value[4] = fare;
                        qam.put(key, value);
                    }
                    else {
                        Double[] value = new Double[5];
                        value[4] = fare;
                        qam.put(key, value);
                    }
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        catch (NumberFormatException ex) {
            sLog.debug(ex.getLocalizedMessage(), ex);
        }

        return qam;
    }

    private HashMap<Integer, Integer[]> initZoneIdMapAndIdMsaMap() {
        sLog.info("Initialize zone id.");
        HashMap<Integer, Integer[]> zone = new HashMap<>();
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.zoneid"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("MSA/NMSA")) {
                    Integer key = Integer.parseInt(ExcelUtils.getColumnValue(1, line));
                    Integer[] value = {Integer.parseInt(ExcelUtils.getColumnValue(2, line)), Integer.parseInt(ExcelUtils.getColumnValue(3, line))};
                    zone.put(key, value);
                    idMsaMap.put(value[0], key);
                }
            }
            br.close();
        }
        catch (IOException ex) {
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
        return zone;
    }

    private String getKey(int o, int d) {
        return Integer.toString(o) + "-" + Integer.toString(d);
    }
}
