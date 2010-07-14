/*
 * Created on 02.04.2005
 */
package net.sourceforge.ganttproject.gui.options.model;

/**
 * @author bard
 */
public interface GPOption {
    String getID();

    void lock();

    void commit();

    void rollback();
    
    String getPersistentValue();
    
    void loadPersistentValue(String value);
    
    boolean isChanged();
}
