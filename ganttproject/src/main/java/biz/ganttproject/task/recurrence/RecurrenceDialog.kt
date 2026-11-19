/*
Copyright 2026 BarD Software s.r.o., GanttProject Cloud OU

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
package biz.ganttproject.task.recurrence

import biz.ganttproject.app.DialogController
import biz.ganttproject.app.Localizer
import biz.ganttproject.app.PropertySheetBuilder
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.createDateConverter
import biz.ganttproject.app.dialog
import biz.ganttproject.core.model.task.RecurrenceProblem
import biz.ganttproject.core.model.task.RecurrenceRule
import biz.ganttproject.core.model.task.RecurrenceUnit
import biz.ganttproject.core.option.ObservableDate
import biz.ganttproject.core.option.ObservableEnum
import biz.ganttproject.core.option.ObservableInt
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapBoth
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.time.LocalDate
import javax.swing.SwingUtilities

/** How a series ends in the dialog: on a date or after a number of tasks. */
enum class RecurrenceEnd { UNTIL, COUNT }

/** Why the values of the dialog fields do not make a rule. */
enum class RecurrenceFieldProblem { INTERVAL_NOT_POSITIVE, COUNT_NOT_POSITIVE, NO_END, BOTH_ENDS }

/**
 * Makes a rule from the values of the dialog fields. Exactly one of [until] and [count] must be set.
 */
fun ruleFromFields(unit: RecurrenceUnit, interval: Int?, until: LocalDate?, count: Int?): Result<RecurrenceRule, RecurrenceFieldProblem> = when {
  interval == null || interval < 1 -> Err(RecurrenceFieldProblem.INTERVAL_NOT_POSITIVE)
  until == null && count == null -> Err(RecurrenceFieldProblem.NO_END)
  until != null && count != null -> Err(RecurrenceFieldProblem.BOTH_ENDS)
  count != null && count < 1 -> Err(RecurrenceFieldProblem.COUNT_NOT_POSITIVE)
  else -> Ok(RecurrenceRule(unit, interval, until, count))
}

/** What a rule would give: the number of tasks, the source task included, and the first and the last date. */
data class RecurrencePreview(val count: Int, val first: LocalDate, val last: LocalDate)

fun previewOf(rule: RecurrenceRule, first: LocalDate, isWorkingDay: (LocalDate) -> Boolean): Result<RecurrencePreview, RecurrenceProblem> {
  rule.problemFor(first, isWorkingDay)?.let { return Err(it) }
  val dates = rule.occurrences(first, isWorkingDay)
  return Ok(RecurrencePreview(dates.size, dates.first(), dates.last()))
}

/**
 * The fields of the recurrence dialog and what follows from them. Knows nothing about the controls, so that it can
 * be tested without them.
 */
class RecurrenceDialogModel(private val task: Task, taskManager: TaskManager, private val i18n: Localizer = RootLocalizer) {
  private val first = task.start.time.toModelLocalDate()
  private val isWorkingDay = workingDayTest(taskManager.calendar)
  private val currentRule = task.recurrenceRule

  val unitOption = ObservableEnum("task.recurrence.unit", currentRule?.unit ?: RecurrenceUnit.WEEK, RecurrenceUnit.values())
  val intervalOption = ObservableInt("task.recurrence.interval", currentRule?.interval ?: 1)
  val endOption = ObservableEnum("task.recurrence.end",
    if (currentRule?.count != null) RecurrenceEnd.COUNT else RecurrenceEnd.UNTIL, RecurrenceEnd.values())
  val untilOption = ObservableDate("task.recurrence.until", currentRule?.until ?: first.plusMonths(3))
  val countOption = ObservableInt("task.recurrence.count", currentRule?.count ?: 10)

  val hasRule: Boolean get() = currentRule != null

  init {
    updateWritable()
    endOption.addWatcher { updateWritable() }
  }

  private fun updateWritable() {
    untilOption.setWritable(endOption.value == RecurrenceEnd.UNTIL)
    countOption.setWritable(endOption.value == RecurrenceEnd.COUNT)
  }

  /** The rule of the fields; only the end field which the end switch selects counts. */
  val rule: Result<RecurrenceRule, RecurrenceFieldProblem> get() = when (endOption.value) {
    RecurrenceEnd.UNTIL -> ruleFromFields(unitOption.value, intervalOption.value, untilOption.value, null)
    RecurrenceEnd.COUNT -> ruleFromFields(unitOption.value, intervalOption.value, null, countOption.value)
  }

  /** The rule which can be applied, or null. */
  val validRule: RecurrenceRule? get() = rule.get()?.takeIf { previewOf(it, first, isWorkingDay).isOk }

  /** The text under the fields: the preview, or why the fields cannot be applied. */
  val previewText: String get() = rule.mapBoth(
    success = { rule ->
      previewOf(rule, first, isWorkingDay).mapBoth(
        success = { preview ->
          i18n.formatText("task.recurrence.preview",
            preview.count, dateConverter.toString(preview.first), dateConverter.toString(preview.last))
        },
        failure = { problem ->
          when (problem) {
            RecurrenceProblem.END_BEFORE_START -> i18n.formatText("task.recurrence.error.endBeforeStart", dateConverter.toString(first))
            RecurrenceProblem.TOO_MANY -> i18n.formatText("task.recurrence.error.tooMany", RecurrenceRule.MAX_OCCURRENCES)
          }
        }
      )
    },
    failure = { problem ->
      i18n.formatText(when (problem) {
        RecurrenceFieldProblem.INTERVAL_NOT_POSITIVE -> "task.recurrence.error.intervalNotPositive"
        RecurrenceFieldProblem.COUNT_NOT_POSITIVE -> "task.recurrence.error.countNotPositive"
        RecurrenceFieldProblem.NO_END -> "task.recurrence.error.noEnd"
        RecurrenceFieldProblem.BOTH_ENDS -> "task.recurrence.error.bothEnds"
      })
    }
  )

  fun addWatcher(watcher: () -> Unit) {
    unitOption.addWatcher { watcher() }
    intervalOption.addWatcher { watcher() }
    endOption.addWatcher { watcher() }
    untilOption.addWatcher { watcher() }
    countOption.addWatcher { watcher() }
  }

  private val dateConverter by lazy { createDateConverter() }
}

/** Opens the dialog which sets or removes the recurrence rule of [task]. */
fun showRecurrenceDialog(task: Task, taskManager: TaskManager, undoManager: GPUndoManager) {
  val model = RecurrenceDialogModel(task, taskManager)
  dialog(RootLocalizer.formatText("task.recurrence.title", task.name.orEmpty()), "task.recurrence") { controller: DialogController ->
    controller.addStyleSheet("/biz/ganttproject/app/Dialog.css")
    controller.addStyleClass("dlg")
    val propertySheet = PropertySheetBuilder(RootLocalizer).pane {
      dropdown(model.unitOption)
      numeric(model.intervalOption) {
        minValue = 1
      }
      dropdown(model.endOption)
      date(model.untilOption)
      numeric(model.countOption) {
        minValue = 1
      }
    }
    val preview = Label().also { it.styleClass.add("task-recurrence-preview") }
    controller.setContent(VBox(propertySheet.node, preview))

    var applyButton: Button? = null
    val update = {
      preview.text = model.previewText
      applyButton?.isDisable = model.validRule == null
    }
    controller.setupButton(ButtonType.APPLY) { button ->
      button.styleClass.add("btn-attention")
      applyButton = button
      button.setOnAction {
        model.validRule?.let { rule ->
          SwingUtilities.invokeLater {
            setRecurrence(task, rule, taskManager, undoManager, RootLocalizer.formatText("task.recurrence.edit.apply"))
          }
        }
        controller.hide()
      }
    }
    if (model.hasRule) {
      controller.setupButton(ButtonType(RootLocalizer.formatText("task.recurrence.remove"), ButtonBar.ButtonData.LEFT)) { button ->
        button.styleClass.addAll("btn-regular", "secondary")
        button.setOnAction {
          SwingUtilities.invokeLater {
            removeRecurrence(task, undoManager, RootLocalizer.formatText("task.recurrence.edit.remove"))
          }
          controller.hide()
        }
      }
    }
    controller.setupButton(ButtonType.CANCEL) { button ->
      button.styleClass.addAll("btn-regular", "secondary")
      button.setOnAction { controller.hide() }
    }
    model.addWatcher { update() }
    update()
  }
}
