/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
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
package biz.ganttproject.app

import biz.ganttproject.FXUtil
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.vbox
import com.sandec.mdfx.MDFXNode
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.stage.Stage
import javafx.util.Callback
import net.sourceforge.ganttproject.gui.NotificationChannel
import net.sourceforge.ganttproject.gui.NotificationItem
import net.sourceforge.ganttproject.gui.NotificationManager
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.ViewLogDialog
import org.controlsfx.control.Notifications
import javax.swing.SwingUtilities

class NotificationManagerImpl(private val getUiFacade: ()->UIFacade) : NotificationManager {
  private lateinit var owner: Stage

  private val notifications = mutableListOf<NotificationItem>()
  private val maxUnreadSeverity = SimpleObjectProperty<NotificationChannel?>()

  fun setOwner(stage: Stage) { owner = stage }

  override fun addNotifications(notifications: List<NotificationItem>) {
    this.notifications.addAll(notifications)
    updateUi()
  }

  override fun showNotification(channel: NotificationChannel) {
    TODO("Not yet implemented")
  }

  private fun updateUi() {
    FXUtil.runLater {
      val maxSeverityItem = notifications.filter { it.isRead.not() }.maxByOrNull {
        when (it.channel) {
          null, NotificationChannel.RSS -> 1
          NotificationChannel.WARNING -> 2
          NotificationChannel.ERROR -> 3
        }
      } ?: return@runLater
      val popupBuilder = Notifications.create().owner(owner).title(maxSeverityItem.myTitle).text(maxSeverityItem.myBody)
      maxUnreadSeverity.set(maxSeverityItem.channel)
      when (maxSeverityItem.channel) {
        NotificationChannel.RSS -> {
          popupBuilder.showInformation()
        }
        NotificationChannel.WARNING -> {
          popupBuilder.showWarning()
        }
        NotificationChannel.ERROR -> {
          popupBuilder.showError()
        }
      }
    }

  }
  fun createStatusBarComponent() = HBox().also {
    it.spacing = 5.0
    it.getStylesheets().add("biz/ganttproject/app/StatusBar.css")
    it.styleClass.addAll("statusbar", "align_right", "notifications")
    val errorButton = Button("Notifications")
    errorButton.onAction = EventHandler { showErrors() }
    val rssButton = Button("News", NEWS_ICON)
    maxUnreadSeverity.addListener { observable, oldValue, newValue ->
      FXUtil.runLater {
        when (newValue) {
          NotificationChannel.WARNING -> {
            errorButton.text = "Errors"
            errorButton.graphic = ERROR_ICON
            errorButton.styleClass.addAll("unread", "error")
          }
          NotificationChannel.ERROR -> {
            errorButton.text = "Warnings"
            errorButton.graphic = WARNING_ICON
            errorButton.styleClass.addAll("unread", "warning")
          }
          NotificationChannel.RSS -> {
            rssButton.styleClass.add("unread")
          }
          null -> {}
        }
      }
    }
    it.children.addAll(rssButton, errorButton)
    HBox.setHgrow(errorButton, Priority.NEVER)
  }

  fun showErrors() {
    dialog {dlg ->
      dlg.addStyleSheet("/biz/ganttproject/app/mdfx.css", "/biz/ganttproject/app/Dialog.css", "/biz/ganttproject/app/NotificationManager.css")
      dlg.setEscCloseEnabled(true)

      dlg.setHeader(
        VBoxBuilder("header").apply {
          addTitleString("Errors and Warnings").also { hbox ->
            hbox.alignment = Pos.CENTER_LEFT
            hbox.isFillHeight = true
            hbox.children.add(Region().also { node -> HBox.setHgrow(node, Priority.ALWAYS) })
          }
        }.vbox
      )

      val listView = ListView<NotificationItem>().also {
        it.items = FXCollections.observableArrayList<NotificationItem>().also {
          it.addAll(notifications.filter { it.channel != NotificationChannel.RSS })
        }
        it.cellFactory = Callback { CellImpl() }
      }

      dlg.setContent(vbox {
        add(listView)
      })

      dlg.setupButton(ButtonType.NEXT) {btn ->
        btn.styleClass.add("btn-regular")
        btn.text = "View Log"
        btn.onAction = EventHandler {
          SwingUtilities.invokeLater {
            ViewLogDialog.show(getUiFacade())
          }
        }
      }
      dlg.setupButton(ButtonType.OK) {btn ->
        btn.styleClass.add("btn-attention")
      }
    }
  }

}

private class CellImpl : ListCell<NotificationItem>() {
  init {
    styleClass.add("column-item-cell")
    alignment = Pos.CENTER_LEFT
  }

  override fun updateItem(item: NotificationItem?, empty: Boolean) {
    super.updateItem(item, empty)
    if (item == null || empty) {
      text = ""
      graphic = null
      return
    }
    text = null
    graphic = MDFXNode("""
      *${item.myTitle}*
      ----
      
      ${item.myBody}
    """.trimIndent())
  }
}
private val ERROR_ICON = FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_CIRCLE)
private val WARNING_ICON = FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_CIRCLE)
private val NEWS_ICON = MaterialIconView(MaterialIcon.ANNOUNCEMENT)
