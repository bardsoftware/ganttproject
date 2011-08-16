/*
 * Created on 09.10.2005
 */
package net.sourceforge.ganttproject.action.project;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;

class PrintAction extends GPAction {

    private final GanttProject myMainFrame;

    PrintAction(GanttProject mainFrame) {
        super("print");
        myMainFrame = mainFrame;
        putValue(Action.ACCELERATOR_KEY, KeyStroke
                .getKeyStroke(KeyEvent.VK_P, MENU_MASK));
    }

    protected String getLocalizedName() {
        return getI18n("printProject");
    }

    protected String getIconFilePrefix() {
        return "print_";
    }

    public void actionPerformed(ActionEvent e) {
        myMainFrame.printProject();
    }

}
