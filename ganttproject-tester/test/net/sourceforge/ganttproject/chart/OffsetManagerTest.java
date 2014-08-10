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

import java.util.Date;

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.chart.grid.OffsetBuilder;
import biz.ganttproject.core.chart.grid.OffsetList;
import biz.ganttproject.core.chart.grid.OffsetLookup;
import biz.ganttproject.core.chart.grid.OffsetManager;
import biz.ganttproject.core.chart.grid.OffsetBuilderImpl;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import net.sourceforge.ganttproject.TestSetupHelper;
import junit.framework.TestCase;

/**
 * Tests OffsetManager
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class OffsetManagerTest extends TestCase {
    class TestOffsetBuilderFactory implements OffsetManager.OffsetBuilderFactory {
        private OffsetBuilder myTopAndBottomOffsetBuilder;
        private OffsetBuilder myAtomicOffsetBuilder;

        public TestOffsetBuilderFactory(Date viewportStart) {
            myTopAndBottomOffsetBuilder = new OffsetBuilderImpl.FactoryImpl()
                .withStartDate(viewportStart)
                .withViewportStartDate(viewportStart)
                .withCalendar(GPCalendarCalc.PLAIN).withTopUnit(GPTimeUnitStack.YEAR).withBottomUnit(GPTimeUnitStack.MONTH)
                .withAtomicUnitWidth(5).withEndOffset(700).withWeekendDecreaseFactor(1.0f)
                .build();
            myAtomicOffsetBuilder = new OffsetBuilderImpl.FactoryImpl()
                .withStartDate(viewportStart)
                .withViewportStartDate(viewportStart)
                .withCalendar(GPCalendarCalc.PLAIN).withTopUnit(GPTimeUnitStack.DAY).withBottomUnit(GPTimeUnitStack.DAY)
                .withAtomicUnitWidth(5).withEndOffset(700).withWeekendDecreaseFactor(1.0f)
                .build();
        }

        @Override
        public OffsetBuilder createTopAndBottomUnitBuilder() {
            return myTopAndBottomOffsetBuilder;
        }

        @Override
        public OffsetBuilder createAtomUnitBuilder() {
            return myAtomicOffsetBuilder;
        }
    }

    /**
     * In this test we create a timeline with years in the top, months in the bottom, and day
     * as atomic time unit. Viewport start date is Oct 18 (Monday). We expect that Oct 31 would have the same offset
     * in month and day offset lists, and Dec 31 would have the same offset in all three lists.
     */
    public void testAlignment() {
        OffsetManager manager = new OffsetManager(new TestOffsetBuilderFactory(TestSetupHelper.newMonday().getTime()));
        OffsetList yearOffsets = manager.getTopUnitOffsets();
        OffsetList monthOffsets = manager.getBottomUnitOffsets();
        OffsetList dayOffsets = manager.getAtomUnitOffsets();

        OffsetLookup lookup = new OffsetLookup();
        {
            Date endOfMonth = GPTimeUnitStack.MONTH.adjustRight(TestSetupHelper.newMonday().getTime());
            int idxDayEomOffset = lookup.lookupOffsetByEndDate(endOfMonth, dayOffsets);
            int idxMonthEomOffset = lookup.lookupOffsetByEndDate(endOfMonth, monthOffsets);
            assertTrue("It is expected that offset pixels are the same for offsets with the same end date" + endOfMonth,
                    dayOffsets.get(idxDayEomOffset).getOffsetPixels() == monthOffsets.get(idxMonthEomOffset).getOffsetPixels());
        }
        {
            Date endOfYear = GPTimeUnitStack.YEAR.adjustRight(TestSetupHelper.newMonday().getTime());
            int idxDayEoyOffset = lookup.lookupOffsetByEndDate(endOfYear, dayOffsets);
            int idxMonthEoyOffset = lookup.lookupOffsetByEndDate(endOfYear, monthOffsets);
            int idxYearEoyOffset = lookup.lookupOffsetByEndDate(endOfYear, yearOffsets);
            assertTrue("It is expected that offset pixels are the same for offsets with the same end date=" + endOfYear,
                    dayOffsets.get(idxDayEoyOffset).getOffsetPixels() == monthOffsets.get(idxMonthEoyOffset).getOffsetPixels());
            assertTrue("It is expected that offset pixels are the same for offsets with the same end date" + endOfYear,
                    yearOffsets.get(idxYearEoyOffset).getOffsetPixels() == monthOffsets.get(idxMonthEoyOffset).getOffsetPixels());

        }

    }
}
