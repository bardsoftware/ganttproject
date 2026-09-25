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
package net.sourceforge.ganttproject

import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.StackPane
import javafx.stage.Screen
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

/**
 * The alert is centred on the window that opens it, so the height of its message is capped against
 * the screen that window sits on. On a multi-monitor setup the primary screen may be taller than
 * that one, and a cap taken from the primary screen would let the dialog grow past the bottom of
 * the screen it is actually shown on.
 *
 * A window that has never been shown has no position at all: its x, y, width and height are NaN,
 * and no screen contains that point. The screen therefore has to be picked with a fallback.
 */
class AlertOwnerScreenTest {
  private val contentWidth = 360.0
  private val maxContentHeightRatio = 0.6

  private fun onFxThread(body: () -> Unit) = runBlocking {
    withContext(Dispatchers.JavaFx) { body() }
  }

  /** An owner window sitting in the middle of the given screen. */
  private fun ownerOn(screen: Screen) = Stage().also {
    val bounds = screen.visualBounds
    it.width = minOf(800.0, bounds.width)
    it.height = minOf(600.0, bounds.height)
    it.x = bounds.minX + (bounds.width - it.width) / 2
    it.y = bounds.minY + (bounds.height - it.height) / 2
  }

  /** The height the message pane of an alert owned by this window caps itself at. */
  private fun capOfAlertOwnedBy(owner: Stage): Double {
    val message = Label("Lorem ipsum dolor sit amet. ".repeat(400)).also { it.isWrapText = true }
    val pane = UIFacadeTestAccess.makeScrollable(message, owner) as ScrollPane
    val root = StackPane(pane)
    Scene(root, contentWidth, 100.0)
    root.applyCss()
    root.layout()
    // The message is long enough that the pane asks for the cap and nothing else.
    return pane.prefHeight(contentWidth)
  }

  @Test
  fun `the screen of the owner window is the one it stands on`() = onFxThread {
    Screen.getScreens().forEach { screen ->
      val owner = ownerOn(screen)
      assertEquals(screen, UIFacadeTestAccess.screenOf(owner),
        "an owner in the middle of $screen was placed on another screen")
    }
  }

  @Test
  fun `the cap follows the screen of the owner, not the primary screen`() = onFxThread {
    val others = Screen.getScreens().filter { it != Screen.getPrimary() }
    // With a single screen every screen-valued expression returns that screen, so there is nothing
    // here that could tell the owner's screen from the primary one.
    assumeTrue(others.isNotEmpty(), "needs more than one screen")
    others.forEach { screen ->
      assertEquals(screen.visualBounds.height * maxContentHeightRatio,
        capOfAlertOwnedBy(ownerOn(screen)), 0.5,
        "the cap of an alert owned by a window on $screen was not taken from that screen")
    }
  }

  @Test
  fun `a window that is on no screen falls back to the primary screen`() = onFxThread {
    // A stage that has never been shown: x, y, width and height are all NaN.
    val unshown = Stage()
    assertTrue(unshown.x.isNaN(), "a stage that was never shown was expected to have no position")

    assertSame(Screen.getPrimary(), UIFacadeTestAccess.screenOf(unshown),
      "a window that is on no screen should fall back to the primary screen")
    assertEquals(0, Screen.getScreensForRectangle(unshown.x, unshown.y, 1.0, 1.0).size,
      "the fallback is only reached while no screen claims the window")
  }

  @Test
  fun `an alert owned by a window that is on no screen still has a usable cap`() = onFxThread {
    val cap = capOfAlertOwnedBy(Stage())
    assertTrue(cap.isFinite() && cap > 0, "the cap of an alert without a placed owner is $cap")
    assertEquals(Screen.getPrimary().visualBounds.height * maxContentHeightRatio, cap, 0.5,
      "the fallback cap should be the same share of the primary screen")
  }
}
