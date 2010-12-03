package net.sourceforge.ganttproject.task;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class handles the custom columns for one single task. It associate a
 * customColumn name with a value. The name of the custom column has to exist in
 * the CustomColumnsStorage otherwise an exception will be thrown. The type
 * (Class) of the given object as value has to match the class in the
 * CustomColumnsManager.
 *
 * @author bbaranne Mar 2, 2005
 */
public class CustomColumnsValues implements Cloneable {
    /**
     * CustomColumnName(String) -> Value (Object)
     */
    private final Map<Object, Object> mapCustomColumnValue = new HashMap<Object, Object>();
	private final CustomColumnsStorage myColumnStorage;

    /**
     * Creates an instance of CustomColumnsValues.
     */
    public CustomColumnsValues(CustomColumnsStorage columnStorage) {
        myColumnStorage = columnStorage;
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
        if (!myColumnStorage.exists(customColName))
            throw new CustomColumnsException(
                    CustomColumnsException.DO_NOT_EXIST, customColName);

        if (value == null) {
        	mapCustomColumnValue.remove(customColName);
        	return;
        }
        Class c1 = myColumnStorage.getCustomColumn(customColName).getType();
        Class c2 = value.getClass();
        // System.out.println(c1 +" - " + c2);
        if (!c1.isAssignableFrom(c2)) {
            throw new CustomColumnsException(
                    CustomColumnsException.CLASS_MISMATCH,
                    "Failed to set value="+value+". value class="+c2+", column class="+c1);
        } else {
            mapCustomColumnValue.put(customColName, value);
        }
    }

    /**
     * Returns the value for the given customColName.
     *
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
        Iterator<Object> it = mapCustomColumnValue.keySet().iterator();
        while (it.hasNext()) {
            Object k = it.next();
            res.mapCustomColumnValue.put(k, mapCustomColumnValue.get(k));
        }
        return res;
    }

    public String toString() {
        return mapCustomColumnValue.toString();
    }

}
