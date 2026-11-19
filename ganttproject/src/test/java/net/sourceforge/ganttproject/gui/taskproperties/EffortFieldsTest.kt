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

import biz.ganttproject.core.option.ObservableBoolean
import biz.ganttproject.core.option.ValidationException
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.gui.resourceproperties.MainPropertiesPanel
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.hasDerivedDuration
import net.sourceforge.ganttproject.task.isDurationDerived
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Locale

class EffortFieldsTest {
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

  private fun newTask(): Task = project.taskManager.newTaskBuilder().withName("t").build()

  @Test
  fun `an empty field is not set, a number is taken as it is`() {
    assertNull(parseDecimal("", BigDecimal.ZERO))
    assertNull(parseDecimal("  ", BigDecimal.ZERO))
    assertNull(parseDecimal(null, BigDecimal.ZERO))
    assertEquals(BigDecimal("12.5"), parseDecimal(" 12.5 ", BigDecimal.ZERO))
    assertEquals(BigDecimal.ZERO, parseDecimal("0", BigDecimal.ZERO))
  }

  @Test
  fun `a negative effort and a text are rejected`() {
    assertThrows(ValidationException::class.java) { effortValidator.parse("-1") }
    assertThrows(ValidationException::class.java) { effortValidator.parse("eight") }
    effortValidator.parse("8")
    effortValidator.parse("")
  }

  @Test
  fun `hours per day must be more than zero and at most a day`() {
    assertThrows(ValidationException::class.java) { hoursPerDayValidator.parse("0") }
    assertThrows(ValidationException::class.java) { hoursPerDayValidator.parse("-2") }
    assertThrows(ValidationException::class.java) { hoursPerDayValidator.parse("24.5") }
    assertThrows(ValidationException::class.java) { hoursPerDayValidator.parse("x") }
    assertEquals(BigDecimal("24"), parseHoursPerDay("24"))
    assertEquals(BigDecimal("0.5"), parseHoursPerDay("0.5"))
    assertNull(parseHoursPerDay(""))
  }

  @Test
  fun `an empty hours per day is rejected if the resource had a value`() {
    val validator = hoursPerDayValidatorFor(BigDecimal("7.5"))
    assertThrows(ValidationException::class.java) { validator.parse("") }
    assertThrows(ValidationException::class.java) { validator.parse("  ") }
    assertThrows(ValidationException::class.java) { validator.parse("0") }
    assertThrows(ValidationException::class.java) { validator.parse("25") }
    validator.parse("24")
    validator.parse("6,5")
  }

  @Test
  fun `an empty hours per day is accepted if the resource had none`() {
    val validator = hoursPerDayValidatorFor(null)
    validator.parse("")
    validator.parse("  ")
    assertThrows(ValidationException::class.java) { validator.parse("0") }
    assertThrows(ValidationException::class.java) { validator.parse("25") }
    validator.parse("24")
  }

  @Test
  fun `effort and the switch are written to the task on commit`() {
    val task = newTask()
    val fields = TaskEffortFields(task)
    assertNull(task.effort)
    assertFalse(task.isEffortDriven)

    fields.effortOption.value = "16"
    fields.effortDrivenOption.value = true
    task.createMutator().also { fields.save(it); it.commit() }

    assertEquals(BigDecimal("16"), task.effort)
    assertTrue(task.isEffortDriven)
  }

  @Test
  fun `the dialog is filled from the task and an emptied field clears the effort`() {
    val task = newTask()
    task.createMutator().also { it.setEffort(BigDecimal("40")); it.setEffortDriven(true); it.commit() }

    val fields = TaskEffortFields(task)
    assertEquals("40", fields.effortOption.value)
    assertTrue(fields.effortDrivenOption.value)

    fields.effortOption.value = ""
    task.createMutator().also { fields.save(it); it.commit() }
    assertNull(task.effort)
  }

  @Test
  fun `a task which was not touched in the dialog keeps its values`() {
    val task = newTask()
    task.createMutator().also { it.setEffort(BigDecimal("8.50")); it.commit() }
    val fields = TaskEffortFields(task)
    task.createMutator().also { fields.save(it); it.commit() }
    assertEquals(BigDecimal("8.50"), task.effort)
  }

  @Test
  fun `the duration which follows from the effort is there after the commit`() {
    val resource = project.humanResourceManager.create("a", 0).also { it.hoursPerDay = BigDecimal("4") }
    val task = newTask()
    task.assignmentCollection.addAssignment(resource).load = 100f
    val fields = TaskEffortFields(task)
    fields.effortOption.value = "20"
    fields.effortDrivenOption.value = true
    task.createMutator().also { fields.save(it); it.commit() }
    assertEquals(5, task.duration.length)
  }

  @Test
  fun `only a task which is not a milestone or a summary task has its duration derived`() {
    assertTrue(isDurationDerived(isEffortDriven = true, isMilestone = false, isSummary = false))
    assertFalse(isDurationDerived(isEffortDriven = false, isMilestone = false, isSummary = false))
    assertFalse(isDurationDerived(isEffortDriven = true, isMilestone = true, isSummary = false))
    assertFalse(isDurationDerived(isEffortDriven = true, isMilestone = false, isSummary = true))
  }

  @Test
  fun `the duration of a task is derived while the switch is on, except for milestones and summary tasks`() {
    val task = newTask()
    assertFalse(task.hasDerivedDuration())

    task.createMutator().also { it.setEffortDriven(true); it.commit() }
    assertTrue(task.hasDerivedDuration())

    task.createMutator().also { it.setMilestone(true); it.commit() }
    assertFalse(task.hasDerivedDuration())

    val summary = newTask().also { it.createMutator().also { m -> m.setEffortDriven(true); m.commit() } }
    project.taskManager.newTaskBuilder().withName("child").withParent(summary).build()
    assertFalse(summary.hasDerivedDuration())
  }

  @Test
  fun `a derived duration locks the duration and the end, the start stays free`() {
    for (part in CalculatedPart.entries) {
      val w = datesWritability(isMilestone = false, isDurationDerived = true, calculatedPart = part)
      assertEquals(DatesWritability(start = true, end = false, duration = false, scheduling = false), w, "$part")
    }
  }

  @Test
  fun `without the switch the dates are editable as before`() {
    assertEquals(DatesWritability(false, true, true, true), datesWritability(false, false, CalculatedPart.START))
    assertEquals(DatesWritability(true, false, true, true), datesWritability(false, false, CalculatedPart.END))
    assertEquals(DatesWritability(true, true, false, true), datesWritability(false, false, CalculatedPart.DURATION))
    assertEquals(DatesWritability(true, false, false, false), datesWritability(true, false, CalculatedPart.END))
  }

  @Test
  fun `the switch in the dialog locks and unlocks the fields of the controller`() {
    val task = newTask()
    val milestone = ObservableBoolean("milestone", false)
    val effortDriven = ObservableBoolean("effortDriven", false)
    val controller = TaskDatesController(task, milestone, effortDriven, CoroutineScope(Dispatchers.Unconfined))
    assertTrue(controller.durationOption.isWritable.value)
    assertTrue(controller.startDateOption.isWritable.value)

    effortDriven.value = true
    assertFalse(controller.durationOption.isWritable.value)
    assertFalse(controller.displayEndDateOption.isWritable.value)
    assertTrue(controller.startDateOption.isWritable.value)

    effortDriven.value = false
    assertTrue(controller.durationOption.isWritable.value)
    assertFalse(controller.displayEndDateOption.isWritable.value)
  }

  @Test
  fun `hours per day of a resource are written when the dialog is saved`() {
    val resource = project.humanResourceManager.create("a", 0)
    val panel = MainPropertiesPanel(resource)
    val option = panel.hoursPerDayOption
    assertNull(resource.hoursPerDay)

    option.value = "6.5"
    panel.save()
    assertEquals(BigDecimal("6.5"), resource.hoursPerDay)

    option.value = ""
    panel.save()
    assertNull(resource.hoursPerDay)
  }

  @Test
  fun `a comma is accepted as the decimal separator`() {
    assertEquals(BigDecimal("20.5"), parseDecimal("20,5", BigDecimal.ZERO))
    assertEquals(BigDecimal("6.5"), parseHoursPerDay(" 6,5 "))
    assertEquals(BigDecimal("20.5"), effortValidator.let { it.parse("20,5"); parseDecimal("20.5", BigDecimal.ZERO) })
    // A comma next to a point, or more than one comma, is not a number.
    assertThrows(ValidationException::class.java) { parseDecimal("1,000.5", BigDecimal.ZERO) }
    assertThrows(ValidationException::class.java) { parseDecimal("1,0,5", BigDecimal.ZERO) }
    // The comma does not get around the bounds.
    assertThrows(ValidationException::class.java) { parseHoursPerDay("24,5") }
  }

  @Test
  fun `actual effort is written to the task on commit and does not change the duration`() {
    val task = newTask()
    val before = task.duration.length
    val fields = TaskEffortFields(task)
    assertEquals("", fields.actualEffortOption.value ?: "")
    fields.actualEffortOption.value = "3,5"
    task.createMutator().also { fields.save(it); it.commit() }
    assertEquals(0, BigDecimal("3.5").compareTo(task.actualEffort))
    assertEquals(before, task.duration.length)
    assertNull(task.effort)
  }

  @Test
  fun `the actual effort field is filled from the task, an emptied field clears it and a negative one is rejected`() {
    val task = newTask()
    task.createMutator().also { it.setActualEffort(BigDecimal("5")); it.commit() }
    val fields = TaskEffortFields(task)
    assertEquals("5", fields.actualEffortOption.value)
    assertThrows(ValidationException::class.java) { effortValidator.parse("-2") }

    fields.actualEffortOption.value = ""
    task.createMutator().also { fields.save(it); it.commit() }
    assertNull(task.actualEffort)
  }

  @Test
  fun `the actual effort field shows the sum over the assignments and is then read only`() {
    val task = newTask()
    task.createMutator().also { it.setActualEffort(BigDecimal("9")); it.commit() }
    val a = task.assignmentCollection.addAssignment(project.humanResourceManager.create("a", 0))
    task.assignmentCollection.addAssignment(project.humanResourceManager.create("b", 1)).actualEffort = BigDecimal("1.5")
    a.actualEffort = BigDecimal("2")
    assertEquals("2", actualEffortText(a))
    val fields = TaskEffortFields(task)
    assertEquals("3.5", fields.actualEffortOption.value)
    assertFalse(fields.actualEffortOption.isWritable.value)

    fields.actualEffortOption.value = "1"
    task.createMutator().also { fields.save(it); it.commit() }
    assertEquals(BigDecimal("9"), task.actualEffort, "a read only field does not write the task's own value")
  }

  @Test
  fun `the effort field takes a comma and writes the number to the task`() {
    val task = newTask()
    val fields = TaskEffortFields(task)
    fields.effortOption.value = "12,5"
    task.createMutator().also { fields.save(it); it.commit() }
    assertEquals(0, BigDecimal("12.5").compareTo(task.effort))
  }

  @Test
  fun `the table of assignments shows the hours per day of the resource`() {
    val withHours = project.humanResourceManager.create("a", 0).also { it.hoursPerDay = BigDecimal("6.5") }
    val withoutHours = project.humanResourceManager.create("b", 1)
    val task = newTask()
    val a = task.assignmentCollection.addAssignment(withHours)
    val b = task.assignmentCollection.addAssignment(withoutHours)
    assertEquals("6.5", hoursPerDayText(a))
    assertEquals("", hoursPerDayText(b))
    assertEquals("", hoursPerDayText(null))
  }

  @Test
  fun `the resource dialog rejects an empty hours per day only if the resource had a value`() {
    val withHours = project.humanResourceManager.create("a", 0).also { it.hoursPerDay = BigDecimal("7.5") }
    val withoutHours = project.humanResourceManager.create("b", 1)
    assertThrows(ValidationException::class.java) { MainPropertiesPanel(withHours).hoursPerDayOption.validator.parse("") }
    MainPropertiesPanel(withoutHours).hoursPerDayOption.validator.parse("")
  }

  @Test
  fun `the OK button is disabled as long as there is a validation error`() {
    val errors = javafx.collections.FXCollections.observableArrayList<String>()
    val disabled = javafx.beans.property.SimpleBooleanProperty(false)
    net.sourceforge.ganttproject.gui.bindDisabledToErrors(errors, disabled)
    assertFalse(disabled.get())
    errors.add("hours per day: 25 is not allowed")
    assertTrue(disabled.get())
    errors.clear()
    assertFalse(disabled.get())
  }

  @Test
  fun `a resource dialog with a validation error does not write the last valid text`() {
    val resource = project.humanResourceManager.create("a", 0)
    val panel = MainPropertiesPanel(resource)
    // Typing 25 passes "2", which is valid, and stops at "25", which is not.
    panel.hoursPerDayOption.value = "2"
    panel.validationErrors.add("25 is not allowed")
    panel.save()
    assertNull(resource.hoursPerDay)
    panel.validationErrors.clear()
    panel.save()
    assertEquals(0, BigDecimal("2").compareTo(resource.hoursPerDay))
  }

  @Test
  fun `original effort is written to the task on commit and does not change the duration`() {
    val task = newTask()
    val before = task.duration.length
    val fields = TaskEffortFields(task)
    assertEquals("", fields.originalEffortOption.value ?: "")
    fields.originalEffortOption.value = "9,5"
    task.createMutator().also { fields.save(it); it.commit() }
    assertEquals(0, BigDecimal("9.5").compareTo(task.originalEffort))
    assertEquals(before, task.duration.length)
    assertNull(task.effort)
  }

  @Test
  fun `the original effort field is filled from the task and an emptied field clears it`() {
    val task = newTask()
    task.createMutator().also { it.setOriginalEffort(BigDecimal("5")); it.commit() }
    val fields = TaskEffortFields(task)
    assertEquals("5", fields.originalEffortOption.value)

    fields.originalEffortOption.value = ""
    task.createMutator().also { fields.save(it); it.commit() }
    assertNull(task.originalEffort)
  }
}
