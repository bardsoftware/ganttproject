/*
Copyright 2003-2018 Dmitry Barashev, BarD Software s.r.o

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
package net.sourceforge.ganttproject.test.task;

import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;
import junit.framework.TestCase;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

import java.text.DateFormat;
import java.util.Locale;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public abstract class TaskTestCase extends TestCase {
  static {
    new CalendarFactory() {
      {
        setLocaleApi(new LocaleApi() {
          public Locale getLocale() {
            return Locale.US;
          }
          public DateFormat getShortDateFormat() {
            return DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
          }
        });
      }
    };
  }
    private TaskManager myTaskManager;

    protected TaskManager getTaskManager() {
        return myTaskManager;
    }

    protected void setTaskManager(TaskManager taskManager) {
      myTaskManager = taskManager;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myTaskManager = newTaskManager();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        myTaskManager = null;
    }

    protected TaskManager newTaskManager() {
        return TestSetupHelper.newTaskManagerBuilder().build();
    }

    protected Task createTask() {
        Task result = getTaskManager().createTask();
        result.move(getTaskManager().getRootTask());
        result.setName(String.valueOf(result.getTaskID()));
        return result;
    }

    protected Task createTask(GanttCalendar start) {
      return createTask(start, 1);
    }

    protected Task createTask(GanttCalendar start, int duration) {
      Task result = createTask();
      result.setStart(start);
      result.setDuration(getTaskManager().createLength(duration));
      return result;
    }

    protected TaskDependency createDependency(Task dependant, Task dependee) throws TaskDependencyException {
        return getTaskManager().getDependencyCollection().createDependency(dependant, dependee);
    }
}