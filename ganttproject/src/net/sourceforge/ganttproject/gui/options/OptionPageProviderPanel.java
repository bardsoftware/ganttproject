package net.sourceforge.ganttproject.gui.options;

import java.awt.Component;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;

public class OptionPageProviderPanel extends GeneralOptionPanel{
    private OptionPageProvider myProvider;
    private IGanttProject myProject;
    private UIFacade myUIFacade;
    private GPOptionGroup[] myGroups;

    public OptionPageProviderPanel(OptionPageProvider provider, IGanttProject project, UIFacade uiFacade) {
        super("", "");
        myProject = project;
        myUIFacade = uiFacade;
        myProvider = provider;
        myGroups = myProvider.getOptionGroups(project, uiFacade);
    }


    public Component getComponent() {
        if (myProvider.hasCustomComponent()) {
            return myProvider.buildPageComponent(myProject, myUIFacade);
        }
        OptionsPageBuilder builder = new OptionsPageBuilder();
        return builder.buildPage(myGroups, myProvider.getPageID());
    }


    public boolean applyChanges(boolean askForApply) {
        for (int i=0; i<myGroups.length; i++) {
            myGroups[i].commit();
        }
        return true;
    }

    public void initialize() {
        for (int i=0; i<myGroups.length; i++) {
            myGroups[i].lock();
        }
    }


    public void rollback() {
        for (int i=0; i<myGroups.length; i++) {
            myGroups[i].rollback();
            myGroups[i].lock();
        }
    }



}
