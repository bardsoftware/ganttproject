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
package net.sourceforge.ganttproject.io

import biz.ganttproject.core.io.parseXmlProject
import biz.ganttproject.core.option.GPOption
import biz.ganttproject.ganttview.TASK_VIEWS_OPTION_ID
import biz.ganttproject.ganttview.NamedTaskView
import biz.ganttproject.ganttview.TaskViewManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.xml.sax.helpers.AttributesImpl
import java.io.ByteArrayOutputStream
import javax.xml.transform.stream.StreamResult

/**
 * The channel the views travel through, exercised end to end without a screen and without a
 * running program: the real OptionSaver writes, the real Jackson reader reads back.
 *
 * WHY THIS FILE IS THE FIRST THING THAT WAS BUILT. The second measurement named exactly one open
 * technical question and said it had to be cleared up FIRST, not last: has a MULTI-LINE value ever
 * travelled through `<view><option>` before? It has not -- in all 17 real `.gan` files the only
 * CDATA option value is seven characters long and fits on one line. Everything else in the design
 * rests on the answer.
 */
class TaskViewsFileTest : SaverBase() {

  /**
   * Builds the same document fragment that ViewSaver builds: the options of the Gantt view inside
   * `<view id="gantt-chart">`.
   */
  private fun writeView(vararg options: GPOption<*>): String {
    val out = ByteArrayOutputStream()
    val handler = createHandler(StreamResult(out))
    handler.startDocument()
    startElement("project", handler)
    val attrs = AttributesImpl()
    addAttribute("id", "gantt-chart", attrs)
    startElement("view", attrs, handler)
    OptionSaver().saveOptionList(handler, *options)
    endElement("view", handler)
    endElement("project", handler)
    handler.endDocument()
    return out.toString(Charsets.UTF_8)
  }

  /**
   * Reads options back exactly the way TaskSerializer.loadGanttView does (:243-247): the list of
   * known options is built BEFORE the file is read, and an unknown id is dropped without a word.
   */
  private fun readView(xml: String, into: List<GPOption<*>>) {
    val xmlProject = parseXmlProject(xml)
    val xmlView = xmlProject.views.first { it.id == "gantt-chart" }
    xmlView.options?.forEach { xmlOption ->
      into.firstOrNull { it.id == xmlOption.id }?.loadPersistentValue(xmlOption.text ?: xmlOption.value)
    }
  }

  /**
   * RED against the state before the build:
   *   Unresolved reference: TaskViewManager -- the class did not exist. Counted separately in the
   *   report as a check that could not be red by an assertion.
   */
  @Test
  fun `three named views reach the file and come back unchanged`() {
    val writer = TaskViewManager()
    writer.addView(NamedTaskView("Rohbau", linkedSetOf("aaaaaaaa1111", "bbbbbbbb2222")))
    writer.addView(NamedTaskView("Ausbau", linkedSetOf("cccccccc3333")))
    writer.addView(NamedTaskView("Nur Anlagentechnik", linkedSetOf("aaaaaaaa1111", "dddddddd4444")))
    writer.activeView = writer.views[2]

    val xml = writeView(writer.option)

    // The value travels in a CDATA section, not in an attribute. That keeps the file readable for
    // a person; the attribute channel would turn every tab into "&#9;".
    assertTrue(xml.contains("<![CDATA["), "the views did not go into a CDATA section:\n$xml")
    assertTrue(xml.contains("Nur Anlagentechnik"), "the view name is not in the file:\n$xml")

    val reader = TaskViewManager()
    readView(xml, listOf(reader.option))

    assertEquals(3, reader.views.size, "not all views came back, the file was:\n$xml")
    assertEquals(listOf("Rohbau", "Ausbau", "Nur Anlagentechnik"), reader.views.map { it.title })
    assertEquals(setOf("aaaaaaaa1111", "bbbbbbbb2222"), reader.views[0].hiddenTaskUids)
    assertEquals("Nur Anlagentechnik", reader.activeView.title,
      "which view was active did not survive the file")
    // A task may be hidden in two views at once -- that is what a single group value at the task
    // could never do.
    assertTrue(reader.views[2].hiddenTaskUids.contains("aaaaaaaa1111"))
  }

  /**
   * The one question the second measurement could not answer without a build: does a multi-line
   * CDATA survive the SAX writer and the Jackson reader?
   *
   * Could NOT be red against the state before the build: it measures behaviour of the existing
   * writer and reader, which this package does not change.
   */
  @Test
  fun `a multi-line value with tabs survives the option channel unchanged`() {
    val payload = "\n1\tRohbau\tuid1,uid2\n0\tNur Anlagentechnik\tuid3"
    val option = StringListOption("probe", payload)

    val xml = writeView(option)
    val back = StringListOption("probe", null)
    readView(xml, listOf(back))

    assertEquals(payload, back.stored,
      "a newline or a tab was lost on the way through the file:\n$xml")
  }

  /**
   * BACKWARDS COMPATIBILITY, the reading half. A GanttProject that does not know `taskViews` walks
   * the same three lines of code, finds no counterpart in its option list and drops the element --
   * no exception, no message, no damage to the rest of the file.
   *
   * Could NOT be red against the state before the build: it measures the existing reader.
   *
   * What this does NOT show is the writing half: the older version drops the option on the next
   * save, because ViewSaver only writes what its own three sources deliver (ViewSaver.java:52-59).
   * That is stated in the report, not tested here -- it would need the older program.
   */
  @Test
  fun `an option id the reader does not know is dropped without an error`() {
    val writer = TaskViewManager()
    writer.addView(NamedTaskView("Rohbau", linkedSetOf("aaaaaaaa1111")))
    val xml = writeView(writer.option)

    // The option list of a GanttProject without this change: the divider and the four filters,
    // no taskViews.
    val stockOptions = listOf(StringListOption("divider", null), StringListOption("filter.completedTasks", null))
    readView(xml, stockOptions)

    assertTrue(stockOptions.all { it.stored == null },
      "an unknown option leaked into a known one")
    // And the rest of the document is still readable.
    val reparsed = parseXmlProject(xml)
    assertEquals(1, reparsed.views.size)
    assertNotNull(reparsed.views[0].options)
  }

  /**
   * The views option must never carry a control character. Measured: U+001F in the CDATA makes the
   * reader throw `Illegal character entity: expansion character (code 0x1f)` and the whole project
   * file becomes unreadable. Tab and newline are the only two that are allowed, and they are the
   * two the grammar uses.
   */
  @Test
  fun `the encoded value carries no forbidden control character`() {
    val writer = TaskViewManager()
    writer.addView(NamedTaskView("Erste Ansicht", linkedSetOf("uid1")))
    writer.addView(NamedTaskView("Zweite Ansicht", linkedSetOf("uid2")))

    val encoded: String? = writer.option.persistentValue
    assertNotNull(encoded)
    // Tab and newline are the two the grammar uses; every other control character is fatal.
    val allowedCodes = setOf(9, 10)
    val forbidden = encoded!!.filter { it.code < 0x20 && !allowedCodes.contains(it.code) }
    assertTrue(forbidden.isEmpty(),
      "the value carries the control characters ${forbidden.map { "U+%04X".format(it.code) }} - " +
      "such a file cannot be read back. The whole value was " +
      "${encoded.map { "U+%04X".format(it.code) }}")

    // And it really does survive the file.
    val reader = TaskViewManager()
    readView(writeView(writer.option), listOf(reader.option))
    assertEquals(2, reader.views.size)
  }

  /**
   * Writes the `<view>` body in the order the saver produces: first the options, then `<filters>`.
   * `color.recent` belongs in the option run and no longer after the filters.
   *
   * WHY THIS TEST EXISTS. The second measurement listed as unchecked whether Jackson still collects
   * `<option>` elements that are separated by `<filters>`, and guessed that it apparently does.
   * IT DOES NOT -- see the test below. That is why the saver was changed.
   *
   * RED against the state before the build:
   *   the views option stood before <filters> and was lost
   *   expected: <1> but was: <0>
   */
  @Test
  fun `in the order the saver writes today no option is lost`() {
    val xml = writeViewBody(recentColorsAfterFilters = false)

    val views = TaskViewManager()
    val colors = StringListOption("color.recent", null)
    readView(xml, listOf(views.option, colors))

    assertEquals(1, views.views.size, "the views option was lost:\n$xml")
    assertEquals("#ff0066", colors.stored, "the recent colours were lost:\n$xml")
    assertFalse(views.views[0].title.contains("filter"), "the two options ran into each other")
  }

  /**
   * The measurement behind the change to the saver, kept as a check so that nobody puts the old
   * order back: the Jackson reader keeps only the LAST uninterrupted run of `<option>` elements.
   * Everything before an element of another kind is dropped WITHOUT AN ERROR.
   *
   * Measured on a real file as well: `HouseBuildingSample.gan` carries six `<option>` elements, and
   * a `<timeline>` stands between them; exactly one is read back.
   *
   * Could NOT be red against the state before the build: it measures the behaviour of the existing
   * reader, which this package does not change.
   */
  @Test
  fun `the reader drops every option that stands before another element`() {
    val xml = writeViewBody(recentColorsAfterFilters = true)

    val views = TaskViewManager()
    val colors = StringListOption("color.recent", null)
    readView(xml, listOf(views.option, colors))

    assertEquals(0, views.views.size,
      "the reader suddenly keeps options before <filters> - then the change to ViewSaver is " +
      "pointless and can be reverted:\n$xml")
    assertEquals("#ff0066", colors.stored, "the last run must survive:\n$xml")
  }

  /** The body of `<view id="gantt-chart">`, once in each of the two orders. */
  private fun writeViewBody(recentColorsAfterFilters: Boolean): String {
    val out = ByteArrayOutputStream()
    val handler = createHandler(StreamResult(out))
    handler.startDocument()
    startElement("project", handler)
    val attrs = AttributesImpl()
    addAttribute("id", "gantt-chart", attrs)
    startElement("view", attrs, handler)
    OptionSaver().saveOptionList(handler, StringListOption("taskViews", "\n1\tRohbau\tuid1"))
    val recentColors = StringListOption("color.recent", "#ff0066")
    if (!recentColorsAfterFilters) {
      OptionSaver().saveOptionList(handler, recentColors)
    }
    startElement("filters", AttributesImpl(), handler)
    val filterAttrs = AttributesImpl()
    addAttribute("title", "filter.completedTasks", filterAttrs)
    emptyElement("filter", filterAttrs, handler)
    endElement("filters", handler)
    if (recentColorsAfterFilters) {
      OptionSaver().saveOptionList(handler, recentColors)
    }
    endElement("view", handler)
    endElement("project", handler)
    handler.endDocument()
    return out.toString(Charsets.UTF_8)
  }

  /**
   * A ListOption whose only job is to hold a persistent value. OptionSaver writes a ListOption into
   * a CDATA section and everything else into the `value` attribute (OptionSaver.java:61-66) -- so
   * the interface, not the content, decides the channel.
   */
  private class StringListOption(id: String, var stored: String?)
    : biz.ganttproject.core.option.GPAbstractOption<String>(id),
      biz.ganttproject.core.option.ListOption<String> {
    override fun getPersistentValue(): String? = stored
    override fun loadPersistentValue(value: String?) { stored = value }
    override fun setValues(values: MutableIterable<String>) = Unit
    override fun getValues(): MutableIterable<String> = mutableListOf()
    override fun setValueIndex(idx: Int) = Unit
    override fun addValue(value: String) = Unit
    override fun updateValue(oldValue: String, newValue: String) = Unit
    override fun removeValueIndex(idx: Int) = Unit
    override fun asEnumerationOption() = throw UnsupportedOperationException()
  }
}
