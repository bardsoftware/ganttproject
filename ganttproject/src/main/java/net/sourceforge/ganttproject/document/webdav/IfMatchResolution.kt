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

/**
 * Which ETag, if any, a write should carry in `If-Match`.
 *
 * Kept apart from the WebDAV plumbing so that it can be decided, and tested, without a server.
 * Both ways of getting it wrong are silent: too weak a condition overwrites a colleague's work,
 * too strict a one reports a conflict that never happened and teaches the user to click
 * "overwrite anyway".
 */
sealed interface IfMatchDecision {
  /** Send this ETag in `If-Match`. */
  data class Send(val etag: String) : IfMatchDecision

  /**
   * Write without a condition. Reached only when nothing is remembered - a first write, or a read
   * whose version the server would not tell us. There is no version to be conditional on.
   */
  data object Unconditional : IfMatchDecision

  /** Somebody else changed the file. Do not write. */
  data object Conflict : IfMatchDecision

  /**
   * The server never answers with a strong ETag, so no write can be made conditional.
   *
   * Refusing rather than falling back to an unconditional write: RFC 9110 requires a weak
   * validator whenever the representation is transformed in transit - `mod_deflate`, nginx with
   * `gzip`, a compressing proxy, a CDN. Behind any of those the tag never becomes strong, so a
   * fallback would not be a rare concession but every single save, and conditional writing would
   * be switched off permanently without a sign of it.
   */
  data object VersioningUnavailable : IfMatchDecision
}

/** `W/"abc"` is weak, `"abc"` is strong. */
fun isWeakEtag(etag: String): Boolean = etag.trimStart().startsWith("W/")

/**
 * The tag without its weakness marker, for recognising one value across a weak/strong change.
 *
 * Not a general ETag comparison: dropping `W/` and treating the rest as equal is exactly the
 * shortcut that would let a stale write through. Used only against a value the server just
 * reported, to tell "our own file, freshly written" from "somebody else's".
 */
fun opaqueEtag(etag: String): String = etag.trim().removePrefix("W/").trim()

/** How long to wait out Apache's sub-second mtime window before asking a second time. */
const val WEAK_ETAG_RETRY_MILLIS = 1100L

/**
 * Decides the `If-Match` value for a write.
 *
 * Apache with `mod_dav_fs` returns no ETag on `PUT`, and for about a second after a write it
 * reports the ETag as weak, afterwards the same value as strong. `If-Match` is compared strongly
 * (RFC 9110), so a weak tag matches nothing at all, not even itself. Sending a freshly fetched
 * weak tag straight back would therefore produce a 412 on every save and show the user a conflict
 * that never happened. Hence the second question after a pause: it separates that window, which
 * passes, from a permanently weak validator, which does not.
 *
 * @param remembered the ETag the pending changes are based on, or null when none is known.
 * @param currentFromServer asks the server for the ETag the file carries right now; null when the
 * question cannot be answered. Consulted only when [remembered] is weak.
 * @param pause waits between the two questions. A parameter so that tests need not really sleep.
 */
// @JvmOverloads: the caller is Java and would not otherwise see Kotlin's default argument.
@JvmOverloads
fun resolveIfMatch(
  remembered: String?,
  currentFromServer: () -> String?,
  pause: () -> Unit = { Thread.sleep(WEAK_ETAG_RETRY_MILLIS) }
): IfMatchDecision {
  if (remembered == null) return IfMatchDecision.Unconditional
  if (!isWeakEtag(remembered)) return IfMatchDecision.Send(remembered)

  when (val first = judge(remembered, currentFromServer())) {
    is Judged.Decided -> return first.decision
    Judged.StillWeak -> {}
  }

  pause()
  return when (val second = judge(remembered, currentFromServer())) {
    is Judged.Decided -> second.decision
    Judged.StillWeak -> IfMatchDecision.VersioningUnavailable
  }
}

private sealed interface Judged {
  data class Decided(val decision: IfMatchDecision) : Judged
  data object StillWeak : Judged
}

private fun judge(remembered: String, current: String?): Judged {
  // Cannot ask: send the weak tag and let the server refuse it. Being refused is the safe
  // direction, writing blind is not.
  if (current == null) return Judged.Decided(IfMatchDecision.Send(remembered))
  if (opaqueEtag(current) != opaqueEtag(remembered)) return Judged.Decided(IfMatchDecision.Conflict)
  return if (isWeakEtag(current)) Judged.StillWeak else Judged.Decided(IfMatchDecision.Send(current))
}
