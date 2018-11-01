
/**
 * Exception for use with ProfileDeleter.<br>
 * To be used when numeric data is expected but not received.
 */
class NonNumericException extends Exception {

    public NonNumericException() {
    }

    public NonNumericException(String message) {
        super(message);
    }

}
