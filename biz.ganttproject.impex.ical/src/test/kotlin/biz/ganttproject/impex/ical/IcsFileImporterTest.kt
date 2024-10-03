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
package biz.ganttproject.impex.ical

import biz.ganttproject.core.calendar.CalendarEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.w3c.util.DateParser

class IcsFileImporterTest {
  @Test
  fun `basic smoke test`() {
    val events = IcsImport.readEvents(IcsFileImporterTest::class.java.getResourceAsStream("/test.ics")!!)
    assertFalse(events.isEmpty())
    assertEquals(DateParser.parse("2023-12-07"), events[0].date)
    assertEquals("Chanukah: 1 Candle", events[0].title)
    assertEquals(CalendarEvent.Type.HOLIDAY ,events[0].type)
  }
}