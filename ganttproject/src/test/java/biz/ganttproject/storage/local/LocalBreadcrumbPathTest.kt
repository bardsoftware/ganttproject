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
package biz.ganttproject.storage.local

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths

/**
 * Tests the translation of the path and the name of the currently opened document into the initial state of the
 * local storage pane. The document may come from a WebDAV server or from GanttProject Cloud, and in that case
 * neither its path nor its name is a local file path.
 */
class LocalBreadcrumbPathTest {
  private val defaultFolder = File(System.getProperty("java.io.tmpdir"), "local-breadcrumb-path-test")

  @Test
  fun `an absolute local path is used as is`() {
    val localPath = File(defaultFolder, "plan.gan").absolutePath
    assertEquals(Paths.get(localPath), localBreadcrumbPath(localPath, "plan.gan", defaultFolder))
  }

  @Test
  fun `a webdav url is not used as a path`() {
    assertEquals(
      defaultFolder.toPath().resolve("plan.gan"),
      localBreadcrumbPath(
        "https://dav.example.com/projects/plan.gan", "https://dav.example.com/projects/plan.gan", defaultFolder
      )
    )
  }

  @Test
  fun `a relative path is not used`() {
    assertEquals(defaultFolder.toPath().resolve("plan.gan"), localBreadcrumbPath("plan.gan", "plan.gan", defaultFolder))
  }

  @Test
  fun `a missing path falls back to the default folder`() {
    assertEquals(defaultFolder.toPath().resolve(DEFAULT_LOCAL_FILE_NAME), localBreadcrumbPath(null, null, defaultFolder))
  }

  @Test
  fun `a path which the file system cannot parse falls back to the default folder`() {
    val unparseable = "/tmp/plan" + 0.toChar() + ".gan"
    assertEquals(defaultFolder.toPath().resolve("plan.gan"), localBreadcrumbPath(unparseable, "plan.gan", defaultFolder))
  }

  @Test
  fun `the name of a webdav document is the last component of its url`() {
    assertEquals("plan.gan", localFileName("https://dav.example.com/projects/plan.gan"))
  }

  @Test
  fun `the name of a local document is left alone`() {
    assertEquals("plan.gan", localFileName("plan.gan"))
  }

  @Test
  fun `a url without a file name falls back to the default name`() {
    assertEquals(DEFAULT_LOCAL_FILE_NAME, localFileName("https://dav.example.com/projects/"))
  }
}
