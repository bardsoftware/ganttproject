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
package net.sourceforge.ganttproject

import biz.ganttproject.core.io.XmlProjectImporter
import biz.ganttproject.core.model.task.CapacityChange
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.io.GanttXMLSaver
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.capacitySchedule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.text.DateFormat
import java.time.Duration
import java.time.LocalDate
import java.util.Locale

/** Working hours which change over time and the utilization of a resource, as they act on the derived duration. */
class EffortCapacityTest {
  private lateinit var project: GanttProjectImpl

  @BeforeEach
  fun setUp() {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : LocaleApi {
          override fun getLocale(): Locale = Locale.US
          override fun getShortDateFormat(): DateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        })
      }
    }
    project = GanttProjectImpl()
  }

  private fun resource(hoursPerDay: String?): HumanResource =
    project.humanResourceManager.create("r", project.humanResourceManager.resources.size).also {
      it.hoursPerDay = hoursPerDay?.let(::BigDecimal)
    }

  private fun change(date: String, hours: String) = CapacityChange(LocalDate.parse(date), BigDecimal(hours))

  /** An effort driven task which starts on Monday 2 November 2026. */
  private fun task(effort: String, resource: HumanResource, load: Float = 100f, start: Int = 2): Task =
    project.taskManager.newTaskBuilder().withName("t").build().also { task ->
      task.createMutator().also {
        it.setStart(CalendarFactory.createGanttCalendar(2026, 10, start))
        it.setEffort(BigDecimal(effort))
        it.setEffortDriven(true)
        it.commit()
      }
      task.assignmentCollection.addAssignment(resource).load = load
    }

  private fun Task.days() = duration.length

  @Test
  fun `without changes and with full utilization the duration is the one of constant hours`() {
    val task = task("20", resource("4"))
    assertEquals(5, task.days())
    assertNull(task.assignments[0].resource.utilization)
    assertEquals(true, task.capacitySchedule().isConstant)
  }

  @Test
  fun `a task over a change of hours is counted day by day`() {
    val resource = resource("4")
    val task = task("20", resource)
    assertEquals(5, task.days())

    resource.hoursSchedule = listOf(change("2026-11-04", "8"))
    // 4 + 4 on Monday and Tuesday, 8 + 8 on Wednesday and Thursday
    assertEquals(4, task.days())
  }

  @Test
  fun `a change after the task does not matter and one before it applies from the start`() {
    val resource = resource("8")
    val task = task("16", resource)
    resource.hoursSchedule = listOf(change("2027-01-01", "2"))
    assertEquals(2, task.days())

    resource.hoursSchedule = listOf(change("2020-01-01", "4"))
    assertEquals(4, task.days())
  }

  @Test
  fun `the days off between the days of a task are not counted`() {
    val resource = resource("8")
    // Friday 6 November, the weekend is skipped
    val task = task("16", resource, start = 6)
    resource.hoursSchedule = listOf(change("2026-01-01", "8"))
    assertEquals(2, task.days())
  }

  @Test
  fun `hours of several resources are added up on every day`() {
    val a = resource("4")
    val b = resource("4")
    val task = task("24", a)
    task.assignmentCollection.addAssignment(b).load = 50f
    // 4 + 2 = 6 hours a day: 4 days; with 8 + 2 = 10 from Wednesday on 6 + 6 + 10 = 22 is not enough, Thursday is needed
    assertEquals(4, task.days())
    a.hoursSchedule = listOf(change("2026-11-04", "8"))
    assertEquals(4, task.days())
    task.createMutator().also { it.setEffort(BigDecimal("30")); it.commit() }
    // 6 + 6 + 10 = 22 is short of 30, Thursday makes 32
    assertEquals(4, task.days())
  }

  @Test
  fun `utilization of 50 percent doubles the duration`() {
    val resource = resource("8")
    val task = task("16", resource)
    assertEquals(2, task.days())
    resource.utilization = 50
    assertEquals(4, task.days())
    resource.utilization = null
    assertEquals(2, task.days())
  }

  @Test
  fun `utilization also scales the hours of a change`() {
    val resource = resource("8")
    val task = task("16", resource)
    resource.hoursSchedule = listOf(change("2026-11-03", "4"))
    resource.utilization = 50
    // 4 + 2 + 2 + 2 + 2 + 2 + 2 = 16 in seven working days, with a weekend in between
    assertEquals(7, task.days())
  }

  @Test
  fun `utilization works together with the load of the assignment`() {
    val resource = resource("8")
    resource.utilization = 50
    // 8 * 50 % * 50 % = 2 hours
    assertEquals(4, task("8", resource, 50f).days())
  }

  @Test
  fun `utilization of zero keeps the duration`() {
    val resource = resource("8")
    val task = task("16", resource)
    assertEquals(2, task.days())
    resource.utilization = 0
    assertEquals(2, task.days())
  }

  @Test
  fun `a period without hours stretches the task over it`() {
    val resource = resource("8")
    val task = task("8", resource)
    resource.hoursSchedule = listOf(change("2026-11-02", "0"), change("2026-11-09", "8"))
    // the five days of the first week count, the work is done on the Monday after
    assertEquals(6, task.days())
  }

  @Test
  fun `hours which never come keep the duration and do not loop`() {
    val resource = resource("8")
    val task = task("16", resource)
    assertEquals(2, task.days())
    assertTimeoutPreemptively(Duration.ofSeconds(30)) {
      resource.hoursSchedule = listOf(change("2026-11-02", "0"))
    }
    assertEquals(2, task.days())
  }

  @Test
  fun `of two changes for one date the later one counts`() {
    val resource = resource("8")
    val task = task("16", resource)
    resource.hoursSchedule = listOf(change("2026-11-02", "2"), change("2026-11-02", "4"))
    assertEquals(listOf(change("2026-11-02", "4")), resource.hoursSchedule)
    assertEquals(4, task.days())
  }

  @Test
  fun `changing the changes of an assigned resource follows, removing them goes back`() {
    val resource = resource("8")
    val task = task("16", resource)
    resource.hoursSchedule = listOf(change("2026-11-02", "2"))
    assertEquals(8, task.days())
    resource.hoursSchedule = emptyList()
    assertEquals(2, task.days())
  }

  private fun save(project: IGanttProject): String {
    val out = ByteArrayOutputStream()
    GanttXMLSaver(project).save(out)
    return out.toString(Charsets.UTF_8)
  }

  private fun projectXml(resourceAttrs: String = "", resourceChildren: String = "") = """
    <project name="p">
      <tasks>
        <task id="1" uid="u1" name="T" start="2026-11-02" duration="1" effort-hours="20" effort-driven="true"/>
      </tasks>
      <resources>
        <resource id="0" name="R" function="Default:0" contacts="" phone="" $resourceAttrs>$resourceChildren</resource>
      </resources>
      <allocations>
        <allocation task-id="1" resource-id="0" function="Default:0" responsible="true" load="100.0"/>
      </allocations>
    </project>
  """.trimIndent()

  @Test
  fun `hours changes and utilization survive a save and load`() {
    val loaded = XmlProjectImporter().import(projectXml(
      """hours-per-day="4" utilization="50"""",
      """<hours-change date="2026-11-04" hours="8"/><hours-change date="2027-01-01" hours="6.5"/>"""))
    val saved = save(loaded)

    val resource = XmlProjectImporter().import(saved).humanResourceManager.getById(0)
    assertEquals(50, resource.utilization, "utilization has to be read back from the saved file:\n$saved")
    assertEquals(listOf(change("2026-11-04", "8"), change("2027-01-01", "6.5")), resource.hoursSchedule,
      "changes have to be read back from the saved file:\n$saved")
  }

  @Test
  fun `loading a file derives the duration from the changes and the utilization`() {
    val loaded = XmlProjectImporter().import(projectXml(
      """hours-per-day="4" utilization="100"""",
      """<hours-change date="2026-11-04" hours="8"/>"""))
    assertEquals(4, loaded.taskManager.getTask(1).duration.length)
  }

  @Test
  fun `a file without the new attributes loads as before and writes none`() {
    val loaded = XmlProjectImporter().import(projectXml("""hours-per-day="4""""))
    val resource = loaded.humanResourceManager.getById(0)
    assertNull(resource.utilization)
    assertEquals(emptyList<CapacityChange>(), resource.hoursSchedule)
    assertEquals(5, loaded.taskManager.getTask(1).duration.length)

    val saved = save(loaded)
    assertFalse(saved.contains("utilization"), "no utilization was set, the document was:\n$saved")
    assertFalse(saved.contains("hours-change"), "no changes were set, the document was:\n$saved")
  }

  @Test
  fun `an entry with an unreadable date is left out when loading`() {
    val loaded = XmlProjectImporter().import(projectXml(
      """hours-per-day="4"""",
      """<hours-change date="soon" hours="8"/><hours-change date="2026-11-04" hours="8"/>"""))
    assertEquals(listOf(change("2026-11-04", "8")), loaded.humanResourceManager.getById(0).hoursSchedule)
  }
}
