package net.sourceforge.ganttproject.gui.options;

import java.awt.Component;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class GanttChartOptionPageProvider implements OptionPageProvider {

    private IGanttProject myProject;
    private UIFacade myUiFacade;

    @Override
    public void init(IGanttProject project, UIFacade uiFacade) {
        myProject = project;
        myUiFacade = uiFacade;
    }

    public GPOptionGroup[] getOptionGroups() {
        return myUiFacade.getGanttChart().getOptionGroups();
    }

    public String getPageID() {
        return "ganttChart";
    }

    public String toString() {
        return GanttLanguage.getInstance().getText("ganttChart");
    }
    
    public boolean hasCustomComponent() {
        return false;
    }

    @Override
    public Component buildPageComponent() {
        throw new UnsupportedOperationException();
    }    

}
