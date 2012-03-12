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

import net.sourceforge.ganttproject.CustomPropertyDefinition;

public class CustomPropertyEvent {

  public static final int EVENT_ADD = 0;

  public static final int EVENT_REMOVE = 1;

  public static final int EVENT_REBUILD = 2;

  public static final int EVENT_NAME_CHANGE = 3;

  public static final int EVENT_TYPE_CHANGE = 4;

  private final int myType;

  private CustomPropertyDefinition myDefinition;

  private CustomPropertyDefinition myOldDef;

  public CustomPropertyEvent(int type, CustomPropertyDefinition definition) {
    myType = type;
    myDefinition = definition;
  }

  public CustomPropertyEvent(int type, CustomPropertyDefinition def, CustomPropertyDefinition oldDef) {
    myType = type;
    myDefinition = def;
    myOldDef = oldDef;
  }

  public CustomPropertyDefinition getDefinition() {
    return myDefinition;
  }

  public CustomPropertyDefinition getOldValue() {
    return myOldDef;
  }

  public String getOldName() {
    return myOldDef.getName();
  }

  public String getColName() {
    return myDefinition.getName();
  }

  public int getType() {
    return myType;
  }

}
