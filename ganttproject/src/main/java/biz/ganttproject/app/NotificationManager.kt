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

class NotificationManagerImpl() : NotificationManager {
  private lateinit var owner: Stage

  fun setOwner(stage: Stage) { owner = stage }

  override fun addNotifications(channel: NotificationChannel, notifications: List<NotificationItem>) {
    when (channel) {
      NotificationChannel.RSS -> {
        Notifications.create().owner(owner).title(notifications[0].myTitle).text(notifications[0].myBody)
          .action(Action("Details") {
            println("DETAILS!")
          }).hideAfter(Duration.INDEFINITE).showInformation()
      }
      NotificationChannel.WARNING -> {
        Notifications.create().owner(owner).title(notifications[0].myTitle).text(notifications[0].myBody)
          .action(Action("Details") {
            println("DETAILS!")
          }).hideAfter(Duration.INDEFINITE).showWarning()
      }
      NotificationChannel.ERROR -> {
        Notifications.create().owner(owner).title(notifications[0].myTitle).text(notifications[0].myBody)
          .action(Action("Details") {
          println("DETAILS!")
        }).hideAfter(Duration.INDEFINITE).showError()
      }
    }
  }

  override fun showNotification(channel: NotificationChannel) {
    TODO("Not yet implemented")
  }

  override fun hideNotification() {
    TODO("Not yet implemented")
  }

  fun createStatusBarComponent() = HBox().also {
    it.getStylesheets().add("biz/ganttproject/app/StatusBar.css")
    it.styleClass.addAll("statusbar", "align_right")
    val label = Button("Notifications")
    it.children.add(label)
    HBox.setHgrow(label, Priority.NEVER)
  }
}
