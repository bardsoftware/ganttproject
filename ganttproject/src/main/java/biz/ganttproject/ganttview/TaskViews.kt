/*
Copyright 2026

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

import biz.ganttproject.core.option.EnumerationOption
import biz.ganttproject.core.option.GPAbstractOption
import biz.ganttproject.core.option.ListOption
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.task.Task

/**
 * Named views for large plans.
 *
 * A view is a NAMED SET OF HIDDEN TASKS. Several of them live side by side in one project, only one
 * is active at a time, and the active one is a second condition next to the task filter -- see
 * TaskTable.doSync. Hiding a summary task takes its whole subtree along, because SyncAlgorithm does
 * not descend into a node it did not create.
 *
 * WHY THE SET HOLDS WHAT IS HIDDEN AND NOT WHAT IS SHOWN. This is the single most important
 * decision of the design. A task created after a view was saved appears in no view's hidden set,
 * so it stays VISIBLE in every view. The other direction -- a list of what is shown -- would make
 * every newly created summary task invisible in every named view at once, without a word. The wrong
 * direction of the two possible errors.
 *
 * WHY ONE OPTION FOR ALL VIEWS, ENCODED. The reader of a project file matches option ids against a
 * list that is built BEFORE the file is read (ProxyDocument:224 -> TaskSerializer.kt:243). An
 * `<option id="view.Rohbau">` would find no counterpart and would be dropped without a message. So
 * all views have to travel in ONE option, and that means a grammar of our own. The grammar is the
 * one GPCloudStorageOptions has been using since 2018: "\n" between records, "\t" between fields.
 * Measured, not assumed: a multi-line CDATA survives the SAX writer and the Jackson reader
 * unchanged -- see TaskViewsRoundTripTest.
 */
class NamedTaskView(
  override var title: String = "",
  val hiddenTaskUids: MutableSet<String> = linkedSetOf(),
  override val isEnabledProperty: BooleanProperty = SimpleBooleanProperty(false)
) : Item<NamedTaskView> {

  /** True when this view hides [task]. Its children disappear with it, but they are not listed. */
  fun hides(task: Task): Boolean = hiddenTaskUids.contains(task.uid)

  fun hide(task: Task) { hiddenTaskUids.add(task.uid) }

  fun show(task: Task) { hiddenTaskUids.remove(task.uid) }

  val hiddenCount: Int get() = hiddenTaskUids.size

  override fun toString() = "NamedTaskView(title='$title', hidden=${hiddenTaskUids.size})"
}

/**
 * The view that hides nothing. Same role as VOID_FILTER: it is what "show everything" means, and it
 * is never written into the project file.
 */
val VOID_TASK_VIEW: NamedTaskView = NamedTaskView("view.void")

/**
 * Id of the single option that carries all views. Deliberately not one option per view, see the
 * comment on [NamedTaskView].
 */
const val TASK_VIEWS_OPTION_ID = "taskViews"

/**
 * Holds the named views of one project, keeps track of which one is active and answers the question
 * the task table asks for every single task.
 */
class TaskViewManager {
  private val viewList = mutableListOf<NamedTaskView>()

  /** All named views, in the order they were created or read. */
  val views: List<NamedTaskView> get() = viewList.toList()

  /** Called whenever the active view changes, so menu entries can update their check mark. */
  val viewListeners = mutableListOf<(NamedTaskView) -> Unit>()

  /** Set by the task table; re-syncs table and chart. */
  internal var sync: () -> Unit = {}

  /** Set by the task table; marks the project dirty so the views reach the file. */
  internal var onModified: () -> Unit = {}

  val option: TaskViewsOption = TaskViewsOption(this)

  private var myActiveView: NamedTaskView = VOID_TASK_VIEW

  /**
   * The active view. [VOID_TASK_VIEW] means "show everything" -- that is the return path, and it is
   * always reachable, see [showAll].
   */
  var activeView: NamedTaskView
    get() = myActiveView
    set(value) {
      if (myActiveView === value) return
      myActiveView = value
      viewList.forEach { it.isEnabledProperty.value = (it === value) }
      viewListeners.forEach { it(value) }
      onModified()
      sync()
    }

  /**
   * The second condition next to the filter. A null child marks the end of a child list in
   * SyncAlgorithm's walk and must pass, exactly as VOID_FILTER_FXN does.
   */
  val viewFxn: TaskFilterFxn = { _, child -> child == null || !activeView.hides(child) }

  fun createView(title: String = ""): NamedTaskView = NamedTaskView(title)

  fun addView(view: NamedTaskView) {
    viewList.add(view)
    onModified()
  }

  fun removeView(view: NamedTaskView) {
    viewList.remove(view)
    if (activeView === view) {
      activeView = VOID_TASK_VIEW
    } else {
      onModified()
    }
  }

  /**
   * Takes over the list from the management dialog. Mirrors TaskFilterManager.importFilters: the
   * FIRST view whose check mark is set wins, and if none is set nothing is hidden.
   */
  fun importViews(items: List<NamedTaskView>) {
    viewList.clear()
    viewList.addAll(items)
    val enabled = items.find { it.isEnabledProperty.value }
    activeView = enabled ?: VOID_TASK_VIEW
    onModified()
    sync()
  }

  /**
   * THE RETURN PATH. Whatever a view hides, this brings it back, and it does not depend on the task
   * table being completely empty -- unlike the placeholder button, which only ever appears when
   * there is nothing left to see at all (TaskTable.kt:512).
   */
  fun showAll() {
    activeView = VOID_TASK_VIEW
  }

  fun isActive(view: NamedTaskView): Boolean = activeView === view

  /** How many tasks the active view hides directly. Children are not counted; they are not listed. */
  val hiddenByActiveView: Int get() = activeView.hiddenCount

  /**
   * Hides [tasks] in the active view. With no view active a new one is created and activated,
   * because "hide this" without a place to record it would be a click without an effect.
   */
  fun hideInActiveView(tasks: Collection<Task>, newViewTitle: () -> String) {
    if (tasks.isEmpty()) return
    if (activeView === VOID_TASK_VIEW) {
      val view = createView(newViewTitle())
      addView(view)
      tasks.forEach(view::hide)
      activeView = view
    } else {
      tasks.forEach(activeView::hide)
      onModified()
      sync()
    }
  }

  /** Brings [tasks] back in the active view. */
  fun showInActiveView(tasks: Collection<Task>) {
    if (tasks.isEmpty() || activeView === VOID_TASK_VIEW) return
    tasks.forEach(activeView::show)
    onModified()
    sync()
  }

  internal fun encode(): String? = encodeTaskViews(viewList)

  /**
   * Reads the views back from the project file. Assigns the backing field directly on purpose:
   * opening a file is not a user action, so it must not mark the fresh project dirty.
   */
  internal fun decode(value: String?) {
    val decoded = decodeTaskViews(value)
    viewList.clear()
    viewList.addAll(decoded)
    myActiveView = decoded.find { it.isEnabledProperty.value } ?: VOID_TASK_VIEW
    viewListeners.forEach { it(myActiveView) }
    sync()
  }
}

/**
 * The option that carries all views of a project.
 *
 * It implements ListOption for one reason only: OptionSaver.saveOption writes a ListOption into a
 * CDATA section and everything else into the `value` attribute (OptionSaver.java:61-66). Both
 * channels were measured and both carry "\n" and "\t" unharmed, but CDATA keeps the file readable
 * for a human, while the attribute channel turns every tab into `&#9;`.
 */
class TaskViewsOption(private val manager: TaskViewManager)
  : GPAbstractOption<String>(TASK_VIEWS_OPTION_ID), ListOption<String> {

  /** null means "write no <option> element at all" -- a project without views stays untouched. */
  override fun getPersistentValue(): String? = manager.encode()

  override fun loadPersistentValue(value: String?) {
    manager.decode(value)
  }

  // The rest of ListOption is not used: the views are edited through TaskViewManager, not through
  // the generic list option user interface. GPCloudStorageOptions does the same for
  // asEnumerationOption.
  override fun setValues(values: Iterable<String>) = throw UnsupportedOperationException()
  override fun getValues(): Iterable<String> = manager.views.map { it.title }
  override fun setValueIndex(idx: Int) = throw UnsupportedOperationException()
  override fun addValue(value: String) = throw UnsupportedOperationException()
  override fun updateValue(oldValue: String, newValue: String) = throw UnsupportedOperationException()
  override fun removeValueIndex(idx: Int) = throw UnsupportedOperationException()
  override fun asEnumerationOption(): EnumerationOption = throw UnsupportedOperationException()
}

// ---------------------------------------------------------------------------------------------
// The grammar. Kept as free functions so it can be tested without a project, a table or a screen.
// ---------------------------------------------------------------------------------------------

private const val RECORD_SEPARATOR = "\n"
private const val FIELD_SEPARATOR = "\t"
private const val UID_SEPARATOR = ","

/**
 * Encodes every view as one record `flag TAB name TAB uid,uid,uid`, records separated by "\n".
 *
 * @return null when there is no view at all, so that OptionSaver writes nothing.
 */
internal fun encodeTaskViews(views: List<NamedTaskView>): String? {
  if (views.isEmpty()) {
    return null
  }
  return buildString {
    views.forEach { view ->
      append(RECORD_SEPARATOR)
      append(if (view.isEnabledProperty.value) "1" else "0")
      append(FIELD_SEPARATOR)
      append(escapeViewField(view.title))
      append(FIELD_SEPARATOR)
      append(view.hiddenTaskUids.joinToString(UID_SEPARATOR))
    }
  }
}

/**
 * Reads back what [encodeTaskViews] wrote. Deliberately forgiving: a record that does not parse is
 * skipped with a log line rather than throwing, because a broken option must not cost the user the
 * whole project file. What it must NEVER do is guess -- a half-read view would hide tasks nobody
 * asked to hide.
 */
internal fun decodeTaskViews(value: String?): List<NamedTaskView> {
  if (value.isNullOrBlank()) {
    return emptyList()
  }
  val result = mutableListOf<NamedTaskView>()
  value.split(RECORD_SEPARATOR).forEach { record ->
    if (record.isBlank()) return@forEach
    val parts = record.split(FIELD_SEPARATOR)
    if (parts.size < 2) {
      LOGGER.error("Cannot read a saved task view from '{}'. It is skipped.", record)
      return@forEach
    }
    val uids = parts.getOrNull(2).orEmpty()
      .split(UID_SEPARATOR)
      .filter { it.isNotBlank() }
      .toCollection(linkedSetOf())
    result.add(NamedTaskView(
      title = unescapeViewField(parts[1]),
      hiddenTaskUids = uids,
      isEnabledProperty = SimpleBooleanProperty(parts[0] == "1")
    ))
  }
  return result
}

/**
 * Makes a view name safe for the grammar. The name is free user text, and a tab pasted into the
 * name field would otherwise split one view into two -- silently, which is the kind of mistake a
 * program gets away with for years. Backslash escapes were measured to pass through the CDATA
 * channel unchanged.
 */
internal fun escapeViewField(value: String): String = buildString {
  value.forEach { c ->
    when (c) {
      '\\' -> append("\\\\")
      '\t' -> append("\\t")
      '\n' -> append("\\n")
      // A raw carriage return does NOT survive: XML end-of-line normalisation turns "\r\n" into
      // "\n" on the way back in. Measured. So it is escaped like the others.
      '\r' -> append("\\r")
      else -> append(c)
    }
  }
}

internal fun unescapeViewField(value: String): String = buildString {
  var i = 0
  while (i < value.length) {
    val c = value[i]
    if (c == '\\' && i + 1 < value.length) {
      when (value[i + 1]) {
        '\\' -> { append('\\'); i += 2 }
        't' -> { append('\t'); i += 2 }
        'n' -> { append('\n'); i += 2 }
        'r' -> { append('\r'); i += 2 }
        else -> { append(c); i++ }
      }
    } else {
      append(c)
      i++
    }
  }
}

private val LOGGER = GPLogger.create("TaskTable.Views")
