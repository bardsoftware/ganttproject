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

import org.osgi.service.prefs.Preferences;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;

public abstract class ImporterBase implements Importer {
  private final String myID;
  private UIFacade myUiFacade;
  private IGanttProject myProject;
  protected static final GanttLanguage language = GanttLanguage.getInstance();

  /**
   * Do not remove: to be used when latest import locations get stored in
   * preferences
   */
  private Preferences myPrefs;

  protected ImporterBase() {
    myID = "";
  }

  protected ImporterBase(String id) {
    myID = id;
  }

  @Override
  public String getFileTypeDescription() {
    if (myID.length() == 0) {
      return null;
    }
    return language.getText(myID);
  }

  @Override
  public String getFileNamePattern() {
    return null;
  }

  @Override
  public GPOptionGroup[] getSecondaryOptions() {
    GPOption[] options = getOptions();
    if (options == null) {
      return new GPOptionGroup[0];
    }
    return new GPOptionGroup[] { new GPOptionGroup("importer." + myID, options) };
  }

  protected GPOption[] getOptions() {
    return null;
  }

  @Override
  public void setContext(IGanttProject project, UIFacade uiFacade, Preferences preferences) {
    myProject = project;
    myUiFacade = uiFacade;
    myPrefs = preferences;
  }

  protected UIFacade getUiFacade() {
    return myUiFacade;
  }

  protected IGanttProject getProject() {
    return myProject;
  }
}
