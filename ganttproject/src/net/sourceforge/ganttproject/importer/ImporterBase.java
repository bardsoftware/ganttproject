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

import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.NotificationChannel;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.collect.Pair;
import net.sourceforge.ganttproject.wizard.WizardPage;
import org.osgi.service.prefs.Preferences;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

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
  private File myFile;

//  protected ImporterBase() {
//    myID = "";
//  }
//
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

  @Override
  public boolean isReady() {
    return myFile != null && myFile.exists() && myFile.canRead();
  }

  @Override
  public abstract void run();

  @Override
  public void setFile(File file) {
    myFile = file;
  }

  protected File getFile() {
    return myFile;
  }

  @Override
  public String getID() {
    return myID;
  }

  @Override
  public WizardPage getCustomPage() {
    return null;
  }

  protected void reportErrors(List<Pair<Level, String>> errors, String loggerName) {
    reportErrors(getUiFacade(), errors, loggerName);
  }

  public static void reportErrors(UIFacade uiFacade, List<Pair<Level, String>> errors, String loggerName) {
    if (!errors.isEmpty()) {
      StringBuilder builder = new StringBuilder("<table><tr><th>Severity</th><th>Message</th></tr>");
      for (Pair<Level, String> message : errors) {
        GPLogger.getLogger(loggerName).log(message.first(), message.second());
        builder.append(String.format("<tr><td valign=top><b>%s</b></td><td valign=top>%s</td></tr>", message.first().getName(), message.second()));
      }
      builder.append("</table>");
      uiFacade.showNotificationDialog(NotificationChannel.WARNING,
          GanttLanguage.getInstance().formatText("impex." + loggerName.toLowerCase() + ".importErrorReport", builder.toString()));
    }
  }
}
