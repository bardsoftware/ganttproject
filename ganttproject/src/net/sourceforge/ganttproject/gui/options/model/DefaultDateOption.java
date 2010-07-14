package net.sourceforge.ganttproject.gui.options.model;

import java.util.Date;

public class DefaultDateOption extends GPAbstractOption implements DateOption {

    private Date myLockedValue;
    private Date myValue;
    
    public DefaultDateOption(String id) {
        super(id);
    }

    public void setValue(Date value) {
        if (!isLocked()) {
            throw new IllegalStateException("Lock option before setting value");
        }
        fireChangeValueEvent(new ChangeValueEvent(getID(), myLockedValue, value));
        myLockedValue = value;
    }

    public Date getValue() {
        return myValue;
    }

    public void commit() {
        super.commit();
        myValue = myLockedValue;
    }

    public boolean isChanged() {
        if (isLocked()) {
            if (myValue!=null) {
                return false==myValue.equals(myLockedValue);
            }
        }
        return false;
    }

    public String getPersistentValue() {
        // TODO Auto-generated method stub
        return null;
    }

    public void loadPersistentValue(String value) {
        // TODO Auto-generated method stub
        
    }    
    
}
