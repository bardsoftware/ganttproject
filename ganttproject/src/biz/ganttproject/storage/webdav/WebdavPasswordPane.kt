/*
Copyright 2020 BarD Software s.r.o

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
package biz.ganttproject.storage.webdav

import biz.ganttproject.lib.fx.VBoxBuilder
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor
import net.sourceforge.ganttproject.language.GanttLanguage
import java.util.function.Consumer

/**
 * This is a UI for WebDAV storage authentication.
 *
 * @author dbarashev@bardsoftware.com
 */
class WebdavPasswordPane(
    private val server: WebDavServerDescriptor,
    private val onDone: Consumer<WebDavServerDescriptor>) {

  fun createUi(): Pane {
    val passwordField = PasswordField()
    return VBoxBuilder("signin-pane", "pane-service-contents").run {
      vbox.stylesheets.add("/biz/ganttproject/app/Util.css")
      addTitle(i18n.formatText("webdav.ui.title.password", server.name)).also {
        it.styleClass += "title-integrated"
      }
      add(Label().also {
        it.styleClass += "medskip"
        it.text = i18n.formatText("option.webdav.server.password.label")
        it.isWrapText = true
      }, Pos.CENTER_LEFT, Priority.NEVER)
      add(passwordField)
      add(Button("Sign In").also {
        it.styleClass.addAll("btn-attention")
        it.addEventHandler(ActionEvent.ACTION) {
          onDone(passwordField.text)
        }
      }, Pos.CENTER_RIGHT, Priority.NEVER).also { it.styleClass.add("smallskip") }
      vbox
    }

  }

  private fun onDone(password: String) {
    server.password = password
    onDone.accept(server)
  }
}
