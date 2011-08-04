package net.sourceforge.ganttproject.gui.options.model;

public class DefaultStringOption extends GPAbstractOption<String> implements StringOption {
    public DefaultStringOption(String id) {
        super(id);
    }
    public DefaultStringOption(String id, String initialValue) {
        super(id, initialValue);
    }

    public String getPersistentValue() {
        return getValue();
    }

    public void loadPersistentValue(String value) {
        setValue(value);
    }
}
