/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Alexandre Thomas, Dmitry Barashev

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
package net.sourceforge.ganttproject.gui.projectwizard;

import java.awt.Component;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.options.ProjectSettingsPanel;

public class ProjectNamePage implements WizardPage {
  private final I18N myI18N;

  private final ProjectSettingsPanel myProjectSettingsPanel;

  public ProjectNamePage(IGanttProject project, I18N i18n) {
    myProjectSettingsPanel = new ProjectSettingsPanel(project);
    myProjectSettingsPanel.initialize();
    myI18N = i18n;
  }

  @Override
  public String getTitle() {
    return myI18N.getNewProjectWizardWindowTitle();
  }

  @Override
  public Component getComponent() {
    return myProjectSettingsPanel.getComponent();
  }

  @Override
  public void setActive(boolean active) {
    if (!active) {
      myProjectSettingsPanel.applyChanges(false);
    }
  }
}
