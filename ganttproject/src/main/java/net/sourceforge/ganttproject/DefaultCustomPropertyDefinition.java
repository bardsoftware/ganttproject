/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 Dmitry Barashev

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
package net.sourceforge.ganttproject;

import biz.ganttproject.customproperty.CalculationMethod;
import biz.ganttproject.customproperty.CustomPropertyClass;
import biz.ganttproject.customproperty.CustomPropertyDefinition;
import biz.ganttproject.customproperty.PropertyTypeEncoder;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class DefaultCustomPropertyDefinition implements CustomPropertyDefinition {
  private String myName;
  private final String myID;
  private Object myDefaultValue;
  private String myDefaultValueAsString;
  private CustomPropertyClass myPropertyClass;
  private String myTypeAsString;
  private final Map<String, String> myAttributes = new HashMap<>();
  private CalculationMethod calculationMethod;

  public DefaultCustomPropertyDefinition(String name) {
    myName = name;
    myID = null;
    myDefaultValue = null;
    myDefaultValueAsString = null;
    myPropertyClass = CustomPropertyClass.TEXT;
    myTypeAsString = CustomPropertyClass.TEXT.getID();
  }

  public DefaultCustomPropertyDefinition(String name, String id, CustomPropertyDefinition stub) {
    myName = name;
    myID = id;
    myDefaultValue = stub.getDefaultValue();
    myDefaultValueAsString = stub.getDefaultValueAsString();
    myPropertyClass = stub.getPropertyClass();
    myTypeAsString = stub.getTypeAsString();
  }

  @Override
  public Object getDefaultValue() {
    return myDefaultValue;
  }

  @Override
  public String getDefaultValueAsString() {
    return myDefaultValueAsString;
  }

  @Override
  public void setDefaultValueAsString(String value) {
    CustomPropertyDefinition stub = PropertyTypeEncoder.INSTANCE.decodeTypeAndDefaultValue(
        getTypeAsString(), value);
    myDefaultValue = stub.getDefaultValue();
    myDefaultValueAsString = stub.getDefaultValueAsString();
  }

  @Nonnull
  @Override
  public Map<String, String> getAttributes() {
    return myAttributes;
  }

  @Override
  public String getId() {
    return myID;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Override
  public void setName(String name) {
    myName = name;
  }

  @Override
  public Class<?> getType() {
    return myPropertyClass.getJavaClass();
  }

  @Nonnull
  @Override
  public CustomPropertyClass getPropertyClass() {
    return myPropertyClass;
  }

  @Override
  public String getTypeAsString() {
    return myTypeAsString;
  }

    @Override
  public void setPropertyClass(CustomPropertyClass propertyClass) {
    myPropertyClass = propertyClass;
    myTypeAsString = propertyClass.getID();
    setDefaultValueAsString(getDefaultValueAsString());
  }

  @Override
  public int hashCode() {
    return myID==null ? myName.hashCode() : myID.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof DefaultCustomPropertyDefinition == false) {
      return false;
    }
    DefaultCustomPropertyDefinition that = (DefaultCustomPropertyDefinition) obj;
    return this.myID.equals(that.myID);
  }

  @Nullable
  @Override
  public CalculationMethod getCalculationMethod() {
    return this.calculationMethod;
  }

  @Override
  public void setCalculationMethod(@Nullable CalculationMethod value) {
    this.calculationMethod = value;
  }
}
