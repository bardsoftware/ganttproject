/*
 * Copyright (c) 2022 Dmitry Barashev, BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.test.task

import biz.ganttproject.storage.db.tables.records.TaskRecord
import net.sourceforge.ganttproject.task.export
import net.sourceforge.ganttproject.task.importFromDatabase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TaskManagerProjectDatabaseTest: TaskTestCase() {
  @BeforeEach
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun `test import from database preserves hierarchy`() {
    val supertask1 = createTask()
    val childTask1 = createTask().also { taskManager.taskHierarchy.move(it, supertask1) }
    val childTask2 = createTask().also { taskManager.taskHierarchy.move(it, supertask1) }

    val supertask2 = createTask()
    val childTask3 = createTask().also { taskManager.taskHierarchy.move(it, supertask2) }

    val exportedHierarchy = taskManager.taskHierarchy.export()
    val clone = taskManager.emptyClone()
    val records = taskManager.tasks.map {task ->
      TaskRecord().also {
        it.uid = task.uid
        it.num = task.taskID
        it.name = task.name
        it.startDate = task.start.toLocalDate()
        it.duration = task.duration.length
        it.completion = task.completionPercentage
        it.webLink = ""
        it.notes = ""
        it.isMilestone = task.isMilestone
      }
    }
    clone.importFromDatabase(records, exportedHierarchy)
    assertEquals(clone.rootTask, clone.tasks.find { it.uid == supertask1.uid }.let { clone.taskHierarchy.getContainer(it) })
    assertEquals(clone.rootTask, clone.tasks.find { it.uid == supertask2.uid }.let { clone.taskHierarchy.getContainer(it) })
    assertEquals(supertask1.uid, clone.tasks.find { it.uid == childTask1.uid }.let { clone.taskHierarchy.getContainer(it) }.uid)
    assertEquals(supertask1.uid, clone.tasks.find { it.uid == childTask2.uid }.let { clone.taskHierarchy.getContainer(it) }.uid)
    assertEquals(supertask2.uid, clone.tasks.find { it.uid == childTask3.uid }.let { clone.taskHierarchy.getContainer(it) }.uid)
  }
}