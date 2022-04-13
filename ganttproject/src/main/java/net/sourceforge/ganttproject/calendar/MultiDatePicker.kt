/*
Copyright 2022 BarD Software s.r.o, Alexander Popov

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


package net.sourceforge.ganttproject.calendar

import com.sun.javafx.scene.control.DatePickerContent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.DateCell
import javafx.scene.control.DatePicker
import javafx.scene.control.skin.DatePickerSkin
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.util.Callback
import java.time.LocalDate


class MultiDatePicker : DatePicker() {
  private val selectedDatesList = mutableListOf<LocalDate>()
  private var lastSelectedDate: LocalDate? = null

  val selectedDates: List<LocalDate> get() = selectedDatesList.toList()
  val popupContent: Node
    get() = (skin as DatePickerSkin).popupContent

  init {
    skin = DatePickerSkin(this)
    val mouseClickedEventHandler = EventHandler { clickEvent: MouseEvent ->
      if (clickEvent.button == MouseButton.PRIMARY) {
        lastSelectedDate?.let {
          selectedDatesList.clear()
          var startDate: LocalDate
          val endDate: LocalDate
          if (value.isAfter(it)) {
            startDate = it
            endDate = value
          } else {
            startDate = value
            endDate = it
          }
          do {
            selectedDatesList.add(startDate)
            startDate = startDate.plusDays(1)
          } while (!startDate.isAfter(endDate))
        } ?: run {
          selectedDatesList.add(value)
        }
        lastSelectedDate = value
      }
      val datePickerContent = popupContent as DatePickerContent
      datePickerContent.updateDayCells()
      clickEvent.consume()
    }
    dayCellFactory = Callback {
      object : DateCell() {
        override fun updateItem(item: LocalDate?, empty: Boolean) {
          super.updateItem(item, empty)
          if (item != null && !empty) {
            addEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedEventHandler)
          } else {
            removeEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedEventHandler)
          }
          styleClass.add(if (selectedDatesList.isNotEmpty() && selectedDatesList.contains(item)) {
            if (item == selectedDatesList.first() || item == selectedDatesList.last()) {
              "edge-of-range"
              // "-fx-background-color: -fx-edge-selected-cell-color"
            } else {
               //"-fx-background-color: -fx-selected-cell-color"
              "item-of-range"
            }
          } else {
            null
          })
        }
      }
    }
  }
}