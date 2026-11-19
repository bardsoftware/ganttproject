/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

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
package biz.ganttproject.app

import biz.ganttproject.core.option.ObservableBoolean
import biz.ganttproject.core.option.ObservableEnum
import biz.ganttproject.core.option.ObservableInt
import biz.ganttproject.core.option.ObservableProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReadOnlyOptionEditorTest {
  private enum class Color { RED, GREEN }

  private fun <T> onFxThread(code: () -> T): T = runBlocking { withContext(Dispatchers.JavaFx) { code() } }

  private fun PropertyPaneBuilderImpl.editors() = rowBuilders.filterIsInstance<PropertyPaneBuilderImpl.OptionRowBuilder>().map { it.editor }

  private fun editorDisabled(property: ObservableProperty<*>, add: PropertyPaneBuilderImpl.() -> Unit): Boolean {
    val builder = PropertyPaneBuilderImpl(DummyLocalizer, PropertyPane())
    builder.add()
    return builder.editors().single().isDisable
  }

  @Test fun `editors of read-only options are disabled from the start`() {
    onFxThread {
      val checkbox = ObservableBoolean("checkbox", true).also { it.setWritable(false) }
      val number = ObservableInt("number", 1).also { it.setWritable(false) }
      val dropdown = ObservableEnum("dropdown", Color.RED, Color.values()).also { it.setWritable(false) }
      assertEquals(true, editorDisabled(checkbox) { checkbox(checkbox) }, "checkbox")
      assertEquals(true, editorDisabled(number) { numeric(number) }, "int")
      assertEquals(true, editorDisabled(dropdown) { dropdown(dropdown) }, "dropdown")
    }
  }

  @Test fun `editors of writable options are enabled and later changes work in both directions`() {
    onFxThread {
      val option = ObservableBoolean("option", true)
      val builder = PropertyPaneBuilderImpl(DummyLocalizer, PropertyPane())
      builder.checkbox(option)
      val editor = builder.editors().single()
      assertEquals(false, editor.isDisable)

      option.setWritable(false)
      assertEquals(true, editor.isDisable)

      option.setWritable(true)
      assertEquals(false, editor.isDisable)
    }
  }
}
