/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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
package net.sourceforge.ganttproject.gui.resourceproperties

import biz.ganttproject.app.ErrorPane
import biz.ganttproject.app.PropertySheetBuilder
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.i18n
import biz.ganttproject.colorFromUiManager
import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.model.task.WorkWeekSchedule
import biz.ganttproject.core.model.task.formatWorkWeek
import biz.ganttproject.core.model.task.parseWorkWeek
import biz.ganttproject.core.option.ObservableChoice
import biz.ganttproject.core.option.ObservableDouble
import biz.ganttproject.core.option.ObservableMoney
import biz.ganttproject.core.option.ObservableString
import biz.ganttproject.core.option.ValidationException
import biz.ganttproject.core.option.ValueValidator
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.geometry.Insets
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.roles.Role
import net.sourceforge.ganttproject.roles.RoleManager
import java.time.DayOfWeek

class MainPropertiesPanel(private val resource: HumanResource, private val projectWeek: Set<DayOfWeek>? = null) {
  constructor(resource: HumanResource, calendar: GPCalendar) : this(resource, projectWorkWeek(calendar))


  val title: String = RootLocalizer.formatText("general")
  val fxComponent by lazy { getFxNode() }
  val validationErrors = FXCollections.observableArrayList<String>()

  private val nameOption = ObservableString("name", resource.name)
  private val phoneOption = ObservableString("phone", resource.phone)
  private val emailOption = ObservableString("email", resource.mail)
  private val roleOption = ObservableChoice<Role>("role", resource.role,
    RoleManager.Access.getInstance().getEnabledRoles().toList(), roleStringConverter)
  internal val workWeekOption = ObservableString("workWeek", formatWorkWeek(resource.workWeek), workWeekValidator)
  private val rateOption = ObservableMoney("standardRate", resource.standardPayRate)
  private val totalCostOption = ObservableMoney("totalCost", resource.totalCost).also {
    it.setWritable(false)
  }
  private val totalLoadOption = ObservableDouble("totalLoad", resource.totalLoad).also {
    it.setWritable(false)
  }
  private var onRequestFocus = {}

  private fun getFxNode() = StackPane().apply {
    background = Background(BackgroundFill("Panel.background".colorFromUiManager(), CornerRadii.EMPTY, Insets.EMPTY))
    val pane = PropertySheetBuilder(i18n).pane {
      stylesheet("/biz/ganttproject/task/TaskPropertiesDialog.css")
      title("section.main")
      text(nameOption)
      text(phoneOption)
      text(emailOption)
      dropdown(roleOption)
      text(workWeekOption)

      skip()
      title("section.rate")
      money(rateOption)
      money(totalCostOption)
      numeric(totalLoadOption)
    }
    pane.validationErrors.addListener(MapChangeListener {
      validationErrors.setAll(pane.validationErrors.values)
    })
    // A week without a working day, or without a day in common with the project, is accepted, so it is a warning below
    // the fields, not a validation error.
    val workWeekHint = ErrorPane()
    workWeekOption.addWatcher { workWeekHint.message(workWeekWarning(it.newValue, projectWeek), ErrorPane.MessageType.WARNING) }
    workWeekHint.message(workWeekWarning(workWeekOption.value, projectWeek), ErrorPane.MessageType.WARNING)
    onRequestFocus = pane::requestFocus
    children.add(VBox(pane.node, workWeekHint.fxNode))
  }

  fun requestFocus() = onRequestFocus()

  fun save() {
    nameOption.ifChanged(resource::setName)
    phoneOption.ifChanged(resource::setPhone)
    emailOption.ifChanged(resource::setMail)
    roleOption.ifChanged(resource::setRole)
    rateOption.ifChanged(resource::setStandardPayRate)
    workWeekOption.ifChanged { text ->
      // An unreadable week is shown as an error and keeps the stored one.
      val workWeek = parseWorkWeek(text ?: "") ?: return@ifChanged
      if (WorkWeekSchedule(workWeek).changes != resource.workWeek) {
        resource.workWeek = workWeek
      }
    }
  }

}

private val workWeekValidator = ValueValidator { text ->
  if (parseWorkWeek(text) == null) {
    throw ValidationException(i18n.formatText("workWeek.error", text))
  }
  text
}

/** The weekdays which the project calendar sets as working days. Exceptions of single dates are not looked at. */
internal fun projectWorkWeek(calendar: GPCalendar): Set<DayOfWeek> = DayOfWeek.values().filter {
  // java.util.Calendar numbers Sunday as 1, java.time numbers it as 7.
  calendar.getWeekDayType(it.value % 7 + 1) == GPCalendar.DayType.WORKING
}.toSet()

/**
 * The warning for a work week with a change on which the resource works on no day, or, if the week of the project is
 * given, with a change whose days are all free in the project, or null. Such a week is valid: the effort driven tasks
 * of the resource just make no progress from that date on, because a task only works on working days of the project.
 */
internal fun workWeekWarning(text: String?, projectWeek: Set<DayOfWeek>? = null): String? {
  val changes = parseWorkWeek(text ?: "") ?: return null
  changes.firstOrNull { it.days.isEmpty() }?.let { noDay ->
    return noDay.from?.let { i18n.formatText("workWeek.warning.noDayFrom", it.toString()) }
      ?: i18n.formatText("workWeek.warning.noDay")
  }
  if (projectWeek != null && changes.any { change -> change.days.none { it in projectWeek } }) {
    return i18n.formatText("workWeek.warning.noProjectDay")
  }
  return null
}

private val roleStringConverter = object : StringConverter<Role>() {
  override fun toString(role: Role): String  = role.name
  override fun fromString(string: String): Role? = RoleManager.Access.getInstance().getRole(string)
}

private val i18n = i18n {
  // We will search for the translation corresponding to a structured key in the current language only.
  default(withFallback = false)
  prefix("option.personProperties.main") {
    // If there is no translation, we'll search for the translation corresponding to the previously used unstructured key,
    // again in the current language only.
    default(withFallback = false)
    transform { key ->
      val key1 = when {
        key.endsWith(".label") -> key.removeSuffix(".label")
        else -> key
      }
      val map = mapOf(
        "phone" to "colPhone",
        "email" to "colMail",
        "role" to "colRole",
        "standardRate" to "colStandardRate",
        "totalCost" to "colTotalCost",
        "totalLoad" to "colTotalLoad",
        "section.rate" to "optionGroup.resourceRate.label"
      )
      map[key1] ?: key1
    }
    fallback {
      // Finally, we'll use the English translation of a structured key.
      default()
      prefix("option.personProperties.main")
    }
  }
}
