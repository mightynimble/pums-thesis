package umd.lu.thesis.poiextract;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Yijing Lu
 */
public class Main {

    private static final int bufferSize = 10485760;

    private static final int notificationPerLines = 100000;

    // max: 1,048,576
    private static final int outFileLines = 800000;

    private static final String NODE_HEAD = "<node";
    
    private static final String NOTE_TAIL = "</node>";

    private static final String TAG_HEAD = "<tag";

//    private static final String AMENITY = "amenity";

//    private static final String NAME = "name";

    private static final String nodePattern = "<node\\s+id=\"\\d+\"\\s+lat=\"(.+)\"\\s+lon=\"(.*\\d{1,3}\\.\\d{7})\"\\s+.*user.+";

    private static final String tagAmenityPattern = "<tag\\s+k=\"amenity\"\\s+v=\"(.+)\"\\s*/>";
    
    private static final String tagNamePattern = "<tag\\s+k=\"name\"\\s+v=\"(.+)\"\\s*/>";

    public static void main(String[] args) throws IOException {
        Character.isWhitespace('\t');
        Character.isWhitespace(' ');
        BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("data.file.path"), bufferSize);
        String inLine = null;
        Pattern nodePtn = Pattern.compile(nodePattern);
        Pattern tagAmenityPtn = Pattern.compile(tagAmenityPattern);
        Pattern tagNamePtn = Pattern.compile(tagNamePattern);
        Matcher amenityMatcher = null;
        Matcher nameMatcher = null;
        Matcher m = null;
        boolean inNodeTag = false;
        String lat = null;
        String lon = null;
        String amenity = null;
        String name = null;
        BufferedWriter bw = null;

        long lineCounter = 0;
        long recordCounter = 1;
        int outFileCounter = 0;
        while ((inLine = br.readLine()) != null) {
            lineCounter ++;
            
            if(!inNodeTag) {
                lat = null;
                lon = null;
                amenity = null;
                name = null;
            }

            inLine = StringUtils.stripStart(inLine, null);
            inLine = StringUtils.chomp(inLine);


            // if the <node> doesn't have any <tag>s, skip
            if(inLine.startsWith(NODE_HEAD) && inLine.endsWith("/>")) {
                inNodeTag = false;
                continue;
            }
            // if the <node> has <tag>s, get the "lat" and "lon"
            else if(inLine.startsWith(NODE_HEAD)) {
                inNodeTag = true;
                m = nodePtn.matcher(inLine);
                if(m.find()) {
                    lat = m.group(1);
                    lon = m.group(2);
                }
            }
            // if this is a <tag> line, get amenity and name, set tagFound = false
            else if(inLine.startsWith(TAG_HEAD)) {
                inNodeTag = true;
                amenityMatcher = tagAmenityPtn.matcher(inLine);
                if(amenityMatcher.find()) {
                    amenity = amenityMatcher.group(1);
                }
                
                nameMatcher = tagNamePtn.matcher(inLine);
                if (nameMatcher.find()) {
                    name = nameMatcher.group(1);
                }
            }
            // if this line is </node>, set tagFound = false
            else if (inLine.equals(NOTE_TAIL)) {
                inNodeTag = false;
            }

            if((lat != null && lon != null && amenity != null && name != null) 
               || (lat != null && lon != null && amenity != null && name == null && !inNodeTag)) {
                // new file
                if(bw == null) {
                    FileWriter fw = new FileWriter(ThesisProperties.getProperties("output.file.base.name") + "." + outFileCounter + ".csv");
                    bw = new BufferedWriter(fw, bufferSize);
                    // write header
                    bw.write("lat, lon, amenity, name\r\n");
                    
                    outFileCounter++;
                }
                else if (recordCounter % outFileLines == 0) {
                    bw.close();
                                       
                    FileWriter fw = new FileWriter(ThesisProperties.getProperties("output.file.base.name") + "." + outFileCounter + ".csv");
                    bw = new BufferedWriter(fw, bufferSize);
                    bw.write("lat, lon, amenity, name\r\n");
                    
                    outFileCounter++;
                }
                
                bw.write(lat + ", " + lon + ", " + amenity + ", " + name + "\r\n");
                
                recordCounter++;
                inNodeTag = false;
            }
            
            if (lineCounter % notificationPerLines == 0) {
                System.out.printf("Line: %-15d, Record: %-15d, File: %-5d, %s\r\n", lineCounter, recordCounter, outFileCounter, inLine);
            }
        }
        if (bw != null) {
            bw.flush();
            bw.close();
        }
    }
}
