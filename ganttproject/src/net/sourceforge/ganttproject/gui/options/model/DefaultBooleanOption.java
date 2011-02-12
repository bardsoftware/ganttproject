package net.sourceforge.ganttproject.gui.options.model;

public class DefaultBooleanOption extends GPAbstractOption<Boolean> implements BooleanOption {

    public DefaultBooleanOption(String id) {
        super(id);
    }

    public DefaultBooleanOption(String id, boolean initialValue) {
        super(id, initialValue);
    }

    public boolean isChecked() {
        return getValue();
    }

    public void toggle() {
        setValue(!getValue());
    }

    public String getPersistentValue() {
        return Boolean.toString(isChecked());
    }

    public void loadPersistentValue(String value) {
        setValue(Boolean.valueOf(value).booleanValue(), true);
    }

}
