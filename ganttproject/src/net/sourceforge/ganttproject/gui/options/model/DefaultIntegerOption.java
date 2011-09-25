package net.sourceforge.ganttproject.gui.options.model;

public class DefaultIntegerOption extends GPAbstractOption<Integer> implements IntegerOption {
    public DefaultIntegerOption(String id) {
        this(id, 0);
    }

    public DefaultIntegerOption(String id, Integer initialValue) {
        super(id, initialValue);
    }

    public String getPersistentValue() {
        int value = getValue();
        return String.valueOf(value);
    }

    public void loadPersistentValue(String value) {
        int intValue = Integer.parseInt(value);
        setValue(intValue, true);
    }
}