/*
 * Created on 14.03.2005
 */
package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;

import javax.swing.event.UndoableEditEvent;

import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.undo.GPUndoListener;
import net.sourceforge.ganttproject.undo.GPUndoManager;

/**
 * @author bard
 */
public class RedoAction extends GPAction implements GPUndoListener {
    private GPUndoManager myUndoManager;
    private UIFacade myUiFacade;

    public RedoAction(GPUndoManager undoManager, UIFacade uiFacade) {
        super("redo");
        myUndoManager = undoManager;
        myUndoManager.addUndoableEditListener(this);
        myUiFacade = uiFacade;
        setEnabled(myUndoManager.canRedo());
    }

    public void actionPerformed(ActionEvent e) {
        myUiFacade.setStatusText(GanttLanguage.getInstance().getText("redo"));
        myUndoManager.redo();
    }

    public void undoableEditHappened(UndoableEditEvent e) {
        setEnabled(myUndoManager.canRedo());
    }

    public void undoOrRedoHappened() {
        setEnabled(myUndoManager.canRedo());
    }

    protected String getIconFilePrefix() {
        return "redo_";
    }
}
