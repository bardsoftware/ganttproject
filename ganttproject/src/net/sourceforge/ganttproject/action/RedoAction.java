/*
 * Created on 14.03.2005
 */
package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;

import javax.swing.event.UndoableEditEvent;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.undo.GPUndoListener;
import net.sourceforge.ganttproject.undo.GPUndoManager;

/**
 * @author bard
 */
public class RedoAction extends GPAction implements GPUndoListener {
    private GPUndoManager myUndoManager;

    private final GanttProject appli;
    
    public RedoAction(GPUndoManager undoManager, String iconSize, GanttProject appli) {
        super(null, iconSize);
        myUndoManager = undoManager;
        myUndoManager.addUndoableEditListener(this);
        this.appli = appli;
        setEnabled(myUndoManager.canRedo());
    }

    public void actionPerformed(ActionEvent e) {
    	appli.getUIFacade().setStatusText(GanttLanguage.getInstance().getText("redo"));    
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

    public void isIconVisible(boolean isNull) {
        setIconVisible(isNull);
    }

    protected String getLocalizedName() {
        return GanttProject.correctLabel(GanttLanguage.getInstance().getText(
                "redo"));
    }

}
