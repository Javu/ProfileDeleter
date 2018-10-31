/**
 * Exception for use with ProfileDeleter.
 * To be used when a specified object does not exist.
 */
public class DoesNotExistException extends Exception {

    public DoesNotExistException() {
    }
    
    public DoesNotExistException(String message) {
        super(message);
    }
    
}
