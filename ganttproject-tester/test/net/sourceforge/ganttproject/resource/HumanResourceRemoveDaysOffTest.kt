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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * A resource can be given a day off through [HumanResource.addDaysOff], but until now it could only
 * be taken away again by reaching into the list handed out by [HumanResource.getDaysOff] and
 * mutating it. These tests pin the counterpart of addDaysOff: [HumanResource.removeDaysOff] for a
 * single interval and [HumanResource.clearDaysOff] for all of them, both notifying exactly like the
 * mutation of the handed-out list does.
 *
 * Every one of the five tests below was seen to fail against the state before this change, each with
 * its own error. The failure is a Kotlin compilation error rather than a failed assertion, because
 * the methods being pinned did not exist yet -- there is no way to name a method that is not there
 * and still compile. The verbatim output of that run is quoted at each test method; the line and
 * column numbers in it refer to this file as it stood in that run, before these very comments were
 * added, so they no longer point at the quoted line.
 */
class HumanResourceRemoveDaysOffTest {
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
    // Deliberately an interval the resource has never been given. GanttDaysOff does not override
    // equals(Object) -- it only overloads equals(GanttDaysOff) -- so an interval with the very same
    // dates is still a different one as far as the list is concerned. That is the behaviour pinned
    // here, so that it cannot change unnoticed.
    assertFalse(person.removeDaysOff(daysOff(9, 10)), "an interval that is not there cannot be removed")

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

  /**
   * The one caller in the tree, GanttDialogPerson.applyChanges(), throws all the intervals away and
   * writes the edited ones back. This pins that going through clearDaysOff() instead of reaching
   * into the handed-out list costs the very same number of notifications -- neither more (which
   * would be a regression) nor zero where there used to be one.
   *
   * Red before the change:
   * e: file:///.../HumanResourceRemoveDaysOffTest.kt:157:19 Unresolved reference 'clearDaysOff'.
   */
  @Test
  fun `the dialog's clear-all-and-rewrite costs the same notifications either way`() {
    // M = intervals before the Ok, N = intervals in the dialog when Ok is pressed.
    for (m in 0..3) {
      for (n in 0..3) {
        val expected = (if (m > 0) 1 else 0) + n

        val (viaList, listView) = newPersonWithView()
        repeat(m) { viaList.addDaysOff(daysOff(it + 1, it + 2)) }
        listView.changed = 0
        viaList.daysOff.clear()
        repeat(n) { viaList.addDaysOff(daysOff(it + 10, it + 11)) }

        val (viaMethod, methodView) = newPersonWithView()
        repeat(m) { viaMethod.addDaysOff(daysOff(it + 1, it + 2)) }
        methodView.changed = 0
        viaMethod.clearDaysOff()
        repeat(n) { viaMethod.addDaysOff(daysOff(it + 10, it + 11)) }

        assertEquals(expected, listView.changed, "M=$m N=$n: the old way through the handed-out list")
        assertEquals(expected, methodView.changed, "M=$m N=$n: the new way through clearDaysOff()")
        assertEquals(n, viaMethod.daysOff.size, "M=$m N=$n: the rewritten intervals must be the only ones")
      }
    }
  }
}
