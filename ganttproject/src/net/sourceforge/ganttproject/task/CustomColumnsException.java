package net.sourceforge.ganttproject.task;

/**
 * Exception to be thrown in several cases : A custom column already exists (and
 * someone tries to add a nex one with the same name) A custom column does not
 * exists (and someone tried to get it) A class mismatch.
 *
 * @author bbaranne (Benoit Baranne) Mar 2, 2005
 */
public class CustomColumnsException extends Exception {
    public static final int ALREADY_EXIST = 0;

    public static final int DO_NOT_EXIST = 1;

    public static final int CLASS_MISMATCH = 2;

    /**
     * Exception type.
     */
    private int type = -1;

    public CustomColumnsException(int type, String message) {
        super(message);
        this.type = type;
    }

    public CustomColumnsException(Throwable cause) {
    	super(cause);
    }

    public int getType() {
        return type;
    }
}
