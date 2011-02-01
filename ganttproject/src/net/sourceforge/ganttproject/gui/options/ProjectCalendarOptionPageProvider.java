package net.sourceforge.ganttproject.gui.options;

import java.awt.Component;

import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;

public class ProjectCalendarOptionPageProvider extends OptionPageProviderBase {
    private WeekendsSettingsPanel myWeekendsPanel;
    public ProjectCalendarOptionPageProvider() {
        super("project.calendar");
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
        myWeekendsPanel = new WeekendsSettingsPanel(getProject());
        myWeekendsPanel.initialize();
        return OptionPageProviderBase.wrapContentComponent(
            myWeekendsPanel, myWeekendsPanel.getTitle(), myWeekendsPanel.getComment());
    }
    @Override
    public void commit() {
        myWeekendsPanel.applyChanges(false);
    }
}
