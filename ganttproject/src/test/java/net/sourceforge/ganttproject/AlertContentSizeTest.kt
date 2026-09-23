/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2026 BarD Software s.r.o, GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject

import com.sandec.mdfx.MarkdownView
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.ButtonType
import javafx.scene.control.DialogPane
import javafx.scene.control.ScrollPane
import javafx.stage.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * showOptionDialog puts a MarkdownView into the dialog pane when the message is HTML or Markdown.
 * A MarkdownView is a VBox that asks for as much width as its longest paragraph needs, and
 * DialogPane takes the preferred size of its content as its own, so a long message used to make
 * the dialog as wide as the screen and taller than the screen, with the buttons below the bottom
 * edge.
 */
class AlertContentSizeTest {
  /** MAX_CONTENT_WIDTH in UIFacadeImpl. */
  private val maxContentWidth = 720.0

  /** The one HTML message the application shows today, from help.recover.autosaveInfo. */
  private val shortMessage =
    "**Name:** plan.gan,\n**Modified on:** Sun Sep 06 20:41:12 UTC 2026\n**Size:** 218861\n\nRecover this file?"
  private val longLine = "The reader stopped at line 17: the attribute \"duration\" of the task " +
    "holds the value -1, but a duration is expected to be a positive whole number of days. "
  private val longMessage = longLine.repeat(40)

  /** Lays out a dialog pane the way the toolkit would, without opening a window. */
  private fun pane(content: Node): DialogPane =
    DialogPane().also {
      it.content = content
      it.buttonTypes.add(ButtonType.OK)
      Scene(it)
      it.applyCss()
      it.layout()
    }

  /**
   * The content node as showOptionDialog built it before the bounds were put on it. The text block
   * it uses there has all of its indentation stripped as incidental, so what reaches MarkdownView
   * is the message and one trailing newline, and nothing else.
   */
  private fun bareMarkdownView(message: String) = MarkdownView(message + "\n")

  @Test
  fun `a long formatted message does not stretch the dialog sideways`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val content = pane(UIFacadeImpl.createFormattedContent(longMessage)).content
      val unbounded = bareMarkdownView(longMessage).prefWidth(-1.0)
      assertTrue(
        content.prefWidth(-1.0) <= maxContentWidth,
        "the message asks for ${unbounded.toInt()} px and the dialog offers " +
          "${content.prefWidth(-1.0).toInt()} px of it, more than the " +
          "${maxContentWidth.toInt()} px a message is meant to be laid out in"
      )
    }
  }

  @Test
  fun `a long formatted message does not stretch the dialog past the screen`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val allowed = Screen.getPrimary().visualBounds.height * 0.6
      val content = pane(UIFacadeImpl.createFormattedContent(longMessage)).content
      assertTrue(
        content.prefHeight(maxContentWidth) <= allowed,
        "the dialog asks for ${content.prefHeight(maxContentWidth).toInt()} px of height on a " +
          "screen of ${Screen.getPrimary().visualBounds.height.toInt()} px, so the buttons below " +
          "it are pushed off the bottom edge"
      )
    }
  }

  @Test
  fun `a long formatted message keeps growing behind a scroll bar`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val once = pane(UIFacadeImpl.createFormattedContent(longMessage)).content
      val fourTimes = pane(UIFacadeImpl.createFormattedContent(longMessage.repeat(4))).content
      assertEquals(
        once.prefHeight(maxContentWidth), fourTimes.prefHeight(maxContentWidth), 0.5,
        "a message four times as long makes the dialog taller, so the height it may take is not " +
          "bounded at all"
      )
      assertTrue(once is ScrollPane, "the message that does not fit cannot be scrolled to")
    }
  }

  @Test
  fun `a short formatted message keeps the dialog it had`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val today = pane(bareMarkdownView(shortMessage))
      val bounded = pane(UIFacadeImpl.createFormattedContent(shortMessage))
      assertEquals(
        today.prefWidth(-1.0), bounded.prefWidth(-1.0), 0.5,
        "a short message no longer opens a dialog of the width it used to have"
      )
      assertEquals(
        today.prefHeight(today.prefWidth(-1.0)), bounded.prefHeight(bounded.prefWidth(-1.0)), 0.5,
        "a short message no longer opens a dialog of the height it used to have"
      )
    }
  }
}
