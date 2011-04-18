package net.sourceforge.ganttproject.gui.options.model;

import java.awt.Component;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;

public interface OptionPageProvider {
    GPOptionGroup[] getOptionGroups();

    String getPageID();
    
    boolean hasCustomComponent();
    Component buildPageComponent();

    void init(IGanttProject project, UIFacade uiFacade);
    
    void commit();
}
