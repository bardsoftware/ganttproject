/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2004-2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
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

import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;

import biz.ganttproject.core.option.GPOptionGroup;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;

public class OptionPageProviderPanel {
  private final OptionPageProvider myProvider;
  private final GPOptionGroup[] myGroups;
  private final UIFacade myUiFacade;

  public OptionPageProviderPanel(OptionPageProvider provider, IGanttProject project, UIFacade uiFacade) {
    myUiFacade = uiFacade;
    myProvider = provider;
    provider.init(project, uiFacade);
    myGroups = myProvider.getOptionGroups();
  }

  public Component getComponent() {
    JComponent providerComponent;
    if (myProvider.hasCustomComponent()) {
      providerComponent = (JComponent) myProvider.buildPageComponent();
    } else {
      OptionsPageBuilder builder = new OptionsPageBuilder();
      builder.setUiFacade(myUiFacade);
      providerComponent = builder.buildPage(myGroups, myProvider.getPageID());
    }
    providerComponent.setBorder(new EmptyBorder(5, 5, 5, 5));
    Component result = providerComponent;
    //JScrollPane result = new JScrollPane(providerComponent);
    return result;
  }

  // public boolean applyChanges(boolean askForApply) {
  // for (int i=0; i<myGroups.length; i++) {
  // myGroups[i].commit();
  // }
  // return true;
  // }

  public void initialize() {
    for (int i = 0; i < myGroups.length; i++) {
      myGroups[i].lock();
    }
  }

  // public void rollback() {
  // for (int i=0; i<myGroups.length; i++) {
  // myGroups[i].rollback();
  // myGroups[i].lock();
  // }
  // }
}
