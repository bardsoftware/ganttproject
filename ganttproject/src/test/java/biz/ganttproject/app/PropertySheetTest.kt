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

import biz.ganttproject.core.option.ObservableDate
import biz.ganttproject.core.option.ObservableString
import javafx.scene.control.TextField
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PropertySheetTest {
  private val coroutineScope = CoroutineScope(SupervisorJob())

  @Test fun `basic string property`() {
    coroutineScope.launch {
      try {
        withContext(Dispatchers.JavaFx) {
          val propertyPaneBuilder = PropertyPaneBuilder(DummyLocalizer, PropertyPane())
          val textProperty = ObservableString("prop1", "Lorem Ipsum")
          val editor = propertyPaneBuilder.createStringOptionEditor(textProperty)
          (editor as? TextField)?.let {
            it.text = "Hello"
            assert(textProperty.value == "Hello")

            textProperty.value = "Hello2"
            assert(it.text == "Hello2")
          }
        }
      } catch (ex: Exception) {
        ex.printStackTrace()
        this.cancel()
      }
    }
  }

  @Test fun `date property considers formatting settings`() {
    coroutineScope.launch {
      withContext(Dispatchers.JavaFx) {
        val propertyPaneBuilder = PropertyPaneBuilder(DummyLocalizer, PropertyPane())
        val dateProperty = ObservableDate("prop1", LocalDate.now())
        val editor = propertyPaneBuilder.createDateOptionEditor(dateProperty, DateDisplayOptions().apply {
          stringConverter = FormatterBasedDateConverter(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        })
        dateProperty.value = LocalDate.of(2025, 9, 22)
        assertEquals("22.09.2025", editor.editor.text)

        editor.editor.text = "23.09.2025"
        assertEquals(LocalDate.of(2025, 9, 23), dateProperty.value)

        editor.value = LocalDate.of(2025, 9, 24)
        assertEquals("24.09.2025", editor.editor.text)
        assertEquals(LocalDate.of(2025, 9, 24), dateProperty.value)
      }
    }
  }

}
