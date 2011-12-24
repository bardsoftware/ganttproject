/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 Benoit Baranne, GanttProject Team

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
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyListener;
import net.sourceforge.ganttproject.DefaultCustomPropertyDefinition;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.DateUtils;

/**
 * TODO Remove the map Name->customColum to keep only the map Id -> CustomColumn
 * This class stores the CustomColumns.
 *
 * @author bbaranne (Benoit Baranne) Mar 2, 2005
 */
public class CustomColumnsStorage {
    public static GanttLanguage language = GanttLanguage.getInstance();

    private static int nextId;

    private final static String ID_PREFIX = "tpc";
    private final List<CustomPropertyListener> myListeners = new ArrayList<CustomPropertyListener>();

    /**
     * Column name (String) -> CustomColumn
     */
    // private Map customColumns = null;
    private final Map<String, CustomColumn> mapIdCustomColum = new HashMap<String, CustomColumn>();

    /**
     * Creates an instance of CustomColumnsStorage.
     */
    public CustomColumnsStorage() {
    }

    public void reset() {
        mapIdCustomColum.clear();
        nextId = 0;
    }

    // public CustomColumnsStorage(Map customCols)
    // {
    // customColumns = customCols;
    // }

    /**
     * Initialize the available types (text, integer, ...)
     */
    /**
     * Changes the language of the available types.
     *
     * @param lang
     *            New language.
     */
    public static void changeLanguage(GanttLanguage lang) {
        language = lang;
    }

    /**
     * Add a custom column.
     *
     * @param col
     *            CustomColumn to be added.
     * @throws CustomColumnsException
     *             A CustomColumnsException of type
     *             CustomColumnsException.ALREADY_EXIST could be thrown if the
     *             CustomColumn that should be added has a name that already
     *             exists.
     */
    public void addCustomColumn(CustomColumn col) {
        assert !getCustomColumnsNames().contains(col.getName()) :
            "column name=" + col.getName() + " already exists:\n" + getCustomColumnsNames();
        String id = col.getId();
        while (id == null) {
            id = ID_PREFIX + nextId++;
            if (existsForID(id))
                id = null;
        }
        mapIdCustomColum.put(id, col);
        col.setId(id);
        CustomPropertyEvent event = new CustomPropertyEvent(CustomPropertyEvent.EVENT_ADD, col);
        fireCustomColumnsChange(event);
    }

    void removeCustomColumn(String name) {
        CustomColumn column = getCustomColumn(name);
        if (column!=null) {
            removeCustomColumn(column);
        }
    }
    /**
     * Removes the CustomColumn whose name is given in parameter. If the column
     * name does not exist, nothing is done.
     *
     * @param name
     *            Name of the column to remove.
     */
    public void removeCustomColumn(CustomPropertyDefinition column) {
        CustomPropertyEvent event = new CustomPropertyEvent(CustomPropertyEvent.EVENT_REMOVE, column);
        fireCustomColumnsChange(event);
        mapIdCustomColum.remove(column.getID());
    }

    /**
     * Returns the number of custom columns.
     *
     * @return The number of custom columns.
     */
    public int getCustomColumnCount() {
        // return customColumns.size();
        return mapIdCustomColum.size();
    }

    /**
     * Returns a collection containing the names of all the stored custom
     * columns.
     *
     * @return A collection containing the names of all the stored custom
     *         columns.
     */
    public List<String> getCustomColumnsNames() {
        // return customColumns.keySet();
        // -----
        List<String> c = new ArrayList<String>();
        Iterator<String> it = mapIdCustomColum.keySet().iterator();
        while (it.hasNext()) {
            String id = it.next();
            c.add(((CustomColumn) mapIdCustomColum.get(id)).getName());
        }
        return c;
    }

    /**
     * Returns a collection with all the stored custom columns.
     *
     * @return A collection with all the stored custom columns.
     */
    public Collection<CustomColumn> getCustomColums() {
        // return customColumns.values();
        return mapIdCustomColum.values();
    }

    /**
     * @param name
     * @return
     * @throws CustomColumnsException
     */
    public CustomColumn getCustomColumn(String name) {
        // if (!exists(name))
        // throw new CustomColumnsException(CustomColumnsException.DO_NOT_EXIST,
        // name);
        // return (CustomColumn) customColumns.get(name);
        String id = getIdFromName(name);
        if (id == null) {
            return null;
        }
        return (CustomColumn) mapIdCustomColum.get(id);
    }

    public CustomColumn getCustomColumnByID(String id) {
        return (CustomColumn) mapIdCustomColum.get(id);
    }

    /**
     * Returns true if a custom column has the same name that the one given in
     * parameter, false otherwise.
     *
     * @param colName
     *            The name of the column to check the existence.
     * @return True if a custom column has the same name that the one given in
     *         parameter, false otherwise.
     */
    public boolean exists(String colName) {
        return getIdFromName(colName) != null;
    }

    public boolean existsForID(String id) {
        return this.mapIdCustomColum.keySet().contains(id);
    }

    public void renameCustomColumn(String name, String newName) {
        // if (customColumns.containsKey(name))
        // {
        // CustomColumn cc = (CustomColumn) customColumns.get(name);
        // cc.setName(newName);
        // customColumns.put(newName, cc);
        // customColumns.remove(name);
        // ((CustomColumn) mapIdCustomColum.get(cc.getId())).setName(newName);
        // }
        String id = getIdFromName(name);
        if (id != null) {
            CustomColumn cc = (CustomColumn) mapIdCustomColum.get(id);
            cc.setName(newName);
        }

    }

    public void changeDefaultValue(String colName, Object newDefaultValue)
            throws CustomColumnsException {
        // if (customColumns.containsKey(colName))
        // {
        // CustomColumn cc = (CustomColumn) customColumns.get(colName);
        // cc.setDefaultValue(newDefaultValue);
        // ((CustomColumn)
        // mapIdCustomColum.get(cc.getId())).setDefaultValue(newDefaultValue);
        // }
        String id = getIdFromName(colName);
        if (id != null) {
            CustomColumn cc = (CustomColumn) mapIdCustomColum.get(id);

            if (newDefaultValue.getClass().isAssignableFrom(cc.getType()))
                cc.setDefaultValue(newDefaultValue);
            else {
                try {
                    if (cc.getType().equals(Boolean.class))
                        cc.setDefaultValue(Boolean.valueOf(newDefaultValue
                                .toString()));
                    else if (cc.getType().equals(Integer.class))
                        cc.setDefaultValue(Integer.valueOf(newDefaultValue
                                .toString()));
                    else if (cc.getType().equals(Double.class))
                        cc.setDefaultValue(Double.valueOf(newDefaultValue
                                .toString()));
                    else if (GregorianCalendar.class.isAssignableFrom(cc
                            .getType()))
                        cc.setDefaultValue(DateUtils.parseDate(newDefaultValue
                                .toString()));

                } catch (Exception ee) {
                    throw new CustomColumnsException(
                            CustomColumnsException.CLASS_MISMATCH,
                            "Try to set a '" + newDefaultValue.getClass()
                                    + "' for '" + cc.getType() + "'");
                }

            }
        }
    }

    public String getIdFromName(String name) {
        String id = null;
        Iterator<CustomColumn> it = mapIdCustomColum.values().iterator();
        while (it.hasNext()) {
            CustomColumn cc = (CustomColumn) it.next();
            if (cc.getName().equals(name)) {
                id = cc.getId();
                break;
            }
        }
        return id;
    }

    public String getNameFromId(String id) {
        CustomColumn column = (CustomColumn) mapIdCustomColum.get(id);
        return column==null ? null : column.getName();
    }

    @Override
    public String toString() {
        return mapIdCustomColum.toString();
    }

    public void importData(CustomColumnsStorage source) {
        for (Iterator<CustomColumn> columns = source.getCustomColums().iterator();
             columns.hasNext();) {
            CustomColumn nextColumn = columns.next();
            if (!exists(nextColumn.getName())) {
                addCustomColumn(nextColumn);
            }
        }
    }

    public void addCustomColumnsListener(CustomPropertyListener listener) {
        myListeners.add(listener);
    }

    private void fireCustomColumnsChange(CustomPropertyEvent event) {
        Iterator<CustomPropertyListener> it = myListeners.iterator();
        while (it.hasNext()) {
            CustomPropertyListener listener = it.next();
            listener.customPropertyChange(event);
        }
    }

    void fireDefinitionChanged(int event, CustomPropertyDefinition def, CustomPropertyDefinition oldDef) {
        CustomPropertyEvent e = new CustomPropertyEvent(event, def, oldDef);
        fireCustomColumnsChange(e);
    }

    void fireDefinitionChanged(CustomPropertyDefinition def, String oldName) {
        CustomPropertyDefinition oldDef = new DefaultCustomPropertyDefinition(oldName, def.getID(), def);
        CustomPropertyEvent e = new CustomPropertyEvent(CustomPropertyEvent.EVENT_NAME_CHANGE, def, oldDef);
        fireCustomColumnsChange(e);
    }

}