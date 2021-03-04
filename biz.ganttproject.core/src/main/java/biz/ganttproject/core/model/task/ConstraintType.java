/*
Copyright 2011-2021 Dmitry Barashev, BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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

package biz.ganttproject.core.model.task;

/**
 * @author dbarashev@bardsoftware.com
 */
public
enum ConstraintType {
  startstart, finishstart, finishfinish, startfinish;
  private static final String[] PERSISTENT_VALUES = new String[]{
      "SS", "FS", "FF", "SF"
  };

  public String getPersistentValue() {
    return String.valueOf(ordinal() + 1);
  }

  public String getReadablePersistentValue() {
    return PERSISTENT_VALUES[ordinal()];
  }

  public static ConstraintType fromPersistentValue(String dependencyTypeAsString) {
    return ConstraintType.values()[Integer.parseInt(dependencyTypeAsString) - 1];
  }

  public static ConstraintType fromReadablePersistentValue(String str) {
    for (int i = 0; i < PERSISTENT_VALUES.length; i++) {
      if (PERSISTENT_VALUES[i].equals(str)) {
        return ConstraintType.values()[i];
      }
    }
    throw new IllegalArgumentException("Can't find constraint by persistent value=" + str);
  }
}
