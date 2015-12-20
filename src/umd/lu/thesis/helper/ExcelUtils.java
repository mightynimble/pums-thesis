package umd.lu.thesis.helper;

/**
 *
 * @author Home
 */
public class ExcelUtils {

    public static String getColumnValue(int column, String line) {
        int pos = 0;
        int c = 1;
        while (c < column) {
            pos = line.indexOf("\t", pos + 1);
            c++;
        }
        int begin = 0;
        int end = 0;
        begin = pos;
        end = line.indexOf("\t", pos + 1);
        if (end != -1) {
            return line.substring(begin, end).trim();
        }
        else {
            return line.substring(begin).trim();
        }
    }

    public static String setColumnValue(int column, String line, String value) throws Exception {
        int pos = 0;
        int c = 1;
        while (c < column) {
            pos = line.indexOf("\t", pos + 1);
            c++;
        }
        int begin = 0;
        int end = 0;
        if(pos != -1) {
            begin = pos;
            end = line.indexOf("\t", pos + 1);
            String prefix = line.substring(0, begin + 1);
            String suffix = line.substring(end);
            return prefix + value + suffix;
        }
        else {
            throw new Exception("Can't locate column " + column + " in line: " + line);
        }
    }

    public static final int a = 1;

    public static final int b = 2;

    public static final int c = 3;

    public static final int d = 4;

    public static final int e = 5;

    public static final int f = 6;

    public static final int g = 7;

    public static final int h = 8;

    public static final int i = 9;

    public static final int j = 10;

    public static final int k = 11;

    public static final int l = 12;

    public static final int m = 13;

    public static final int n = 14;

    public static final int o = 15;

    public static final int p = 16;

    public static final int q = 17;

    public static final int r = 18;

    public static final int s = 19;

    public static final int t = 20;

    public static final int u = 21;

    public static final int v = 22;

    public static final int w = 23;

    public static final int x = 24;

    public static final int y = 25;

    public static final int z = 26;

    public static final int aa = 27;

    public static final int ab = 28;

    public static final int ac = 29;

    public static final int ad = 30;

    public static final int ae = 31;

    public static final int af = 32;

    public static final int ag = 33;

    public static final int ah = 34;

    public static final int ai = 35;

    public static final int aj = 36;

    public static final int ak = 37;

    public static final int al = 38;

    public static final int am = 39;

    public static final int an = 40;
    
    public static final int bo = 67;
    
    public static final int br = 70;
    
    public static final int bw = 75;
    
    public static final int by = 77;
    
    public static final int bz = 78;
        
    public static final int cg = 85;
    
    public static final int cp = 94;
    
    public static final int cz = 104;
    
    public static final int db = 106;
    
    public static final int eg = 137;
}
