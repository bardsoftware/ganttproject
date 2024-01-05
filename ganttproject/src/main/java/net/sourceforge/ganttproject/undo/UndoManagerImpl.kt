/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Alexandre Thomas, Dmitry Barashev

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

import biz.ganttproject.storage.AutoSaveManager
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.parser.ParserFactory
import net.sourceforge.ganttproject.storage.ProjectDatabase
import java.io.IOException
import javax.swing.UIManager
import javax.swing.undo.CannotRedoException
import javax.swing.undo.CannotUndoException
import javax.swing.undo.UndoManager
import javax.swing.undo.UndoableEditSupport

/**
 * UndoManager implementation, it manages the undoable edits in GanttProject
 *
 * @author bard
 */
open class UndoManagerImpl(
  val project: IGanttProject?,
  private val myParserFactory: ParserFactory?,
  val documentManager: DocumentManager,
  val projectDatabase: ProjectDatabase
) : GPUndoManager {
  private val myUndoEventDispatcher = UndoableEditSupport()
  private val mySwingUndoManager: UndoManager = UndoManager()
  private var swingEditImpl: UndoableEditImpl? = null

  init {
    GanttLanguage.getInstance().addListener {
      UIManager.getDefaults()["AbstractUndoableEdit.undoText"] = GanttLanguage.getInstance().getText("undo")
      UIManager.getDefaults()["AbstractUndoableEdit.redoText"] = GanttLanguage.getInstance().getText("redo")
    }
  }

  override fun undoableEdit(localizedName: String, editImpl: Runnable) {
    try {
      swingEditImpl = UndoableEditImpl(UndoableEditImpl.Args(
        displayName = localizedName,
        newAutosave = { autoSaveManager.newAutoSaveDocument() },
        restore = { project?.restore(it) },
        projectDatabase = projectDatabase
      ), editImpl)
      mySwingUndoManager.addEdit(swingEditImpl)
      fireUndoableEditHappened(swingEditImpl!!)
    } catch (e: IOException) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err)
      }
    }
  }

  private fun fireUndoableEditHappened(swingEditImpl: UndoableEditImpl) {
    myUndoEventDispatcher.postEdit(swingEditImpl)
  }

  private fun fireUndoOrRedoHappened() {
    for (listener in myUndoEventDispatcher.undoableEditListeners) {
      (listener as GPUndoListener).undoOrRedoHappened()
    }
  }

  private fun fireUndoReset() {
    for (listener in myUndoEventDispatcher.undoableEditListeners) {
      (listener as GPUndoListener).undoReset()
    }
  }


  val autoSaveManager: AutoSaveManager
    get() = AutoSaveManager(documentManager)

  protected open val parserFactory: ParserFactory?
    get() = myParserFactory

  override fun canUndo(): Boolean {
    return mySwingUndoManager.canUndo()
  }

  override fun canRedo(): Boolean {
    return mySwingUndoManager.canRedo()
  }

  @Throws(CannotUndoException::class)
  override fun undo() {
    mySwingUndoManager.undo()
    fireUndoOrRedoHappened()
  }

  @Throws(CannotRedoException::class)
  override fun redo() {
    mySwingUndoManager.redo()
    fireUndoOrRedoHappened()
  }

  override fun getUndoPresentationName(): String {
    return mySwingUndoManager.undoPresentationName
  }

  override fun getRedoPresentationName(): String {
    return mySwingUndoManager.redoPresentationName
  }

  override fun addUndoableEditListener(listener: GPUndoListener) {
    myUndoEventDispatcher.addUndoableEditListener(listener)
  }

  override fun removeUndoableEditListener(listener: GPUndoListener) {
    myUndoEventDispatcher.removeUndoableEditListener(listener)
  }

  override fun die() {
    if (swingEditImpl != null) {
      swingEditImpl!!.die()
    }
    mySwingUndoManager.discardAllEdits()
    fireUndoReset()
  }
}
