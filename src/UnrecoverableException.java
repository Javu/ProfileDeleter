/**
 * Exception for use with ProfileDeleter.
 * To be used when the program reaches a state it cannot recover from.
 */
class UnrecoverableException extends Exception {
    
    public UnrecoverableException() {
    }
    
    public UnrecoverableException(String message) {
        super(message);
    }
    
}
