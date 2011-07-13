package net.sourceforge.ganttproject.gui.options.model;

public class DefaultStringOption extends GPAbstractOption<String> implements StringOption {

    private String myLockedValue;

    // TODO GPAbstractOption also contains a myValue, are those the same?? (If so they should be merged and made protected)
    private String myValue;

    public DefaultStringOption(String id) {
        this(id, null);
    }

    public DefaultStringOption(String id, String initialValue) {
        super(id, initialValue);
    }

    public void setValue(String value) {
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
}
