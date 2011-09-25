/*
 * Created on 02.04.2005
 */
package net.sourceforge.ganttproject.gui.options.model;

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
}
