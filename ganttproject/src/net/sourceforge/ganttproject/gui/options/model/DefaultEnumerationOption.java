/*
 * Created on 18.06.2005
 */
package net.sourceforge.ganttproject.gui.options.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultEnumerationOption<T> extends GPAbstractOption<String>
implements EnumerationOption, ChangeValueDispatcher {
    private final String[] myValues;
    private final Map<String, T> myStringValue_ObjectValue = new HashMap<String, T>();

    public DefaultEnumerationOption(String id, String[] values) {
        super(id);
        myValues = values;
    }

    public DefaultEnumerationOption(String id, List<String> values) {
        super(id);
        myValues = values.toArray(new String[0]);
    }

    public DefaultEnumerationOption(String id, T[] values) {
        super(id);
        List<String> buf = new ArrayList<String>();
        for (T nextValue : values) {
            buf.add(objectToString(nextValue));
        }
        myValues = buf.toArray(new String[0]);
        fillStringObjectValueMapping(values);
    }

    private void fillStringObjectValueMapping(T[] values) {
        assert myValues.length == values.length;
        for (int i = 0; i < values.length; i++) {
            myStringValue_ObjectValue.put(myValues[i], values[i]);
        }
    }
    protected String objectToString(T obj) {
        assert obj!=null;
        return obj.toString();
    }
    protected T stringToObject(String value) {
        if (myStringValue_ObjectValue.isEmpty()) {
            return null;
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