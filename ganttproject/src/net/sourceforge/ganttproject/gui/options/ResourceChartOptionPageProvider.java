package net.sourceforge.ganttproject.gui.options;

import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class ResourceChartOptionPageProvider extends OptionPageProviderBase {

    public ResourceChartOptionPageProvider() {
        super("resourceChart");
    }
    
    public GPOptionGroup[] getOptionGroups() {
        return getUiFacade().getResourceChart().getOptionGroups();
    }

    public String getPageID() {
        return "resourceChart";
    }

    public String toString() {
        return GanttLanguage.getInstance().getText("resourcesChart");
    }
}
