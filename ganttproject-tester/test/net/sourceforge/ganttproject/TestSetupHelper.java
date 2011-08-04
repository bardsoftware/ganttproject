package net.sourceforge.ganttproject;

import java.awt.Color;
import java.net.URL;

import net.sourceforge.ganttproject.calendar.AlwaysWorkingTimeCalendarImpl;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleManagerImpl;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerConfig;
import net.sourceforge.ganttproject.time.TimeUnitStack;
import net.sourceforge.ganttproject.time.gregorian.GPTimeUnitStack;

public class TestSetupHelper {
    public static class TaskManagerBuilder implements TaskManagerConfig {
        private GPCalendar myGPCalendar = new AlwaysWorkingTimeCalendarImpl();

        private TimeUnitStack myTimeUnitStack;

        private HumanResourceManager myResourceManager;

        private RoleManager myRoleManager;

        public TaskManagerBuilder() {
            myTimeUnitStack = new GPTimeUnitStack(GanttLanguage.getInstance());
            myRoleManager = new RoleManagerImpl();
            myResourceManager = new HumanResourceManager(myRoleManager
                    .getDefaultRole());
        }

        public Color getDefaultColor() {
            return null;
        }

        public GPCalendar getCalendar() {
            return myGPCalendar;
        }

        public TimeUnitStack getTimeUnitStack() {
            return myTimeUnitStack;
        }

        public HumanResourceManager getResourceManager() {
            return myResourceManager;
        }

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
