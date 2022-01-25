/*
Copyright 2021 BarD Software s.r.o

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
package biz.ganttproject.app

import biz.ganttproject.lib.fx.VBoxBuilder
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region

data class AlertPane(val contents: Node, val btnClose: Button?)

fun buildAlertPane(title: (VBoxBuilder)->HBox, body: Node, withClose: Boolean): AlertPane {
  val vboxBuilder = VBoxBuilder("alert-box")
  var btnClose: Button?
  title(vboxBuilder).also { hbox ->
    hbox.alignment = Pos.CENTER_LEFT
    hbox.isFillHeight = true
    hbox.children.add(Region().also { node -> HBox.setHgrow(node, Priority.ALWAYS) })
    btnClose = if (withClose) {
      Button(null, FontAwesomeIconView(FontAwesomeIcon.TIMES)).also { btn ->
        btn.styleClass.add("alert-dismiss")
        hbox.children.add(btn)
      }
    } else null
  }
  vboxBuilder.add(body, Pos.CENTER, Priority.ALWAYS)
  return AlertPane(vboxBuilder.vbox, btnClose)
}
