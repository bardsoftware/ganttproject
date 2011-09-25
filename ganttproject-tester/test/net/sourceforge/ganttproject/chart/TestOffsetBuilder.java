package net.sourceforge.ganttproject.chart;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.time.gregorian.GPTimeUnitStack;
import junit.framework.TestCase;

public class TestOffsetBuilder extends TestCase {
    public void testBasicOffsets() {
        GPCalendar calendar = GPCalendar.PLAIN;
        GPTimeUnitStack timeUnitStack = new GPTimeUnitStack(GanttLanguage.getInstance());
        Date start = TestSetupHelper.newMonday().getTime();

        OffsetBuilder builder = new RegularFrameOffsetBuilder.FactoryImpl()
            .withStartDate(start).withViewportStartDate(start)
            .withCalendar(calendar).withTopUnit(timeUnitStack.WEEK).withBottomUnit(timeUnitStack.DAY)
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
