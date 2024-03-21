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
package net.sourceforge.ganttproject.undo

import net.sourceforge.ganttproject.document.FileDocument
import net.sourceforge.ganttproject.storage.SQL_PROJECT_DATABASE_OPTIONS
import net.sourceforge.ganttproject.storage.SqlProjectDatabaseImpl
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.RuntimeException
import java.nio.file.Files

class UndoManagerTest {
  @Test
  fun `failed undoable edit closes transaction`() {
    val dataSource = JdbcDataSource().also {
      it.setURL("jdbc:h2:mem:test$SQL_PROJECT_DATABASE_OPTIONS")
    }
    val projectDatabase = SqlProjectDatabaseImpl(dataSource).also {
      it.startLog(0)
    }
    projectDatabase.init()

    var called = false
    UndoableEditImpl(args = UndoableEditImpl.Args(
      displayName = "Test",
      newAutosave = { FileDocument(Files.createTempFile("qwe", "asd").toFile()) },
      restore = {},
      projectDatabase = projectDatabase
    )) {
      called = true
      throw RuntimeException()
    }
    assertTrue(called)
    // If we didn't rollback txn on error, we would not be able to start a new one.
    projectDatabase.startTransaction("Foo").rollback()
  }
}