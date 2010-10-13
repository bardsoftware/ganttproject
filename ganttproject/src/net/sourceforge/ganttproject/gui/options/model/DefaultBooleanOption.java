package net.sourceforge.ganttproject.gui.options.model;

public class DefaultBooleanOption extends GPAbstractOption<Boolean> implements
        BooleanOption {

    // TODO GPAbstractOption also contains a myValue, are those the same?? (If so they should be merged and made protected)
    private boolean myValue;

    private boolean myLockedValue;

    public DefaultBooleanOption(String id) {
        super(id);
    }

    public boolean isChecked() {
        return myValue;
    }

    public void toggle() {
        ChangeValueEvent event = new ChangeValueEvent(
                getID(), Boolean.valueOf(myLockedValue), Boolean.valueOf(!myLockedValue));
        fireChangeValueEvent(event);
        myLockedValue = !myLockedValue;
    }

    public void lock() {
        super.lock();
        myLockedValue = myValue;
    }

    public void commit() {
        super.commit();
        myValue = myLockedValue;
    }

    public String getPersistentValue() {
        return Boolean.toString(isChecked());
    }

    public void loadPersistentValue(String value) {
        myLockedValue = Boolean.valueOf(value).booleanValue();
    }

}
