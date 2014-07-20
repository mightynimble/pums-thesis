/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package umd.lu.thesis.exceptions;

/**
 *
 * @author lousia
 */
public class InvalidValueException extends Exception {

    public InvalidValueException(String message) {
        super(message);
    }

    public InvalidValueException(String message, Throwable ex) {
        super(message, ex);
    }
}
