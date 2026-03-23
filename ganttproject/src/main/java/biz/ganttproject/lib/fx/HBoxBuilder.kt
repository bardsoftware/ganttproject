/*
 * Copyright (c) 2026 Dmitry Barashev, BarD Software s.r.o.
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
package biz.ganttproject.lib.fx

import biz.ganttproject.createButton
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableStringValue
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import net.sourceforge.ganttproject.action.GPAction

/**
 * This builder constructs an HBox with a label to the left side and a set of buttons to the right side.
 */
data class HBoxBuilder(
  var label: ObservableStringValue = SimpleStringProperty(""),
  val actions: MutableList<GPAction> = mutableListOf(),
  var isSelected: Boolean = false,
  val styleClasses: MutableList<String> = mutableListOf()
) {
  fun build(): Region {
    val btnBox = HBox().also {
      it.children.addAll(actions.map(::createButton))
      it.styleClass.add("action-buttons")
    }
    return HBox().also {
      it.children.addAll(listOf(Label().also { labelControl ->
        labelControl.textProperty().bind(label)
        labelControl.maxWidth = Double.MAX_VALUE
        HBox.setHgrow(labelControl, Priority.ALWAYS)
      }, btnBox))
      it.styleClass.addAll(styleClasses)
      it.styleClass.add("hbox")
      if (isSelected) {
        it.styleClass.add("selected")
      }
    }
  }

  fun String.asObservable() = SimpleStringProperty(this)
}

fun hbox(init: HBoxBuilder.() -> Unit) = HBoxBuilder().apply(init).build()