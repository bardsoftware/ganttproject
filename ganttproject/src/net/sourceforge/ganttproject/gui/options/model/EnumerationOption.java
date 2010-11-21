/*
 * Created on 02.04.2005
 */
package net.sourceforge.ganttproject.gui.options.model;

/**
 * @author bard
 */
public interface EnumerationOption extends GPOption {
    String[] getAvailableValues();

    void setValue(String value);

    String getValue();
}
