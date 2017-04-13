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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import net.sourceforge.ganttproject.CustomPropertyClass;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.DefaultCustomPropertyDefinition;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class CustomColumn implements CustomPropertyDefinition {
  private String id = null;

  private String name = null;

  private Object defaultValue = null;

  private final CustomColumnsManager myManager;

  private CustomPropertyClass myPropertyClass;
  private final Map<String, String> myAttributes = new HashMap<>();

  CustomColumn(CustomColumnsManager manager, String colName, CustomPropertyClass propertyClass, Object colDefaultValue) {
    name = colName;
    myPropertyClass = propertyClass;
    defaultValue = colDefaultValue;
    myManager = manager;
  }

  public String getId() {
    return id;
  }

  public void setId(String newId) {
    id = newId;
  }

  @Override
  public Object getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public void setDefaultValueAsString(String value) {
    CustomPropertyDefinition stub = CustomPropertyManager.PropertyTypeEncoder.decodeTypeAndDefaultValue(
        getTypeAsString(), value);
    defaultValue = stub.getDefaultValue();
  }

  @Nonnull
  @Override
  public Map<String, String> getAttributes() {
    return myAttributes;
  }

  @Nonnull
  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    String oldName = this.name;
    this.name = name;
    myManager.fireDefinitionChanged(this, oldName);
  }

  @Nonnull
  @Override
  public CustomPropertyClass getPropertyClass() {
    return myPropertyClass;
  }

  @Override
  public Class<?> getType() {
    return myPropertyClass.getJavaClass();
  }

  @Override
  public String toString() {
    return this.name + " [" + getType() + "] <" + this.defaultValue + ">";
  }

  @Override
  public String getDefaultValueAsString() {
    return this.defaultValue == null ? null : this.defaultValue.toString();
  }

  @Override
  public String getID() {
    return getId();
  }

  @Override
  public String getTypeAsString() {
    return CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(getType());
  }

  @Override
  public IStatus canSetPropertyClass(CustomPropertyClass propertyClass) {
    return Status.OK_STATUS;
  }

  @Override
  public IStatus setPropertyClass(CustomPropertyClass propertyClass) {
    CustomPropertyDefinition oldValue = new DefaultCustomPropertyDefinition(name, id, this);
    myPropertyClass = propertyClass;
    String defaultValue = getDefaultValueAsString();
    if (defaultValue == null) {
      defaultValue = propertyClass.getDefaultValueAsString();
    }
    setDefaultValueAsString(defaultValue);
    myManager.fireDefinitionChanged(CustomPropertyEvent.EVENT_TYPE_CHANGE, this, oldValue);
    return Status.OK_STATUS;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof CustomColumn == false) {
      return false;
    }
    CustomColumn that = (CustomColumn) obj;
    return this.id.equals(that.id);
  }
}
