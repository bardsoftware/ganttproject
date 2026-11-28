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
package biz.ganttproject.ganttview

import biz.ganttproject.app.DefaultLocalizer
import biz.ganttproject.app.MenuBuilderAsList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.swing.Action

/**
 * What the views button and the view management dialog show.
 */
class TaskViewMenuTest {

  /**
   * The menu entries are cached per view, and the dialog renames the very view object the entry
   * was made for. RED before the fix: expected <OhneGleich> but was <taskView.defaultName>.
   */
  @Test
  fun `the menu shows the new name of a view renamed in the dialog`() {
    val mgr = TaskViewManager()
    val view = mgr.createView("taskView.defaultName")
    mgr.addView(view)
    mgr.activeView = view
    val actionSet = TaskViewActionSet(mgr)
    actionSet.tableViewActions(MenuBuilderAsList())

    // What ViewEditor.saveData and the Apply button do
    view.title = "OhneGleich"
    mgr.importViews(listOf(view))

    val menu = MenuBuilderAsList().also(actionSet::tableViewActions)
    assertEquals("OhneGleich", menu.actions().first().getValue(Action.NAME))
  }

  /**
   * The keys of the view dialog are not in the translation bundle yet. Then the dialog has to show
   * its own keys, not the texts of the filter dialog. RED before the fix:
   * expected <[title, addItem, visibility.label]> but was <[Task Filters, New Filter, Enable filter]>.
   */
  @Test
  fun `the view dialog does not borrow the texts of the filter dialog`() {
    val i18n = viewDialogLocalizer(mapLocalizer(
      "taskTable.filterDialog.title" to "Task Filters",
      "taskTable.filterDialog.addItem" to "New Filter",
      "taskTable.filterDialog.visibility.label" to "Enable filter"
    ))

    assertEquals(listOf("title", "addItem", "visibility.label"), dialogKeys.map { i18n.formatText(it) })
  }

  @Test
  fun `the view dialog uses its own texts once they are there`() {
    val i18n = viewDialogLocalizer(mapLocalizer(
      "taskViewDialog.title" to "Task Views",
      "taskViewDialog.addItem" to "New View",
      "taskViewDialog.visibility.label" to "Use this view",
      "taskTable.filterDialog.title" to "Task Filters"
    ))

    assertEquals(listOf("Task Views", "New View", "Use this view"), dialogKeys.map { i18n.formatText(it) })
  }

  private val dialogKeys = listOf("title", "addItem", "visibility.label")

  private fun mapLocalizer(vararg texts: Pair<String, String>) = object : DefaultLocalizer() {
    override fun formatTextOrNull(key: String, vararg args: Any): String? = mapOf(*texts)[key]
  }
}
