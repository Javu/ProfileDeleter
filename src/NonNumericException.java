/**
 * Exception for use with ProfileDeleter.
 * To be used when numeric data is expected but not received.
 */
public class NonNumericException extends Exception {

    public NonNumericException() {
    }
    
    public NonNumericException(String message) {
        super(message);
    }
    
}
