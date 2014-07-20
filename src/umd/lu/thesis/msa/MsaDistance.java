package umd.lu.thesis.msa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import umd.lu.thesis.common.ThesisConstants;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.ExcelUtils;
import umd.lu.thesis.helper.MathUtils;
import umd.lu.thesis.objects.Point;

/**
 * Program assumes input rows are ordered by MSA column.
 *
 * @author lousia
 */
public class MsaDistance {

    private static HashMap<Integer, ArrayList<Point>> msaMap = new HashMap<>();

    private static int currentMsa = 0;
    private static int nextMsa = 0;

    public static void main(String[] args) throws Exception {
        initMsaHashMap();

        FileWriter fw = new FileWriter(ThesisProperties.getProperties("output.file.path"));
        BufferedWriter bw = new BufferedWriter(fw, ThesisConstants.bufferSize);
        Integer[] msaList = msaMap.keySet().toArray(new Integer[msaMap.keySet().size()]);
        for (int i = 0; i < msaList.length; i++) {
            currentMsa = msaList[i];
            if (i < msaList.length - 1) {
                for (int j = i + 1; j < msaList.length; j++) {
                    nextMsa = msaList[j];
                    double distance = 0.0;
                    for (Point p1 : msaMap.get(currentMsa)) {
                        for (Point p2 : msaMap.get(nextMsa)) {
                            distance += MathUtils.distance(p1, p2);
                        }
                    }
                    distance = distance / (msaMap.get(currentMsa).size() * msaMap.get(nextMsa).size());
                    bw.write(Integer.toString(currentMsa) + "\t" + Integer.toString(nextMsa) + "\t" + Double.toString(distance) + "\n");
                }
            }
        }
        bw.flush();
        bw.close();
    }

    private static void initMsaHashMap() {
        try (FileInputStream fstream = new FileInputStream(ThesisProperties.getProperties("data.file.path"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("STATEFP00")) {
                    Integer msa = Integer.parseInt(ExcelUtils.getColumnValue(8, line));
                    if (msaMap.containsKey(msa)) {
                        ArrayList<Point> value = msaMap.get(msa);
                        value.add(new Point(
                                Double.parseDouble(ExcelUtils.getColumnValue(6, line)),
                                Double.parseDouble(ExcelUtils.getColumnValue(7, line))));
                        msaMap.put(msa, value);
                    } else {
                        ArrayList<Point> value = new ArrayList<>();
                        value.add(new Point(
                                Double.parseDouble(ExcelUtils.getColumnValue(6, line)),
                                Double.parseDouble(ExcelUtils.getColumnValue(7, line))));
                        msaMap.put(msa, value);
                    }
                }
            }
        } catch (IOException ex) {

        }
    }

}
