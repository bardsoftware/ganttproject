/*
 * Created on 06.10.2005
 */
package net.sourceforge.ganttproject.action.project;

import java.awt.event.ActionEvent;
import java.io.IOException;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;

class SaveProjectAsAction extends GPAction {
    private GanttProject myMainFrame;

    SaveProjectAsAction(GanttProject mainFrame) {
        super("saveas");
        myMainFrame = mainFrame;
    }
    
    protected String getLocalizedName() {
        return getI18n("saveAsProject");
    }

    protected String getIconFilePrefix() {
        return "saveas_";
    }

    public void actionPerformed(ActionEvent e) {
        try {
            myMainFrame.saveAsProject();
        } catch (IOException e1) {
            myMainFrame.showErrorDialog(e1);
        }
    }

}
