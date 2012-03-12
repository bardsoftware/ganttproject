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
package net.sourceforge.ganttproject.delay;

import net.sourceforge.ganttproject.task.TaskInfo;

/**
 * This class represents very basicaly delays. There are three type of delays :
 * - No delay - Normal delay - Critical delay (when the related task is
 * critical)
 * 
 * @author bbaranne
 */
public class Delay implements TaskInfo {
  public static final int NONE = -1;

  public static final int NORMAL = 0;

  public static final int CRITICAL = 1;

  private int myType = NONE;

  private Delay(int type) {
    myType = type;
  }

  public int getType() {
    return myType;
  }

  public void setType(int type) {
    myType = type;
  }

  public static Delay getDelay(int type) {
    return new Delay(type);
  }
}
