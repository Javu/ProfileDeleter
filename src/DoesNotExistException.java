
/**
 * Exception for use with ProfileDeleter.<br>
 * To be used when a specified object does not exist.
 */
class DoesNotExistException extends Exception {

    public DoesNotExistException() {
    }

    public DoesNotExistException(String message) {
        super(message);
    }

}
