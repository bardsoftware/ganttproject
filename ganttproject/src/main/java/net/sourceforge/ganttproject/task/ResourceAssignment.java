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

import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.roles.Role;

import java.math.BigDecimal;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 05.02.2004
 */
public interface ResourceAssignment {
  Task getTask();

  HumanResource getResource();

  float getLoad();

  void setLoad(float load);

  /** Deletes this assignment */
  void delete();

  void setCoordinator(boolean responsible);

  boolean isCoordinator();

  /**
   * The resource has to be present on every day of the task: levelling does not place the task on a day off of this
   * resource, whatever the load. False by default.
   */
  void setBlocking(boolean blocking);

  boolean isBlocking();

  /**
   * The resource stays on the task but brings no hours to its effort, such as a reviewer. False by default, so that
   * a file without the flag means that every assignment contributes.
   */
  void setNoEffort(boolean noEffort);

  boolean isNoEffort();

  Role getRoleForAssignment();

  void setRoleForAssignment(Role role);

  /** Effort which the resource actually spent on the task, in the unit defined by {@code TaskEffort}. {@code null} means none was recorded. */
  BigDecimal getActualEffort();

  void setActualEffort(BigDecimal actualEffort);
}
