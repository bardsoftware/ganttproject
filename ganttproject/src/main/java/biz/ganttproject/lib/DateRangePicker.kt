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

import biz.ganttproject.lib.fx.VBoxBuilder
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.skin.ComboBoxListViewSkin
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.util.Callback
import net.sourceforge.ganttproject.chart.Chart
import net.sourceforge.ganttproject.language.GanttLanguage
import org.controlsfx.control.PopOver
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

data class DateRange(val startDate: Date, val endDate: Date, val title: String, val isEditable: Boolean)

class DateRangePicker(private val chart: Chart) {
  var onRangeChange: (Date, Date) -> Unit = {_, _ ->}
  private val button = Button("").also { btn ->
    btn.onAction = EventHandler {
      PopOver(buildPopoverContent()).also {
        it.arrowLocation = PopOver.ArrowLocation.TOP_CENTER
        it.show(btn)
      }
    }
  }
  /*
  private val dropdown = ComboBox<DateRange>().also {
    it.skin = object : ComboBoxListViewSkin<DateRange>(it) {
      init {
        isHideOnClick = false
      }
    }
    it.buttonCell = ButtonListCell()
    it.cellFactory = Callback { listView -> DropdownListCell(listView) }
    it.items.addAll(
      DateRange(chart.startDate, chart.endDate, "Current view", false),
      DateRange(chart.project.taskManager.projectStart, chart.project.taskManager.projectEnd, "The whole project", false),
      DateRange(chart.startDate, chart.endDate, "Custom", true)
    )
    it.onAction = EventHandler { _ ->
      it.selectionModel.selectedItem?.let { selected ->
        onRangeChange(selected.startDate, selected.endDate)
      }
    }
    it.selectionModel.select(it.items[0])
  }

   */

  val component: Control get() = button

  private fun buildPopoverContent(): Node {
    val toggleGroup = ToggleGroup()
    val radioCurrentView = RadioButton("Current View").also {
      it.toggleGroup = toggleGroup
    }
    val radioWholeProject = RadioButton("Whole Project").also {
      it.toggleGroup = toggleGroup
    }
    val radioCustom = RadioButton().also {
      it.graphic = DatePicker()
      it.contentDisplay = ContentDisplay.GRAPHIC_ONLY
      it.toggleGroup = toggleGroup
    }
    return VBoxBuilder().let {
      it.add(radioCurrentView)
      it.add(radioWholeProject)
      it.add(radioCustom)
      it.vbox
    }

  }

}

private class ButtonListCell : ListCell<DateRange>() {
  override fun updateItem(item: DateRange?, empty: Boolean) {
    super.updateItem(item, empty);

    graphic = if (item == null || empty) {
      null;
    } else {
      Label(item.title)
    }
  }
}
/*
private class DropdownListCell(private val listView: ListView<DateRange>) : ListCell<DateRange>() {
  override fun updateItem(item: DateRange?, empty: Boolean) {
    super.updateItem(item, empty);

    if (item == null || empty) {
      graphic = null;
    } else {
      graphic = VBoxBuilder().let { builder ->
        if (item.isEditable) {
          builder.add(TitledPane().also { titledPane ->
            titledPane.text = "Custom: ${GanttLanguage.getInstance().formatShortDate(item.startDate)}..${GanttLanguage.getInstance().formatShortDate(item.endDate)}"
            titledPane.content = GridPane().also { grid ->
              grid.add(Label("Start date"), 0, 0)
              grid.add(DatePicker(LocalDate.ofInstant(item.startDate.toInstant(), ZoneId.systemDefault())), 1, 0)
              grid.add(Label("End date"), 0, 1)
              grid.add(DatePicker(LocalDate.ofInstant(item.endDate.toInstant(), ZoneId.systemDefault())), 1, 1)
            }
            titledPane.isExpanded = false
            titledPane.expandedProperty().addListener { observable, oldValue, newValue ->
              listView.requestLayout()
            }
          })
        } else {
          builder.addTitle(item.title)
          builder.add(Label("${GanttLanguage.getInstance().formatShortDate(item.startDate)}..${GanttLanguage.getInstance().formatShortDate(item.endDate)}"))
        }
        builder.vbox
      }
    }
  }
}
*/