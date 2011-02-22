package net.sourceforge.ganttproject.gui.options;

import java.awt.Component;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;

public class OptionPageProviderPanel {
    private OptionPageProvider myProvider;
    private GPOptionGroup[] myGroups;

    public OptionPageProviderPanel(OptionPageProvider provider, IGanttProject project, UIFacade uiFacade) {
        myProvider = provider;
        provider.init(project, uiFacade);
        myGroups = myProvider.getOptionGroups();
    }


    public Component getComponent() {
        if (myProvider.hasCustomComponent()) {
            return myProvider.buildPageComponent();
        }
        OptionsPageBuilder builder = new OptionsPageBuilder();
        return builder.buildPage(myGroups, myProvider.getPageID());
    }


//    public boolean applyChanges(boolean askForApply) {
//        for (int i=0; i<myGroups.length; i++) {
//            myGroups[i].commit();
//        }
//        return true;
//    }

    public void initialize() {
        for (int i=0; i<myGroups.length; i++) {
            myGroups[i].lock();
        }
    }


//    public void rollback() {
//        for (int i=0; i<myGroups.length; i++) {
//            myGroups[i].rollback();
//            myGroups[i].lock();
//        }
//    }



}
