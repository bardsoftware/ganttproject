/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2010 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.chart.ChartModelBase.ScrollingSession;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitStack;

public interface MouseInteraction {
    void apply(MouseEvent event);

    void finish();

    void paint(Graphics g);
    
    static interface TimelineFacade {
        ScrollingSession createScrollingSession(int xpos);
        Date getDateAt(int x);
        TaskLength createTimeInterval(TimeUnit timeUnit, Date startDate, Date endDate);
        TimeUnitStack getTimeUnitStack();
        GPCalendar getCalendar();
        Date getEndDateAt(int i);
    }
}