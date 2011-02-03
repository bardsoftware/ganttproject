package net.sourceforge.ganttproject.gui.options;

import java.awt.Component;

import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class ProjectRolesOptionPageProvider extends OptionPageProviderBase {
    private RolesSettingsPanel myRolesPanel;
    public ProjectRolesOptionPageProvider() {
        super("project.roles");
    }
    @Override
    public GPOptionGroup[] getOptionGroups() {
        return new GPOptionGroup[0];
    }
    @Override
    public boolean hasCustomComponent() {
        return true;
    }
    @Override
    public Component buildPageComponent() {
        myRolesPanel = new RolesSettingsPanel(getProject());
        myRolesPanel.initialize();
        return OptionPageProviderBase.wrapContentComponent(
            myRolesPanel, 
            GanttLanguage.getInstance().getText("resourceRole"), 
            GanttLanguage.getInstance().getText("settingsRoles"));
        
        
    }
    @Override
    public void commit() {
        myRolesPanel.applyChanges(false);
    }
}
