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
package biz.ganttproject.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests the choice of the storage which is initially selected in the storage dialog.
 */
class InitialStorageIdTest {
  private val webdavRootUrls = listOf("https://dav.example.com/projects", "https://other.example.com/dav")

  @Test
  fun `save preselects the webdav server which holds the current document`() {
    assertEquals(
      "https://other.example.com/dav",
      initialStorageId(
        selectedId = null,
        mode = StorageDialogBuilder.Mode.SAVE,
        documentUrl = "https://other.example.com/dav/plan.gan",
        webdavRootUrls = webdavRootUrls,
        recentProjectsId = RECENT_ID,
        localStorageId = LOCAL_ID
      )
    )
  }

  @Test
  fun `save preselects the local storage for a local document`() {
    assertEquals(
      LOCAL_ID,
      initialStorageId(null, StorageDialogBuilder.Mode.SAVE, "file:/home/joe/plan.gan", webdavRootUrls, RECENT_ID, LOCAL_ID)
    )
  }

  @Test
  fun `save preselects the local storage when the document url is unknown`() {
    assertEquals(
      LOCAL_ID,
      initialStorageId(null, StorageDialogBuilder.Mode.SAVE, null, webdavRootUrls, RECENT_ID, LOCAL_ID)
    )
  }

  @Test
  fun `a webdav server with a blank root url matches no document`() {
    assertEquals(
      LOCAL_ID,
      initialStorageId(null, StorageDialogBuilder.Mode.SAVE, "file:/home/joe/plan.gan", listOf(""), RECENT_ID, LOCAL_ID)
    )
  }

  @Test
  fun `open preselects the recent projects no matter where the document lives`() {
    assertEquals(
      RECENT_ID,
      initialStorageId(null, StorageDialogBuilder.Mode.OPEN, "https://dav.example.com/projects/plan.gan", webdavRootUrls, RECENT_ID, LOCAL_ID)
    )
  }

  @Test
  fun `an explicitly selected storage wins over the document location`() {
    assertEquals(
      "cloud",
      initialStorageId("cloud", StorageDialogBuilder.Mode.SAVE, "https://dav.example.com/projects/plan.gan", webdavRootUrls, RECENT_ID, LOCAL_ID)
    )
  }
}

private const val RECENT_ID = "recent"
private const val LOCAL_ID = "desktop"
