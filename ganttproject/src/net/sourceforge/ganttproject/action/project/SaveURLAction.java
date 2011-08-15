/*
 * Created on 08.10.2005
 */
package net.sourceforge.ganttproject.action.project;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.language.GanttLanguage;

class SaveURLAction extends GPAction {
    private final GanttProject myMainFrame;

    SaveURLAction(GanttProject mainFrame){
        myMainFrame = mainFrame;
    }

    protected String getLocalizedName() {
        return GanttLanguage.getInstance().correctLabel(getI18n("saveToServer"));
    }

    protected String getIconFilePrefix() {
        return null;
    }

    public void actionPerformed(ActionEvent e) {
        try {
            myMainFrame.saveAsURLProject();
        } catch (Exception ex) {
            myMainFrame.getUIFacade().showErrorDialog(ex);
        }
    }

}
