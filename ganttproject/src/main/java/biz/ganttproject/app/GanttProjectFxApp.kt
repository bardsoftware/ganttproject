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
import biz.ganttproject.lib.fx.installDockIcon
import biz.ganttproject.lib.fx.vbox
import javafx.application.Application
import javafx.application.Platform
import javafx.application.Preloader
import javafx.event.EventHandler
import javafx.geometry.Rectangle2D
import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.stage.Screen
import javafx.stage.Stage
import net.sourceforge.ganttproject.APP_LOGGER
import net.sourceforge.ganttproject.GanttProject
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

// This barrier is used to connect non-UI code with the JavaFX objects at the early stage
// when JavaFX app is being initialized.
val applicationBarrier = SimpleBarrier<GanttProjectFxApp>()

/**
 * GanttProject JavaFX application. This class is the entry point for the entire user interface.
 */
class GanttProjectFxApp : Application() {

  lateinit var ganttProject: GanttProject
  lateinit var stage: Stage

  override fun start(stage: Stage) {
    this.stage = stage
    applicationBarrier.resolve(this)
    try {
      APP_LOGGER.debug(">>> start()")
      DialogPlacement.applicationWindow = stage

      // The topmost component, a vertical box that includes a menu bar, a toolbar, a tabbed pane with the views,
      // and a status bar.
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
      val initialGeometry = ganttProject.windowGeometry.sanitized()
      APP_LOGGER.debug("... initial window geometry: {}", initialGeometry)
      // Giving the scene an explicit size makes the window request a proper size before it is mapped. Otherwise,
      // on Linux, GTK may configure the window with its default 200x200 size, and that may win over our resize
      // requests.
      val appScene = Scene(vbox, initialGeometry.width, initialGeometry.height)
      appScene.stylesheets.add("/biz/ganttproject/app/Main.css")
      stage.setScene(appScene)
      // The minimum size is passed to the window manager as a size hint, so it will not shrink the window below it.
      Screen.getPrimary().visualBounds.let { screen ->
        stage.minWidth = minOf(MIN_WINDOW_WIDTH, screen.width)
        stage.minHeight = minOf(MIN_WINDOW_HEIGHT, screen.height)
      }
      stage.x = initialGeometry.leftX
      stage.y = initialGeometry.topY
      stage.width = initialGeometry.width
      stage.height = initialGeometry.height
      APP_LOGGER.debug("... app scene done.")
      stage.onShown = EventHandler {
        ganttProject.viewManager.init(ganttProject.ganttViewProvider, ganttProject.resourceViewProvider)
        APP_LOGGER.debug("onShown(): resolving the barrier...")
        ganttProject.uiFacade.windowOpenedBarrier.resolve(true)
        Platform.runLater {
          // Some window managers ignore the position and the maximized state requested before the window is mapped,
          // so we apply the geometry once again.
          initialGeometry.let {
            stage.x = it.leftX
            stage.y = it.topY
            stage.width = it.width
            stage.height = it.height
            stage.isMaximized = it.isMaximized
          }
        }
//        ganttProject.notificationManager.addNotifications(
//          listOf(NotificationItem(NotificationChannel.RSS, "Got some news", "Lorem ipsum dolor sit amet", NotificationManager.DEFAULT_HYPERLINK_LISTENER))
//        )
      }
      stage.onCloseRequest = EventHandler {
        ganttProject.windowGeometry =
          WindowGeometry(stage.x, stage.y, stage.width, stage.height, stage.isMaximized).sanitized()
        if (ganttProject.isModified) {
          it.consume()
        }
        ganttProject.quitApplication(true).await {result ->
          if (result) {
            Platform.exit()
          }
        }
      }
      ganttProject.title.let {
        stage.title = it.value
        it.addListener { _, _, newValue ->
          FXUtil.runLater {
            stage.title = newValue
          }
        }
      }
      APP_LOGGER.debug("... geometry, icons and title done.")
//      ganttProject.appLevelActions.forEach {
//        println("Action: ${it.name} ${it.keyCombination}")
//        appScene.accelerators.put(it.keyCombination) {
//          it.actionPerformed(null)
//        }
//      }
      appScene.onKeyPressed = EventHandler { event ->
        ganttProject.appLevelActions.firstOrNull { it.triggeredBy(event) }?.let { action ->
          event.consume()
          SwingUtilities.invokeLater {
              action.actionPerformed(null)
          }
        }
      }

      APP_LOGGER.debug("... showing the stage.")
      stage.show()
      APP_LOGGER.debug("... done.")
    } catch (ex: Exception) {
      ex.printStackTrace()
      val msg = """
        Failed to start GanttProject: ${ex.message}
        Please refer to the log file for more details.
        """
      JOptionPane.showMessageDialog(null, msg, "Failed to start", JOptionPane.ERROR_MESSAGE)
      System.exit(1)
    }
    APP_LOGGER.debug("<<< start()")
  }
}

class GanttProjectFxPreloader: Preloader() {
  override fun start(stage: Stage) {
    com.sun.glass.ui.Application.GetApplication().setName(RootLocalizer.formatText("appliTitle"));
    stage.installDockIcon()
  }

}
data class WindowGeometry(
  val leftX: Double = 0.0,
  val topY: Double = 0.0,
  val width: Double = 600.0,
  val height: Double = 600.0,
  val isMaximized: Boolean = false
)

private const val MIN_WINDOW_WIDTH = 640.0
private const val MIN_WINDOW_HEIGHT = 480.0
// If less than this share of the window area is visible on the screens, we move the window to the primary screen.
private const val MIN_VISIBLE_AREA_SHARE = 0.25

/**
 * Returns the geometry which is guaranteed to be not smaller than the minimum window size and to be reasonably
 * visible on the available screens.
 */
fun WindowGeometry.sanitized(screens: List<Rectangle2D> = Screen.getScreens().map { it.visualBounds },
                             primary: Rectangle2D = Screen.getPrimary().visualBounds): WindowGeometry {
  fun visibleAreaOn(screen: Rectangle2D): Double {
    val w = minOf(leftX + width, screen.maxX) - maxOf(leftX, screen.minX)
    val h = minOf(topY + height, screen.maxY) - maxOf(topY, screen.minY)
    return if (w > 0 && h > 0) w * h else 0.0
  }
  val area = width * height
  val visibleArea = screens.sumOf(::visibleAreaOn)
  val isTooSmall = width < MIN_WINDOW_WIDTH || height < MIN_WINDOW_HEIGHT
  if (!isTooSmall && area > 0 && visibleArea / area >= MIN_VISIBLE_AREA_SHARE) {
    return this
  }
  // Fit the window into the screen where it is mostly visible, or into the primary screen if it is not visible at all.
  val screen = screens.maxByOrNull(::visibleAreaOn)?.takeIf { visibleArea > 0 } ?: primary
  val fitWidth = width.coerceIn(minOf(MIN_WINDOW_WIDTH, screen.width), screen.width)
  val fitHeight = height.coerceIn(minOf(MIN_WINDOW_HEIGHT, screen.height), screen.height)
  val fitX = leftX.coerceIn(screen.minX, screen.maxX - fitWidth)
  val fitY = topY.coerceIn(screen.minY, screen.maxY - fitHeight)
  return WindowGeometry(fitX, fitY, fitWidth, fitHeight, isMaximized)
}
