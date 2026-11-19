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
import biz.ganttproject.core.model.task.ConstraintType
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.io.GanttXMLSaver
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.text.DateFormat
import java.time.Duration
import java.util.Locale

class EffortDrivenDurationTest {
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

  private fun resource(name: String, hoursPerDay: String?): HumanResource =
    project.humanResourceManager.create(name, project.humanResourceManager.resources.size).also {
      it.hoursPerDay = hoursPerDay?.let(::BigDecimal)
    }

  private fun task(effort: String? = null, effortDriven: Boolean = true): Task =
    project.taskManager.newTaskBuilder().withName("t").build().also { task ->
      task.createMutator().also {
        it.setEffort(effort?.let(::BigDecimal))
        it.setEffortDriven(effortDriven)
        it.commit()
      }
    }

  private fun Task.assign(resource: HumanResource, load: Float) {
    assignmentCollection.addAssignment(resource).load = load
  }

  private fun Task.days() = duration.length

  @Test
  fun `duration is effort divided by the hours of one resource, rounded up`() {
    val task = task("20")
    task.assign(resource("a", "4"), 100f)
    assertEquals(5, task.days())

    task.createMutator().also { it.setEffort(BigDecimal("21")); it.commit() }
    assertEquals(6, task.days())
  }

  @Test
  fun `hours of several resources are added up weighted by load`() {
    val task = task("25")
    task.assign(resource("a", "8"), 100f)
    task.assign(resource("b", "4"), 50f)
    // 8 + 4 * 0.5 = 10 hours a day, 2.5 days
    assertEquals(3, task.days())
  }

  @Test
  fun `a resource without hours per day counts as 8`() {
    val task = task("24")
    task.assign(resource("a", null), 100f)
    assertEquals(3, task.days())
  }

  @Test
  fun `a load which is not a binary fraction does not push the result over a whole number`() {
    val task = task("33.3")
    task.assign(resource("a", "10"), 33.3f)
    // 10 * 33.3 / 100 = 3.33 hours a day, exactly 10 days
    assertEquals(10, task.days())
  }

  @Test
  fun `without the flag the duration stays as it is`() {
    val task = task("20", effortDriven = false)
    task.assign(resource("a", "4"), 100f)
    assertEquals(1, task.days())
  }

  @Test
  fun `turning the flag on derives the duration`() {
    val task = task("20", effortDriven = false)
    task.assign(resource("a", "4"), 100f)
    task.createMutator().also { it.setEffortDriven(true); it.commit() }
    assertEquals(5, task.days())
  }

  @Test
  fun `adding and removing an assignment follows`() {
    val task = task("40")
    val a = resource("a", "4")
    val b = resource("b", "4")
    task.assign(a, 100f)
    assertEquals(10, task.days())
    task.assign(b, 100f)
    assertEquals(5, task.days())
    task.assignmentCollection.getAssignment(b).delete()
    assertEquals(10, task.days())
  }

  @Test
  fun `deleting the assignment from the resource side, as the resource table does, follows`() {
    val task = task("40")
    val a = resource("a", "4")
    val b = resource("b", "4")
    task.assign(a, 100f)
    task.assign(b, 100f)
    assertEquals(5, task.days())
    val assignment = b.assignments.single()
    assignment.delete()
    task.assignmentCollection.deleteAssignment(assignment.resource)
    assertEquals(10, task.days())
  }

  @Test
  fun `changing the load of an assignment follows`() {
    val task = task("40")
    val a = resource("a", "8")
    task.assign(a, 100f)
    assertEquals(5, task.days())
    task.assignmentCollection.getAssignment(a).load = 50f
    assertEquals(10, task.days())
  }

  @Test
  fun `changing hours per day of an assigned resource follows`() {
    val task = task("20")
    val a = resource("a", "2")
    task.assign(a, 100f)
    assertEquals(10, task.days())
    a.hoursPerDay = BigDecimal("4")
    assertEquals(5, task.days())
  }

  @Test
  fun `deleting an assigned resource follows`() {
    val task = task("40")
    val a = resource("a", "4")
    val b = resource("b", "4")
    task.assign(a, 100f)
    task.assign(b, 100f)
    assertEquals(5, task.days())
    b.delete()
    assertEquals(10, task.days())
  }

  @Test
  fun `a task without assignments keeps its duration`() {
    val task = task("20")
    assertEquals(1, task.days())
  }

  @Test
  fun `no hours available keeps the duration`() {
    val task = task("20")
    task.assign(resource("zero hours", "0"), 100f)
    assertEquals(1, task.days())
    task.assign(resource("zero load", "8"), 0f)
    assertEquals(1, task.days())
  }

  @Test
  fun `no effort or zero effort keeps the duration`() {
    val none = task(null)
    val zero = task("0")
    val a = resource("a", "4")
    none.assign(a, 100f)
    zero.assign(a, 100f)
    assertEquals(1, none.days())
    assertEquals(1, zero.days())
  }

  private fun scheduleChangesOf(task: Task, action: () -> Unit): Int {
    var count = 0
    project.taskManager.addTaskListener(object : TaskListenerAdapter() {
      override fun taskScheduleChanged(e: TaskScheduleEvent) {
        if (e.task == task) count++
      }
    })
    action()
    return count
  }

  @Test
  fun `a milestone keeps its duration`() {
    val task = task("20")
    task.createMutator().also { it.setMilestone(true); it.commit() }
    val before = task.days()
    val changes = scheduleChangesOf(task) { task.assign(resource("a", "4"), 100f) }
    assertEquals(0, changes, "the milestone must not be touched")
    assertEquals(before, task.days())
  }

  @Test
  fun `a supertask keeps the duration derived from its children`() {
    val parent = task("100")
    val child = project.taskManager.newTaskBuilder().withName("c").withParent(parent).build()
    child.createMutator().also { it.setDuration(project.taskManager.createLength(3)); it.commit() }
    val changes = scheduleChangesOf(parent) { parent.assign(resource("a", "1"), 100f) }
    assertEquals(0, changes, "the supertask must not be touched")
    assertEquals(3, parent.days())
  }

  @Test
  fun `a duration set by hand is replaced while the flag is on`() {
    val task = task("20")
    task.assign(resource("a", "4"), 100f)
    task.createMutator().also { it.setDuration(project.taskManager.createLength(9)); it.commit() }
    assertEquals(5, task.days())
  }

  @Test
  fun `a duration set by hand stays while the flag is off`() {
    val task = task("20", effortDriven = false)
    task.assign(resource("a", "4"), 100f)
    task.createMutator().also { it.setDuration(project.taskManager.createLength(9)); it.commit() }
    assertEquals(9, task.days())
  }

  @Test
  fun `loading a file with the flag on gives the derived duration`() {
    val loaded = XmlProjectImporter().import("""
      <project name="p">
        <tasks>
          <task id="1" uid="u1" name="T" start="2026-11-09" duration="1" effort-hours="20" effort-driven="true"/>
        </tasks>
        <resources>
          <resource id="0" name="R" function="Default:0" contacts="" phone="" hours-per-day="4"/>
        </resources>
        <allocations>
          <allocation task-id="1" resource-id="0" function="Default:0" responsible="true" load="100.0"/>
        </allocations>
      </project>""".trimIndent())
    assertEquals(5, loaded.taskManager.getTask(1).days())
  }

  @Test
  fun `after a load with events off the algorithm derives the durations`() {
    project.taskManager.setEventsEnabled(false)
    project.humanResourceManager.setEventsEnabled(false)
    val task = task("20")
    task.assign(resource("a", "4"), 100f)
    assertEquals(1, task.days())
    project.taskManager.setEventsEnabled(true)
    project.humanResourceManager.setEventsEnabled(true)
    project.taskManager.algorithmCollection.effortDrivenDurationAlgorithm.run()
    assertEquals(5, task.days())
  }

  @Test
  fun `the file saved before an edit holds the effort and the duration of that moment`() {
    // Undo restores the project from such a file, so effort and the duration derived from it have to be in it together.
    val task = task("20")
    task.assign(resource("a", "4"), 100f)
    val before = save()

    task.createMutator().also { it.setEffort(BigDecimal("40")); it.commit() }
    assertEquals(10, task.days())

    val restored = XmlProjectImporter().import(before).taskManager.getTask(task.taskID)
    assertEquals(BigDecimal("20"), restored.effort)
    assertEquals(5, restored.days())
  }

  @Test
  fun `a milestone made effort driven in one commit keeps its duration and its dates`() {
    // The control is a milestone which is not effort driven: whatever master does to a milestone, this one must match.
    // This pins the behaviour, it does not tell the milestone checks in the algorithm and in the mutator apart:
    // measured with both removed, the test stays green because master keeps the duration of a milestone anyway.
    val control = project.taskManager.newTaskBuilder().withName("c").build()
    control.createMutator().also { it.setMilestone(true); it.commit() }

    val milestone = project.taskManager.newTaskBuilder().withName("m").build()
    milestone.createMutator().also {
      it.setMilestone(true)
      it.setEffort(BigDecimal("20"))
      it.setEffortDriven(true)
      it.commit()
    }
    milestone.assign(resource("a", "4"), 100f)
    assertEquals(control.days(), milestone.days())
    assertEquals(milestone.start.time, milestone.end.time)
  }

  private fun Task.moveStartTo(day: Int) {
    createMutator().also {
      it.setStart(CalendarFactory.createGanttCalendar(2026, 10, day))
      it.commit()
    }
  }

  @Test
  fun `moving the start of an effort driven task derives the duration from the new start`() {
    // Dragging the start of the bar sets only the start, which leaves the end where it was.
    val task = task("20")
    task.assign(resource("a", "4"), 100f)
    task.moveStartTo(9)
    val end = task.end.time
    task.moveStartTo(11)
    assertEquals(5, task.days())
    assertEquals(false, task.end.time == end, "the end has to move along with the start")
  }

  @Test
  fun `moving the start of a task without the flag leaves the end where it was`() {
    val task = task("20", effortDriven = false)
    task.assign(resource("a", "4"), 100f)
    task.moveStartTo(9)
    task.createMutator().also { it.setDuration(project.taskManager.createLength(9)); it.commit() }
    val end = task.end.time
    task.moveStartTo(11)
    assertEquals(end, task.end.time)
    assertEquals(true, task.days() < 9, "the duration shrinks as in master, it was ${task.days()}")
  }

  private fun Task.setEndTo(day: Int) {
    createMutator().also {
      it.setEnd(CalendarFactory.createGanttCalendar(2026, 10, day))
      it.commit()
    }
  }

  @Test
  fun `setting only the end of an effort driven task gives the derived duration and takes the end back`() {
    val task = task("20")
    task.assign(resource("a", "4"), 100f)
    val end = task.end.time
    task.setEndTo(27)
    assertEquals(5, task.days())
    assertEquals(end, task.end.time, "the end has to follow from the start and the derived duration")
  }

  @Test
  fun `setting only the end of a task without the flag keeps the end`() {
    val task = task("20", effortDriven = false)
    task.assign(resource("a", "4"), 100f)
    val end = task.end.time
    task.setEndTo(27)
    assertEquals(false, end == task.end.time, "the end has to stay where it was set")
    assertEquals(true, task.days() > 5, "the duration follows the end as in master, it was ${task.days()}")
  }

  @Test
  fun `moving the start of an effort driven task moves the end along also when the derived duration is the stored one`() {
    val task = task("20")
    task.assign(resource("a", "4"), 100f)
    assertEquals(5, task.days())
    task.moveStartTo(9)
    assertEquals(5, task.days(), "the duration stays the derived one")
    assertEquals(project.taskManager.calendar.shiftDate(task.start.time, task.duration), task.end.time,
      "the end follows from the start and the derived duration")
  }

  private fun dependency(dependant: Task, dependee: Task, type: ConstraintType) {
    project.taskManager.dependencyCollection.createDependency(dependant, dependee, project.taskManager.createConstraint(type))
  }

  private fun plainTask(startDay: Int, days: Long): Task =
    project.taskManager.newTaskBuilder().withName("p").withStartDate(CalendarFactory.createGanttCalendar(2026, 10, startDay).time)
      .withDuration(project.taskManager.createLength(days)).build()

  private fun effortTask(startDay: Int): Task = task("20").also {
    it.assign(resource("r${project.humanResourceManager.resources.size}", "4"), 100f)
    it.moveStartTo(startDay)
  }

  @Test
  fun `a finish-finish dependency moves the whole effort driven task and keeps its derived duration`() {
    val predecessor = plainTask(9, 10)
    val task = effortTask(9)
    dependency(task, predecessor, ConstraintType.finishfinish)
    project.taskManager.algorithmCollection.scheduler.run()
    assertEquals(5, task.days())
    assertEquals(predecessor.end.time, task.end.time, "the dependency is fulfilled")
    assertEquals(project.taskManager.calendar.shiftDate(task.start.time, task.duration), task.end.time)
  }

  @Test
  fun `a finish-start dependency moves the whole effort driven task and keeps its derived duration`() {
    val predecessor = plainTask(9, 10)
    val task = effortTask(9)
    dependency(task, predecessor, ConstraintType.finishstart)
    project.taskManager.algorithmCollection.scheduler.run()
    assertEquals(5, task.days())
    assertEquals(true, task.start.time >= predecessor.end.time, "the dependency is fulfilled")
    assertEquals(project.taskManager.calendar.shiftDate(task.start.time, task.duration), task.end.time)
  }

  @Test
  fun `a chain of two effort driven tasks is scheduled and ends`() {
    val predecessor = plainTask(9, 10)
    val first = effortTask(9)
    val second = effortTask(9)
    dependency(first, predecessor, ConstraintType.finishfinish)
    dependency(second, first, ConstraintType.finishfinish)
    assertTimeoutPreemptively(Duration.ofSeconds(30)) { project.taskManager.algorithmCollection.scheduler.run() }
    assertEquals(predecessor.end.time, first.end.time)
    assertEquals(first.end.time, second.end.time)
    assertEquals(5, first.days())
    assertEquals(5, second.days())
  }

  private fun save(): String {
    val out = ByteArrayOutputStream()
    GanttXMLSaver(project).save(out)
    return out.toString(Charsets.UTF_8)
  }
}
