package umd.lu.thesis.simulation.app2000;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
/**
 *
 * @author lousia
 */
public class App2000Executable {
    
    private static final Logger sLog = LogManager.getLogger(App2000Executable.class);

    public static void main(String[] args) throws Exception {
        sLog.info("App2000 Simulation Started.");
        App2000 app = new App2000();
//        app.run();
        sLog.info("App2000 Simulation Stopped.");
    }

}
