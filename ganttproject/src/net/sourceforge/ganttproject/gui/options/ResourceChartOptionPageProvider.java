package net.sourceforge.ganttproject.gui.options;

import java.awt.Component;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class ResourceChartOptionPageProvider implements OptionPageProvider {

    public GPOptionGroup[] getOptionGroups(IGanttProject project,
            UIFacade uiFacade) {
        return uiFacade.getResourceChart().getOptionGroups();
    }

    public String getPageID() {
        return "resourceChart";
    }

    public String toString() {
        return GanttLanguage.getInstance().getText("resourcesChart");
    }
    
    public boolean hasCustomComponent() {
        return false;
    }
    public Component buildPageComponent(IGanttProject project, UIFacade uiFacade) {
        throw new UnsupportedOperationException();
    }
    

}
