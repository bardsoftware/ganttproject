/*
 * Created on 07.10.2005
 */
package net.sourceforge.ganttproject.action.project;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class OpenURLAction extends GPAction {
    private final GanttProject myMainFrame;
    
    OpenURLAction(GanttProject mainFrame) {
        myMainFrame = mainFrame;
    }
    protected String getIconFilePrefix() {
        return null;
    }

    public void actionPerformed(ActionEvent e) {
		if (myMainFrame.getProjectUIFacade().ensureProjectSaved(myMainFrame)) {
            myMainFrame.openURL();
        }
    }

    protected String getLocalizedName() {
        return GanttLanguage.getInstance().correctLabel(getI18n("openFromServer"));
    }

    protected String getTooltipText() {
        return getLocalizedName();
    }

    
}
