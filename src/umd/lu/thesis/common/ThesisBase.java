/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package umd.lu.thesis.common;

import java.lang.management.*;
import org.apache.log4j.MDC;

/**
 *
 * @author Bo Sun
 */
public class ThesisBase {
    
    public ThesisBase() {
        MDC.put("UUID", java.util.UUID.randomUUID().toString().split("-")[0]);
    }
}
