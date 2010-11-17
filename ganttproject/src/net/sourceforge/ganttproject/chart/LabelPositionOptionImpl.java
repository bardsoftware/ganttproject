/*
 * Created on 02.04.2005
 */
package net.sourceforge.ganttproject.chart;

import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPAbstractOption;

/**
 * @author bard
 */
abstract class LabelPositionOptionImpl extends GPAbstractOption<String> implements
        EnumerationOption {
    static final String OFF = "off";

    static final String UP = "up";

    static final String DOWN = "down";

    static final String LEFT = "left";

    static final String RIGHT = "right";

    static final String[] VALUES = new String[] { OFF, UP, DOWN, LEFT, RIGHT };

    private String myValue;

    private String myLockedValue;

    LabelPositionOptionImpl(String id) {
        super(id);
        myValue = VALUES[0];
    }

    public String[] getAvailableValues() {
        return VALUES;
    }

    public void setValue(String value) {
        myLockedValue = value;
    }

    public String getValue() {
        return myValue;
    }

    public void commit() {
        super.commit();
        myValue = myLockedValue;
    }

}
