/*
 * Created on 27.09.2005
 */
package net.sourceforge.ganttproject.action.project;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;

class OpenProjectAction extends GPAction {
    private GanttProject myMainFrame;

    OpenProjectAction(GanttProject mainFrame) {
        super("openProject", "16");
        myMainFrame = mainFrame;
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_MASK));
    }
    protected String getIconFilePrefix() {
        return "open_";
    }

    public void actionPerformed(ActionEvent e) {
        try {
            myMainFrame.openFile();
        } catch (Exception ex) {
            myMainFrame.getUIFacade().showErrorDialog(ex);
        }
    }
    protected String getLocalizedName() {
        return getI18n("openProject");
    }

}
