/*
 * Copyright (c) 2021 Dmitry Barashev, BarD Software s.r.o.
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

package biz.ganttproject.lib

import biz.ganttproject.app.Localizer
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.lib.fx.vbox
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.TitledPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import net.sourceforge.ganttproject.chart.Chart
import net.sourceforge.ganttproject.language.GanttLanguage
import org.w3c.util.DateParser
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

/**
 * Just a range of dates with some identifier. Although it looks like generic, in practice there are three
 * date ranges: "view" (with the dates corresponding to the current chart view), "project" (with the dates matching the
 * project date range) and "custom" (with user-defined date range).
 */
data class DateRange(val startDate: Date, val endDate: Date, val id: String, val isEditable: Boolean) {
  val persistentValue: String
    get() =
      when (id) {
        "custom" -> "${DateParser.getIsoDateNoHours(startDate)} ${DateParser.getIsoDateNoHours(endDate)}"
        else -> id
      }

  val rangeLabel: String = GanttLanguage.getInstance().let { "${it.formatShortDate(this.startDate)}..${it.formatShortDate(this.endDate)}" }

  fun asClosedRange() = startDate.rangeTo(endDate)
  fun withStartDate(newStartDate: Date) = DateRange(newStartDate, endDate, id, isEditable)
  fun withEndDate(newEndDate: Date) = DateRange(startDate, newEndDate, id, isEditable)
}

/**
 * The model class for the date picker component. It keeps all available date ranges and the selected date range.
 */
class DateRangePickerModel(chart: Chart) {
  val allRanges: List<DateRange> get() = listOf(rangeCurrentView, rangeWholeProject, rangeCustom)

  private val rangeCurrentView = DateRange(
    chart.startDate, chart.endDate, "view", false
  )
  private val rangeWholeProject = if (chart.project.taskManager.projectLength.value > 0.0) {
    DateRange(
      chart.project.taskManager.projectStart, chart.project.taskManager.projectEnd, "project", false
    )
  } else {
    DateRange(chart.startDate, chart.endDate, "project", false)
  }
  var rangeCustom = DateRange(
    chart.startDate, chart.endDate, "custom", true
  )
  set(value) {
    val current = field
    field = value
    if (current == selectedRange.value) {
      selectedRange.set(value)
    }
  }
  val selectedRange = SimpleObjectProperty(rangeCurrentView)

  fun init(value: String) {
    selectedRange.set(when (value) {
      "view" -> rangeCurrentView
      "project" -> rangeWholeProject
      else -> value.split(" ", limit = 2).let {
        rangeCustom.withStartDate(DateParser.parse(it[0])).withEndDate(DateParser.parse(it[1]))
      }
    })
  }
}

/**
 * The date range picker component. It consists of a dropdown, a range label and optional control with the date pickers.
 * The dropdown lists the names of the possible date ranges, the range label displays the start and end date of the selected
 * range and date pickers allow for choosing the start and end dates if the selected range is "custom".
 */
class DateRangePicker(private val model: DateRangePickerModel, private val i18n: Localizer = RootLocalizer) {
  private val dropdown = ComboBox(FXCollections.observableList(model.allRanges.map { i18n.formatText(it.id) }.toList())).also { dropdown ->
    dropdown.setOnAction {
      model.selectedRange.set(model.allRanges[dropdown.selectionModel.selectedIndex])
    }
  }

  private val expandablePane = TitledPane()
  private val startDatePicker = DatePicker(
    LocalDate.ofInstant(model.rangeCustom.startDate.toInstant(), ZoneId.systemDefault())
  ).also { dp ->
    dp.onAction = EventHandler {
      model.rangeCustom = model.rangeCustom.withStartDate(dp.value.asDate())
    }
  }
  private val endDatePicker = DatePicker(
    LocalDate.ofInstant(model.rangeCustom.endDate.toInstant(), ZoneId.systemDefault())
  ).also { dp ->
    dp.onAction = EventHandler {
      model.rangeCustom = model.rangeCustom.withEndDate(dp.value.asDate())
    }
  }

  private fun updateSelectedRange(range: DateRange) {
    dropdown.selectionModel.select(i18n.formatText(range.id))
    if (range.id == "custom") {
      expandablePane.content = vbox {
        add(Label(i18n.formatText("option.export.range.start.label")).also {
          VBox.setMargin(it, Insets(5.0, 0.0, 3.0, 0.0))
        })
        add(startDatePicker)
        add(Label(i18n.formatText("option.export.range.end.label")).also {
          VBox.setMargin(it, Insets(5.0, 0.0, 3.0, 0.0))
        })
        add(endDatePicker)
      }
      expandablePane.isExpanded = true
      expandablePane.isCollapsible = false
    } else {
      expandablePane.content = Pane()
      expandablePane.isExpanded = false
      expandablePane.isCollapsible = false
    }
    expandablePane.text = range.rangeLabel
  }
  val component by lazy {
    model.selectedRange.addListener { _, _, newRange -> updateSelectedRange(newRange) }
    updateSelectedRange(model.selectedRange.value)
    vbox {
      add(dropdown)
      add(expandablePane)
    }
  }
}

private fun LocalDate.asDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
