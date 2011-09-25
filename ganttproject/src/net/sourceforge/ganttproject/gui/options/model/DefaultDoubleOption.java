package net.sourceforge.ganttproject.gui.options.model;

public class DefaultDoubleOption extends GPAbstractOption<Double> implements DoubleOption {
    public DefaultDoubleOption(String id) {
        this(id, 0.0);
    }

    public DefaultDoubleOption(String id, Double initialValue) {
        super(id, initialValue);
    }

    @Override
    public String getPersistentValue() {
        return getValue() == null ? "" : getValue().toString();
    }

    @Override
    public void loadPersistentValue(String value) {
        try {
            setValue(Double.parseDouble(value), true);
        }
        catch (NumberFormatException e) {
            setValue(null, true);
        }
    }
}
