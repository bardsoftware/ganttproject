/*
 * Created on 05.10.2005
 */
package net.sourceforge.ganttproject.action.project;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;

class ExitAction extends GPAction {
    private final GanttProject myMainFrame;

    ExitAction(GanttProject mainFrame) {
        super("quit");
        myMainFrame = mainFrame;
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MASK));
    }

    protected String getLocalizedName() {
        return getI18n("quit");
    }

    protected String getTooltipText() {
        return getI18n("quit");
    }


    protected String getIconFilePrefix() {
        return "exit_";
    }

    public void actionPerformed(ActionEvent e) {
        myMainFrame.quitApplication();
    }

}
