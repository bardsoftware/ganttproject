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
public class UndoAction extends GPAction implements GPUndoListener {
    private GPUndoManager myUndoManager;

    private final GanttProject appli;

    public UndoAction(GPUndoManager undoManager, String iconSize, GanttProject appli) {
        super(null, iconSize);
        myUndoManager = undoManager;
        myUndoManager.addUndoableEditListener(this);
        this.appli = appli;
        setEnabled(myUndoManager.canUndo());        
    }

    public void actionPerformed(ActionEvent e) {
    	appli.getUIFacade().setStatusText(GanttLanguage.getInstance().getText("undo"));
        myUndoManager.undo();
    }

    public void undoableEditHappened(UndoableEditEvent e) {
        setEnabled(myUndoManager.canUndo());
    }

    public void undoOrRedoHappened() {
        setEnabled(myUndoManager.canUndo());
    }

    protected String getIconFilePrefix() {
        return "undo_";
    }

    public void isIconVisible(boolean isNull) {
        setIconVisible(isNull);
    }

    protected String getLocalizedName() {
        return GanttProject.correctLabel(GanttLanguage.getInstance().getText(
                "undo"));
    }

}
