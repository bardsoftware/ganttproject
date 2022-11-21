/*
 * Created on 02.04.2005
 */
package biz.ganttproject.core.option;

import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeListener;

/**
 * @author bard
 */
public interface GPOption<T> {
  T getValue();

  void setValue(T value);
  void setValue(T value, Object clientId);

  String getID();

  void lock();

  void commit();

  void rollback();

  String getPersistentValue();

  void loadPersistentValue(String value);

  boolean isChanged();

  Runnable addChangeValueListener(ChangeValueListener listener);

  Runnable addChangeValueListener(ChangeValueListener listener, int priority);

  ObservableProperty<Boolean> getIsWritableProperty();
  boolean isWritable();

  void addPropertyChangeListener(PropertyChangeListener listener);

  void removePropertyChangeListener(PropertyChangeListener listener);

  boolean isScreened();

  void setScreened(boolean isScreened);

  boolean hasUi();

  void setHasUi(boolean hasUi);

  @Nullable ValueValidator<T> getValidator();

  void setValidator(ValueValidator<T> validator);
}
