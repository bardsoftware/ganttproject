/*
GanttProject is an opensource project management tool.
Copyright (C) 2005- 2011 Benoit Barrane, GanttProject Team

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
package net.sourceforge.ganttproject.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyHolder;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttCalendar;

/**
 * This class handles the custom columns for one single task. It associate a
 * customColumn name with a value. The name of the custom column has to exist in
 * the CustomColumnsStorage otherwise an exception will be thrown. The type
 * (Class) of the given object as value has to match the class in the
 * CustomColumnsManager.
 *
 * @author bbaranne
 */
public class CustomColumnsValues implements CustomPropertyHolder, Cloneable {
    /**
     * CustomColumnName(String) -> Value (Object)
     */
    private final Map<String, Object> mapCustomColumnValue = new HashMap<String, Object>();
    private final CustomColumnsStorage myColumnStorage;
    private final CustomColumnsManager myManager;

    /**
     * Creates an instance of CustomColumnsValues.
     */
    public CustomColumnsValues(CustomColumnsStorage columnStorage) {
        myColumnStorage = columnStorage;
        myManager = new CustomColumnsManager(myColumnStorage);
    }

    /**
     * Set the value for the customColumn whose name is given.
     *
     * @param customColName
     *            The name of the CustomColumn.
     * @param value
     *            The associated value.
     * @throws CustomColumnsException
     *             Throws if <code>customColName</code> does not exist or
     *             <code>value</code> class does not match the CustomColum
     *             class.
     */
    public void setValue(String customColName, Object value)
            throws CustomColumnsException {
        if (!myColumnStorage.exists(customColName)) {
            throw new CustomColumnsException(
                    CustomColumnsException.DO_NOT_EXIST, customColName);
        }

        if (value == null) {
            mapCustomColumnValue.remove(customColName);
            return;
        }
        Class<?> c1 = myColumnStorage.getCustomColumn(customColName).getType();
        Class<?> c2 = value.getClass();
        if (!c1.isAssignableFrom(c2)) {
            throw new CustomColumnsException(
                    CustomColumnsException.CLASS_MISMATCH,
                    "Failed to set value="+value+". value class="+c2+", column class="+c1);
        } else {
            mapCustomColumnValue.put(customColName, value);
        }
    }

    /**
     * @param customColName
     *            The name of the custom column we want to get the value.
     * @return The value for the given customColName.
     */
    public Object getValue(String customColName) {
        return mapCustomColumnValue.get(customColName);
    }

    /**
     * Remove the custom column (and also its value) from this
     * CustomColumnValues.
     *
     * @param colName
     *            Name of the column to remove.
     */
    public void removeCustomColumn(String colName) {
        mapCustomColumnValue.remove(colName);
    }

    public void renameCustomColumn(String oldName, String newName) {
        Object o = mapCustomColumnValue.get(oldName);
        mapCustomColumnValue.put(newName, o);
        mapCustomColumnValue.remove(oldName);
    }

    public Object clone() {
        CustomColumnsValues res = new CustomColumnsValues(myColumnStorage);
        res.mapCustomColumnValue.putAll(this.mapCustomColumnValue);
        return res;
    }

    public String toString() {
        return mapCustomColumnValue.toString();
    }

    @Override
    public List<CustomProperty> getCustomProperties() {
        List<CustomProperty> result = new ArrayList<CustomProperty>(mapCustomColumnValue.size());
        for (Entry<String, Object> entry : mapCustomColumnValue.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            CustomPropertyDefinition def = getCustomPropertyDefinition(myManager, name);
            if (def != null) {
                result.add(new CustomPropertyImpl(def, value));
            }
        }
        return result;
    }

    private static CustomPropertyDefinition getCustomPropertyDefinition(CustomPropertyManager manager, String name) {
        for (CustomPropertyDefinition def : manager.getDefinitions()) {
            if (name.equals(def.getName())) {
                return def;
            }
        }
        return null;
    }

    @Override
    public CustomProperty addCustomProperty(CustomPropertyDefinition definition, String defaultValueAsString) {
        // TODO Auto-generated method stub
        return null;
    }

    private static class CustomPropertyImpl implements CustomProperty {
        private CustomPropertyDefinition myDefinition;
        private Object myValue;

        public CustomPropertyImpl(CustomPropertyDefinition definition,
                Object value) {
            myDefinition = definition;
            myValue = value;
        }

        public CustomPropertyDefinition getDefinition() {
            return myDefinition;
        }

        public Object getValue() {
            return myValue;
        }

        public String getValueAsString() {
            return CustomColumnsValues.getValueAsString(myValue);
        }
    }

    static String getValueAsString(Object value) {
        String result = null;
        if (value != null) {
            if (value instanceof GanttCalendar) {
                result = ((GanttCalendar)value).toXMLString();
            }
            else {
                result = String.valueOf(value);
            }
        }
        return result;
    }
}
