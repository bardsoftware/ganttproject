/*
 * Created on 02.04.2005
 */
package biz.ganttproject.core.option;

import java.beans.PropertyChangeListener;

/**
 * @author bard
 */
public interface GPOption<T> {
  T getValue();

  void setValue(T value);

  String getID();

  void lock();

  void commit();

  void rollback();

  String getPersistentValue();

  void loadPersistentValue(String value);

  boolean isChanged();

  void addChangeValueListener(ChangeValueListener listener);

  boolean isWritable();

  void addPropertyChangeListener(PropertyChangeListener listener);

  void removePropertyChangeListener(PropertyChangeListener listener);

  boolean isScreened();

  void setScreened(boolean isScreened);
}
