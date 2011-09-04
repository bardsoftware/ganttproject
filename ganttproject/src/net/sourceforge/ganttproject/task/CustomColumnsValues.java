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
 * @author bbaranne Mar 2, 2005
 */
public class CustomColumnsValues implements CustomPropertyHolder, Cloneable {
    /**
     * CustomColumnName(String) -> Value (Object)
     */
    private final Map<String, Object> mapCustomColumnValue = new HashMap<String, Object>();
    private final CustomPropertyManager myManager;

    /**
     * Creates an instance of CustomColumnsValues.
     */
    public CustomColumnsValues(CustomPropertyManager customPropertyManager) {
        myManager = customPropertyManager;
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
        CustomPropertyDefinition def = getCustomPropertyDefinition(myManager, customColName);
        if (def == null) {
            throw new CustomColumnsException(CustomColumnsException.DO_NOT_EXIST, customColName);
        }
        setValue(def, value);
    }

    public void setValue(CustomPropertyDefinition def, Object value) throws CustomColumnsException {
        if (value == null) {
            mapCustomColumnValue.remove(def.getName());
            return;
        }
        Class<?> c1 = def.getType();
        Class<?> c2 = value.getClass();
        if (!c1.isAssignableFrom(c2)) {
            throw new CustomColumnsException(
                    CustomColumnsException.CLASS_MISMATCH,
                    "Failed to set value=" + value + ". value class=" + c2
                            + ", column class=" + c1);
        } else {
            mapCustomColumnValue.put(def.getName(), value);
        }
    }

    /**
     * @param customColName
     *            The name of the custom column we want to get the value.
     * @return The value for the given customColName.
     */
    public Object getValue(String customColName) {
        Object result = mapCustomColumnValue.get(customColName);
        return (result == null) ? tryGetDefaultValue(customColName) : result;
    }

    public boolean hasOwnValue(String propertyName) {
        return mapCustomColumnValue.containsKey(propertyName);
    }

    private Object tryGetDefaultValue(String customColName) {
        CustomPropertyDefinition def = getCustomPropertyDefinition(myManager, customColName);
        return def == null ? null : def.getDefaultValue();
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

    @Override
    public Object clone() {
        CustomColumnsValues res = new CustomColumnsValues(myManager);
        res.mapCustomColumnValue.putAll(this.mapCustomColumnValue);
        return res;
    }

    @Override
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
    public CustomProperty addCustomProperty(CustomPropertyDefinition definition, String valueAsString) {
        CustomPropertyDefinition defStub = CustomPropertyManager.PropertyTypeEncoder.decodeTypeAndDefaultValue(
                definition.getTypeAsString(), valueAsString);
        try {
            setValue(definition, defStub.getDefaultValue());
        } catch (CustomColumnsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new CustomPropertyImpl(definition, defStub.getDefaultValue());
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
