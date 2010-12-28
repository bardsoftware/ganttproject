/*
 * Created on 18.06.2005
 */
package net.sourceforge.ganttproject.gui.options.model;

import java.util.ArrayList;
import java.util.List;

public class DefaultEnumerationOption extends GPAbstractOption<String>
implements EnumerationOption, ChangeValueDispatcher {
    private final String[] myValues;

    public DefaultEnumerationOption(String id, String[] values) {
        super(id);
        myValues = values;
    }

    public DefaultEnumerationOption(String id, List<String> values) {
        super(id);
        myValues = values.toArray(new String[0]);
    }

    public DefaultEnumerationOption(String id, Object[] values) {
        super(id);
        List<String> buf = new ArrayList<String>();
        for (Object nextValue : values) {
            buf.add(objectToString(nextValue));
        }
        myValues = buf.toArray(new String[0]);
    }

    protected String objectToString(Object nextValue) {
        assert nextValue!=null;
        return nextValue.toString();
    }

    public String[] getAvailableValues() {
        return myValues;
    }

    public String getPersistentValue() {
        return getValue();
    }

    public void loadPersistentValue(String value) {
        setValue(value);
    }
}