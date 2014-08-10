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

import java.util.GregorianCalendar;

import net.sourceforge.ganttproject.language.GanttLanguage;

public enum CustomPropertyClass {
  TEXT("text", "", String.class), INTEGER("integer", "0", Integer.class), DOUBLE("double", "0.0", Double.class), DATE(
      "date", null, GregorianCalendar.class), BOOLEAN("boolean", "false", Boolean.class);

  private final String myI18Ntifier;
  private final Class myJavaClass;
  private final String myDefaultValue;

  private CustomPropertyClass(String i18ntifier, String defaultValue, Class<?> javaClass) {
    myI18Ntifier = i18ntifier;
    myDefaultValue = defaultValue;
    myJavaClass = javaClass;
  }

  public String getDisplayName() {
    return GanttLanguage.getInstance().getText(myI18Ntifier);
  }

  public Class<?> getJavaClass() {
    return myJavaClass;
  }

  @Override
  public String toString() {
    return getDisplayName();
  }

  public String getID() {
    return myI18Ntifier;
  }

  public String getDefaultValueAsString() {
    return null;
  }

  public static CustomPropertyClass fromJavaClass(Class<?> javaClass) {
    for (CustomPropertyClass klass : CustomPropertyClass.values()) {
      if (klass.getJavaClass().equals(javaClass)) {
        return klass;
      }
    }
    return null;
  }
}
