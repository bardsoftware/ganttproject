package net.sourceforge.ganttproject.gui.options;

import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;

public class GanttChartOptionPageProvider extends OptionPageProviderBase {

    public GanttChartOptionPageProvider() {
        super("ganttChart");
    }

    @Override
    public GPOptionGroup[] getOptionGroups() {
        return getUiFacade().getGanttChart().getOptionGroups();
    }
}
