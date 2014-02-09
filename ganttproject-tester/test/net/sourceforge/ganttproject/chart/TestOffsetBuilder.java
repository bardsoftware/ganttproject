/*
Copyright 2012 GanttProject Team

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
package net.sourceforge.ganttproject.chart;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.chart.grid.OffsetBuilder;
import biz.ganttproject.core.chart.grid.OffsetList;
import biz.ganttproject.core.chart.grid.OffsetBuilderImpl;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import net.sourceforge.ganttproject.TestSetupHelper;
import junit.framework.TestCase;

/**
 * Tests OffsetBuilder
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class TestOffsetBuilder extends TestCase {
    public void testBasicOffsets() {
        GPCalendarCalc calendar = GPCalendarCalc.PLAIN;
        Date start = TestSetupHelper.newMonday().getTime();

        OffsetBuilder builder = new OffsetBuilderImpl.FactoryImpl()
            .withStartDate(start).withViewportStartDate(start)
            .withCalendar(calendar).withTopUnit(GPTimeUnitStack.WEEK).withBottomUnit(GPTimeUnitStack.DAY)
            .withAtomicUnitWidth(20).withEndOffset(210).withWeekendDecreaseFactor(1.0f)
            .build();
        OffsetList bottomUnitOffsets = new OffsetList();
        builder.constructOffsets(new ArrayList<Offset>(), bottomUnitOffsets);

        assertEquals(11, bottomUnitOffsets.size());
        assertEquals(20, bottomUnitOffsets.get(0).getOffsetPixels());
        assertEquals(220, bottomUnitOffsets.get(10).getOffsetPixels());

        Calendar c = (Calendar) Calendar.getInstance().clone();
        c.setTime(start);
        c.add(Calendar.DAY_OF_YEAR, 11);
        assertEquals(c.getTime(), bottomUnitOffsets.get(10).getOffsetEnd());

        c.add(Calendar.DAY_OF_YEAR, -1);
        assertEquals(c.getTime(), bottomUnitOffsets.get(10).getOffsetStart());
    }
}
