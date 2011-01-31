package net.sourceforge.ganttproject.gui.options;

import java.awt.Component;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class InterfaceOptionPageProvider implements OptionPageProvider {

    private IGanttProject myProject;
    private UIFacade myUiFacade;

    @Override
    public void init(IGanttProject project, UIFacade uiFacade) {
        myProject = project;
        myUiFacade = uiFacade;
    }

    @Override
    public GPOptionGroup[] getOptionGroups() {
        return new GPOptionGroup[] {myUiFacade.getOptions()};
    }

    @Override
    public String getPageID() {
        return "ui.general";
    }

    @Override
    public boolean hasCustomComponent() {
        return false;
    }

    @Override
    public Component buildPageComponent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return GanttLanguage.getInstance().getText(new OptionsPageBuilder.I18N().getCanonicalOptionPageTitleKey(getPageID()));
    }
}
