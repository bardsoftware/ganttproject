/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2012 GanttProject Team

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
package net.sourceforge.ganttproject.export;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

import javax.swing.SwingUtilities;

import org.osgi.service.prefs.Preferences;

import biz.ganttproject.core.option.BooleanOption;
import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.DefaultBooleanOption;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.projectwizard.WizardImpl;
import net.sourceforge.ganttproject.plugins.PluginManager;

/**
 * @author bard
 */
public class ExportFileWizardImpl extends WizardImpl {

  private final IGanttProject myProject;

  private final State myState;

  private static Exporter ourLastSelectedExporter;
  private static List<Exporter> ourExporters;

  public ExportFileWizardImpl(UIFacade uiFacade, IGanttProject project, Preferences pluginPreferences) {
    super(uiFacade, language.getText("exportWizard.dialog.title"));
    final Preferences exportNode = pluginPreferences.node("/instance/net.sourceforge.ganttproject/export");
    myProject = project;
    myState = new State();
    myState.myPublishInWebOption.setValue(exportNode.getBoolean("publishInWeb", false));
    myState.myPublishInWebOption.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        exportNode.putBoolean("publishInWeb", myState.myPublishInWebOption.getValue());
      }
    });
    if (ourExporters == null) {
      ourExporters = PluginManager.getExporters();
    }
    myState.setExporter(ourLastSelectedExporter == null ? ourExporters.get(0) : ourLastSelectedExporter);
    for (Exporter e : ourExporters) {
      e.setContext(project, uiFacade, pluginPreferences);
    }
    addPage(new ExporterChooserPage(ourExporters, myState));
    addPage(new FileChooserPage(myState, myProject, this, exportNode));
  }

  @Override
  protected boolean canFinish() {
    return myState.getExporter() != null && myState.myUrl != null && "file".equals(myState.getUrl().getProtocol());
  }

  @Override
  protected void onOkPressed() {
    super.onOkPressed();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          ExportFinalizationJob finalizationJob = new ExportFinalizationJobImpl();
          if ("file".equals(myState.getUrl().getProtocol())) {
            myState.getExporter().run(new File(myState.getUrl().toURI()), finalizationJob);
          }
        } catch (Exception e) {
          GPLogger.log(e);
        }
      }
    });
  }

  private class ExportFinalizationJobImpl implements ExportFinalizationJob {
    @Override
    public void run(File[] exportedFiles) {
      if (myState.getPublishInWebOption().isChecked() && exportedFiles.length > 0) {
        WebPublisher publisher = new WebPublisher();
        publisher.run(exportedFiles, myProject.getDocumentManager().getFTPOptions());
      }
    }
  }

  static class State {
    private Exporter myExporter;

    private final BooleanOption myPublishInWebOption = new DefaultBooleanOption("exporter.publishInWeb");

    private URL myUrl;

    void setExporter(Exporter exporter) {
      myExporter = exporter;
      ExportFileWizardImpl.ourLastSelectedExporter = exporter;
    }

    Exporter getExporter() {
      return myExporter;
    }

    BooleanOption getPublishInWebOption() {
      return myPublishInWebOption;
    }

    void setUrl(URL url) {
      myUrl = url;
    }

    public URL getUrl() {
      return myUrl;
    }
  }
}