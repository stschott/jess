package de.upb.sse.jess.exceptions;

public class AmbiguityException extends RuntimeException {
    public AmbiguityException() {

    }
    public AmbiguityException (String message) {
        super (message);
    }

    public AmbiguityException (Throwable cause) {
        super (cause);
    }

    public AmbiguityException (String message, Throwable cause) {
        super (message, cause);
    }
}
