/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyListener;
import net.sourceforge.ganttproject.CustomPropertyManager;

/**
 * This class has to be used to add or remove new custom columns. It will
 * perform the changes to the linked treetable.
 * 
 * @author bbaranne (Benoit Baranne) Mar 4, 2005
 */
public class CustomColumnsManager implements CustomPropertyManager {
  private final CustomColumnsStorage myStorage;

  public CustomColumnsManager() {
    myStorage = new CustomColumnsStorage(this);
  }

  private void addNewCustomColumn(CustomColumn customColumn) {
    assert customColumn != null;
    myStorage.addCustomColumn(customColumn);
  }

  @Override
  public void addListener(CustomPropertyListener listener) {
    myStorage.addCustomColumnsListener(listener);
  }

  @Override
  public List<CustomPropertyDefinition> getDefinitions() {
    return new ArrayList<CustomPropertyDefinition>(myStorage.getCustomColums());
  }

  @Override
  public CustomPropertyDefinition createDefinition(String id, String typeAsString, String name,
      String defaultValueAsString) {
    CustomPropertyDefinition stub = CustomPropertyManager.PropertyTypeEncoder.decodeTypeAndDefaultValue(typeAsString,
        defaultValueAsString);
    CustomColumn result = new CustomColumn(this, name, stub.getPropertyClass(), stub.getDefaultValue());
    result.setId(id);
    addNewCustomColumn(result);
    return result;
  }

  @Override
  public CustomPropertyDefinition createDefinition(String typeAsString, String colName, String defValue) {
    return createDefinition(myStorage.createId(), typeAsString, colName, defValue);
  }

  @Override
  public Map<CustomPropertyDefinition, CustomPropertyDefinition> importData(CustomPropertyManager source) {
    return myStorage.importData(((CustomColumnsManager) source).myStorage);
  }

  @Override
  public CustomPropertyDefinition getCustomPropertyDefinition(String id) {
    return myStorage.getCustomColumnByID(id);
  }

  @Override
  public void deleteDefinition(CustomPropertyDefinition def) {
    myStorage.removeCustomColumn(def);
  }

  void fireDefinitionChanged(int event, CustomPropertyDefinition def, CustomPropertyDefinition oldDef) {
    myStorage.fireDefinitionChanged(event, def, oldDef);
  }

  void fireDefinitionChanged(CustomPropertyDefinition def, String oldName) {
    myStorage.fireDefinitionChanged(def, oldName);
  }

  @Override
  public void reset() {
    myStorage.reset();
  }
}
