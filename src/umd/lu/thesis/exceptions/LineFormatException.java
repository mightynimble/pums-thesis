package umd.lu.thesis.exceptions;

/**
 *
 * @author Home
 */
public class LineFormatException extends Exception {

    public LineFormatException(String message) {
        super(message);
    }

    public LineFormatException(String message, Throwable ex) {
        super(message, ex);
    }
}
