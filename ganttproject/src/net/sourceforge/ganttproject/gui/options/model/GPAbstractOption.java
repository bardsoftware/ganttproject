package net.sourceforge.ganttproject.gui.options.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public abstract class GPAbstractOption<T> implements GPOption, ChangeValueDispatcher {
    private final String myID;

    private List<ChangeValueListener> myListeners = new ArrayList<ChangeValueListener>();

    private PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

    private boolean isWritable = true;

    private T myValue;
    private T myInitialValue;

    protected GPAbstractOption(String id) {
        this(id, null);
    }

    protected GPAbstractOption(String id, T initialValue) {
        myID = id;
        myInitialValue = initialValue;
        myValue = initialValue;
    }

    public String getID() {
        return myID;
    }

    public T getValue() {
        return myValue;
    }

    public void setValue(T value) {
        setValue(value, false);
    }

    protected void setValue(T value, boolean resetInitial) {
        if (resetInitial) {
            myInitialValue = value;
        }
        ChangeValueEvent event = new ChangeValueEvent(getID(), myValue, value);
        myValue = value;
        fireChangeValueEvent(event);
    }

    public boolean isChanged() {
        if (myInitialValue==null) {
            return myValue!=null;
        }
        return !myInitialValue.equals(myValue);
    }

    public void lock() {
    }

    public void commit() {
    }

    public void rollback() {
    }

    public void addChangeValueListener(ChangeValueListener listener) {
        myListeners.add(listener);
    }

    protected void fireChangeValueEvent(ChangeValueEvent event) {
        for (ChangeValueListener listener : myListeners) {
            listener.changeValue(event);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        myPropertyChangeSupport.addPropertyChangeListener(listener);
    }
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        myPropertyChangeSupport.removePropertyChangeListener(listener);
    }

    public boolean isWritable() {
        return isWritable;
    }

    public void setWritable(boolean isWritable) {
        this.isWritable = isWritable;
        myPropertyChangeSupport.firePropertyChange("isWritable", Boolean.valueOf(!isWritable), Boolean.valueOf(isWritable));
    }
}