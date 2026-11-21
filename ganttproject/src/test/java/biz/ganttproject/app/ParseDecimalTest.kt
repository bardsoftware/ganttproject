/*
Copyright 2026 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.app

import biz.ganttproject.core.option.ValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Locale

/**
 * Tests for the locale-aware decimal parsing used when editing decimal cells. See issue #2900.
 */
class ParseDecimalTest {
  private fun assertDecimal(expected: String, actual: BigDecimal?) =
    assertEquals(0, BigDecimal(expected).compareTo(actual), "expected $expected, got $actual")

  @Test
  fun `comma is a decimal separator in German and Russian`() {
    assertDecimal("3.1415", parseDecimal("3,1415", Locale.GERMAN))
    assertDecimal("3.1415", parseDecimal("3,1415", Locale.forLanguageTag("ru")))
  }

  @Test
  fun `period is a decimal separator in English`() {
    assertDecimal("3.1415", parseDecimal("3.1415", Locale.ENGLISH))
  }

  @Test
  fun `grouping separators are accepted`() {
    assertDecimal("1234.5", parseDecimal("1.234,5", Locale.GERMAN))
    assertDecimal("1234.5", parseDecimal("1,234.5", Locale.ENGLISH))
    // Regular space typed instead of the locale's non-breaking space.
    assertDecimal("1234.5", parseDecimal("1 234,5", Locale.forLanguageTag("ru")))
  }

  @Test
  fun `precision is preserved`() {
    assertDecimal("0.1", parseDecimal("0,1", Locale.GERMAN))
    assertDecimal("12345678901234567890.123456789", parseDecimal("12345678901234567890.123456789", Locale.ENGLISH))
  }

  @Test
  fun `blank text gives null`() {
    assertNull(parseDecimal("  ", Locale.GERMAN))
  }

  @Test
  fun `garbage is rejected`() {
    assertThrows<ValidationException> { parseDecimal("abc", Locale.GERMAN) }
    assertThrows<ValidationException> { parseDecimal("3,14abc", Locale.GERMAN) }
  }

  @Test
  fun `editing format keeps all fraction digits`() {
    assertEquals("1,23456", formatDecimalForEditing(BigDecimal("1.23456"), Locale.GERMAN))
    assertEquals("1.234,5", formatDecimalForEditing(BigDecimal("1234.50"), Locale.GERMAN))
    assertEquals("3", formatDecimalForEditing(BigDecimal("3.000"), Locale.ENGLISH))
  }

  @Test
  fun `editing format round-trips through parsing`() {
    listOf("1.23456", "0.1", "1234567.891011", "12345678901234567890.123456789").forEach {
      val value = BigDecimal(it)
      listOf(Locale.GERMAN, Locale.ENGLISH, Locale.forLanguageTag("ru"), Locale.FRENCH).forEach { locale ->
        assertDecimal(it, parseDecimal(formatDecimalForEditing(value, locale), locale))
      }
    }
  }
}
