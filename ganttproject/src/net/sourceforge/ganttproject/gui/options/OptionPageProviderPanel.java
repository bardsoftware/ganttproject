/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.gui.options;

import java.awt.Component;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;

public class OptionPageProviderPanel {
    private final OptionPageProvider myProvider;
    private final GPOptionGroup[] myGroups;

    public OptionPageProviderPanel(OptionPageProvider provider, IGanttProject project, UIFacade uiFacade) {
        myProvider = provider;
        provider.init(project, uiFacade);
        myGroups = myProvider.getOptionGroups();
    }

    public Component getComponent() {
        if (myProvider.hasCustomComponent()) {
            return myProvider.buildPageComponent();
        }
        final OptionsPageBuilder builder = new OptionsPageBuilder();
        return builder.buildPage(myGroups, myProvider.getPageID());
    }

//    public boolean applyChanges(boolean askForApply) {
//        for (int i=0; i<myGroups.length; i++) {
//            myGroups[i].commit();
//        }
//        return true;
//    }

    public void initialize() {
        for (int i = 0; i < myGroups.length; i++) {
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
