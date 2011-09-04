package net.sourceforge.ganttproject.task;

public class DurationParsingException extends RuntimeException {
    public DurationParsingException() {
    }

    public DurationParsingException(String message) {
        super(message);
    }

    public DurationParsingException(Throwable cause) {
        super(cause);
    }

    public DurationParsingException(String message, Throwable cause) {
        super(message, cause);
    }

}