/*
 * Created on 26.09.2005
 */
package net.sourceforge.ganttproject.action.project;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class NewProjectAction extends GPAction {
    private GanttProject myMainFrame;

    public NewProjectAction(GanttProject mainFrame) {
        super("newProject", "16");
        myMainFrame = mainFrame;
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, MENU_MASK));
    }
    @Override
    protected String getIconFilePrefix() {
        return "new_";
    }

    public void actionPerformed(ActionEvent e) {
        myMainFrame.newProject();
    }

    @Override
    protected String getLocalizedName() {
        return GanttLanguage.getInstance().getText("newProject");
    }



}
