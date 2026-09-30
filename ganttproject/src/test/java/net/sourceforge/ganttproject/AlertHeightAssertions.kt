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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * The tolerance every height comparison in the tests of the alert message pane is made with.
 *
 * The cap on the message height is a share of the screen height and is therefore hardly ever a
 * whole number, while the layout hands a child only whole device pixels: Region rounds the size it
 * gives out up to the next one. A pane that asks for 1024 * 0.6 = 614.4 is given 615.0.
 *
 * The gap is ceil(cap * scale) / scale - cap, so it is below one device pixel by construction. For
 * an integer screen height at scale 1 it is one of 0, 0.4, 0.8, 0.2 or 0.6 depending on the height
 * modulo 5, at most 0.8; a render scale above 1 only makes it smaller. One whole pixel therefore
 * covers the rounding on any display, and it covers nothing beyond it.
 *
 * That is also why these tests pass on a 1920x1080 screen, whose cap of 648.0 is already whole, and
 * fail on a 1280x1024 one with `expected: <614.4> but was: <615.0>`.
 *
 * The tolerance is small against everything the tests have to tell apart: the smallest of those
 * differences is the 581 pixels between the height of a short message and the cap it must not be
 * stretched to, and the others are above 4000. And it is only used on numbers that come out of a
 * laid-out scene. The exact invariants -- that the capped height never exceeds the cap, that the
 * message is taller than the viewport that shows it -- are still asserted strictly, so no tolerance
 * can hide a cap that fails to bite.
 */
private const val PIXEL_ROUNDING_TOLERANCE = 1.0

/**
 * Asserts that a height measured in a laid-out scene is the one expected, up to the rounding the
 * layout applies to whole pixels.
 */
internal fun assertSameHeight(expected: Double, actual: Double, message: String) =
  assertEquals(expected, actual, PIXEL_ROUNDING_TOLERANCE, message)

/**
 * Asserts that a height measured in a laid-out scene stays within the given limit, up to the
 * rounding the layout applies to whole pixels.
 */
internal fun assertHeightAtMost(limit: Double, actual: Double, message: String) =
  assertTrue(actual <= limit + PIXEL_ROUNDING_TOLERANCE, message)
