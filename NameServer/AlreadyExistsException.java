package NameServer;

import java.io.Serializable;

public class AlreadyExistsException extends Exception {
    // Parameterless Constructor
    public AlreadyExistsException() {}

    // Constructor that accepts a message
    public AlreadyExistsException(String message)
    {
        super(message);
    }
    public AlreadyExistsException(Throwable cause) {
        super (cause);
    }

    public AlreadyExistsException(String message, Throwable cause) {
        super (message, cause);
    }
}
