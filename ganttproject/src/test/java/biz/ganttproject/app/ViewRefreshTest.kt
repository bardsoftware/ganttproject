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
package biz.ganttproject.app

import javafx.application.Platform
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.sourceforge.ganttproject.chart.Chart
import net.sourceforge.ganttproject.chart.ChartSelection
import org.easymock.EasyMock.createNiceMock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

class ViewRefreshTest {
  @Test
  fun `refresh calls queued while the JavaFX thread is busy lay the view out once`() {
    var refreshCount = 0
    val view = runBlocking {
      withContext(Dispatchers.JavaFx) {
        ViewImpl(TabPane(), Tab(), createNiceMock(ChartSelection::class.java), createNiceMock(Chart::class.java),
          null, null, null) { refreshCount++ }
      }
    }
    val busy = CountDownLatch(1)
    val release = CountDownLatch(1)
    Platform.runLater {
      busy.countDown()
      release.await()
    }
    busy.await()
    // A weekly recurrence until February adds the group and 13 repeated tasks, each one asks for a refresh.
    repeat(14) { view.refresh() }
    release.countDown()
    runBlocking {
      withContext(Dispatchers.JavaFx) {}
    }
    assertEquals(1, refreshCount, "the refreshes which queued up must be served by one layout")
  }
}
