/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
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
package biz.ganttproject.impex.msproject2

import biz.ganttproject.core.model.task.ConstraintType
import biz.ganttproject.core.model.task.TestTaskFactory
import net.sourceforge.ganttproject.GanttProjectImpl
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

class ProjectExportImportTest  {

  @BeforeEach
  fun setUp() {
    initLocale()
  }

  @Test
  fun `basic test - two tasks and dependency`() {
    val project = GanttProjectImpl()
    val taskFactory = TestTaskFactory(project.taskManager)
    val task1 = taskFactory.createTask("2024-07-12")
    val task2 = taskFactory.createTask("2024-07-15")
    val dep = taskFactory.createDependency(task2, task1)
    val projectFile = ProjectFileExporter(project).run()

    GanttProjectImpl().let { project2 ->
      ProjectFileImporterImpl(projectFile, project2).run()
      assertEquals(2, project2.taskManager.taskCount)
      val imp1 = project2.taskManager.tasks.find { it.name == task1.name }!!.also {
        assertEquals(task1.start, it.start)
        assertEquals(task1.end, it.end)
      }
      project2.taskManager.tasks.find { it.name == task2.name }!!.also {
        assertEquals(task2.start, it.start)
        assertEquals(task2.end, it.end)
        assertEquals(ConstraintType.finishstart, it.dependenciesAsDependant.getDependency(imp1).constraint.type)
      }
    }
  }
}
