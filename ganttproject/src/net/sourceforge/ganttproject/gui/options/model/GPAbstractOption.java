/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.gui.options.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.language.GanttLanguage;

public abstract class GPAbstractOption<T> implements GPOption<T>, ChangeValueDispatcher {
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

    protected T getInitialValue() {
        return myInitialValue;
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
        if (myInitialValue == null) {
            return myValue != null;
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

    protected static String i18n(String key) {
        return GanttLanguage.getInstance().getText(key);
    }
}