package net.sourceforge.ganttproject.test.time;

import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;
//import net.sourceforge.ganttproject.time.TimeUnitDateFrameableImpl;
import net.sourceforge.ganttproject.time.TimeUnitFunctionOfDate;
import net.sourceforge.ganttproject.time.gregorian.GregorianTimeUnitStack;
import junit.framework.TestCase;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by IntelliJ IDEA.
 *
 * @author bard Date: 01.02.2004
 */
public class GregorianTimeStackTest extends TestCase {
    public GregorianTimeStackTest(String name) {
        super(name);
    }

    public void testDayContains1Day() throws Exception {
        assertTrue("Day isn't constructed from days :(",
                GregorianTimeUnitStack.DAY
                        .isConstructedFrom(GregorianTimeUnitStack.DAY));
        assertEquals("Unexpected days count in one day", 1,
                GregorianTimeUnitStack.DAY
                        .getAtomCount(GregorianTimeUnitStack.DAY));
    }

    public void testDayContains24Hours() throws Exception {
        assertTrue("Day isn't constructed from hours :(",
                GregorianTimeUnitStack.DAY
                        .isConstructedFrom(GregorianTimeUnitStack.HOUR));
        assertEquals("Unexpected hours count in one day", 24,
                GregorianTimeUnitStack.DAY
                        .getAtomCount(GregorianTimeUnitStack.HOUR));
    }

    public void testDayContains1440Minutes() throws Exception {
        assertTrue("Day isn't constructed from minutes :(",
                GregorianTimeUnitStack.DAY
                        .isConstructedFrom(GregorianTimeUnitStack.MINUTE));
        assertEquals("Unexpected minutes count in one day", 1440,
                GregorianTimeUnitStack.DAY
                        .getAtomCount(GregorianTimeUnitStack.MINUTE));
    }

    public void testDayContains86400Seconds() throws Exception {
        assertTrue("Day isn't constructed from seconds :(",
                GregorianTimeUnitStack.DAY
                        .isConstructedFrom(GregorianTimeUnitStack.SECOND));
        assertEquals("Unexpected minutes count in one day", 86400,
                GregorianTimeUnitStack.DAY
                        .getAtomCount(GregorianTimeUnitStack.SECOND));
    }

    public void testDayAndHoursTimeFrameEvenBounds() throws Exception {
        Calendar c = (Calendar) GregorianCalendar.getInstance().clone();
        c.set(2000, Calendar.JANUARY, 1, 0, 0);
        GregorianTimeUnitStack stack = new GregorianTimeUnitStack();
        TimeFrame timeFrame = stack.createTimeFrame(c.getTime(),
                GregorianTimeUnitStack.DAY, GregorianTimeUnitStack.HOUR);
        assertEquals(
                "Unexpected number of days in the time frame=" + timeFrame, 1,
                timeFrame.getUnitCount(GregorianTimeUnitStack.DAY));
        assertEquals("Unexpected number of hours in the time frame="
                + timeFrame, 24, timeFrame
                .getUnitCount(GregorianTimeUnitStack.HOUR));
    }

    public void testDayAndHoursTimeFrameUnevenBounds() throws Exception {
        Calendar c = (Calendar) GregorianCalendar.getInstance().clone();
        c.set(2000, Calendar.JANUARY, 1, 12, 0);
        GregorianTimeUnitStack stack = new GregorianTimeUnitStack();
        TimeFrame timeFrame = stack.createTimeFrame(c.getTime(),
                GregorianTimeUnitStack.DAY, GregorianTimeUnitStack.HOUR);
        assertEquals(
                "Unexpected number of days in the time frame=" + timeFrame, 1,
                timeFrame.getUnitCount(GregorianTimeUnitStack.DAY));
        assertEquals("Unexpected number of hours in the time frame="
                + timeFrame, 12, timeFrame
                .getUnitCount(GregorianTimeUnitStack.HOUR));
    }

    public void testMonthAndDayFrameEvenBounds() throws Exception {
        Calendar c = (Calendar) GregorianCalendar.getInstance().clone();
        c.set(2000, Calendar.JANUARY, 1, 0, 0);
        GregorianTimeUnitStack stack = new GregorianTimeUnitStack();
        TimeUnit monthUnit = ((TimeUnitFunctionOfDate) GregorianTimeUnitStack.MONTH)
                .createTimeUnit(c.getTime());
        TimeUnit dayUnit = GregorianTimeUnitStack.DAY;
        TimeFrame timeFrame = stack.createTimeFrame(c.getTime(), monthUnit,
                dayUnit);
        assertEquals("Unexpected number of monthes in the time frame="
                + timeFrame, 1, timeFrame.getUnitCount(monthUnit));
        assertEquals(
                "Unexpected number of days in the time frame=" + timeFrame, 31,
                timeFrame.getUnitCount(dayUnit));
    }

    public void testMonthAndDayFrameUnevenBounds() throws Exception {
        Calendar c = (Calendar) GregorianCalendar.getInstance().clone();
        c.set(2000, Calendar.JANUARY, 16, 0, 0);
        GregorianTimeUnitStack stack = new GregorianTimeUnitStack();
        TimeUnit monthUnit = ((TimeUnitFunctionOfDate) GregorianTimeUnitStack.MONTH)
                .createTimeUnit(c.getTime());
        TimeUnit dayUnit = GregorianTimeUnitStack.DAY;
        TimeFrame timeFrame = stack.createTimeFrame(c.getTime(), monthUnit,
                dayUnit);
        assertEquals("Unexpected number of monthes in the time frame="
                + timeFrame, 1, timeFrame.getUnitCount(monthUnit));
        assertEquals(
                "Unexpected number of days in the time frame=" + timeFrame, 16,
                timeFrame.getUnitCount(dayUnit));
        // fail("just fail");
    }

}
