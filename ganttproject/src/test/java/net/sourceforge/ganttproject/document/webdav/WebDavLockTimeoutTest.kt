/*
Copyright 2026 BarD Software s.r.o

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
package net.sourceforge.ganttproject.document.webdav

import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.document.DocumentCreator
import net.sourceforge.ganttproject.document.ProxyDocument
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.parser.ParserFactory
import org.easymock.EasyMock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord

private const val DOCUMENT_URL = "http://webdav.example.invalid/projects/test.gan"

/**
 * Records everything which is written to a java.util.logging logger.
 */
private class RecordingHandler : Handler() {
  val records = mutableListOf<LogRecord>()
  override fun publish(record: LogRecord?) { record?.let { records.add(it) } }
  override fun flush() {}
  override fun close() {}
}

private fun createDocumentManager(): DocumentCreator {
  val project = EasyMock.createNiceMock<IGanttProject>(IGanttProject::class.java)
  val uiFacade = EasyMock.createNiceMock<UIFacade>(UIFacade::class.java)
  val parserFactory = EasyMock.createNiceMock<ParserFactory>(ParserFactory::class.java)
  EasyMock.replay(project, uiFacade, parserFactory)
  return DocumentCreator(project, uiFacade, parserFactory)
}

private fun <T> withRecordedLog(code: () -> T): Pair<T, List<LogRecord>> {
  val logger = GPLogger.getLogger(HttpDocument::class.java)
  val handler = RecordingHandler()
  val wasUsingParentHandlers = logger.useParentHandlers
  val oldLevel = logger.level
  logger.addHandler(handler)
  logger.useParentHandlers = false
  logger.level = Level.ALL
  try {
    return code() to handler.records.toList()
  } finally {
    logger.removeHandler(handler)
    logger.useParentHandlers = wasUsingParentHandlers
    logger.level = oldLevel
  }
}

class WebDavLockTimeoutTest {
  /**
   * The webdav.lockTimeout option is editable and persisted, so it must arrive at the document
   * which DocumentManager creates from a WebDAV URL.
   */
  @Test
  fun `configured lock timeout reaches the document`() {
    val documentManager = createDocumentManager()
    val timeoutOption = (documentManager.webDavStorageUi as WebDavStorageImpl).webDavLockTimeoutOption
    timeoutOption.value = 120

    val document = (documentManager.getDocument(DOCUMENT_URL) as ProxyDocument).realDocument
    assertInstanceOf(HttpDocument::class.java, document)
    assertEquals(120, (document as HttpDocument).lockTimeout)
  }

  /**
   * The default value of the option is -1, and it must arrive at the document as well:
   * we want the value which is configured, not a value which is hardcoded elsewhere.
   */
  @Test
  fun `default lock timeout reaches the document`() {
    val documentManager = createDocumentManager()
    val timeoutOption = (documentManager.webDavStorageUi as WebDavStorageImpl).webDavLockTimeoutOption
    assertEquals(HttpDocument.NO_LOCK, timeoutOption.value)

    val document = (documentManager.getDocument(DOCUMENT_URL) as ProxyDocument).realDocument
    assertEquals(HttpDocument.NO_LOCK, (document as HttpDocument).lockTimeout)
  }

  /**
   * A negative timeout means "never lock". Locking is silently skipped today: the user who has
   * once set the value negative works without locks and finds no trace of it anywhere.
   */
  @Test
  fun `negative lock timeout is reported in the log`() {
    val resource = EasyMock.createMock<WebDavResource>(WebDavResource::class.java)
    EasyMock.expect(resource.url).andReturn(DOCUMENT_URL).anyTimes()
    EasyMock.replay(resource)

    val (acquired, records) = withRecordedLog {
      HttpDocument(resource, "user", "password", HttpDocument.NO_LOCK).acquireLock()
    }

    // Nothing failed, so the return value stays `true`: the setting says "never lock".
    assertTrue(acquired)
    // No WebDAV operation whatsoever was attempted: the mock would have failed on any other call.
    EasyMock.verify(resource)
    // ... but the fact is written to the log.
    val message = records.singleOrNull { it.level.intValue() >= Level.WARNING.intValue() }?.message
    assertTrue(message != null && message.contains(DOCUMENT_URL)) {
      "Expected a warning mentioning $DOCUMENT_URL, got ${records.map { "${it.level}: ${it.message}" }}"
    }
  }

  /**
   * A non-negative timeout locks the resource, and minutes are converted to seconds.
   */
  @Test
  fun `positive lock timeout locks the resource`() {
    val resource = EasyMock.createMock<WebDavResource>(WebDavResource::class.java)
    EasyMock.expect(resource.url).andReturn(DOCUMENT_URL).anyTimes()
    EasyMock.expect(resource.exists()).andReturn(true)
    resource.lock(120 * 60)
    EasyMock.expectLastCall<Any>().once()
    EasyMock.replay(resource)

    val (acquired, records) = withRecordedLog {
      HttpDocument(resource, "user", "password", 120).acquireLock()
    }

    assertTrue(acquired)
    EasyMock.verify(resource)
    assertFalse(records.any { it.level.intValue() >= Level.WARNING.intValue() }) {
      "Did not expect a warning, got ${records.map { "${it.level}: ${it.message}" }}"
    }
  }
}
