/*
 * Created on 08.11.2004
 */
package net.sourceforge.ganttproject.test.time;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import biz.ganttproject.core.time.impl.WeekFramerImpl;

import junit.framework.TestCase;

/**
 * @author bard
 */
public class TestWeekFramer extends TestCase {
    static class TestCalendarFactory implements WeekFramerImpl.ICalendarFactory {
        @Override
        public Calendar newCalendar() {
            return GregorianCalendar.getInstance(Locale.UK);  // Monday is the first day of week in UK locale.
        }
    }

    public void testAdjustLeft() {
        WeekFramerImpl framer = new WeekFramerImpl(new TestCalendarFactory());
        Date adjusted = framer.adjustLeft(newMonday());
        Calendar c = (Calendar) Calendar.getInstance().clone();
        c.setTime(adjusted);
        c.add(Calendar.MILLISECOND, -1);
        assertEquals("Unexpected day of week", Calendar.SUNDAY, c
                .get(Calendar.DAY_OF_WEEK));

        Date adjustedSunday = framer.adjustLeft(newSunday());
        assertEquals(
                "Adjusted sunday is expected to be equal to adjusted monday",
                adjusted, adjustedSunday);
    }

    public void testAdjustRight() {
        WeekFramerImpl framer = new WeekFramerImpl(new TestCalendarFactory());
        Date adjustedMonday = framer.adjustRight(newMonday());
        Date adjustedSunday = framer.adjustRight(newSunday());
        assertEquals(adjustedMonday, adjustedSunday);
        Calendar c = (Calendar) Calendar.getInstance().clone();
        c.setTime(adjustedMonday);
        assertEquals("Unexpected day of week", Calendar.MONDAY, c
                .get(Calendar.DAY_OF_WEEK));
        c.add(Calendar.MILLISECOND, -1);
        assertEquals("Unexpected day of week", Calendar.SUNDAY, c
                .get(Calendar.DAY_OF_WEEK));
    }

    public void testJumpLeft() {
        WeekFramerImpl framer = new WeekFramerImpl(new TestCalendarFactory());
        Date adjustedMonday = framer.jumpLeft(newMonday());
        Date adjustedSunday = framer.jumpLeft(newSunday());
        assertNotSame(adjustedMonday, adjustedSunday);
        Calendar c = (Calendar) Calendar.getInstance().clone();
        c.setTime(adjustedMonday);
        assertTrue("Unexpected day of week, date=" + c.getTime(),
                Calendar.MONDAY == c.get(Calendar.DAY_OF_WEEK));
        assertNotSame(adjustedMonday, newMonday());
        c.setTime(adjustedSunday);
        assertEquals("Unexpected day of week, date=" + c.getTime(),
                Calendar.SUNDAY, c.get(Calendar.DAY_OF_WEEK));
        assertNotSame(adjustedMonday, newSunday());
    }

    private Date newMonday() {
        Calendar c = new TestCalendarFactory().newCalendar();
        c.clear();
        c.set(Calendar.YEAR, 2004);
        c.set(Calendar.MONTH, Calendar.NOVEMBER);
        c.set(Calendar.DAY_OF_MONTH, 8);
        return c.getTime();
    }

    private Date newSunday() {
        Calendar c = new TestCalendarFactory().newCalendar();
        c.clear();
        c.set(Calendar.YEAR, 2004);
        c.set(Calendar.MONTH, Calendar.NOVEMBER);
        c.set(Calendar.DAY_OF_MONTH, 14);
        return c.getTime();
    }
}
