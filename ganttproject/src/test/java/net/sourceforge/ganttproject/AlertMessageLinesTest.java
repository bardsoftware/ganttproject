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
package net.sourceforge.ganttproject;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the plain-text splitting that backs the scrollable alert content. The split decides both
 * where the line breaks are and how deep each line hangs, so it is worth testing without a window.
 */
public class AlertMessageLinesTest {
  @Test
  public void testSingleLineIsNotIndented() {
    assertEquals(
      List.of(new UIFacadeImpl.MessageLine(0, "Connection successful")),
      UIFacadeImpl.splitMessageLines("Connection successful"));
  }

  @Test
  public void testLeadingSpacesBecomeIndentAndLeaveTheText() {
    assertEquals(
      List.of(new UIFacadeImpl.MessageLine(2, "- first")),
      UIFacadeImpl.splitMessageLines("  - first"));
  }

  @Test
  public void testEachLineKeepsItsOwnIndent() {
    assertEquals(
      List.of(
        new UIFacadeImpl.MessageLine(0, "Heading"),
        new UIFacadeImpl.MessageLine(2, "- first"),
        new UIFacadeImpl.MessageLine(4, "detail of the first"),
        new UIFacadeImpl.MessageLine(2, "- second")),
      UIFacadeImpl.splitMessageLines("Heading\n  - first\n    detail of the first\n  - second"));
  }

  @Test
  public void testBlankLinesAreKept() {
    assertEquals(
      List.of(
        new UIFacadeImpl.MessageLine(0, "Heading"),
        new UIFacadeImpl.MessageLine(0, ""),
        new UIFacadeImpl.MessageLine(0, "Body")),
      UIFacadeImpl.splitMessageLines("Heading\n\nBody"));
  }

  @Test
  public void testWhitespaceOnlyLineDoesNotBecomeAnIndentedEmptyLine() {
    assertEquals(
      List.of(new UIFacadeImpl.MessageLine(0, "")),
      UIFacadeImpl.splitMessageLines("   "));
  }

  @Test
  public void testWindowsLineSeparatorsDoNotLeaveCarriageReturns() {
    assertEquals(
      List.of(
        new UIFacadeImpl.MessageLine(0, "Heading"),
        new UIFacadeImpl.MessageLine(2, "- first")),
      UIFacadeImpl.splitMessageLines("Heading\r\n  - first"));
  }

  @Test
  public void testEmptyMessageStillYieldsOneLine() {
    assertEquals(
      List.of(new UIFacadeImpl.MessageLine(0, "")),
      UIFacadeImpl.splitMessageLines(""));
  }
}
