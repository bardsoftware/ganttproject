/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject team

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
package biz.ganttproject.core.time;

import java.util.Date;

/**
 * @author bard
 */
public class TimeUnitDateFrameableImpl extends TimeUnitImpl {
  private final DateFrameable myFramer;

  public TimeUnitDateFrameableImpl(String name, TimeUnitGraph timeUnitGraph, TimeUnit atomUnit, DateFrameable framer) {
    super(name, timeUnitGraph, atomUnit);
    myFramer = framer;
  }

  @Override
  public Date adjustRight(Date baseDate) {
    return myFramer.adjustRight(baseDate);
  }

  @Override
  public Date adjustLeft(Date baseDate) {
    return myFramer.adjustLeft(baseDate);
  }

  @Override
  public Date jumpLeft(Date baseDate) {
    return myFramer.jumpLeft(baseDate);
  }
}
