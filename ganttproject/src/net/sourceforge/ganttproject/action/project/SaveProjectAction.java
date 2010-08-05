/*
 * Created on 03.10.2005
 */
package net.sourceforge.ganttproject.action.project;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.ProjectEventListener;
import net.sourceforge.ganttproject.action.GPAction;

class SaveProjectAction extends GPAction implements ProjectEventListener {
    private final GanttProject myMainFrame;

    SaveProjectAction(GanttProject mainFrame) {
        super("saveProject", "16");
        myMainFrame = mainFrame;
        mainFrame.addProjectEventListener(this);
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MASK));
        setEnabled(false);
    }
    
    protected String getLocalizedName() {
        return getI18n("saveProject");
    }

    protected String getTooltipText() {
        return getI18n("saveProject");
    }

    protected String getIconFilePrefix() {
        return "save_";
    }

    public void actionPerformed(ActionEvent e) {
        try {
            myMainFrame.saveProject();
        } catch (IOException e1) {
        	myMainFrame.getUIFacade().showErrorDialog(e1);
        }
    }

    public void projectModified() {
        setEnabled(true);
    }

    public void projectSaved() {
        setEnabled(false);
    }

    public void projectClosed() {
        setEnabled(false);
    }
    
    

}
