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
import java.util.List;

import org.osgi.service.prefs.Preferences;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.projectwizard.WizardPage;

public interface Importer {
  String getFileTypeDescription();

  String getFileNamePattern();

  GPOptionGroup[] getSecondaryOptions();

  void run(File selectedFile);

  String EXTENSION_POINT_ID = "net.sourceforge.ganttproject.importer";

  void setContext(IGanttProject project, UIFacade uiFacade, Preferences pluginPreferences);

  /**
   * @return a list of additional wizard pages, required for the importer, or
   *         null if no additional pages are available
   */
  List<WizardPage> getAdditionalPages();
}
