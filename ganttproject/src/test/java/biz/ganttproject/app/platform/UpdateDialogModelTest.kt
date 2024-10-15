/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.app.platform

import biz.ganttproject.app.DummyLocalizer
import biz.ganttproject.platform.ApplyAction
import biz.ganttproject.platform.UpdateDialogLocalizationKeys.BUTTON_OK
import biz.ganttproject.platform.UpdateDialogLocalizationKeys.INSTALL_FROM_CHANNEL
import biz.ganttproject.platform.UpdateDialogLocalizationKeys.INSTALL_FROM_ZIP
import biz.ganttproject.platform.UpdateDialogLocalizationKeys.MAJOR_UPDATE_DOWNLOAD
import biz.ganttproject.platform.UpdateDialogModel
import com.bardsoftware.eclipsito.update.UpdateMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UpdateDialogModelTest {
  @Test
  fun `initial state with major update`() {
    val updates = listOf(
      UpdateMetadata("3.4.3401", "http://example.com", "minor update", "2024-12-01", 1024, "", false),
      UpdateMetadata("3.5.3500", "http://example.com", "major update", "2024-12-08", 1024, "", true)
    )
    val model = UpdateDialogModel(updates, updates, installedVersion = "3.3.3400", restarter = {})
    model.localizer = DummyLocalizer
    model.initState()
    assertEquals(ApplyAction.DOWNLOAD_MAJOR, model.state)
    assertEquals(MAJOR_UPDATE_DOWNLOAD, model.btnApplyText.value)
  }

  @Test
  fun `initial state with minor update`() {
    val updates = listOf(
      UpdateMetadata("3.4.3401", "http://example.com", "minor update", "2024-12-01", 1024, "", false),
      UpdateMetadata("3.4.3402", "http://example.com", "major update", "2024-12-08", 1024, "", false)
    )
    val model = UpdateDialogModel(updates, updates, installedVersion = "3.3.3400", restarter = {})
    model.localizer = DummyLocalizer
    model.initState()
    assertEquals(ApplyAction.INSTALL_FROM_CHANNEL, model.state)
    assertEquals(BUTTON_OK, model.btnApplyText.value)
  }

  @Test
  fun `switch between channels`() {
    val updates = listOf(
      UpdateMetadata("3.4.3401", "http://example.com", "minor update", "2024-12-01", 1024, "", false),
      UpdateMetadata("3.5.3500", "http://example.com", "major update", "2024-12-08", 1024, "", true)
    )
    val model = UpdateDialogModel(updates, updates, installedVersion = "3.3.3400", restarter = {})
    model.localizer = DummyLocalizer
    model.initState()

    model.state = ApplyAction.INSTALL_FROM_CHANNEL
    assertEquals(BUTTON_OK, model.btnApplyText.value)
    assertEquals(INSTALL_FROM_ZIP, model.btnToggleSourceText.value)
    model.state = ApplyAction.INSTALL_FROM_ZIP
    assertEquals(BUTTON_OK, model.btnApplyText.value)
    assertEquals(INSTALL_FROM_CHANNEL, model.btnToggleSourceText.value)
  }


}