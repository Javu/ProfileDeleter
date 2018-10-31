/**
 * Exception for use with ProfileDeleter.
 * To be used when a folder, file or registry value in Windows cannot be edited.
 */
class CannotEditException extends Exception {

    public CannotEditException() {
    }
    
    public CannotEditException(String message) {
        super(message);
    }
      
}
