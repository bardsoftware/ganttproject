/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.importer;

import java.io.File;

import org.osgi.service.prefs.Preferences;

import biz.ganttproject.core.option.GPOptionGroup;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.wizard.WizardPage;

public interface Importer {
  String getID();
  String getFileTypeDescription();

  String getFileNamePattern();

  GPOptionGroup[] getSecondaryOptions();

  String EXTENSION_POINT_ID = "net.sourceforge.ganttproject.importer";

  void setContext(IGanttProject project, UIFacade uiFacade, Preferences pluginPreferences);

  boolean isReady();
  void run();

  void setFile(File file);

  WizardPage getCustomPage();
}
