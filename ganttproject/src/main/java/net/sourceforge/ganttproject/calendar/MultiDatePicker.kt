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
    private val selectedDates: ArrayList<LocalDate?> = ArrayList()
    private var lastSelectedDate: LocalDate? = null

    init {
        skin = DatePickerSkin(this)
        onRangeSelectionMode()
    }

    fun onRangeSelectionMode(): MultiDatePicker {
        val mouseClickedEventHandler = EventHandler { clickEvent: MouseEvent ->
            if (clickEvent.button == MouseButton.PRIMARY) {
                if (lastSelectedDate == null) {
                    selectedDates.add(value)
                } else {
                    selectedDates.clear()
                    var startDate: LocalDate
                    val endDate: LocalDate
                    if (value.isAfter(lastSelectedDate)) {
                        startDate = lastSelectedDate as LocalDate
                        endDate = value
                    } else {
                        startDate = value
                        endDate = lastSelectedDate as LocalDate
                    }
                    do {
                        selectedDates.add(startDate)
                        startDate = startDate.plusDays(1)
                    } while (!startDate.isAfter(endDate))
                }
                lastSelectedDate = value
            }
            val datePickerContent = popupContent as DatePickerContent
            datePickerContent.updateDayCells()
            clickEvent.consume()
        }
        dayCellFactory = Callback { param: DatePicker? ->
            object : DateCell() {
                override fun updateItem(item: LocalDate, empty: Boolean) {
                    super.updateItem(item, empty)
                    //...
                    if (item != null && !empty) {
                        //...
                        addEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedEventHandler)
                    } else {
                        //...
                        removeEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedEventHandler)
                    }
                    style = if (!selectedDates.isEmpty() && selectedDates.contains(item)) {
                        if (item == selectedDates.toTypedArray()[0] || item == selectedDates.toTypedArray()[selectedDates.size - 1]) {
                            "-fx-background-color: rgba(3, 169, 1, 0.7);"
                        } else {
                            "-fx-background-color: rgba(3, 169, 244, 0.7);"
                        }
                    } else {
                        null
                    }
                }
            }
        }
        return this
    }

    val popupContent: Node
        get() = (skin as DatePickerSkin).popupContent

    fun getSelectedDates(): List<LocalDate?> {
        return selectedDates
    }
}