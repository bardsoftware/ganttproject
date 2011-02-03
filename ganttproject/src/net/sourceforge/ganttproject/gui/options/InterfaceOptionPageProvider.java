package net.sourceforge.ganttproject.gui.options;

import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;

public class InterfaceOptionPageProvider extends OptionPageProviderBase {

    public InterfaceOptionPageProvider() {
        super("ui.general");
    }
    @Override
    public GPOptionGroup[] getOptionGroups() {
        return new GPOptionGroup[] {getUiFacade().getOptions()};
    }
}
