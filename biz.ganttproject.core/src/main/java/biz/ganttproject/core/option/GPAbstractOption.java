/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package biz.ganttproject.core.option;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.SortedSet;

public abstract class GPAbstractOption<T> implements GPOption<T> {
  public abstract static class I18N {
    private static I18N ourInstance;

    protected static void setI18N(I18N i18n) {
      ourInstance = i18n;
    }

    protected abstract String i18n(String key);
  }

  private final String myID;

  //private final List<ChangeValueListener> myListeners = new ArrayList<ChangeValueListener>();
  private final Listeners myListeners = new Listeners();
  private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

  private boolean isWritable = true;

  private T myValue;
  private T myInitialValue;

  private boolean isScreened;

  private boolean myHasUi = true;

  protected GPAbstractOption(String id) {
    this(id, null);
  }

  protected GPAbstractOption(String id, T initialValue) {
    myID = id;
    myInitialValue = initialValue;
    myValue = initialValue;
  }

  @Override
  public String getID() {
    return myID;
  }

  @Override
  public T getValue() {
    return myValue;
  }

  @Override
  public void setValue(T value) {
    resetValue(value, false, null);
  }

  public void setValue(T value, Object clientId) {
    resetValue(value, false, clientId);
  }

  protected T getInitialValue() {
    return myInitialValue;
  }

  protected void resetValue(T value, boolean resetInitial) {
    resetValue(value, resetInitial, null);
  }
  protected void resetValue(T value, boolean resetInitial, Object triggerId) {
    if (resetInitial) {
      myInitialValue = value;
    }
    ChangeValueEvent event = new ChangeValueEvent(getID(), myValue, value, triggerId);
    myValue = value;
    fireChangeValueEvent(event);
  }

  @Override
  public boolean isChanged() {
    if (myInitialValue == null) {
      return myValue != null;
    }
    return !myInitialValue.equals(myValue);
  }

  @Override
  public void lock() {
  }

  @Override
  public void commit() {
  }

  @Override
  public void rollback() {
  }

  @Override
  public Runnable addChangeValueListener(final ChangeValueListener listener) {
    return myListeners.add(listener, Listeners.DEFAULT_PRIORITY);
  }

  @Override
  public Runnable addChangeValueListener(final ChangeValueListener listener, int priority) {
    return myListeners.add(listener, priority);
  }

  protected void fireChangeValueEvent(ChangeValueEvent event) {
    myListeners.fire(event);
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    myPropertyChangeSupport.addPropertyChangeListener(listener);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    myPropertyChangeSupport.removePropertyChangeListener(listener);
  }

  @Override
  public boolean isWritable() {
    return isWritable;
  }

  public void setWritable(boolean isWritable) {
    this.isWritable = isWritable;
    myPropertyChangeSupport.firePropertyChange("isWritable", Boolean.valueOf(!isWritable), Boolean.valueOf(isWritable));
  }

  @Override
  public boolean isScreened() {
    return isScreened;
  }

  @Override
  public void setScreened(boolean value) {
    isScreened = value;
  }

  public boolean hasUi() {
    return myHasUi;
  }

  public void setHasUi(boolean hasUi) {
    myHasUi = hasUi;
  }

  protected PropertyChangeSupport getPropertyChangeSupport() {
    return myPropertyChangeSupport;
  }

  protected static String i18n(String key) {
    return I18N.ourInstance.i18n(key);
  }

  static class Listeners {
    public static final int DEFAULT_PRIORITY = 0;

    class Entry implements Comparable {
      final int priority;
      final int ordinal;
      final ChangeValueListener listener;

      Entry(ChangeValueListener listener, int ordinal, int priority) {
        this.listener = Preconditions.checkNotNull(listener);
        this.ordinal = ordinal;
        this.priority = priority;
      }
      @Override
      public int compareTo(Object o) {
        Preconditions.checkArgument(o instanceof Entry);
        Entry that = (Entry) o;
        int result = this.priority - that.priority;
        return result != 0 ? result : this.ordinal - that.ordinal;
      }
    }
    private SortedSet<Entry> myListeners = Sets.newTreeSet();
    Runnable add(ChangeValueListener listener, int priority) {
      final Entry e = new Entry(listener, myListeners.size(), priority);
      myListeners.add(e);
      return new Runnable() {
        @Override
        public void run() {
          myListeners.remove(e);
        }
      };
    }

    void fire(ChangeValueEvent event) {
      for (Entry e : myListeners) {
        e.listener.changeValue(event);
      }
    }
  }
}