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
import javafx.scene.layout.Priority
import javafx.stage.Stage
import net.sourceforge.ganttproject.GanttProject

class GanttProjectFxApp(private val ganttProject: GanttProject) : Application() {

  override fun start(stage: Stage) {
    try {
      val vbox = vbox {
        add(convertMenu(ganttProject.menuBar))
        add(ganttProject.createToolbar().build().toolbar)
        add(ganttProject.viewManager.fxComponent, null, Priority.ALWAYS)
        add(ganttProject.createStatusBar().lockPanel)
      }
      stage.setScene(Scene(vbox))
      stage.onShown = EventHandler {
        ganttProject.uiFacade.windowOpenedBarrier.resolve(true)
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
      stage.show()
    } catch (ex: Exception) {
      ex.printStackTrace()
    }
  }
}

data class WindowGeometry(
  val leftX: Double = 0.0,
  val topY: Double = 0.0,
  val width: Double = 600.0,
  val height: Double = 600.0,
  val isMaximized: Boolean = false
)
