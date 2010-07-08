package net.sourceforge.ganttproject.test.task;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.WeekendCalendarImpl;
import net.sourceforge.ganttproject.chart.OffsetCalculatorImpl;
import net.sourceforge.ganttproject.chart.ChartModelBase.Offset;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnitFunctionOfDate;
import net.sourceforge.ganttproject.time.TimeUnitStack;
import net.sourceforge.ganttproject.time.gregorian.GPTimeUnitStack;

public class TestOffsetCalculation extends TaskTestCase {
    public void testOffsetOfActivityStartingOnFriday() {
        Task task = getTaskManager().createTask();
        GanttCalendar monday = new GanttCalendar(2000, 0, 3); 
        GanttCalendar friday = new GanttCalendar(2000, 0, 7);
        task.setStart(friday);
        task.setDuration(getTaskManager().createLength(2));
        OffsetCalculatorImpl offsetCalculator = new OffsetCalculatorImpl(myStack);
        TimeUnitFunctionOfDate monthUnit = (TimeUnitFunctionOfDate) myStack.MONTH;
        TimeFrame weekTimeFrame = myStack.createTimeFrame(monday.getTime(), monthUnit.createTimeUnit(monday.getTime()), myStack.WEEK_AS_BOTTOM_UNIT);
        Offset[] offsets = offsetCalculator.calculateOffsets(weekTimeFrame, myStack.WEEK_AS_BOTTOM_UNIT, monday.getTime(), myStack.getDefaultTimeUnit(), 70);
        assertEquals("Unexpected offsets count", 7, offsets.length);
        TaskActivity fridayActivity = task.getActivities()[0];
        int pixelOffset = 0;
        for (int i=0; i<offsets.length; i++) {
            Offset next = offsets[i];
            pixelOffset = next.getOffsetPixels();
            if (fridayActivity.getStart().equals(next.getOffsetEnd())) {
                break;
            }
        }
        assertEquals("Unexpected offset in pixels, for task starting on friday", 40, pixelOffset);
    }

    private GPTimeUnitStack myStack;

    
    protected void setUp() throws Exception {
        myStack = new GPTimeUnitStack(GanttLanguage.getInstance());
        super.setUp();
    }

    public GPCalendar getCalendar() {
        return new WeekendCalendarImpl();
    }

    public TimeUnitStack getTimeUnitStack() {
        return myStack;
    }
    
    
    
}
