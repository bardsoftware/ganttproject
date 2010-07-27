package net.sourceforge.ganttproject.gui.projectwizard;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.roles.RoleSet;

public class NewProjectWizardWindow extends WizardImpl {
    private I18N myI18n;

    public NewProjectWizardWindow(UIFacade uiFacade, I18N i18n) {
        super(uiFacade, i18n.getNewProjectWizardWindowTitle());
        myI18n = i18n;
    }

    public void addRoleSetPage(RoleSet[] roleSets) {
        WizardPage roleSetPage = new RoleSetPage(roleSets, myI18n);
        addPage(roleSetPage);
    }

    public void addProjectNamePage(IGanttProject project) {
        WizardPage projectNamePage = new ProjectNamePage(null, project, myI18n);
        addPage(projectNamePage);
    }

    public void addWeekendConfigurationPage(GPCalendar calendar,
            IGanttProject project) {
        WizardPage weekendPage;
        try {
            weekendPage = new WeekendConfigurationPage(calendar, myI18n,
                    project, true);
            addPage(weekendPage);
        } catch (Exception e) {
            getUIFacade().showErrorDialog(e);
        }
    }

}
