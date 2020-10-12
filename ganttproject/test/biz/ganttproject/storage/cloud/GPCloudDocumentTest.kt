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

import biz.ganttproject.storage.LatestVersion
import biz.ganttproject.storage.OnlineDocumentMode
import biz.ganttproject.storage.checksum
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.runBlocking
import net.sourceforge.ganttproject.document.FileDocument
import org.easymock.EasyMock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.net.UnknownHostException
import java.time.Instant
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

data class TestResponse(override val decodedBody: ByteArray = byteArrayOf(),
                        override val rawBody: ByteArray = byteArrayOf(),
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
class GPCloudDocumentTest {
  private lateinit var testMirrorFolder: File
  private lateinit var mockHttpClient: GPCloudHttpClient

  @BeforeEach
  fun setUp() {
    testMirrorFolder = Files.createTempDir()
    mockHttpClient = EasyMock.createMock(GPCloudHttpClient::class.java)
  }

  @AfterEach
  fun tearDown() {
    testMirrorFolder.deleteRecursively()
  }

  private fun prepareReadCall(doc: GPCloudDocument, body: ByteArray) {
    prepareReadCall(doc) {
      val checksum = body.checksum()
      TestResponse(decodedBody = body, headers = mapOf(
          "ETag" to "100500",
          "Digest" to "crc32c=$checksum"
      ))
    }
  }

  private fun prepareReadCall(doc: GPCloudDocument, responseBuilder: ()->TestResponse) {
    doc.httpClientFactory = { mockHttpClient }
    doc.offlineDocumentFactory = { path -> FileDocument(File(testMirrorFolder, path)) }
    doc.executor = MoreExecutors.newDirectExecutorService()

    EasyMock.reset(mockHttpClient)
    EasyMock.expect(mockHttpClient.sendGet("/p/read?projectRefid=prj1")).andReturn(
        responseBuilder()
    )
    EasyMock.replay(mockHttpClient)

  }

  val BODY_239 = byteArrayOf(2, 3, 9)
  val BODY_566 = byteArrayOf(5, 6, 6)

  /**
   * The very simple test which checks that document is fetched and gets ONLINE mode.
   */
  @Test
  fun testBasicOnlineDocumentRead() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)
    val fetch = runBlocking { doc.fetch().also { it.update() }}
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
  @Test
  fun testOnlineGoesMirrored() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)
    runBlocking { doc.fetch().also { it.update() }}
    doc.setMirrored(true)

    assertNotNull(doc.offlineMirror)
    assertEquals(OnlineDocumentMode.MIRROR, doc.mode.value)
    assertArrayEquals(BODY_239, ByteStreams.toByteArray(doc.offlineMirror!!.inputStream))
    assertEquals(100500L, GPCloudOptions.cloudFiles.files[doc.projectIdFingerprint]!!.lastOnlineVersion?.toLong())
    assertEquals(BODY_239.checksum(), GPCloudOptions.cloudFiles.files[doc.projectIdFingerprint]!!.lastOnlineChecksum)
    assertEquals(BODY_239.checksum(), doc.offlineMirror!!.checksum())
    assertEquals("Project 1", GPCloudOptions.cloudFiles.files[doc.projectIdFingerprint]!!.name)
  }

  /**
   * Test that write to document in MIRROR mode makes both HTTP call and local disk write.
   */
  @Test
  fun testWriteMirrored() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)
    runBlocking { doc.fetch().also { it.update() }}
    doc.setMirrored(true)

    EasyMock.reset(mockHttpClient)
    EasyMock.expect(mockHttpClient.sendPost("/p/write", mapOf(
        "projectRefid" to "prj1",
        "teamRefid" to "team1",
        "filename" to "Project 1",
        "fileContents" to Base64.getEncoder().encodeToString(BODY_566),
        "lockToken" to null,
        "oldVersion" to "100500"
    ))).andReturn(TestResponse(
        headers = mapOf(
          "ETag" to "146512",
          "Digest" to "crc32c=${BODY_566.checksum()}"
        ),
        rawBody = JACKSON.writeValueAsBytes(ProjectWriteResponse(projectRefid = "prj1"))
    ))
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
  @Test
  fun testMirroredGoesOnline() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)
    runBlocking { doc.fetch().also { it.update() }}
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
  @Test
  fun testMirroredAutomaticallyWhenHasOptions() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)

    val mirrorFile = File(testMirrorFolder, doc.projectIdFingerprint)
    mirrorFile.writeBytes(BODY_239)
    GPCloudOptions.cloudFiles.getFileOptions(doc.projectIdFingerprint).let {
      it.offlineMirror = mirrorFile.absolutePath
    }
    runBlocking { doc.fetch().also { it.update() }}
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
  @Test
  fun testOnlineGoesOfflineOnWrite() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)
    val fetch = runBlocking { doc.fetch().also { it.update() }}
    assertGoesOffline(doc, fetch.actualVersion)

    val doc1 = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc1, BODY_239)
    val fetch1 = runBlocking { doc1.fetch().also { it.update() }}
    doc1.setMirrored(true)
    assertGoesOffline(doc1, fetch1.actualVersion)
    assertArrayEquals(BODY_566, ByteStreams.toByteArray(doc1.offlineMirror!!.inputStream))
  }

  /**
   * Test that when writing in OFFLINE_ONLY mode we only touch disk
   */
  @Test
  fun testWriteOffline() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc, BODY_239)
    val fetch = runBlocking { doc.fetch().also { it.update() }}
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
   * Test that we reconnect automatically in a while after going OFFLINE_ONLY.
   *
   * TODO: make this test great again. Currently it is ignored because of move of reconnecting feature
   * from GPCloudDocument into GPCloudStatusBar.
   */
  fun testOfflineGoesOnlineOnReconnect() {
    val retryCounter = CountDownLatch(2)
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
    ))).andReturn(TestResponse(
        headers = mapOf(
          "ETag" to "146512",
          "Digest" to "crc32c=${BODY_566.checksum()}"
        ),
        rawBody = JACKSON.writeValueAsBytes(ProjectWriteResponse(projectRefid = "prj1"))
    ))
    EasyMock.replay(mockHttpClient)

    retryCounter.await()
    Thread.sleep(1000)
    assertEquals(OnlineDocumentMode.MIRROR, doc.mode.value)
  }

  @Test
  fun `ProjectChange event triggers document fetch`() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc) {
      val checksum = BODY_239.checksum()
      TestResponse(decodedBody = BODY_239, headers = mapOf(
          "ETag" to "1",
          "Digest" to "crc32c=$checksum"
      ))
    }
    runBlocking { doc.fetch().also { it.update() }}

    prepareReadCall(doc) {
      val checksum = BODY_566.checksum()
      TestResponse(decodedBody = BODY_566, headers = mapOf(
          "ETag" to "2",
          "Digest" to "crc32c=$checksum"
      ))
    }
    runBlocking {
      doc.onWebSocketContentChange(JACKSON.createObjectNode().apply {
        put("teamRefid", "team1")
        put("projectRefid", "prj1")
        put("timestamp", 100500)
        set<ObjectNode>("author", JACKSON.createObjectNode().apply {
          put("id", "joedoe")
          put("name", "Joe Doe")
        })
      })
    }

    assertEquals(LatestVersion(100500, "Joe Doe"), doc.latestVersionProperty.get()) {
      "It is expected that latestVersion property is updated with the results of the last project fetch"
    }
    assertArrayEquals(BODY_239, ByteStreams.toByteArray(doc.inputStream)) {
      "It is expected that the last fetch has not been applied, and document contents is still as it was"
    }
  }

  @Test
  fun `ProjectChange event won't trigger latest version change if version matches`() {
    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1", projectJson = null)
    prepareReadCall(doc) {
      val checksum = BODY_239.checksum()
      TestResponse(decodedBody = BODY_239, headers = mapOf(
          "ETag" to "1",
          "Digest" to "crc32c=$checksum"
      ))
    }
    runBlocking { doc.fetch().also { it.update() }}
    doc.latestVersionProperty.set(LatestVersion(0, "God"))

    prepareReadCall(doc) {
      val checksum = BODY_239.checksum()
      TestResponse(decodedBody = BODY_239, headers = mapOf(
          "ETag" to "1",
          "Digest" to "crc32c=$checksum"
      ))
    }
    runBlocking {
      doc.onWebSocketContentChange(JACKSON.createObjectNode().apply {
        put("teamRefid", "team1")
        put("projectRefid", "prj1")
        put("timestamp", 100500)
        set<ObjectNode>("author", JACKSON.createObjectNode().apply {
          put("id", "joedoe")
          put("name", "Joe Doe")
        })
      })
    }

    assertEquals(LatestVersion(0, "God"), doc.latestVersionProperty.get()) {
      "It is expected that latestVersion property remains as it ws if read returns the same ETag"
    }
  }

  private fun ObjectNode.buildLock(user: String) {
    put("uid", user)
    put("expirationEpochTs", Instant.now().plusSeconds(3600).toEpochMilli())
  }

  @Test
  fun `Lock data gets into lock status`() {
    GPCloudOptions.userId.value = "MeMyself"
    GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1",
        projectJson = ProjectJsonAsFolderItem(JACKSON.createObjectNode().apply {
          putObject("lock").also{ it.buildLock("User1") }
          put("refid", "prj1")
        })
    ).also {
      assertTrue(it.status.get().locked)
      assertTrue(it.status.get().lockedBySomeone)
    }

    GPCloudOptions.userId.value = "MrLockHolder"
    GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1",
        projectJson = ProjectJsonAsFolderItem(JACKSON.createObjectNode().apply {
          putObject("lock").also{ it.buildLock("MrLockHolder") }
          put("refid", "prj1")
        })
    ).also {
      assertTrue(it.status.get().locked)
      assertFalse(it.status.get().lockedBySomeone)
    }
  }

//  @Test
//  fun `Unlock event changes lock status`() {
//    GPCloudOptions.userId.value = "MeMyself"
//    val doc = GPCloudDocument(teamRefid = "team1", teamName = "Team 1", projectRefid = "prj1", projectName = "Project 1",
//        projectJson = ProjectJsonAsFolderItem(JACKSON.createObjectNode().apply {
//          putObject("lock").also{ it.buildLock("User1") }
//          put("refid", "prj1")
//        })
//    )
//  }
}

private val JACKSON = ObjectMapper()
