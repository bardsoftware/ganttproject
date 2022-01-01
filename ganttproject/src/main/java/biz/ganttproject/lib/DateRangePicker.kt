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
import biz.ganttproject.lib.fx.VBoxBuilder
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.chart.Chart
import net.sourceforge.ganttproject.language.GanttLanguage
import org.controlsfx.control.PopOver
import org.w3c.util.DateParser
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

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

class DateRangePickerModel(chart: Chart) {
  val rangeCurrentView = DateRange(
    chart.startDate, chart.endDate, "view", false
  )
  val rangeWholeProject = if (chart.project.taskManager.projectLength.value > 0.0) {
    DateRange(
      chart.project.taskManager.projectStart, chart.project.taskManager.projectEnd, "project", false
    )
  } else {
    DateRange(chart.startDate, chart.endDate, "project", false)
  }
  var rangeCustom = DateRange(
    chart.startDate, chart.endDate, "custom", true
  )
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
class DateRangePicker(private val model: DateRangePickerModel, private val i18n: Localizer = RootLocalizer) {
  private val selectedRangeText: String get() =
    model.selectedRange.get().let { "${i18n.formatText(it.id)}: ${it.rangeLabel}" }
  val button = Button(selectedRangeText).also { btn ->
    btn.onAction = EventHandler {
      PopOver(popoverContent).also {
        it.arrowLocation = PopOver.ArrowLocation.TOP_CENTER
        it.show(btn)
      }
    }
    model.selectedRange.addListener { _, _, _ ->
      btn.text = selectedRangeText
      btn.parent.requestLayout()
    }
  }
  val component: Control get() = button

  private fun createRadioButton(toggleGroup: ToggleGroup, dateRange: DateRange) =
    RadioButton().also { radio ->
      radio.toggleGroup = toggleGroup
      radio.contentDisplay = ContentDisplay.GRAPHIC_ONLY
      radio.graphic = VBoxBuilder("section").let {
        it.addTitle(i18n.formatText(dateRange.id))
        it.add(Label(dateRange.rangeLabel).also { label ->
          label.styleClass.add("helpline")
        })
        it.vbox
      }
      radio.userData = dateRange
    }

  private val popoverContent: Node by lazy {
    val toggleGroup = ToggleGroup().also {
      it.selectedToggleProperty().addListener { _, _, _ ->
        it.toggles.forEach { toggle ->
          (toggle as RadioButton).graphic.styleClass.remove("selected")
          toggle.graphic.isDisable = true
        }
        it.selectedToggle.let { toggle ->
          (toggle as RadioButton).graphic.styleClass.add("selected")
          toggle.graphic.isDisable = false
        }
      }
    }
    val radioCurrentView = createRadioButton(toggleGroup, model.rangeCurrentView)
    val radioWholeProject = createRadioButton(toggleGroup, model.rangeWholeProject)
    val radioCustom = RadioButton().also { radio ->
      radio.contentDisplay = ContentDisplay.GRAPHIC_ONLY
      radio.toggleGroup = toggleGroup
      radio.userData = model.rangeCustom
      radio.graphic = VBoxBuilder("section").let {
        it.addTitle("Custom")
        it.add(GridPane().also { grid ->
          grid.vgap = 5.0
          grid.add(Label(i18n.formatText("option.export.range.start.label")), 0, 0)
          val startDatePicker = DatePicker(
            LocalDate.ofInstant(model.rangeCustom.startDate.toInstant(), ZoneId.systemDefault())
          ).also { dp ->
            dp.onAction = EventHandler {
              radio.userData = (radio.userData as DateRange).withStartDate(dp.value.asDate())
            }
          }
          grid.add(startDatePicker, 1, 0)

          val endDatePicker = DatePicker(
            LocalDate.ofInstant(model.rangeCustom.endDate.toInstant(), ZoneId.systemDefault())
          ).also { dp ->
            dp.onAction = EventHandler {
              radio.userData = (radio.userData as DateRange).withEndDate(dp.value.asDate())
            }
          }
          grid.add(Label(i18n.formatText("option.export.range.end.label")), 0, 1)
          grid.add(endDatePicker, 1, 1)
        })
        it.vbox
      }
    }
    toggleGroup.selectToggle(when (model.selectedRange.get()) {
      model.rangeCurrentView -> radioCurrentView
      model.rangeWholeProject -> radioWholeProject
      else -> radioCustom
    })
    VBoxBuilder("popover-content").let {
      it.addStylesheets("/biz/ganttproject/lib/DateRangePicker.css")
      it.add(radioCurrentView)
      it.add(radioWholeProject)
      it.add(radioCustom)
      it.add(Button(i18n.formatText("apply")).also { btn ->
        btn.styleClass.add("btn-small-attention")
        btn.onAction = EventHandler {
          model.selectedRange.set(toggleGroup.selectedToggle.userData as DateRange)
        }
      }, Pos.BASELINE_RIGHT, Priority.NEVER)
      it.vbox
    }
  }
}

private fun LocalDate.asDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
