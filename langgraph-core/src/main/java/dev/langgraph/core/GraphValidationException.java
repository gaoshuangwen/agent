package dev.langgraph.core;

public class GraphValidationException extends Exception {
    
    public GraphValidationException(String message) {
        super(message);
    }

    public GraphValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
