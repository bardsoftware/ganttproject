/*
 * Created on 18.06.2005
 */
package net.sourceforge.ganttproject.gui.options.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DefaultEnumerationOption extends GPAbstractOption implements
        EnumerationOption, ChangeValueDispatcher {


    private final String[] myValues;

    private String myValue;

    private String myLockedValue;

    public DefaultEnumerationOption(String id, String[] values) {
        super(id);
        myValues = values; 
    }

    public DefaultEnumerationOption(String id, List values) {
        super(id);
        myValues = (String[]) values.toArray(new String[0]);
    }

    public String[] getAvailableValues() {
        return myValues;
    }

    public void setValue(String value) {
        if (!isLocked()) {
            throw new IllegalStateException("Lock option before setting value");
        }

        ChangeValueEvent event = new ChangeValueEvent(getID(), myLockedValue,
                value);
        myLockedValue = value;
        fireChangeValueEvent(event);
    }

    public String getValue() {
        return myValue;
    }

    public void commit() {
        super.commit();
        myValue = myLockedValue;
    }


    public String getPersistentValue() {
        return getValue();
    }

    public void loadPersistentValue(String value) {
        setValue(value);
    }    

    public boolean isChanged() {
        if (isLocked()) {
            if (myValue!=null) {
                return false==myValue.equals(myLockedValue);
            }
        }
        return false;
    }

}
