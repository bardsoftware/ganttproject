package net.sourceforge.ganttproject.gui.projectwizard;

import java.awt.Component;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.options.ProjectSettingsPanel;

public class ProjectNamePage implements WizardPage {
    private final I18N myI18N;

    private final ProjectSettingsPanel myProjectSettingsPanel;

    public ProjectNamePage(IGanttProject project, I18N i18n) {
        myProjectSettingsPanel = new ProjectSettingsPanel(project);
        myProjectSettingsPanel.initialize();
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
