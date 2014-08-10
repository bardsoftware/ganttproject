/*
Copyright 2005-2012 GanttProject Team

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
package net.sourceforge.ganttproject.export;

import java.awt.Component;
import java.io.File;
import java.util.List;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;

import org.osgi.service.prefs.Preferences;

import biz.ganttproject.core.option.GPOptionGroup;

/**
 * @author bard
 */
public interface Exporter {
  String getFileTypeDescription();

  GPOptionGroup getOptions();

  List<GPOptionGroup> getSecondaryOptions();

  String getFileNamePattern();

  void run(File outputFile, ExportFinalizationJob finalizationJob) throws Exception;

  // File proposeOutputFile(IGanttProject project);
  String proposeFileExtension();

  String[] getFileExtensions();

  String[] getCommandLineKeys();

  void setContext(IGanttProject project, UIFacade uiFacade, Preferences prefs);

  Component getCustomOptionsUI();
}
