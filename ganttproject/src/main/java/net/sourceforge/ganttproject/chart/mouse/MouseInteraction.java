/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.chart.mouse;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Date;

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.core.time.TimeUnitStack;
import net.sourceforge.ganttproject.chart.ChartModelBase.ScrollingSession;

public interface MouseInteraction {
  void apply(MouseEvent event);

  void finish();

  void paint(Graphics g);

  static interface TimelineFacade {
    ScrollingSession createScrollingSession(int xpos, int ypos);

    Date getDateAt(int x);

    TimeDuration createTimeInterval(TimeUnit timeUnit, Date startDate, Date endDate);

    TimeUnitStack getTimeUnitStack();

    GPCalendarCalc getCalendar();

    Date getEndDateAt(int i);
  }
}