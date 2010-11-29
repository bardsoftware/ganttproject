package net.sourceforge.ganttproject.gui.projectwizard;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.PrjInfos;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.roles.RoleSet;

public class NewProjectWizard {

    public PrjInfos createNewProject(IGanttProject project, UIFacade uiFacade) {
        RoleSet[] roleSets = project.getRoleManager().getRoleSets();
        NewProjectWizardWindow newProjectWizard = new NewProjectWizardWindow(
                uiFacade, new I18N());
        newProjectWizard.addProjectNamePage(project);
        newProjectWizard.addRoleSetPage(roleSets);
        newProjectWizard.addWeekendConfigurationPage(project
                .getActiveCalendar(), project);
        newProjectWizard.show();
        return new PrjInfos();
    }

}
