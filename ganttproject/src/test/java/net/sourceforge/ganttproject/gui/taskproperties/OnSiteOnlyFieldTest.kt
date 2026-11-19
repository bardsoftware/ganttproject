/*
Copyright 2026 BarD Software s.r.o., GanttProject Team

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
package net.sourceforge.ganttproject.gui.taskproperties

import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.task.TaskView
import org.easymock.EasyMock.createNiceMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.replay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The "on site only" tick of the task dialog is written back to the task with the other fields. */
class OnSiteOnlyFieldTest {
  private fun panelFor(onSite: Boolean): Pair<MainPropertiesPanel, net.sourceforge.ganttproject.task.Task> {
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val task = taskManager.createTask()
    task.createMutator().also { it.setOnSiteOnly(onSite); it.commit() }
    val taskView = createNiceMock<TaskView>(TaskView::class.java)
    expect(taskView.timelineTasks).andStubReturn(mutableSetOf())
    replay(taskView)
    return MainPropertiesPanel(task, taskView) to task
  }

  @Test
  fun `the tick shows the mark of the task`() {
    assertEquals(true, panelFor(true).first.onSiteOnlyOption.value)
    assertEquals(false, panelFor(false).first.onSiteOnlyOption.value)
  }

  @Test
  fun `a tick reaches the task when the dialog commits, and removing it takes the mark away`() {
    val (panel, task) = panelFor(false)
    panel.onSiteOnlyOption.set(true)
    task.createMutator().also { panel.save(it); it.commit() }
    assertTrue(task.isOnSiteOnly)

    val (second, marked) = panelFor(true)
    second.onSiteOnlyOption.set(false)
    marked.createMutator().also { second.save(it); it.commit() }
    assertEquals(false, marked.isOnSiteOnly)
  }
}
