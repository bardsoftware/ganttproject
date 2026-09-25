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

import javafx.scene.Node
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The message of an alert goes into a scroll pane whose preferred height is capped at a share of
 * the screen height, so that a long message scrolls instead of growing the dialog until the window
 * manager clips it and pushes the buttons off the screen.
 *
 * The numbers below are all preferred heights asked for at the same width, the one the dialog pane
 * hard-codes for its content. A preferred height asked for without a width is measured without
 * wrapping and says nothing about a wrapped message, so the width is always passed in.
 */
class AlertContentHeightTest {
  /** The preferred content width that DialogPane.createContentLabel hard-codes. */
  private val contentWidth = 360.0
  /** The share of the screen height a message may fill before it starts scrolling. */
  private val maxContentHeightRatio = 0.6

  /** A message long enough to overflow any screen this test could run on. */
  private val longMessage = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
    "eiusmod tempor incididunt ut labore et dolore magna aliqua. ".repeat(40)
  private val shortMessage = "The project file could not be saved."

  /**
   * A label built the way UIFacadeImpl builds the one for a plain-text message: wrapping, with the
   * padding that modena.css gives a content label restored by hand.
   */
  private fun messageLabel(message: String) = Label(message).also {
    it.isWrapText = true
    it.style = "-fx-padding: 1.333em 0.833em 0 0.833em;"
  }

  /**
   * The same scroll pane without the cap, to have the height the content would ask for if it were
   * not capped. It has to be built and laid out exactly like the capped one, or the two numbers
   * would not be comparable.
   */
  private fun uncappedScrollPane(content: Node) = ScrollPane(content).also {
    it.prefWidth = contentWidth
    it.isFitToWidth = true
    it.isFocusTraversable = true
    it.style = "-fx-background-color: transparent; -fx-padding: 0;"
  }

  /** Lays the node out in a scene of the given size and hands back the root it was put into. */
  private fun layOut(node: Node, width: Double, height: Double): StackPane {
    val root = StackPane(node)
    Scene(root, width, height)
    root.resize(width, height)
    root.applyCss()
    root.layout()
    return root
  }

  /** Runs the body on the JavaFX thread, where a stage may be created, and rethrows what it threw. */
  private fun onFxThread(body: () -> Unit) = runBlocking {
    withContext(Dispatchers.JavaFx) { body() }
  }

  /** An owner window that sits on a screen, so that the cap is taken from that screen. */
  private fun ownerOnScreen(screen: Screen = Screen.getPrimary()) = Stage().also {
    val bounds = screen.visualBounds
    it.width = 800.0
    it.height = 600.0
    it.x = bounds.minX + (bounds.width - it.width) / 2
    it.y = bounds.minY + (bounds.height - it.height) / 2
  }

  private fun capFor(screen: Screen) = screen.visualBounds.height * maxContentHeightRatio

  @Test
  fun `a long message asks for no more than the capped height`() = onFxThread {
    val owner = ownerOnScreen()
    val cap = capFor(Screen.getPrimary())

    val capped = UIFacadeTestAccess.makeScrollable(messageLabel(longMessage), owner) as ScrollPane
    val uncapped = uncappedScrollPane(messageLabel(longMessage))
    layOut(capped, contentWidth, cap)
    layOut(uncapped, contentWidth, cap)

    val uncappedHeight = uncapped.prefHeight(contentWidth)
    val cappedHeight = capped.prefHeight(contentWidth)

    // Without this the test would pass on a message that is short enough to fit anyway, and would
    // therefore say nothing about the cap.
    assertTrue(uncappedHeight > cap,
      "the test message is too short to reach the cap: it asks for $uncappedHeight, the cap is $cap")
    assertTrue(cappedHeight <= cap,
      "a long message asks for $cappedHeight, which is more than the cap of $cap")
    assertEquals(minOf(uncappedHeight, cap), cappedHeight, 0.5,
      "the capped height should be the uncapped one limited to the cap")
  }

  @Test
  fun `a short message keeps the height it would have without the cap`() = onFxThread {
    val owner = ownerOnScreen()
    val cap = capFor(Screen.getPrimary())

    val capped = UIFacadeTestAccess.makeScrollable(messageLabel(shortMessage), owner) as ScrollPane
    val uncapped = uncappedScrollPane(messageLabel(shortMessage))
    layOut(capped, contentWidth, cap)
    layOut(uncapped, contentWidth, cap)

    val uncappedHeight = uncapped.prefHeight(contentWidth)
    assertTrue(uncappedHeight < cap,
      "the short message should fit on the screen, but it asks for $uncappedHeight against a cap of $cap")
    assertEquals(uncappedHeight, capped.prefHeight(contentWidth), 0.5,
      "a message that fits should not be changed by the cap")
  }

  @Test
  fun `what does not fit stays reachable by scrolling`() = onFxThread {
    val owner = ownerOnScreen()
    val cap = capFor(Screen.getPrimary())

    val capped = UIFacadeTestAccess.makeScrollable(messageLabel(longMessage), owner) as ScrollPane
    // The dialog pane gives the scroll pane the height it asked for, which is the cap.
    val root = layOut(capped, contentWidth, capped.prefHeight(contentWidth))

    val viewportHeight = capped.viewportBounds.height
    val contentHeight = capped.content.boundsInLocal.height

    assertTrue(viewportHeight <= cap + 0.5,
      "the visible part is $viewportHeight high, which is more than the cap of $cap")
    // The message is laid out in full behind a smaller window onto it: that is what makes the rest
    // reachable. Were it squeezed into the viewport instead, it would be clipped, not scrollable.
    assertTrue(contentHeight > viewportHeight,
      "the message is $contentHeight high and the visible part $viewportHeight: nothing is left to scroll to")

    // The scroll position is applied by the skin, which moves the sheet the message sits on rather
    // than the message itself, so it is measured here in scene coordinates.
    val beforeScrolling = capped.content.localToScene(capped.content.boundsInLocal)
    capped.vvalue = capped.vmax
    root.applyCss()
    root.layout()
    val afterScrolling = capped.content.localToScene(capped.content.boundsInLocal)

    assertEquals(contentHeight - viewportHeight, beforeScrolling.minY - afterScrolling.minY, 1.0,
      "scrolling to the end does not move the message by everything that did not fit")
    // And with that the end of the message stands inside the visible part.
    val viewport = capped.localToScene(capped.boundsInLocal)
    assertTrue(afterScrolling.maxY <= viewport.maxY + 1.0,
      "the end of the message is at ${afterScrolling.maxY}, below the bottom of the pane at ${viewport.maxY}")
  }
}
