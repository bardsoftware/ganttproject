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
package net.sourceforge.ganttproject.undo;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * @author bard
 */
public interface GPUndoManager {
  void undoableEdit(String localizedName, Runnable runnableEdit);

  boolean canUndo();

  boolean canRedo();

  void undo() throws CannotUndoException;

  void redo() throws CannotRedoException;

  String getUndoPresentationName();

  String getRedoPresentationName();

  void addUndoableEditListener(GPUndoListener listener);

  void removeUndoableEditListener(GPUndoListener listener);

  void die();
}
