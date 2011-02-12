/*
 * Created on 12.03.2005
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
