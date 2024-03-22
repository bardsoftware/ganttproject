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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.stage.Stage
import javafx.util.Duration
import net.sourceforge.ganttproject.gui.NotificationChannel
import net.sourceforge.ganttproject.gui.NotificationItem
import net.sourceforge.ganttproject.gui.NotificationManager
import org.controlsfx.control.Notifications
import org.controlsfx.control.action.Action

class NotificationManagerImpl : NotificationManager {
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
    val maxSeverityItem = notifications.filter { it.isRead.not() }.maxByOrNull {
      when (it.channel) {
        null, NotificationChannel.RSS -> 1
        NotificationChannel.WARNING -> 2
        NotificationChannel.ERROR -> 3
      }
    } ?: return
    val popupBuilder = Notifications.create().owner(owner).title(maxSeverityItem.myTitle).text(maxSeverityItem.myBody)
      .action(Action("Details") {
        println("DETAILS!")
      })
      .hideAfter(Duration.INDEFINITE)
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
  fun createStatusBarComponent() = HBox().also {
    it.getStylesheets().add("biz/ganttproject/app/StatusBar.css")
    it.styleClass.addAll("statusbar", "align_right")
    val label = Button("Notifications")
    maxUnreadSeverity.addListener { observable, oldValue, newValue ->
      FXUtil.runLater {
        when (newValue) {
          NotificationChannel.ERROR -> {
            label.text = "Error"
            label.graphic = ERROR_ICON
          }
          NotificationChannel.WARNING -> {
            label.text = "Warning"
            label.graphic = WARNING_ICON
          }
          NotificationChannel.RSS -> {
            label.text = "News"
            label.graphic = NEWS_ICON
          }
          null -> {}
        }
      }
    }
    it.children.add(label)
    HBox.setHgrow(label, Priority.NEVER)
  }
}

private val ERROR_ICON = FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_CIRCLE)
private val WARNING_ICON = FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_CIRCLE)
private val NEWS_ICON = MaterialIconView(MaterialIcon.NEW_RELEASES)
