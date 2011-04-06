package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.GPViewManager;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.undo.GPUndoManager;

public class EditMenu {
    private final IGanttProject myProject;
    private final UIFacade myUiFacade;
    private final GPViewManager myViewManager;
    private final UndoAction myUndoAction;
    private final RedoAction myRedoAction;

    public EditMenu(IGanttProject project, UIFacade uiFacade, GPViewManager viewManager) {
        myProject = project;
        myUiFacade = uiFacade;
        myViewManager = viewManager;
        myUndoAction = new UndoAction(getUndoManager(), getUIFacade());
        myUndoAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, GPAction.MENU_MASK));
        myRedoAction = new RedoAction(getUndoManager(), getUIFacade());
        myRedoAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, GPAction.MENU_MASK));
    }

    public JMenu create() {
        JMenu result = new JMenu(new GPAction("edit") {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            }
        });
        result.add(getUndoAction());

        result.add(getRedoAction());
        result.addSeparator();
        result.add(new RefreshViewAction(getUIFacade()));
        result.addSeparator();
        result.add(getViewManager().getCutAction());
        result.add(getViewManager().getCopyAction());
        result.add(getViewManager().getPasteAction());
        result.addSeparator();
        result.add(new SettingsDialogAction(getProject(), getUIFacade()));

        return result;
    }

    private UIFacade getUIFacade() {
        return myUiFacade;
    }

    private IGanttProject getProject() {
        return myProject;
    }

    private GPUndoManager getUndoManager() {
        return myUiFacade.getUndoManager();
    }

    private GPViewManager getViewManager() {
        return myViewManager;
    }

    public Action getUndoAction() {
        return myUndoAction;
    }

    public Action getRedoAction() {
        return myRedoAction;
    }
}
