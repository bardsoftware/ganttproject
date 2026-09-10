/*
Copyright 2026 BarD Software s.r.o

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

import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.javafx.JavaFx
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The status bar is a horizontal box that holds the cloud indicator on the left, the notification
 * buttons on the right and a growing pane in between. Both indicators carry the .statusbar style
 * class, so whatever width that class claims is claimed twice, and the growing pane is left with
 * what remains.
 */
class StatusBarWidthTest {
  /** The width of the smallest window GanttProject is expected to work in. */
  private val windowWidth = 1024.0
  private val barHeight = 32.0
  /** The left and right padding of a status bar item, from StatusBar.css. */
  private val itemPadding = 24.0

  private fun statusBarItem(child: javafx.scene.Node, vararg styles: String) =
    HBox(child).also {
      it.stylesheets.add("biz/ganttproject/app/StatusBar.css")
      it.styleClass.add("statusbar")
      it.styleClass.addAll(*styles)
    }

  private fun layOut(root: Pane, width: Double = windowWidth) {
    Scene(root, width, barHeight)
    root.resize(width, barHeight)
    root.applyCss()
    root.layout()
  }

  @Test
  fun `the space between the status bar items can be used`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val cloudIndicator = statusBarItem(Label("Cloud: disconnected"))
      val notifications = statusBarItem(Button("Errors"), "align_right", "notifications")
      val filler = Pane()
      val bar = HBox(cloudIndicator, filler, notifications)
      HBox.setHgrow(filler, Priority.ALWAYS)
      layOut(bar)

      assertTrue(
        filler.width > windowWidth / 2,
        "the two status bar items leave only ${filler.width} px of the $windowWidth px window " +
          "for what is put between them: the cloud indicator takes ${cloudIndicator.width} px and " +
          "the notification buttons take ${notifications.width} px"
      )
    }
  }

  @Test
  fun `the notification buttons stay inside the window`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val button = Button("Errors")
      val notifications = statusBarItem(button, "align_right", "notifications")
      val filler = Pane()
      val bar = HBox(statusBarItem(Label("Cloud: disconnected")), filler, notifications)
      HBox.setHgrow(filler, Priority.ALWAYS)
      layOut(bar)

      assertEquals(
        windowWidth - itemPadding, button.localToScene(button.boundsInLocal).maxX, 0.5,
        "the notification buttons do not end at the right edge of the window"
      )
    }
  }

  @Test
  fun `a single status bar item keeps the place it had`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val label = Label("Cloud: disconnected")
      val cloudIndicator = statusBarItem(label)
      layOut(HBox(cloudIndicator))

      assertEquals(
        itemPadding, label.localToScene(label.boundsInLocal).minX, 0.5,
        "the text of the cloud indicator no longer starts behind the padding of its item"
      )
      assertTrue(
        label.width >= label.prefWidth(-1.0),
        "the cloud indicator is given ${label.width} px for a text that asks for " +
          "${label.prefWidth(-1.0)} px, so it is cut short"
      )
    }
  }
}
