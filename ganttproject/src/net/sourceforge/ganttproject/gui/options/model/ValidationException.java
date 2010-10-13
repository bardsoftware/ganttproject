package net.sourceforge.ganttproject.gui.options.model;

public class ValidationException extends RuntimeException {
    public ValidationException() {
    }
    public ValidationException(String message) {
        super(message);
    }
    public ValidationException(Throwable cause) {
        super(cause);
    }
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}