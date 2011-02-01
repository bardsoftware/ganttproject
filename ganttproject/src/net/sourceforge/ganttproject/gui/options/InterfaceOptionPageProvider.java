package net.sourceforge.ganttproject.gui.options;

import java.awt.Component;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class InterfaceOptionPageProvider extends OptionPageProviderBase {

    public InterfaceOptionPageProvider() {
        super("ui.general");
    }
    @Override
    public GPOptionGroup[] getOptionGroups() {
        return new GPOptionGroup[] {getUiFacade().getOptions()};
    }
}
