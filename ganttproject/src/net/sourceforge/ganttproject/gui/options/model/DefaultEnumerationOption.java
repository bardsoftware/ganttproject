/*
 * Created on 18.06.2005
 */
package net.sourceforge.ganttproject.gui.options.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultEnumerationOption extends GPAbstractOption<String>
implements EnumerationOption, ChangeValueDispatcher {
    private final String[] myValues;
    private final Map<String, Object> myStringValue_ObjectValue = new HashMap<String, Object>();
    
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
        fillStringObjectValueMapping(values);
    }

    private void fillStringObjectValueMapping(Object[] values) {
        assert myValues.length == values.length;
        for (int i = 0; i < values.length; i++) {
            myStringValue_ObjectValue.put(myValues[i], values[i]);
        }
    }
    protected String objectToString(Object nextValue) {
        assert nextValue!=null;
        return nextValue.toString();
    }
    protected Object stringToObject(String value) {
        if (myStringValue_ObjectValue.isEmpty()) {
            return value;
        }
        return myStringValue_ObjectValue.get(value);
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