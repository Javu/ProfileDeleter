
/**
 * Exception for use with ProfileDeleter.<br>
 * To be used when a needed variable is not initialised.
 */
class NotInitialisedException extends Exception {

    public NotInitialisedException() {
    }

    public NotInitialisedException(String message) {
        super(message);
    }

}
