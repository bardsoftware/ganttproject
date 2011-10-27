/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.gui.options.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultEnumerationOption<T> extends GPAbstractOption<String>
implements EnumerationOption {
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
        assert obj != null;
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

    public T getSelectedValue() {
        return stringToObject(getValue());
    }
    public void setSelectedValue(T value) {
        setValue(objectToString(value));
    }
}