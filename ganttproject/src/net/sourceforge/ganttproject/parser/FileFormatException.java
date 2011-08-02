package net.sourceforge.ganttproject.parser;

public class FileFormatException extends Exception {
    public FileFormatException() {
    }

    public FileFormatException(String message) {
        super(message);
    }

    public FileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileFormatException(Throwable cause) {
        super(cause);
    }
}
