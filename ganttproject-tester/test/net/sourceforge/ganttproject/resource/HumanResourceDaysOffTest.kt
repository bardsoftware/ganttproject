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
package net.sourceforge.ganttproject.resource

import biz.ganttproject.core.calendar.GanttDaysOff
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.customproperty.CustomColumnsManager
import net.sourceforge.ganttproject.roles.Role
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.text.DateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

/**
 * Everything about a resource's days off: how they are added, how they are taken away again, what
 * getDaysOff() hands out, and how many notifications each way costs.
 *
 * The thread running through all of it is that adding and removing must be symmetric with respect
 * to notification. Whoever listens to a resource has to learn about both, because both change the
 * resource's load distribution. Adding always went through [HumanResource.addDaysOff], which resets
 * the loads and fires; removing used to notify nobody, because there was no entry point for it and
 * the only way was to mutate the list handed out by [HumanResource.getDaysOff], which nothing was
 * watching. Removal now goes through [HumanResource.removeDaysOff], [HumanResource.clearDaysOff] or
 * [HumanResource.setDaysOff], and that list is an unmodifiable view, so the old way is gone.
 *
 * These tests were three classes -- HumanResourceDaysOffTest, HumanResourceDaysOffViewTest and
 * HumanResourceRemoveDaysOffTest -- split by which method they pinned. They are one class now: they
 * share the whole fixture (the locale, the counting view, the manager-with-one-resource) and they
 * describe one feature from several sides rather than three features.
 */
class HumanResourceDaysOffTest {
  init {
    // GanttDaysOff builds GanttCalendars, and those need a locale.
    object : CalendarFactory() {
      init {
        setLocaleApi(object : CalendarFactory.LocaleApi {
          override fun getLocale(): Locale = Locale.GERMANY
          override fun getShortDateFormat(): DateFormat =
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMANY)
        })
      }
    }
  }

  private class CountingView : ResourceView {
    var changed = 0
    override fun resourceAdded(event: ResourceEvent) {}
    override fun resourcesRemoved(event: ResourceEvent) {}
    override fun resourceChanged(e: ResourceEvent) { changed++ }
    override fun resourceAssignmentsChanged(e: ResourceEvent) {}
    override fun resourceStructureChanged() {}
    override fun resourceModelReset() {}
  }

  private fun september(day: Int): Date = CalendarFactory.createGanttCalendar(2026, 8, day).time

  private fun daysOff(from: Int, to: Int) = GanttDaysOff(september(from), september(to))

  /** The same day of September, but carrying a time of day. See the hashCode test below. */
  private fun septemberAt(day: Int, hour: Int, minute: Int): Date =
    GregorianCalendar(2026, 8, day, hour, minute).time

  private fun newPerson(): HumanResource {
    val manager = HumanResourceManager(null as Role?, CustomColumnsManager())
    val person = manager.newHumanResource()
    person.name = "Tester"
    manager.add(person)
    return person
  }

  /** A manager with one resource in it, plus a view counting the resourceChanged notifications. */
  private fun newPersonWithView(): Pair<HumanResource, CountingView> {
    val manager = HumanResourceManager(null as Role?, CustomColumnsManager())
    val person = manager.newHumanResource()
    person.name = "Tester"
    manager.add(person)
    val view = CountingView()
    manager.addView(view)
    return person to view
  }

  // ---------------------------------------------------------------- adding and removing

  @Test
  fun `removing the last day off notifies the listeners just as adding it does`() {
    val (person, view) = newPersonWithView()

    person.addDaysOff(GanttDaysOff(september(9), september(10)))
    assertEquals(1, view.changed, "adding a day off must notify the listeners")

    view.changed = 0
    person.clearDaysOff()
    assertEquals(
      1, view.changed,
      "removing the LAST day off must notify the listeners too -- otherwise nobody recalculates"
    )
  }

  // Red before the change:
  // e: file:///.../HumanResourceRemoveDaysOffTest.kt:88:23 Unresolved reference 'removeDaysOff'.
  @Test
  fun `removing a single day off takes it off the resource and notifies once`() {
    val (person, view) = newPersonWithView()
    val first = daysOff(9, 10)
    val second = daysOff(20, 21)
    person.addDaysOff(first)
    person.addDaysOff(second)

    view.changed = 0
    assertTrue(person.removeDaysOff(first), "removeDaysOff must report that it removed something")

    assertEquals(1, view.changed, "removing a day off must notify the listeners exactly once")
    assertEquals(1, person.daysOff.size, "the other day off must still be there")
    assertEquals(second, person.daysOff.get(0), "the WRONG day off was removed")
  }

  // Red before the change:
  // e: file:///.../HumanResourceRemoveDaysOffTest.kt:105:24 Unresolved reference 'removeDaysOff'.
  @Test
  fun `removing a day off the resource does not have changes nothing and notifies nobody`() {
    val (person, view) = newPersonWithView()
    person.addDaysOff(daysOff(9, 10))

    view.changed = 0
    // Deliberately an interval the resource has never been given, and one that does not equal the
    // interval it does have. Matching is by value now, so "not there" means no equal interval is
    // there -- see the by-value test below for the other half of that sentence.
    assertFalse(person.removeDaysOff(daysOff(24, 25)), "an interval that is not there cannot be removed")

    assertEquals(0, view.changed, "a removal that removed nothing must not notify anybody")
    assertEquals(1, person.daysOff.size, "the existing day off must be untouched")
  }

  // Red before the change:
  // e: file:///.../HumanResourceRemoveDaysOffTest.kt:119:12 Unresolved reference 'clearDaysOff'.
  @Test
  fun `clearing the days off empties the list and notifies once, whatever the count`() {
    val (person, view) = newPersonWithView()
    person.addDaysOff(daysOff(9, 10))
    person.addDaysOff(daysOff(20, 21))
    person.addDaysOff(daysOff(24, 25))

    view.changed = 0
    person.clearDaysOff()

    assertEquals(0, person.daysOff.size, "clearDaysOff must leave no day off behind")
    assertEquals(1, view.changed, "clearing three days off must notify exactly once, as clear() does")
  }

  // Red before the change:
  // e: file:///.../HumanResourceRemoveDaysOffTest.kt:130:12 Unresolved reference 'clearDaysOff'.
  @Test
  fun `clearing an empty list of days off notifies nobody`() {
    val (person, view) = newPersonWithView()

    view.changed = 0
    person.clearDaysOff()

    assertEquals(0, view.changed, "there was nothing to remove, so there is nothing to report")
  }

  // ---------------------------------------------------------------- equality by value

  /**
   * GanttDaysOff used to carry an overload, equals(GanttDaysOff), which no List ever calls: remove()
   * and contains() call equals(Object). Removal by value therefore did nothing at all, and an
   * interval could only be taken off the resource by handing back the very object it was given.
   *
   * Since HumanResource delegates removal to its list, that is the list's matching rule, so this is
   * what makes removeDaysOff usable by a caller that did not keep the original object -- a caller
   * rebuilding intervals from a dialog, say.
   */
  @Test
  fun `a day off is removed by value and not by object identity`() {
    val (person, view) = newPersonWithView()
    person.addDaysOff(daysOff(9, 10))
    person.addDaysOff(daysOff(20, 21))

    val sameValueOtherObject = daysOff(9, 10)
    assertNotEquals(
      System.identityHashCode(person.daysOff.get(0)), System.identityHashCode(sameValueOtherObject),
      "the test is meaningless unless these really are two different objects"
    )

    view.changed = 0
    assertTrue(
      person.removeDaysOff(sameValueOtherObject),
      "an interval equal to one the resource holds must remove it, whoever built it"
    )
    assertEquals(1, view.changed, "a removal by value must notify exactly once, like any other")
    assertEquals(1, person.daysOff.size, "exactly one interval must be gone")
    assertEquals(daysOff(20, 21), person.daysOff.get(0), "and it must be the OTHER one that stayed")
  }

  /**
   * The hashCode contract: equal objects must have equal hash codes.
   *
   * This is not free here. GanttDaysOff holds two GanttCalendars, and GanttCalendar overrides
   * equals(Object) -- comparing year, month and day only -- without overriding hashCode(). It thus
   * inherits GregorianCalendar.hashCode(), which takes the time of day into account, so two equal
   * GanttCalendars can have different hash codes. A GanttDaysOff whose hashCode delegated to its
   * calendars would inherit that, and days off built from a Date keep that Date's time of day, so
   * the case below is the ordinary one and not a contrivance.
   */
  @Test
  fun `equal days off have equal hash codes even when built from different times of day`() {
    val morning = GanttDaysOff(septemberAt(9, 8, 0), septemberAt(10, 8, 0))
    val evening = GanttDaysOff(septemberAt(9, 19, 45), septemberAt(10, 19, 45))

    assertEquals(morning, evening, "same two days means the same day off interval, whatever the clock said")
    assertEquals(
      morning.hashCode(), evening.hashCode(),
      "equal days off must have equal hash codes -- delegating to GanttCalendar.hashCode() breaks this"
    )

    // And a HashSet, which is what the contract is actually for.
    assertEquals(1, hashSetOf(morning, evening).size, "two equal days off must collapse into one set entry")
  }

  @Test
  fun `days off with different dates are not equal, and neither is a non-day-off`() {
    assertNotEquals(daysOff(9, 10), daysOff(9, 11), "a different end date is a different interval")
    assertNotEquals(daysOff(9, 10), daysOff(8, 10), "a different start date is a different interval")
    assertFalse(daysOff(9, 10).equals(null), "nothing equals null")
    assertFalse(daysOff(9, 10).equals("2026-09-09 -> 2026-09-10"), "and nothing equals a String either")
  }

  // ---------------------------------------------------------------- the handed-out view

  @Test
  fun `the list handed out by getDaysOff cannot be modified`() {
    val person = newPerson()
    person.addDaysOff(daysOff(9, 10))
    val handedOut = person.daysOff

    assertThrows<UnsupportedOperationException>("clearing the handed-out list must not be possible") {
      handedOut.clear()
    }
    assertThrows<UnsupportedOperationException>("adding to the handed-out list must not be possible") {
      handedOut.add(0, daysOff(20, 21))
    }
    assertThrows<UnsupportedOperationException>("removing from the handed-out list must not be possible") {
      handedOut.removeAt(0)
    }
    assertThrows<UnsupportedOperationException>("overwriting in the handed-out list must not be possible") {
      handedOut.set(0, daysOff(20, 21))
    }

    assertEquals(1, person.daysOff.size, "none of the rejected calls may have changed the resource")
  }

  @Test
  fun `the list handed out by getDaysOff is a view and not a copy`() {
    val person = newPerson()
    person.addDaysOff(daysOff(9, 10))

    val handedOut = person.daysOff
    assertEquals(1, handedOut.size, "the day off given before the call must be in the handed-out list")

    person.addDaysOff(daysOff(20, 21))
    assertEquals(
      2, handedOut.size,
      "a day off given AFTER the call must show up too -- a copy would still show one"
    )
    assertEquals(daysOff(20, 21).start, handedOut.get(1).start, "and it must be the new interval")
  }

  // ---------------------------------------------------------------- replacing the whole list

  /**
   * The point of setDaysOff() is the number of notifications, so this counts them on both ways of
   * writing the same end state, side by side.
   *
   * Replacing M intervals with N by hand costs (M > 0 ? 1 : 0) + N notifications, and each one
   * resets the resource's load distribution, so every partial state on the way -- the resource with
   * some of the new intervals but not all of them -- is announced to every listener as if it were
   * real. setDaysOff() announces the end state and nothing else: exactly one.
   *
   * The third way, reaching into the handed-out list, is checked over the same matrix: it is
   * rejected, so it changes nothing and reports nothing. This test carries every assertion of the
   * old HumanResourceRemoveDaysOffTest.`the dialog's clear-all-and-rewrite still costs the same
   * notifications`, which is why that method has no separate successor here.
   */
  @Test
  fun `setDaysOff replaces the whole list and notifies exactly once`() {
    for (m in 0..3) {
      for (n in 0..3) {
        val replacement = (0 until n).map { daysOff(it + 10, it + 11) }

        // The way it was written back before there was a clearDaysOff() at all: straight into the
        // handed-out list. That is rejected now, so it costs nothing and reports nothing.
        val (viaList, listView) = newPersonWithView()
        repeat(m) { viaList.addDaysOff(daysOff(it + 1, it + 2)) }
        listView.changed = 0
        assertThrows<UnsupportedOperationException>("M=$m N=$n: the old way must be rejected") {
          viaList.daysOff.clear()
        }
        assertEquals(m, viaList.daysOff.size, "M=$m N=$n: the rejected call must not have removed anything")
        assertEquals(0, listView.changed, "M=$m N=$n: the rejected call must not have notified anybody")

        // The way GanttDialogPerson.applyChanges() wrote the edited intervals back until now.
        val (byHand, handView) = newPersonWithView()
        repeat(m) { byHand.addDaysOff(daysOff(it + 1, it + 2)) }
        handView.changed = 0
        byHand.clearDaysOff()
        replacement.forEach { byHand.addDaysOff(it) }
        assertEquals(
          (if (m > 0) 1 else 0) + n, handView.changed,
          "M=$m N=$n: the way through clearDaysOff() costs one notification per step"
        )
        assertEquals(n, byHand.daysOff.size, "M=$m N=$n: the rewritten intervals must be the only ones")

        // The way it writes them back now.
        val (atOnce, onceView) = newPersonWithView()
        repeat(m) { atOnce.addDaysOff(daysOff(it + 1, it + 2)) }
        onceView.changed = 0
        atOnce.setDaysOff(replacement)

        val expected = if (m == 0 && n == 0) 0 else 1
        assertEquals(
          expected, onceView.changed,
          "M=$m N=$n: setDaysOff() must notify once, or not at all when nothing changed"
        )
        assertEquals(n, atOnce.daysOff.size, "M=$m N=$n: the resource must end up with the new intervals")
        assertEquals(replacement, atOnce.daysOff, "M=$m N=$n: and with exactly those, in order")
      }
    }
  }

  /**
   * Writing back what is already there is what the properties dialog does whenever the user opens it
   * and presses Ok without touching the days off. Nothing changed, so nothing is reported -- the
   * rule addDaysOff, removeDaysOff and clearDaysOff already follow.
   *
   * The comparison is by value, so it only means anything because GanttDaysOff overrides
   * equals(Object): the dialog rebuilds its intervals from the date pickers and hands over fresh
   * objects, never the ones the resource holds.
   */
  @Test
  fun `setDaysOff with equal intervals changes nothing and notifies nobody`() {
    val (person, view) = newPersonWithView()
    person.addDaysOff(daysOff(9, 10))
    person.addDaysOff(daysOff(20, 21))

    view.changed = 0
    person.setDaysOff(listOf(daysOff(9, 10), daysOff(20, 21)))

    assertEquals(0, view.changed, "an unchanged list of days off must not be announced")
    assertEquals(2, person.daysOff.size, "and the resource must still have both intervals")

    // Same intervals, other order: that IS a change, and it is reported.
    person.setDaysOff(listOf(daysOff(20, 21), daysOff(9, 10)))
    assertEquals(1, view.changed, "a reordering is a change to the list and must be announced once")
  }

  /**
   * getDaysOff() hands out a view onto the resource's own list. Passing it straight back into
   * setDaysOff() must therefore not empty the resource: the implementation has to copy before it
   * clears. This is the obvious thing for a caller to write, so it must not be a trap.
   */
  @Test
  fun `setDaysOff survives being handed the resource's own list back`() {
    val (person, view) = newPersonWithView()
    person.addDaysOff(daysOff(9, 10))
    person.addDaysOff(daysOff(20, 21))

    view.changed = 0
    person.setDaysOff(person.daysOff)

    assertEquals(2, person.daysOff.size, "handing the view back must not empty the resource")
    assertEquals(0, view.changed, "and nothing changed, so nobody is told")
  }

  /**
   * The sharp version of the test above. Handing back the whole view is caught by the
   * nothing-changed shortcut before anything is cleared, so it does not actually exercise the copy.
   * A sublist of the view does: it is a live window onto the resource's own list and it is NOT equal
   * to it, so the shortcut does not fire and the list really is cleared -- pulling the argument out
   * from under the call unless it was copied first.
   */
  @Test
  fun `setDaysOff survives being handed a live sublist of the resource's own list`() {
    val (person, view) = newPersonWithView()
    person.addDaysOff(daysOff(9, 10))
    person.addDaysOff(daysOff(20, 21))
    person.addDaysOff(daysOff(24, 25))

    view.changed = 0
    person.setDaysOff(person.daysOff.subList(0, 2))

    assertEquals(
      listOf(daysOff(9, 10), daysOff(20, 21)), person.daysOff,
      "the resource must end up with the two intervals it was handed, not with none"
    )
    assertEquals(1, view.changed, "and that is one change, announced once")
  }
}
