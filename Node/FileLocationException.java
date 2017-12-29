package Node;

public class FileLocationException extends Exception {
    // Parameterless Constructor
    public FileLocationException() {}

    // Constructor that accepts a message
    public FileLocationException(String message)
    {
        super(message);
    }
    public FileLocationException(Throwable cause) {
        super (cause);
    }

    public FileLocationException(String message, Throwable cause) {
        super (message, cause);
    }
}