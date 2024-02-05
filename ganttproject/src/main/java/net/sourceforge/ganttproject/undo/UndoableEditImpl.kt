/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2024 GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.undo

import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.storage.DummyTxn
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.storage.ProjectDatabaseException
import net.sourceforge.ganttproject.storage.ProjectDatabaseTxn
import java.io.IOException
import javax.swing.undo.AbstractUndoableEdit
import javax.swing.undo.CannotRedoException
import javax.swing.undo.CannotUndoException

/**
 * @author bard
 */
class UndoableEditImpl(
  private val args: Args,
  editImpl: Runnable
) : AbstractUndoableEdit() {
  data class Args(
    val displayName: String,
    val newAutosave: ()->Document,
    val restore: (Document)->Unit,
    val projectDatabase: ProjectDatabase
  )
  private val myDocumentBefore: Document

  private val myDocumentAfter: Document

  private val projectDatabaseTxn: ProjectDatabaseTxn

  init {
    myDocumentBefore = saveFile()
    projectDatabaseTxn = try {
      args.projectDatabase.startTransaction(args.displayName)
    } catch (ex: ProjectDatabaseException) {
      DummyTxn()
    }
    try {
      editImpl.run()
      projectDatabaseTxn.commit()
    } catch (ex: Exception) {
      GPLogger.log(ex)
      //projectDatabaseTxn.rollback()
    }
    myDocumentAfter = saveFile()
  }

  @Throws(IOException::class)
  private fun saveFile(): Document {
    val doc = args.newAutosave()
    doc.write()
    return doc
  }

  override fun canUndo(): Boolean {
    return myDocumentBefore.canRead()
  }

  override fun canRedo(): Boolean {
    return myDocumentAfter.canRead()
  }

  @Throws(CannotRedoException::class)
  override fun redo() {
    try {
      restoreDocument(myDocumentAfter)
      try {
        projectDatabaseTxn.redo()
      } catch (e: ProjectDatabaseException) {
        GPLogger.log(e)
      }
    } catch (e: Document.DocumentException) {
      undoRedoExceptionHandler(e)
    } catch (e: IOException) {
      undoRedoExceptionHandler(e)
    }
  }

  @Throws(CannotUndoException::class)
  override fun undo() {
    try {
      restoreDocument(myDocumentBefore)
      try {
        projectDatabaseTxn.undo()
      } catch (e: ProjectDatabaseException) {
        GPLogger.log(e)
      }
    } catch (e: Document.DocumentException) {
      undoRedoExceptionHandler(e)
    } catch (e: IOException) {
      undoRedoExceptionHandler(e)
    }
  }

  @Throws(IOException::class, Document.DocumentException::class)
  private fun restoreDocument(document: Document) {
    args.restore(document)
  }

  override fun getPresentationName(): String {
    return args.displayName
  }

  private fun undoRedoExceptionHandler(e: Exception) {
    if (!GPLogger.log(e)) {
      e.printStackTrace(System.err)
    }
    throw CannotRedoException()
  }
}
