/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.chart.gantt;

import java.util.Collections;

import com.google.common.collect.ImmutableList;

import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import junit.framework.TestCase;

/**
 * Tests for clipboard operations with tasks.
 *
 * @author dbarashev
 */
public class ClipboardTaskProcessorTest extends TestCase {
  public void testCopyDates() {
    TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder().build();
    GanttTask task = taskManager.createTask();
    task.setDuration(taskManager.createLength(3));

    ClipboardTaskProcessor clipboardProcessor = new ClipboardTaskProcessor(taskManager);
    Task pastedTask = clipboardProcessor.paste(taskManager.getRootTask(), ImmutableList.<Task>of(task), Collections.<TaskDependency>emptyList()).get(0);
    assertEquals(task.getStart(), pastedTask.getStart());
    assertEquals(task.getEnd(), pastedTask.getEnd());
    assertEquals(task.getDuration(), pastedTask.getDuration());
  }
}
