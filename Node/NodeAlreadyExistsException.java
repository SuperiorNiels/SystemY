package Node;

public class NodeAlreadyExistsException extends Exception {
    // Parameterless Constructor
    public NodeAlreadyExistsException() {}

    // Constructor that accepts a message
    public NodeAlreadyExistsException(String message)
    {
        super(message);
    }
    public NodeAlreadyExistsException(Throwable cause) {
        super (cause);
    }

    public NodeAlreadyExistsException(String message, Throwable cause) {
        super (message, cause);
    }
}
