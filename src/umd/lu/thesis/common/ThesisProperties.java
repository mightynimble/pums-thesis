package umd.lu.thesis.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author Bo Sun
 */
public class ThesisProperties {
    
    private static Properties prop = new Properties();
    static {
        loadProperties();
    }

    private static void loadProperties() {
        try {
            prop.load(new FileInputStream(System.getProperty("user.dir") + "/resources/config.properties"));
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public static String getProperties(String name) {
        return prop.getProperty(name);
    }
}
