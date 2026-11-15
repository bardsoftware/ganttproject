/*
Copyright 2026 BarD Software s.r.o

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
package net.sourceforge.ganttproject.importer

import javafx.application.Platform
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Window
import javafx.stage.WindowEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.awt.GraphicsEnvironment
import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Shows the real dialog and closes it the ways a user can. The import waits for the answer, so the
 * answer has to come exactly once whichever way the dialog is closed.
 */
class BaselineNameCollisionDialogTest {
  private val answers = Collections.synchronizedList(mutableListOf<BaselineNameCollision?>())
  private var window: Window? = null

  @AfterEach
  fun closeLeftovers() {
    window?.let { w -> onFx { if (w.isShowing) w.hide() } }
  }

  @Test
  fun `window close button answers null`() {
    val window = showDialog()
    onFx { window.fireEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST)) }

    assertEquals(listOf<BaselineNameCollision?>(null), awaitAnswers())
  }

  @Test
  fun `escape answers null`() {
    val window = showDialog()
    onFx {
      window.scene.root.fireEvent(KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false))
    }

    assertEquals(listOf<BaselineNameCollision?>(null), awaitAnswers())
  }

  private fun showDialog(): Window {
    val before = onFx { Window.getWindows().toSet() }
    showBaselineNameCollisionDialog(listOf("Baseline")) { answers.add(it) }
    val deadline = System.currentTimeMillis() + 10_000
    while (System.currentTimeMillis() < deadline) {
      onFx { Window.getWindows().firstOrNull { it.isShowing && it !in before } }?.let {
        window = it
        return it
      }
      Thread.sleep(50)
    }
    throw AssertionError("the dialog has not been shown")
  }

  /** Waits until the dialog is hidden and gives late answers a moment to arrive. */
  private fun awaitAnswers(): List<BaselineNameCollision?> {
    val deadline = System.currentTimeMillis() + 5_000
    while (System.currentTimeMillis() < deadline && (answers.isEmpty() || onFx { window!!.isShowing })) {
      Thread.sleep(50)
    }
    onFx { }
    Thread.sleep(200)
    onFx { }
    return answers.toList()
  }

  private fun <T> onFx(code: () -> T): T {
    val result = CompletableFuture<T>()
    Platform.runLater {
      try {
        result.complete(code())
      } catch (ex: Throwable) {
        result.completeExceptionally(ex)
      }
    }
    return result.get(10, TimeUnit.SECONDS)
  }

  companion object {
    @BeforeAll
    @JvmStatic
    fun startToolkit() {
      assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display to show the dialog")
      try {
        Platform.startup {}
      } catch (ex: IllegalStateException) {
        // already started by another test in this JVM
      }
      Platform.setImplicitExit(false)
    }
  }
}
