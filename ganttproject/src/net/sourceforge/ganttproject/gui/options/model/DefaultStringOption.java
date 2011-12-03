package net.sourceforge.ganttproject.gui.options.model;

public class DefaultStringOption extends GPAbstractOption<String> implements StringOption {
    public DefaultStringOption(String id) {
        super(id);
    }
    public DefaultStringOption(String id, String initialValue) {
        super(id, initialValue);
    }

    @Override
    public String getPersistentValue() {
        return getValue();
    }

    @Override
    public void loadPersistentValue(String value) {
        setValue(value);
    }
}
