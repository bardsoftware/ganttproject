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
import javafx.scene.control.CheckBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BooleanOptionEditorTest {
  private fun <T> onFxThread(code: () -> T): T = runBlocking { withContext(Dispatchers.JavaFx) { code() } }

  @Test fun `checkbox shows the initial value of the option`() {
    onFxThread {
      val builder = PropertyPaneBuilderImpl(DummyLocalizer, PropertyPane())
      val checked = builder.createBooleanOptionEditor(ObservableBoolean("checked", true)) as CheckBox
      val unchecked = builder.createBooleanOptionEditor(ObservableBoolean("unchecked", false)) as CheckBox
      assertEquals(true, checked.isSelected)
      assertEquals(false, unchecked.isSelected)
    }
  }

  @Test fun `initial state does not change the option and later changes are propagated`() {
    onFxThread {
      val builder = PropertyPaneBuilderImpl(DummyLocalizer, PropertyPane())
      val option = ObservableBoolean("option", true)
      var changeCount = 0
      option.addWatcher { changeCount++ }
      val checkBox = builder.createBooleanOptionEditor(option) as CheckBox
      assertEquals(0, changeCount)

      option.value = false
      assertEquals(false, checkBox.isSelected)

      checkBox.fire()
      assertEquals(true, option.value)
    }
  }
}
