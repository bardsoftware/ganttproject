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
package cloud.ganttproject.colloboque

import net.sourceforge.ganttproject.storage.BinaryPred
import net.sourceforge.ganttproject.storage.OperationDto
import net.sourceforge.ganttproject.storage.XlogRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProjectFileUpdaterTest {

  @BeforeEach fun setUp() {
    localeApi  // This will set a static field in CalendarFactory
  }

  @Test fun `empty xlog`() {
    assertEquals(PROJECT_XML_TEMPLATE, ProjectFileUpdater().applyXlog(PROJECT_XML_TEMPLATE, XlogRecord(emptyList())))
  }

  @Test fun `apply task name change`() {
    val changes = XlogRecord(
      listOf(
        OperationDto.UpdateOperationDto("task",
          updateBinaryConditions = mutableListOf(Triple("uid", BinaryPred.EQ, "qwerty")),
          updateRangeConditions = mutableListOf(),
          newValues = mutableMapOf("name" to "Task2")
        )
      )
    )
    val updatedXml = updateProjectXml(PROJECT_XML_TEMPLATE, changes)
    assertTrue(updatedXml.lines().any { it.matches(""".*<task.*name=.Task2.*>""".toRegex()) }) {
      """The result of applying updates:
        
        $updatedXml
        """.trimIndent()
    }
  }
}


private val PROJECT_XML_TEMPLATE = """
<?xml version="1.0" encoding="UTF-8"?>
<project name="" company="" webLink="" view-date="2022-01-01" view-index="0" gantt-divider-location="374" resource-divider-location="322" version="3.0.2906" locale="en">
  <tasks empty-milestones="true">
      <task id="0" uid="qwerty" name="Task1" color="#99ccff" meeting="false" start="2022-02-10" duration="25" complete="85" expand="true"/>
  </tasks>
</project>
        """.trimIndent()
