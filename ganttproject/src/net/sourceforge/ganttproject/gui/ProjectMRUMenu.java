/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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
package net.sourceforge.ganttproject.gui;

import java.util.Collection;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.project.OpenMRUDocumentAction;
import net.sourceforge.ganttproject.document.DocumentMRUListener;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;

/**
 * Menu that shows the Most Recently Used documents. It is a regular menu,
 * except is implements the DocumentsMRUListener interface. When it is added to
 * the DocumentsMRU listeners the menu is kept up to date with the MRU list. The
 * automatically added menu items open their corresponding document when
 * clicked.
 */
public class ProjectMRUMenu extends JMenu implements DocumentMRUListener {
  private final IGanttProject myProject;
  private final UIFacade myUIFacade;
  private final ProjectUIFacade myProjectUIFacade;

  public ProjectMRUMenu(IGanttProject project, UIFacade uiFacade, ProjectUIFacade projectUIFacade, String key) {
    super(GPAction.createVoidAction(key));
    myProject = project;
    myUIFacade = uiFacade;
    myProjectUIFacade = projectUIFacade;
    setToolTipText(null);
  }

  @Override
  public JMenuItem add(Action a) {
    JMenuItem result = super.add(a);
    result.setToolTipText(null);
    return result;
  }

  @Override
  public void mruListChanged(Collection<String> newMRUList) {
    removeAll();
    int index = 0;
    for (String doc : newMRUList) {
      index++;
      add(new OpenMRUDocumentAction(index, doc, myProject, myUIFacade, myProjectUIFacade));
    }
  }
}
