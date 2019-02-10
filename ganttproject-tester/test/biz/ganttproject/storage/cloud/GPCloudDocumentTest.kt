/*
Copyright 2019 BarD Software s.r.o

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
package biz.ganttproject.storage.cloud

import biz.ganttproject.storage.OnlineDocumentMode
import biz.ganttproject.storage.checksum
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import com.google.common.util.concurrent.MoreExecutors
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import net.sourceforge.ganttproject.document.FileDocument
import org.easymock.EasyMock
import org.junit.Assert.assertArrayEquals
import java.io.File
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

data class TestResponse(override val body: ByteArray = byteArrayOf(),
                        override val code: Int = 200,
                        override val reason: String = "",
                        val headers: Map<String, String> = mapOf()) : GPCloudHttpClient.Response {
  override fun header(name: String): String? {
    return headers[name]
  }
}


/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudDocumentTest : TestCase() {
  private lateinit var testMirrorFolder: File
  private lateinit var mockHttpClient: GPCloudHttpClient

  override fun setUp() {
    super.setUp()
    testMirrorFolder = Files.createTempDir()
    mockHttpClient = EasyMock.createMock(GPCloudHttpClient::class.java)
  }

  override fun tearDown() {
    testMirrorFolder.deleteRecursively()
  }

  private fun prepareReadCall(doc: GPCloudDocument, body: ByteArray) {
    doc.httpClientFactory = { mockHttpClient }
    doc.offlineDocumentFactory = { path -> FileDocument(File(testMirrorFolder, path)) }
    doc.executor = MoreExecutors.newDirectExecutorService()


    val checksum = body.checksum()
    EasyMock.reset(mockHttpClient)
    EasyMock.expect(mockHttpClient.sendGet("/p/read?projectRefid=prj1")).andReturn(
        TestResponse(body = body, headers = mapOf(
            "ETag" to "100500",
            "Digest" to "crc32c=$checksum"
        ))
    )
    EasyMock.replay(mockHttpClient)
  }

  val BODY_239 = byteArrayOf(2, 3, 9)
  val BODY_566 = byteArrayOf(5, 6, 6)

  /**
   * The very simple test which checks that document is fetched and gets ONLINE mode.
   */
  fun testBasicOnlineDocumentRead() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)
    val fetch = runBlocking { doc.fetch() }
    assertEquals(100500, fetch.actualVersion)
    assertEquals(BODY_239.checksum(), fetch.actualChecksum)
    assertArrayEquals(BODY_239, fetch.body)
    //
    val readBody = ByteStreams.toByteArray(doc.inputStream)
    assertArrayEquals(BODY_239, readBody)
    assertEquals(OnlineDocumentMode.ONLINE_ONLY, doc.mode.value)
  }

  /**
   * Test that once we call setMirrored(true), which happens in the UI on button click, we get mirror document on disk.
   */
  fun testOnlineGoesMirrored() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)
    runBlocking { doc.fetch() }
    doc.setMirrored(true)

    assertNotNull(doc.offlineMirror)
    assertEquals(OnlineDocumentMode.MIRROR, doc.mode.value)
    assertArrayEquals(BODY_239, ByteStreams.toByteArray(doc.offlineMirror!!.inputStream))
    assertEquals(100500L, GPCloudOptions.cloudFiles.files[doc.projectIdFingerprint]!!.lastOnlineVersion?.toLong())
    assertEquals(BODY_239.checksum(), GPCloudOptions.cloudFiles.files[doc.projectIdFingerprint]!!.lastOnlineChecksum)
    assertEquals(BODY_239.checksum(), doc.offlineMirror!!.checksum())
  }

  /**
   * Test that write to document in MIRROR mode makes both HTTP call and local disk write.
   */
  fun testWriteMirrored() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)
    runBlocking { doc.fetch() }
    doc.setMirrored(true)

    EasyMock.reset(mockHttpClient)
    EasyMock.expect(mockHttpClient.sendPost("/p/write", mapOf(
        "projectRefid" to "prj1",
        "teamRefid" to "team1",
        "filename" to "Project 1",
        "fileContents" to Base64.getEncoder().encodeToString(BODY_566),
        "lockToken" to null,
        "oldVersion" to "100500"
    ))).andReturn(TestResponse(headers = mapOf(
        "ETag" to "146512",
        "Digest" to "crc32c=${BODY_566.checksum()}"
    )))
    EasyMock.replay(mockHttpClient)
    doc.outputStream.use {
      ByteStreams.copy(byteArrayOf(5, 6, 6).inputStream(), it)
    }

    assertEquals(OnlineDocumentMode.MIRROR, doc.mode.value)
    assertArrayEquals(BODY_566, ByteStreams.toByteArray(doc.offlineMirror!!.inputStream))
    assertEquals(146512L, GPCloudOptions.cloudFiles.files[doc.projectIdFingerprint]!!.lastOnlineVersion?.toLong())
    assertEquals(BODY_566.checksum(), GPCloudOptions.cloudFiles.files[doc.projectIdFingerprint]!!.lastOnlineChecksum)
    assertEquals(BODY_566.checksum(), doc.offlineMirror!!.checksum())
  }

  /**
   * Test that mirror document is deleted when mode is switched to ONLINE_ONLY
   */
  fun testMirroredGoesOnline() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)
    runBlocking { doc.fetch() }
    doc.setMirrored(true)

    assertNotNull(doc.offlineMirror)
    val file = File(doc.offlineMirror!!.filePath)
    assertTrue(file.exists())
    assertEquals(OnlineDocumentMode.MIRROR, doc.mode.value)

    doc.setMirrored(false)
    assertNull(doc.offlineMirror)
    assertEquals(OnlineDocumentMode.ONLINE_ONLY, doc.mode.value)
    assertFalse(file.exists())
    assertNull(GPCloudOptions.cloudFiles.files[doc.projectIdFingerprint]!!.lastOnlineChecksum)
    assertNull(GPCloudOptions.cloudFiles.files[doc.projectIdFingerprint]!!.lastOnlineVersion)
  }

  /**
   * Test that if we have records in the options which indicate mirror document location, we get MIRROR mode
   * automatically after opening online document.
   */
  fun testMirroredAutomaticallyWhenHasOptions() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)

    val mirrorFile = File(testMirrorFolder, doc.projectIdFingerprint)
    mirrorFile.writeBytes(BODY_239)
    GPCloudOptions.cloudFiles.getFileOptions(doc.projectIdFingerprint).let {
      it.offlineMirror = mirrorFile.absolutePath
    }
    runBlocking { doc.fetch() }
    ByteStreams.toByteArray(doc.inputStream)
    assertEquals(OnlineDocumentMode.MIRROR, doc.mode.value)
    assertTrue(mirrorFile.exists())
  }

  private fun assertGoesOffline(doc: GPCloudDocument, version: Long? = null) {
    EasyMock.reset(mockHttpClient)
    EasyMock.expect(mockHttpClient.sendPost("/p/write", mapOf(
        "projectRefid" to "prj1",
        "teamRefid" to "team1",
        "filename" to "Project 1",
        "fileContents" to Base64.getEncoder().encodeToString(BODY_566),
        "lockToken" to null,
        "oldVersion" to version?.toString()
    ))).andThrow(UnknownHostException())
    EasyMock.replay(mockHttpClient)

    doc.outputStream.use {
      ByteStreams.copy(BODY_566.inputStream(), it)
    }
    assertEquals(OnlineDocumentMode.OFFLINE_ONLY, doc.mode.value)
  }

  /**
   * Test that we go into OFFLINE_ONLY mode automatically once we receive an indicator of offline, such as
   * UnknownHostException
   */
  fun testOnlineGoesOfflineOnWrite() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)
    val fetch = runBlocking { doc.fetch() }
    assertGoesOffline(doc, fetch.actualVersion)

    val doc1 = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc1, BODY_239)
    val fetch1 = runBlocking { doc1.fetch() }
    doc1.setMirrored(true)
    assertGoesOffline(doc1, fetch1.actualVersion)
    assertArrayEquals(BODY_566, ByteStreams.toByteArray(doc1.offlineMirror!!.inputStream))
  }

  /**
   * Test that when writing in OFFLINE_ONLY mode we only touch disk
   */
  fun testWriteOffline() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)
    val fetch = runBlocking { doc.fetch() }
    ByteStreams.toByteArray(doc.inputStream)
    doc.setMirrored(true)

    doc.isNetworkAvailable = Callable { false }
    doc.executor = Executors.newSingleThreadExecutor()
    assertGoesOffline(doc, fetch.actualVersion)

    doc.outputStream.use {
      ByteStreams.copy("Lorem ipsum".toByteArray().inputStream(), it)
    }

    assertEquals("Lorem ipsum", doc.offlineMirror!!.inputStream.bufferedReader().readText())
  }

  /**
   * Test that we reconnect automatically in a while after going OFFLINE_ONLY
   */
  fun testOfflineGoesOnlineOnReconnect() {
    var retryCounter = CountDownLatch(2)
    val executor = Executors.newSingleThreadExecutor()
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    doc.httpClientFactory = { mockHttpClient }
    doc.offlineDocumentFactory = { path -> FileDocument(File(testMirrorFolder, path)) }
    doc.executor = executor
    doc.isNetworkAvailable = Callable {
      if (retryCounter.count > 0) {
        retryCounter.countDown()
      }
      retryCounter.count == 0L
    }

    assertGoesOffline(doc, null)

    EasyMock.reset(mockHttpClient)
    EasyMock.expect(mockHttpClient.sendPost("/p/write", mapOf(
        "projectRefid" to "prj1",
        "teamRefid" to "team1",
        "filename" to "Project 1",
        "fileContents" to Base64.getEncoder().encodeToString(BODY_566),
        "lockToken" to null,
        "oldVersion" to null
    ))).andReturn(TestResponse(headers = mapOf(
        "ETag" to "146512",
        "Digest" to "crc32c=${BODY_566.checksum()}"
    )))
    EasyMock.replay(mockHttpClient)

    retryCounter.await()
    Thread.sleep(1000)
    assertEquals(OnlineDocumentMode.MIRROR, doc.mode.value)
  }
}
