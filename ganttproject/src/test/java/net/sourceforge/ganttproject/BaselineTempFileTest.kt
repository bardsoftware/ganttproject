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
package net.sourceforge.ganttproject

import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.text.DateFormat
import java.util.Locale

/**
 * The temporary file behind a baseline, from writing it to removing it.
 *
 * [GanttPreviousState] keeps every baseline in a file of its own in the system temporary
 * directory, named `_GanttProject_ps_<random>.gan`. The tests below note which files of that
 * name exist before a baseline is created, so the one file belonging to this baseline can be
 * named without reaching into the private state of the class.
 *
 * `java.io.tmpdir` cannot be redirected from inside the test: `File.createTempFile` reads the
 * property once when `java.io.File` is loaded and keeps it.
 */
class BaselineTempFileTest {
  private lateinit var tmpDir: File
  private lateinit var before: Set<File>

  @BeforeEach
  fun setUp() {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : LocaleApi {
          override fun getLocale(): Locale = Locale.US
          override fun getShortDateFormat(): DateFormat =
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        })
      }
    }
    // Ask the JDK itself where its temporary files go, rather than trusting the property.
    val probe = File.createTempFile("baseline-temp-file-test", ".probe")
    tmpDir = probe.parentFile
    probe.delete()
    before = baselineFiles()
  }

  @AfterEach
  fun tearDown() {
    (baselineFiles() - before).forEach { it.walkBottomUp().forEach { f -> f.delete() } }
  }

  private fun baselineFiles(): Set<File> =
    (tmpDir.listFiles { f: File -> f.name.startsWith("_GanttProject_ps_") } ?: emptyArray())
      .toSet()

  /** The files this test's own baselines have created, and nobody else's. */
  private fun ownFiles(): Set<File> = baselineFiles() - before

  private fun newBaseline(): GanttPreviousState {
    val start = CalendarFactory.createGanttCalendar(2026, 8, 11)
    val tasks = listOf(GanttPreviousStateTask(1, start, 3, false, false))
    return GanttPreviousState("a baseline", tasks)
  }

  @Test
  fun `the temporary file of a removed baseline is gone`() {
    val baseline = newBaseline()
    baseline.init()
    baseline.saveFile()
    assertEquals(1, ownFiles().size, "saving a baseline writes exactly one temporary file")

    baseline.remove()

    assertEquals(emptySet<File>(), ownFiles(),
      "remove() has to leave no file behind. On Windows it can only succeed if the stream " +
        "that wrote the file has been closed: the system refuses to delete an open file.")
  }

  @Test
  fun `removing a baseline reports success`() {
    val baseline = newBaseline()
    baseline.init()
    baseline.saveFile()

    assertTrue(baseline.remove(), "a delete that worked has to be reported as a success")
  }

  @Test
  fun `a failed delete is reported, not swallowed`() {
    val baseline = newBaseline()
    baseline.init()
    // Turn the baseline's file into a directory that is not empty. File.delete() refuses that
    // on every platform, which gives the failure path something to run on outside Windows too.
    val path = ownFiles().single()
    assertTrue(path.delete())
    assertTrue(path.mkdir())
    assertTrue(File(path, "occupied").createNewFile())

    assertFalse(baseline.remove(), "a delete that did not work must not be reported as a success")
  }

  @Test
  fun `a failed delete names the file in the log`() {
    val logged = mutableListOf<String>()
    val appender = object : AppenderBase<ILoggingEvent>() {
      override fun append(event: ILoggingEvent) {
        if (event.level == Level.ERROR) logged.add(event.formattedMessage)
      }
    }
    val logger = LoggerFactory.getLogger("Baseline") as Logger
    appender.start()
    logger.addAppender(appender)
    try {
      val baseline = newBaseline()
      baseline.init()
      val path = ownFiles().single()
      assertTrue(path.delete())
      assertTrue(path.mkdir())
      assertTrue(File(path, "occupied").createNewFile())

      baseline.remove()

      assertEquals(1, logged.size, "a failed delete has to produce exactly one error line")
      assertTrue(logged.single().contains(path.absolutePath),
        "the error line has to name the file that stayed behind, it said: ${logged.single()}")
    } finally {
      logger.detachAppender(appender)
      appender.stop()
    }
  }

  @Test
  fun `the written baseline file is complete`() {
    val baseline = newBaseline()
    baseline.init()
    baseline.saveFile()
    val file = ownFiles().single()
    assertTrue(file.readText().contains("a baseline"),
      "closing the stream must not cost the content: the baseline name has to be in the file")
  }
}
