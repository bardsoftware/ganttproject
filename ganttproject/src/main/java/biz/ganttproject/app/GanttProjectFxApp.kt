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

import biz.ganttproject.lib.fx.vbox
import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.stage.Stage
import net.sourceforge.ganttproject.APP_LOGGER
import net.sourceforge.ganttproject.GanttProject

class GanttProjectFxApp(private val ganttProject: GanttProject) : Application() {

  override fun start(stage: Stage) {
    try {
      APP_LOGGER.debug(">>> start()")
      val vbox = vbox {
        add(convertMenu(ganttProject.menuBar))
        add(ganttProject.createToolbar().build().toolbar)
        add(ganttProject.viewManager.fxComponent, null, Priority.ALWAYS)
        add(
          HBox().also {
            it.children.add(ganttProject.createStatusBar().lockPanel)
            val filler = Pane()
            it.children.add(filler)
            HBox.setHgrow(filler, Priority.ALWAYS)
            val notificationBar = ganttProject.notificationManagerImpl.createStatusBarComponent()
            it.children.add(notificationBar)
            HBox.setHgrow(notificationBar, Priority.NEVER)
          }
        )
      }
      stage.setScene(Scene(vbox))
      APP_LOGGER.debug("... app scene done.")
      stage.onShown = EventHandler {
        APP_LOGGER.debug("onShown(): resolving the barrier...")
        ganttProject.uiFacade.windowOpenedBarrier.resolve(true)
//        ganttProject.notificationManager.addNotifications(
//          listOf(NotificationItem(NotificationChannel.RSS, "Got some news", "Lorem ipsum dolor sit amet", NotificationManager.DEFAULT_HYPERLINK_LISTENER))
//        )
      }
      stage.onCloseRequest = EventHandler {
        ganttProject.windowGeometry = WindowGeometry(stage.x, stage.y, stage.width, stage.height, stage.isMaximized)
        if (ganttProject.isModified) {
          it.consume()
          ganttProject.quitApplication(true).await {result ->
            if (result) {
              Platform.exit()
            }
          }
        }
      }
      ganttProject.windowGeometry.let {
        stage.x = it.leftX
        stage.y = it.topY
        stage.width = it.width
        stage.height = it.height
        stage.isMaximized = it.isMaximized
      }

      stage.icons += Image(GanttProjectFxApp::class.java.getResourceAsStream("/icons/ganttproject-logo-512.png"))
      ganttProject.title.let {
        stage.title = it.value
        it.addListener { _, _, newValue ->
          Platform.runLater {
            stage.title = newValue
          }
        }
      }
      APP_LOGGER.debug("... geometry, icons and title done.")
      val insertTask = KeyCodeCombination(KeyCode.INSERT)
      stage.addEventHandler(KeyEvent.KEY_PRESSED) {
        println("event=$it")
        if (insertTask.match(it)) {
          println("INSERT pressed")
          it.consume()
        }
      }
      APP_LOGGER.debug("... showing the stage.")
      stage.show()
      APP_LOGGER.debug("... done.")
    } catch (ex: Exception) {
      ex.printStackTrace()
    }
    APP_LOGGER.debug("<<< start()")
  }
}

data class WindowGeometry(
  val leftX: Double = 0.0,
  val topY: Double = 0.0,
  val width: Double = 600.0,
  val height: Double = 600.0,
  val isMaximized: Boolean = false
)
