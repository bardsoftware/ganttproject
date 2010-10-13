package net.sourceforge.ganttproject.gui.options.model;

import java.awt.Color;

import net.sourceforge.ganttproject.util.ColorConvertion;

public class DefaultColorOption extends GPAbstractOption<Color> implements ColorOption {
    private Color myLockedValue;

    // TODO GPAbstractOption also contains a myValue, are those the same?? (If so they should be merged and made protected)
    private Color myValue;

    public DefaultColorOption(String id) {
        super(id);
    }

    public Color getValue() {
        return myValue;
    }

    public void setValue(Color value) {
        myLockedValue = value;
    }

    public void commit() {
        super.commit();
        myValue = myLockedValue;
    }

    public String getPersistentValue() {
        return getValue()==null ? null : ColorConvertion.getColor(getValue());
    }

    public void loadPersistentValue(String value) {
        if (value!=null) {
            myLockedValue = ColorConvertion.determineColor(value);
        }
    }
}
