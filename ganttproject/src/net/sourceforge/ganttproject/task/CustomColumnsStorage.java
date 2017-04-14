/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 Benoit Baranne, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
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

import com.google.common.base.Objects;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyListener;
import net.sourceforge.ganttproject.DefaultCustomPropertyDefinition;
import net.sourceforge.ganttproject.language.GanttLanguage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
  private final Map<String, CustomColumn> mapIdCustomColum = new LinkedHashMap<String, CustomColumn>();

  private final CustomColumnsManager myManager;

  CustomColumnsStorage(CustomColumnsManager manager) {
    myManager = manager;
  }

  String createId() {
    while (true) {
      String id = ID_PREFIX + nextId++;
      if (!mapIdCustomColum.containsKey(id)) {
        return id;
      }
    }
  }

  public void reset() {
    mapIdCustomColum.clear();
    nextId = 0;
  }

  public static void changeLanguage(GanttLanguage lang) {
    language = lang;
  }

  public void addCustomColumn(CustomColumn col) {
    assert !mapIdCustomColum.containsKey(col.getID()) : "column with id =" + col.getID()
        + " already exists. All existing columns:\n" + getCustomColums();
    mapIdCustomColum.put(col.getID(), col);
    CustomPropertyEvent event = new CustomPropertyEvent(CustomPropertyEvent.EVENT_ADD, col);
    fireCustomColumnsChange(event);
  }

  public void removeCustomColumn(CustomPropertyDefinition column) {
    CustomPropertyEvent event = new CustomPropertyEvent(CustomPropertyEvent.EVENT_REMOVE, column);
    fireCustomColumnsChange(event);
    mapIdCustomColum.remove(column.getID());
  }

  public int getCustomColumnCount() {
    return mapIdCustomColum.size();
  }

  public Collection<CustomColumn> getCustomColums() {
    return mapIdCustomColum.values();
  }

  public CustomColumn getCustomColumnByID(String id) {
    return mapIdCustomColum.get(id);
  }

  @Override
  public String toString() {
    return mapIdCustomColum.toString();
  }

  public Map<CustomPropertyDefinition, CustomPropertyDefinition> importData(CustomColumnsStorage source) {
    Map<CustomPropertyDefinition, CustomPropertyDefinition> result = new HashMap<CustomPropertyDefinition, CustomPropertyDefinition>();
    for (CustomColumn thatColumn : source.getCustomColums()) {
      CustomColumn thisColumn = findByName(thatColumn.getName());
      if (thisColumn == null || !thisColumn.getPropertyClass().equals(thatColumn.getPropertyClass())) {
        thisColumn = new CustomColumn(myManager, thatColumn.getName(), thatColumn.getPropertyClass(), thatColumn.getDefaultValue());
        thisColumn.setId(createId());
        thisColumn.getAttributes().putAll(thatColumn.getAttributes());
        addCustomColumn(thisColumn);
      }
      result.put(thatColumn, thisColumn);
    }
    return result;
  }

  private CustomColumn findByName(String name) {
    for (CustomColumn cc : mapIdCustomColum.values()) {
      if (Objects.equal(cc.getName(), name)) {
        return cc;
      }
    }
    return null;
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
