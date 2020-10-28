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
package net.sourceforge.ganttproject;

import biz.ganttproject.core.time.CalendarFactory;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

public interface CustomPropertyManager {
  List<CustomPropertyDefinition> getDefinitions();

  CustomPropertyDefinition createDefinition(String id, String typeAsString, String name, String defaultValueAsString);

  CustomPropertyDefinition createDefinition(String typeAsString, String colName, String defValue);

  CustomPropertyDefinition getCustomPropertyDefinition(String id);

  void deleteDefinition(CustomPropertyDefinition def);

  Map<CustomPropertyDefinition, CustomPropertyDefinition> importData(CustomPropertyManager source);

  void addListener(CustomPropertyListener listener);

  void reset();

  class PropertyTypeEncoder {
    public static String encodeFieldType(Class<?> fieldType) {
      String result = null;
      if (fieldType.equals(String.class)) {
        result = "text";
      } else if (fieldType.equals(Boolean.class)) {
        result = "boolean";
      } else if (fieldType.equals(Integer.class)) {
        result = "int";
      } else if (fieldType.equals(Double.class)) {
        result = "double";
      } else if (GregorianCalendar.class.isAssignableFrom(fieldType)) {
        result = "date";
      }
      return result;
    }

    public static CustomPropertyDefinition decodeTypeAndDefaultValue(
        final String typeAsString, final String valueAsString) {
      final CustomPropertyClass propertyClass;
      final Object defaultValue;
      if (typeAsString.equals("text")) {
        propertyClass = CustomPropertyClass.TEXT;
        defaultValue = valueAsString == null ? null : valueAsString.toString();
      } else if (typeAsString.equals("boolean")) {
        propertyClass = CustomPropertyClass.BOOLEAN;
        defaultValue = valueAsString == null ? null : Boolean.valueOf(valueAsString);
      } else if (typeAsString.equals("int") || "integer".equals(typeAsString)) {
        propertyClass = CustomPropertyClass.INTEGER;
        Integer intValue;
        try {
          intValue = valueAsString == null ? null : Integer.valueOf(valueAsString);
        } catch (NumberFormatException e) {
          intValue = null;
        }
        defaultValue = intValue;
      } else if (typeAsString.equals("double")) {
        propertyClass = CustomPropertyClass.DOUBLE;
        Double doubleValue;
        try {
          doubleValue = valueAsString == null ? null : Double.valueOf(valueAsString);
        } catch (NumberFormatException e) {
          doubleValue = null;
        }
        defaultValue = doubleValue;
      } else if (typeAsString.equals("date")) {
        propertyClass = CustomPropertyClass.DATE;
        if (StringUtils.isEmptyOrNull(valueAsString)) {
          defaultValue = null;
        } else {
          Date defaultDate = null;
          try {
            defaultDate = DateParser.parse(valueAsString);
          } catch (InvalidDateException e) {
            defaultDate = GanttLanguage.getInstance().parseDate(valueAsString);
          }
          defaultValue = defaultDate == null ? null : CalendarFactory.createGanttCalendar(defaultDate);
        }
      } else {
        propertyClass = CustomPropertyClass.TEXT;
        defaultValue = "";
      }
      return new CustomPropertyDefinition() {
        @Override
        public Object getDefaultValue() {
          return defaultValue;
        }

        @Override
        public String getDefaultValueAsString() {
          return valueAsString;
        }

        @Override
        public void setDefaultValueAsString(String value) {
          throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public Map<String, String> getAttributes() {
          return Collections.emptyMap();
        }

        @Override
        public String getID() {
          return null;
        }

        @Nonnull
        @Override
        public String getName() {
          return null;
        }

        @Override
        public void setName(String name) {
          throw new UnsupportedOperationException();
        }

        @Override
        public Class<?> getType() {
          return propertyClass.getJavaClass();
        }

        @Override
        public String getTypeAsString() {
          return typeAsString;
        }

        @Nonnull
        @Override
        public CustomPropertyClass getPropertyClass() {
          return propertyClass;
        }

        @Override
        public IStatus canSetPropertyClass(CustomPropertyClass propertyClass) {
          return Status.CANCEL_STATUS;
        }

        @Override
        public IStatus setPropertyClass(CustomPropertyClass propertyClass) {
          throw new UnsupportedOperationException();
        }
      };
    }

  }
}
