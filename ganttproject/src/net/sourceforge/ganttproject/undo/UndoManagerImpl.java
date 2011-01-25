/*
 * Created on 12.03.2005
 */
package net.sourceforge.ganttproject.undo;

import java.io.IOException;

import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.parser.ParserFactory;

/**
 * @author bard
 */
public class UndoManagerImpl implements GPUndoManager {
    private UndoableEditSupport myUndoEventDispatcher;

    private UndoManager mySwingUndoManager;

    private DocumentManager myDocumentManager;

    private ParserFactory myParserFactory;

    private IGanttProject myProject;

    private UndoableEditImpl swingEditImpl;

    public UndoManagerImpl(IGanttProject project, ParserFactory parserFactory,
            DocumentManager documentManager) {
        myProject = project;
        myParserFactory = parserFactory;
        myDocumentManager = documentManager;
        mySwingUndoManager = new UndoManager();
        myUndoEventDispatcher = new UndoableEditSupport();

    }

    public void undoableEdit(String localizedName, Runnable editImpl) {

        try {
            swingEditImpl = new UndoableEditImpl(localizedName, editImpl, this);
            mySwingUndoManager.addEdit(swingEditImpl);
            fireUndoableEditHappened(swingEditImpl);
        } catch (IOException e) {
        	if (!GPLogger.log(e)) {
        		e.printStackTrace(System.err);
        	}
        }
    }

    private void fireUndoableEditHappened(UndoableEditImpl swingEditImpl) {
        myUndoEventDispatcher.postEdit(swingEditImpl);
    }

    private void fireUndoOrRedoHappened() {
        UndoableEditListener[] listeners = myUndoEventDispatcher
                .getUndoableEditListeners();
        for (int i = 0; i < listeners.length; i++) {
            ((GPUndoListener) listeners[i]).undoOrRedoHappened();
        }
    }

    DocumentManager getDocumentManager() {
        return myDocumentManager;
    }

    protected ParserFactory getParserFactory() {
        return myParserFactory;
    }

    IGanttProject getProject() {
        return myProject;
    }

    public boolean canUndo() {
        return mySwingUndoManager.canUndo();
    }

    public boolean canRedo() {
        return mySwingUndoManager.canRedo();
    }

    public void undo() throws CannotUndoException {
        mySwingUndoManager.undo();
        fireUndoOrRedoHappened();
    }

    public void redo() throws CannotRedoException {
        mySwingUndoManager.redo();
        fireUndoOrRedoHappened();
    }

    public String getUndoPresentationName() {
        return mySwingUndoManager.getUndoPresentationName();
    }

    public String getRedoPresentationName() {
        return mySwingUndoManager.getRedoPresentationName();
    }

    public void addUndoableEditListener(GPUndoListener listener) {
        myUndoEventDispatcher.addUndoableEditListener(listener);
    }

    public void removeUndoableEditListener(GPUndoListener listener) {
        myUndoEventDispatcher.removeUndoableEditListener(listener);
    }

    public void die() {
        if (swingEditImpl != null)
            swingEditImpl.die();
        if (mySwingUndoManager != null)
            mySwingUndoManager.discardAllEdits();
    }
}
