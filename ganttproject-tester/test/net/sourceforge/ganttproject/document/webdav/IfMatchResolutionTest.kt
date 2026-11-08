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
package net.sourceforge.ganttproject.document.webdav

import junit.framework.TestCase

/**
 * The decision behind conditional writing, checked without a server.
 *
 * This covers the rule, not the defect from issue #2818: the rule was never wrong, the ETag simply
 * never reached it. That the ETag is asked for at all is asserted by
 * [MiltonResourceEtagTest], and that is the regression test for the issue.
 *
 * Both failure directions are covered on purpose. Too weak a condition overwrites somebody else's
 * work in silence; too strict a one reports a conflict that never happened and teaches the user to
 * click "overwrite anyway".
 */
class IfMatchResolutionTest : TestCase() {

  /** Never asked: the extra request exists for the weak case only. */
  private val neverAsked: () -> String? = { fail("the server was asked although the tag was strong"); null }

  fun testAStrongTagIsSentAsItIs() {
    assertEquals(IfMatchDecision.Send("\"abc\""), resolveIfMatch("\"abc\"", neverAsked))
  }

  /** Nothing known to be conditional on. A first write, and the only case where that is right. */
  fun testWithoutATagTheWriteIsUnconditional() {
    assertEquals(IfMatchDecision.Unconditional, resolveIfMatch(null, neverAsked))
  }

  /**
   * What Apache produces within a second of a write: the same value, now reported strongly.
   * Sending the strong form is right, it still identifies our own content.
   */
  fun testAWeakTagIsReplacedByTheStrongOneTheServerNowReports() {
    assertEquals(IfMatchDecision.Send("\"abc\""), resolveIfMatch("W/\"abc\"", currentFromServer = { "\"abc\"" }))
  }

  /**
   * Still weak on the first ask, strong on the second: Apache's sub-second mtime window. Waiting
   * it out and asking again is what keeps ordinary saving working.
   */
  fun testStillWeakThenStrongAfterWaiting() {
    val answers = mutableListOf("W/\"abc\"", "\"abc\"")
    var paused = 0
    val decision = resolveIfMatch("W/\"abc\"", { answers.removeAt(0) }, { paused++ })
    assertEquals(IfMatchDecision.Send("\"abc\""), decision)
    assertEquals("the second question must come after waiting, not immediately", 1, paused)
  }

  /**
   * A server that stays weak gets no write at all, rather than an unconditional one.
   *
   * Weakness is not always Apache's mtime window: RFC 9110 requires a weak validator whenever the
   * representation is transformed in transit - mod_deflate, nginx with gzip, a compressing proxy,
   * a CDN. Behind any of those it never becomes strong, so a fallback to an unconditional write
   * would not be a rare concession but every single save, and the protection would be off without
   * a sign of it.
   */
  fun testAServerThatStaysWeakGetsNoWriteAtAll() {
    assertEquals(
      IfMatchDecision.VersioningUnavailable,
      resolveIfMatch("W/\"abc\"", { "W/\"abc\"" }, {})
    )
  }

  /** Somebody else wrote while we were waiting out the window. Still a conflict, not a write. */
  fun testAChangeDuringTheWaitIsAConflict() {
    val answers = mutableListOf("W/\"abc\"", "\"xyz\"")
    assertEquals(
      IfMatchDecision.Conflict,
      resolveIfMatch("W/\"abc\"", { answers.removeAt(0) }, {})
    )
  }

  /** No waiting, and no second question, when the first answer already decides it. */
  fun testTheServerIsNotAskedTwiceWithoutNeed() {
    var asked = 0
    val decision = resolveIfMatch(
      "W/\"abc\"",
      { asked++; "\"abc\"" },
      { fail("waited although the first answer already decided it") }
    )
    assertEquals(IfMatchDecision.Send("\"abc\""), decision)
    assertEquals(1, asked)
  }

  /** The case the whole mechanism exists for: somebody else wrote in the meantime. */
  fun testADifferentValueIsAConflict() {
    assertEquals(IfMatchDecision.Conflict, resolveIfMatch("W/\"abc\"", currentFromServer = { "\"xyz\"" }))
    assertEquals(IfMatchDecision.Conflict, resolveIfMatch("W/\"abc\"", currentFromServer = { "W/\"xyz\"" }))
  }

  /**
   * The server cannot be asked. Send the weak tag anyway and let it refuse: being refused is the
   * safe direction. A wrong conflict costs a click, a lost change costs work.
   */
  fun testWhenTheServerCannotBeAskedTheWeakTagGoesOutAnyway() {
    assertEquals(IfMatchDecision.Send("W/\"abc\""), resolveIfMatch("W/\"abc\"", currentFromServer = { null }))
  }

  /**
   * `W/` is stripped only to recognise one value across a weak/strong change. It must never make
   * two different tags look equal - that shortcut is what would let a stale write pass.
   */
  fun testTheWeaknessMarkerNeverMakesDifferentTagsEqual() {
    assertEquals("\"abc\"", opaqueEtag("W/\"abc\""))
    assertEquals("\"abc\"", opaqueEtag("\"abc\""))
    assertFalse(opaqueEtag("W/\"abc\"") == opaqueEtag("\"abcd\""))
  }

  fun testWeaknessIsRecognised() {
    assertTrue(isWeakEtag("W/\"abc\""))
    assertFalse(isWeakEtag("\"abc\""))
    // Not a weakness marker: a tag whose content happens to start with W.
    assertFalse(isWeakEtag("\"W/abc\""))
  }
}
