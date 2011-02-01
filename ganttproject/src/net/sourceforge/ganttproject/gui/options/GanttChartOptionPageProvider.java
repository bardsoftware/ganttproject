package net.sourceforge.ganttproject.gui.options;

import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class GanttChartOptionPageProvider extends OptionPageProviderBase {

    public GanttChartOptionPageProvider() {
        super("ganttChart");
    }

    public GPOptionGroup[] getOptionGroups() {
        return getUiFacade().getGanttChart().getOptionGroups();
    }

    public String getPageID() {
        return "ganttChart";
    }

    public String toString() {
        return GanttLanguage.getInstance().getText("ganttChart");
    }    
}
