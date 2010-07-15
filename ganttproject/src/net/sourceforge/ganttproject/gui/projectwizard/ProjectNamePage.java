package net.sourceforge.ganttproject.gui.projectwizard;

import java.awt.Component;
import java.awt.Frame;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.options.ProjectSettingsPanel;

public class ProjectNamePage implements WizardPage {
    private final I18N myI18N;

    private ProjectSettingsPanel myProjectSettingsPanel;

    public ProjectNamePage(Frame owner, IGanttProject project, I18N i18n) {
        myProjectSettingsPanel = new ProjectSettingsPanel(owner, project);
        myI18N = i18n;
    }

    public String getTitle() {
        return myI18N.getNewProjectWizardWindowTitle();
    }

    public Component getComponent() {
        return myProjectSettingsPanel;
    }

    public void setActive(boolean active) {
        if (!active) {
            myProjectSettingsPanel.applyChanges(false);
        }
    }

}
