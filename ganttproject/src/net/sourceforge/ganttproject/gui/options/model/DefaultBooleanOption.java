package net.sourceforge.ganttproject.gui.options.model;

public class DefaultBooleanOption extends GPAbstractOption<Boolean> implements BooleanOption {

    public DefaultBooleanOption(String id) {
        super(id);
    }

    public DefaultBooleanOption(String id, boolean initialValue) {
        super(id, initialValue);
    }

    @Override
    public boolean isChecked() {
        return getValue();
    }

    @Override
    public Boolean getValue() {
        return super.getValue() == null ? Boolean.FALSE : super.getValue();
    }

    @Override
    public void toggle() {
        setValue(!getValue());
    }

    @Override
    public String getPersistentValue() {
        return Boolean.toString(isChecked());
    }

    @Override
    public void loadPersistentValue(String value) {
        setValue(Boolean.valueOf(value).booleanValue(), true);
    }

}
