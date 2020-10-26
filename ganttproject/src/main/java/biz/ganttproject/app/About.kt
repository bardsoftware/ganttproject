/*
Copyright 2020 BarD Software s.r.o

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
import biz.ganttproject.lib.fx.openInBrowser
import com.sandec.mdfx.MDFXNode
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import net.sourceforge.ganttproject.GanttProject
import org.apache.commons.lang3.StringEscapeUtils
import org.eclipse.core.runtime.Platform
import java.time.LocalDateTime

/**
 * Show a dialog with GanttProject version number, links to about pages and license information.
 *
 * @author dbarashev@bardsoftware.com
 */
fun showAboutDialog() {
  dialog(title = LocalizedString("about.title", RootLocalizer)) { dialogApi ->
    val installedVersion = Platform.getUpdater()?.installedUpdateVersions?.maxOrNull() ?: "3.0"
    dialogApi.addStyleClass("dlg-platform-update")
    dialogApi.addStyleSheet(
        "/biz/ganttproject/platform/Update.css",
        "/biz/ganttproject/storage/StorageDialog.css",
        "/biz/ganttproject/storage/cloud/GPCloudStorage.css",
    )

    val logo = ImageView(Image(
        GanttProject::class.java.getResourceAsStream("/icons/ganttproject-logo-512.png"),
        64.0, 64.0, false, true)
    )
    dialogApi.setHeader(
        VBoxBuilder("header").apply {
          addTitle(LocalizedString("about.title", RootLocalizer)).also { hbox ->
            hbox.alignment = Pos.CENTER_LEFT
            hbox.isFillHeight = true
            hbox.children.add(Region().also { node -> HBox.setHgrow(node, Priority.ALWAYS) })
            hbox.children.add(logo)
          }
        }.vbox
    )

    val bodyBuilder = VBoxBuilder("content-pane")
    bodyBuilder.add(
        object : MDFXNode(
            """
            |# ${RootLocalizer.formatText("appliTitle")} ${installedVersion}
            |  
            |${RootLocalizer.formatText("about.summary")}
            |
            |# ${RootLocalizer.formatText("license")}
            |  
            |${RootLocalizer.formatText("about.license")}              
            |  
            """.trimMargin().also(StringEscapeUtils::unescapeJava)
        ) {
          init {
            prefWidth = 600.0
            prefHeight = 500.0
            stylesheets.clear()
            stylesheets.add("/biz/ganttproject/app/mdfx.css")
          }

          override fun setLink(node: Node, link: String, description: String?) {
            node.cursor = Cursor.HAND
            node.setOnMouseClicked { openInBrowser(link.trim()) }
          }
        }
    )
    bodyBuilder.add(Label(
        RootLocalizer.formatText("about.copyright", LocalDateTime.now().year.toString())
    ))
    dialogApi.setContent(bodyBuilder.vbox)
  }
}
