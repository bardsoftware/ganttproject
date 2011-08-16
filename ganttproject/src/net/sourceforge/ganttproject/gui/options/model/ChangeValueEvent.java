package net.sourceforge.ganttproject.gui.options.model;

/**
 * Event that discribes a change in a value.
 * @author bbaranne
 *
 */
public class ChangeValueEvent {

    private Object myID;

    private Object myOldValue;

    private Object myNewValue;

    public ChangeValueEvent(Object id, Object oldValue, Object newValue) {
        myID = id;
        myOldValue = oldValue;
        myNewValue = newValue;
    }

    public Object getID() {
        return myID;
    }

    public Object getOldValue() {
        return myOldValue;
    }

    public Object getNewValue() {
        return myNewValue;
    }

    public String toString() {
        return "[id:" + myID + ", old:" + myOldValue + ", new: " + myNewValue
                + "]";
    }
}
