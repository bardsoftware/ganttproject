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
package net.sourceforge.ganttproject.action.project;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.ProjectEventListener;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;

import java.awt.event.ActionEvent;

public class SaveProjectAction extends GPAction implements ProjectEventListener {
  private final IGanttProject myProject;
  private final ProjectUIFacade myProjectUiFacade;

  SaveProjectAction(IGanttProject mainFrame, ProjectUIFacade projectFacade) {
    this(mainFrame, projectFacade, IconSize.MENU);
  }

  private SaveProjectAction(IGanttProject mainFrame, ProjectUIFacade projectFacade, IconSize size) {
    super("project.save", size);
    myProject = mainFrame;
    myProjectUiFacade = projectFacade;
    mainFrame.addProjectEventListener(this);
    setEnabled(false);
  }

  @Override
  public GPAction withIcon(IconSize size) {
    return new SaveProjectAction(myProject, myProjectUiFacade, size);
  }

  @Override
  protected String getIconFilePrefix() {
    return "save_";
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (calledFromAppleScreenMenu(e)) {
      return;
    }
    myProjectUiFacade.saveProject(myProject, null);
  }

  @Override
  public void projectModified() {
    setEnabled(true);
  }

  @Override
  public void projectSaved() {
    setEnabled(false);
  }

  @Override
  public void projectClosed() {
    setEnabled(false);
  }

  @Override
  public void projectCreated() {
    setEnabled(false);
  }

  @Override
  public void projectOpened() {
    setEnabled(false);
  }

  public SaveProjectAction asToolbarAction() {
    SaveProjectAction result = new SaveProjectAction(myProject, myProjectUiFacade);
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    return result;
  }
}
