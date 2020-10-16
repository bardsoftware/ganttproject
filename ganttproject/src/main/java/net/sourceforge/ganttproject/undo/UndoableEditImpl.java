/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

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
package net.sourceforge.ganttproject.undo;

import java.io.IOException;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmCollection;

/**
 * @author bard
 */
class UndoableEditImpl extends AbstractUndoableEdit {
  private String myPresentationName;

  private Document myDocumentBefore;

  private Document myDocumentAfter;

  private UndoManagerImpl myManager;

  UndoableEditImpl(String localizedName, Runnable editImpl, UndoManagerImpl manager) throws IOException {
    myManager = manager;
    myPresentationName = localizedName;
    myDocumentBefore = saveFile();
    editImpl.run();
    myDocumentAfter = saveFile();
  }

  private Document saveFile() throws IOException {
    Document doc = myManager.getDocumentManager().newAutosaveDocument();
    doc.write();
    return doc;
  }

  @Override
  public boolean canUndo() {
    return myDocumentBefore.canRead();
  }

  @Override
  public boolean canRedo() {
    return myDocumentAfter.canRead();
  }

  @Override
  public void redo() throws CannotRedoException {
    try {
      restoreDocument(myDocumentAfter);
    } catch (DocumentException e) {
      undoRedoExceptionHandler(e);
    } catch (IOException e) {
      undoRedoExceptionHandler(e);
    }
  }

  @Override
  public void undo() throws CannotUndoException {
    try {
      restoreDocument(myDocumentBefore);
    } catch (DocumentException e) {
      undoRedoExceptionHandler(e);
    } catch (IOException e) {
      undoRedoExceptionHandler(e);
    }
  }

  private void restoreDocument(Document document) throws IOException, DocumentException {
    Document projectDocument = myManager.getProject().getDocument();
    myManager.getProject().close();
    AlgorithmCollection algs = myManager.getProject().getTaskManager().getAlgorithmCollection();
    try {
      algs.getScheduler().setEnabled(false);
      algs.getRecalculateTaskScheduleAlgorithm().setEnabled(false);
      algs.getAdjustTaskBoundsAlgorithm().setEnabled(false);
      document.read();
    } finally {
      algs.getRecalculateTaskScheduleAlgorithm().setEnabled(true);
      algs.getAdjustTaskBoundsAlgorithm().setEnabled(true);
      algs.getScheduler().setEnabled(true);
    }
    myManager.getProject().setDocument(projectDocument);

  }

  @Override
  public String getPresentationName() {
    return myPresentationName;
  }

  private void undoRedoExceptionHandler(Exception e) {
    if (!GPLogger.log(e)) {
      e.printStackTrace(System.err);
    }
    throw new CannotRedoException();
  }
}
