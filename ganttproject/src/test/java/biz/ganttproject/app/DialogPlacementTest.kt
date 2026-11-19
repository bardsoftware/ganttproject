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

import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.shape.Rectangle
import javafx.stage.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests the size which a dialog gets when it is opened again with the bounds saved under its id.
 *
 * It checks the preferred width of the dialog pane, which the dialog code sets together with the window width.
 * Without a window manager, as under Xvfb in the tests, the toolkit resets the window to the size of its content,
 * so the window width would show that size whatever the dialog code did.
 */
class DialogPlacementTest {
  private val threeButtons = listOf("task.recurrence.remove, a label as long as a key", "Apply", "Cancel")

  /** Runs the body on the JavaFX thread, where a dialog may be shown, and rethrows what it threw. */
  private fun onFxThread(body: suspend () -> Unit) = runBlocking {
    withContext(Dispatchers.JavaFx) { body() }
  }

  /** Shows a dialog with the given buttons and waits until the code which runs when it is shown is done. */
  private suspend fun showDialog(id: String, buttons: List<String>): Dialog<Unit> =
    dialogFxBuild(id = id) { controller ->
      controller.setContent(Label("Content"))
      buttons.forEach { controller.setupButton(ButtonType(it, ButtonBar.ButtonData.OTHER)) }
    }.also {
      it.show()
      delay(500)
    }

  /** The width of a dialog with [buttons] when it is opened for the first time, i.e. the width of its content. */
  private suspend fun contentWidth(buttons: List<String>): Double {
    val fresh = showDialog("test.dialogPlacement.fresh", buttons)
    return fresh.dialogPane.scene.window.width.also { fresh.hide() }
  }

  @Test
  fun `a dialog is not made narrower than its content by the bounds saved before`() = onFxThread {
    val id = "test.dialogPlacement.narrow"
    val savedWidth = 200.0
    val contentWidth = contentWidth(threeButtons)
    // Without this the test would pass if the buttons fitted into the saved width anyway, and would say nothing
    // about the saved bounds.
    assertTrue(contentWidth > savedWidth + 20, "The buttons must need more room than saved: $contentWidth")

    DialogPlacement.setBounds(id, Rectangle(100.0, 100.0, savedWidth, 150.0))
    val dialog = showDialog(id, threeButtons)
    val prefWidth = dialog.dialogPane.prefWidth
    dialog.hide()
    assertTrue(prefWidth >= contentWidth, "Restored width $prefWidth, saved $savedWidth, content $contentWidth")
  }

  @Test
  fun `a dialog which has grown is moved back onto the screen`() = onFxThread {
    val id = "test.dialogPlacement.edge"
    val screen = Screen.getPrimary().visualBounds
    val contentWidth = contentWidth(threeButtons)
    // Without this the dialog could not fit onto the screen at all, and the test would say nothing.
    assertTrue(contentWidth < screen.width, "The test dialog must fit onto the screen: $contentWidth vs ${screen.width}")

    val savedX = screen.maxX - 250.0
    DialogPlacement.setBounds(id, Rectangle(savedX, screen.minY + 100, 200.0, 150.0))
    val dialog = showDialog(id, threeButtons)
    val x = dialog.dialogPane.scene.window.x
    val width = dialog.dialogPane.prefWidth
    dialog.hide()
    assertTrue(x + width <= screen.maxX + 0.5, "Right edge ${x + width}, screen ${screen.maxX}")
    assertTrue(x < savedX, "Moved to the left from $savedX to $x")
  }

  @Test
  fun `a dialog keeps a saved size which is larger than its content`() = onFxThread {
    val id = "test.dialogPlacement.wide"
    val savedWidth = contentWidth(threeButtons) + 200

    DialogPlacement.setBounds(id, Rectangle(100.0, 100.0, savedWidth, 400.0))
    val dialog = showDialog(id, threeButtons)
    val prefWidth = dialog.dialogPane.prefWidth
    val prefHeight = dialog.dialogPane.prefHeight
    val x = dialog.dialogPane.scene.window.x
    dialog.hide()
    assertEquals(savedWidth, prefWidth, 0.5)
    assertEquals(400.0, prefHeight, 0.5)
    assertEquals(100.0, x, 0.5)
  }
}
