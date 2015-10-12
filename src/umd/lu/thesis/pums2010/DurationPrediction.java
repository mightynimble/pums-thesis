package umd.lu.thesis.pums2010;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.pums2010.objects.Person2010;
import umd.lu.thesis.simulation.app2000.objects.TripType;

/**
 *
 * @author Home
 */
public class DurationPrediction extends NationalTravelDemand {
    private final static Logger sLog = LogManager.getLogger(DurationPrediction.class);
    
    public DurationPrediction() {
    }
    
    public static void main(String[] args) throws Exception {
        
        DurationPrediction dp = new DurationPrediction();
        
        File f = new File(ThesisProperties.getProperties("simulation.pums2010.duration.prediction.output"));
        String line;
        Person2010 p = new Person2010();
        int d = -1;
        int toy = -1;
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("simulation.pums2010.duration.prediction.input"));
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                FileWriter fw = new FileWriter(f); 
                BufferedWriter bw = new BufferedWriter(fw)) {
            while ((line = br.readLine()) != null) {
                if (!line.toLowerCase().startsWith("id")) {
                    // Columns needed: EG(137), BO(67), BR(70), BW(75), CP(94), CZ(104), CG(85)
                    d = Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.eg, line));
                    p.setHhType(Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.bo, line)));
                    p.setNp(Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.br, line)));
                    p.setIncLevel(Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.bw, line)));
                    p.setEmpStatus(Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.cp, line)));
                    toy = Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.cz, line));
                    p.setAge(Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.cg, line)));
                    
                    line += "\t" + dp.findTourDuration(p, d, TripType.BUSINESS, toy) + "\n";
                }
                else {
                    line += "\tDURATION_PREDICTION\n";
                }
                
                bw.write(line);
            }
            br.close();
            bw.close();
        } catch (Exception ex) {
            System.out.println("---------------------" + d);
            sLog.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
    }
}
