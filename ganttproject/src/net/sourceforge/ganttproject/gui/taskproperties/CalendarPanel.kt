/*
Copyright 2018 Dmitry Barashev, BarD Software s.r.o

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

import biz.ganttproject.core.option.ColorOption
import javafx.embed.swing.JFXPanel
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javax.swing.JComponent

/**
 * Pane with task calendar UI controls.
 *
 * @author dbarashev@bardsoftware.com
 */
class CalendarPanel(val background: java.awt.Color) {
  fun getComponent(): JComponent {
    val result = JFXPanel()
    result.scene = createScene()
    return result
  }

   fun createScene(): Scene {
     val grid = GridPane()

     val weekendsWorking = CheckBox("Weekends are working days")

     val title = Label("Calendar Exceptions")
     title.style = "-fx-border-width: 0 0 1 0; -fx-padding: 0 0 3 0; -fx-border-color: black; -fx-font-size: 125%;"
     val exceptionsGroup = VBox()
     exceptionsGroup.children.addAll(title, weekendsWorking)
     VBox.setMargin(weekendsWorking, Insets(10.0, 0.0,0.0,0.0))

     grid.add(exceptionsGroup, 0, 0)
     grid.style = "-fx-background-color:${ColorOption.Util.getColor(background)}; -fx-opacity:1; -fx-padding: 20 5 5 5;"

     val stackPane = StackPane(grid)
     val scene = Scene(stackPane)
     return scene
   }
}