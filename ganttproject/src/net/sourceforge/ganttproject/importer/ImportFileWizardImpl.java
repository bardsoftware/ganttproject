/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

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
package net.sourceforge.ganttproject.importer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttOptions;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.projectwizard.WizardImpl;
import net.sourceforge.ganttproject.gui.projectwizard.WizardPage;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.plugins.PluginManager;
import net.sourceforge.ganttproject.wizard.AbstractWizard;

/**
 * @author bard
 */
public class ImportFileWizardImpl extends AbstractWizard {
  private static List<Importer> ourImporters = getImporters();

  private static GanttLanguage i18n = GanttLanguage.getInstance();

  public ImportFileWizardImpl(UIFacade uiFacade, IGanttProject project, GanttOptions options) {
    super(uiFacade, i18n.getText("importWizard.dialog.title"),
        new ImporterChooserPage(ourImporters, uiFacade, options.getPluginPreferences().node("/instance/net.sourceforge.ganttproject/import")));
    for (Importer importer : ourImporters) {
      importer.setContext(project, uiFacade, options.getPluginPreferences());
    }
  }

  private static List<Importer> getImporters() {
    return PluginManager.getExtensions(Importer.EXTENSION_POINT_ID, Importer.class);
  }

//  @Override
//  protected void onOkPressed() {
//    super.onOkPressed();
//    try {
//      myState.getImporter().run();
//    } catch (Throwable e) {
//      GPLogger.log(e);
//    }
//  }

//  class State {
//    private Importer myImporter;
//
//    private URL myUrl;
//
//    public void setUrl(URL url) {
//      if (url == null) {
//        return;
//      }
//      myUrl = url;
//      if ("file".equals(url.getProtocol())) {
//        try {
//          String path = URLDecoder.decode(url.getPath(), "utf-8");
//          myState.getImporter().setFile(new File(path));
//        } catch (UnsupportedEncodingException e) {
//          GPLogger.log(e);
//        }
//      } else {
//        GPLogger.logToLogger(new Exception(String.format("URL=%s is not a file", url.toString())));
//      }
//    }
//
//    public URL getUrl() {
//      return myUrl;
//    }
//
//    Importer getImporter() {
//      return myImporter;
//    }
//
//    void setImporter(Importer importer) {
//      myImporter = importer;
//      addImporterPages(importer);
//    }
//  }
}
