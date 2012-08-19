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
package biz.ganttproject.core.time;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 31.01.2004
 */
public interface TimeUnit extends DateFrameable {
  public String getName();

  public boolean isConstructedFrom(TimeUnit unit);

  /**
   * @return number of atoms used to create current TimeUnit
   * @throws UnsupportedOperationException
   *           if current TimeUnit does not have constant number of atoms
   */
  public int getAtomCount(TimeUnit atomUnit);

  /** @return the TimeUnit which is used to build the current TimeUnit */
  public TimeUnit getDirectAtomUnit();

  public int DAY = 0;
}
