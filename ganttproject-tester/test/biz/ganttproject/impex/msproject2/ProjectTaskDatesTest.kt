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

import net.sf.mpxj.ProjectFile
import net.sourceforge.ganttproject.GanttProjectImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.w3c.util.DateParser
import java.time.LocalDateTime

class ProjectTaskDatesTest {

  @BeforeEach
  fun setUp() {
    initLocale()
  }

  @Test
  fun `task with start time 8-00 and end time 17-00 imports with adjusted dates`() {
    // Create a ProjectFile with a single task
    val projectFile = ProjectFile()
    val mpxjTask = projectFile.addTask()
    mpxjTask.name = "Test Task"

    // Set start date to 2023-01-03 8:00
    mpxjTask.start = LocalDateTime.of(2023, 1, 3, 8, 0)

    // Set finish date to 2023-01-05 17:00
    mpxjTask.finish = LocalDateTime.of(2023, 1, 5, 17, 0)

    // Import into GanttProjectImpl
    val ganttProject = GanttProjectImpl()
    ProjectFileImporterImpl(projectFile, ganttProject).run()

    // Verify the imported task
    assertEquals(1, ganttProject.taskManager.taskCount)
    val importedTask = ganttProject.taskManager.tasks.first()

    // Check that start date is 2023-01-03 (time should be adjusted to start of day)
    assertEquals("2023-01-03", DateParser.getIsoDateNoHours(importedTask.start.time))

    // Check that end date is 2023-01-06 (finish date 2023-01-03 17:00 becomes 2023-01-04)
    assertEquals("2023-01-06", DateParser.getIsoDateNoHours(importedTask.end.time))
  }
}
