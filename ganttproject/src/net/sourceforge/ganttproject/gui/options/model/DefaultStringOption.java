package net.sourceforge.ganttproject.gui.options.model;

public class DefaultStringOption extends GPAbstractOption implements StringOption {

    private String myLockedValue;
    private String myValue;
    
    public DefaultStringOption(String id) {
        super(id);
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

    public String getUncommitedValue() {
        return myLockedValue;
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
