package net.sourceforge.ganttproject;

import java.awt.Color;
import java.net.URL;

import biz.ganttproject.core.time.GanttCalendar;
import biz.ganttproject.core.time.TimeUnitStack;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;

import net.sourceforge.ganttproject.calendar.AlwaysWorkingTimeCalendarImpl;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleManagerImpl;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerConfig;

public class TestSetupHelper {
    public static class TaskManagerBuilder implements TaskManagerConfig {
        private GPCalendar myGPCalendar = new AlwaysWorkingTimeCalendarImpl();

        private TimeUnitStack myTimeUnitStack;

        private HumanResourceManager myResourceManager;

        private RoleManager myRoleManager;

        public TaskManagerBuilder() {
            myTimeUnitStack = new GPTimeUnitStack();
            myRoleManager = new RoleManagerImpl();
            myResourceManager = new HumanResourceManager(myRoleManager
                    .getDefaultRole(), null);
        }

        @Override
        public Color getDefaultColor() {
            return null;
        }

        @Override
        public GPCalendar getCalendar() {
            return myGPCalendar;
        }

        @Override
        public TimeUnitStack getTimeUnitStack() {
            return myTimeUnitStack;
        }

        @Override
        public HumanResourceManager getResourceManager() {
            return myResourceManager;
        }

        @Override
        public URL getProjectDocumentURL() {
            return null;
        }

        public TaskManagerBuilder withCalendar(GPCalendar calendar) {
            myGPCalendar = calendar;
            return this;
        }

        public TaskManager build() {
            return TaskManager.Access.newInstance(null, this);
        }
    }

    public static TaskManagerBuilder newTaskManagerBuilder() {
        return new TaskManagerBuilder();
    }

    public static GanttCalendar newFriday() {
        return new GanttCalendar(2004, 9, 15);
    }

    public static GanttCalendar newSaturday() {
        return new GanttCalendar(2004, 9, 16);
    }

    public static GanttCalendar newSunday() {
        return new GanttCalendar(2004, 9, 17);
    }

    public static GanttCalendar newTuesday() {
        return new GanttCalendar(2004, 9, 19);
    }

    public static GanttCalendar newMonday() {
        return new GanttCalendar(2004, 9, 18);
    }

    public static GanttCalendar newWendesday() {
        return new GanttCalendar(2004, 9, 20);
    }

    public static GanttCalendar newThursday() {
        return new GanttCalendar(2004, 9, 21);
    }


}
