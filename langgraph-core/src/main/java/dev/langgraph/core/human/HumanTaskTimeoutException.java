package dev.langgraph.core.human;

public class HumanTaskTimeoutException extends RuntimeException {
    
    public HumanTaskTimeoutException(String message) {
        super(message);
    }

    public HumanTaskTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
