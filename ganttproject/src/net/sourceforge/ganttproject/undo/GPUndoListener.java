/*
 * Created on 14.03.2005
 */
package net.sourceforge.ganttproject.undo;

import javax.swing.event.UndoableEditListener;

/**
 * @author bard
 */
public interface GPUndoListener extends UndoableEditListener {
    void undoOrRedoHappened();

}
