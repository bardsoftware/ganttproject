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

import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
import net.sourceforge.ganttproject.document.webdav.HttpDocument
import net.sourceforge.ganttproject.document.webdav.WebDavResource
import org.easymock.EasyMock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.File

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
        currentDocument = webdavDocument("https://other.example.com/dav/plan.gan"),
        webdavRootUrls = webdavRootUrls,
        recentProjectsId = RECENT_ID,
        localStorageId = LOCAL_ID
      )
    )
  }

  /**
   * A space is legal in a WebDAV resource name, and the Milton client hands it over decoded, so the URL which
   * HttpDocument holds contains a raw space. java.net.URI rejects it, and the preselection used to go through
   * java.net.URI.
   */
  @Test
  fun `save preselects the webdav server when the document name contains a space`() {
    val document = webdavDocument("https://dav.example.com/projects/plan neu.gan")
    assertNull(document.uri, "precondition: HttpDocument cannot build a URI from a name with a space")
    assertEquals(
      "https://dav.example.com/projects",
      initialStorageId(null, StorageDialogBuilder.Mode.SAVE, document, webdavRootUrls, RECENT_ID, LOCAL_ID)
    )
  }

  @Test
  fun `save preselects the webdav server when the document name is percent-encoded`() {
    assertEquals(
      "https://dav.example.com/projects",
      initialStorageId(
        null, StorageDialogBuilder.Mode.SAVE, webdavDocument("https://dav.example.com/projects/plan%20neu.gan"),
        webdavRootUrls, RECENT_ID, LOCAL_ID
      )
    )
  }

  @Test
  fun `save preselects the webdav server when the document name contains a non-ascii letter`() {
    assertEquals(
      "https://dav.example.com/projects",
      initialStorageId(
        null, StorageDialogBuilder.Mode.SAVE, webdavDocument("https://dav.example.com/projects/plän.gan"),
        webdavRootUrls, RECENT_ID, LOCAL_ID
      )
    )
  }

  @Test
  fun `save preselects the local storage for a local document`() {
    assertEquals(
      LOCAL_ID,
      initialStorageId(
        null, StorageDialogBuilder.Mode.SAVE, FileDocument(File("/home/joe/plan.gan")),
        webdavRootUrls, RECENT_ID, LOCAL_ID
      )
    )
  }

  @Test
  fun `save preselects the local storage for a local document whose name contains a space`() {
    assertEquals(
      LOCAL_ID,
      initialStorageId(
        null, StorageDialogBuilder.Mode.SAVE, FileDocument(File("/home/joe/plan neu.gan")),
        webdavRootUrls, RECENT_ID, LOCAL_ID
      )
    )
  }

  @Test
  fun `save preselects the local storage when there is no current document`() {
    assertEquals(
      LOCAL_ID,
      initialStorageId(null, StorageDialogBuilder.Mode.SAVE, null, webdavRootUrls, RECENT_ID, LOCAL_ID)
    )
  }

  @Test
  fun `save preselects the local storage when the document has no location`() {
    assertEquals(
      LOCAL_ID,
      initialStorageId(null, StorageDialogBuilder.Mode.SAVE, webdavDocument(""), webdavRootUrls, RECENT_ID, LOCAL_ID)
    )
  }

  @Test
  fun `a webdav server with a blank root url matches no document`() {
    assertEquals(
      LOCAL_ID,
      initialStorageId(
        null, StorageDialogBuilder.Mode.SAVE, FileDocument(File("/home/joe/plan.gan")),
        listOf(""), RECENT_ID, LOCAL_ID
      )
    )
  }

  @Test
  fun `open preselects the recent projects no matter where the document lives`() {
    assertEquals(
      RECENT_ID,
      initialStorageId(
        null, StorageDialogBuilder.Mode.OPEN, webdavDocument("https://dav.example.com/projects/plan.gan"),
        webdavRootUrls, RECENT_ID, LOCAL_ID
      )
    )
  }

  @Test
  fun `an explicitly selected storage wins over the document location`() {
    assertEquals(
      "cloud",
      initialStorageId(
        "cloud", StorageDialogBuilder.Mode.SAVE, webdavDocument("https://dav.example.com/projects/plan.gan"),
        webdavRootUrls, RECENT_ID, LOCAL_ID
      )
    )
  }

  @Test
  fun `the storage url of a webdav document is its resource url, space or no space`() {
    assertEquals(
      "https://dav.example.com/projects/plan.gan",
      documentStorageUrl(webdavDocument("https://dav.example.com/projects/plan.gan"))
    )
    assertEquals(
      "https://dav.example.com/projects/plan neu.gan",
      documentStorageUrl(webdavDocument("https://dav.example.com/projects/plan neu.gan"))
    )
  }

  @Test
  fun `a document without a location has no storage url`() {
    assertNull(documentStorageUrl(null))
    assertNull(documentStorageUrl(webdavDocument("")))
  }

  /**
   * A WebDAV document which only knows its URL, with no server behind it. The constructor of HttpDocument which
   * takes a resource issues no requests, and neither does WebDavResource.getUrl().
   */
  private fun webdavDocument(url: String): Document {
    val resource: WebDavResource = EasyMock.createNiceMock<WebDavResource>(WebDavResource::class.java)
    EasyMock.expect(resource.url).andReturn(url).anyTimes()
    EasyMock.replay(resource)
    return HttpDocument(resource, "joe", "secret", HttpDocument.NO_LOCK)
  }
}

private const val RECENT_ID = "recent"
private const val LOCAL_ID = "desktop"
