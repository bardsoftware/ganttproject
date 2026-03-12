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
import java.util.List;

import javax.swing.SwingUtilities;

import biz.ganttproject.app.InternationalizationCoreKt;
import kotlin.Unit;
import net.sourceforge.ganttproject.gui.projectwizard.WizardImplFxKt;
import org.osgi.service.prefs.Preferences;

import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.plugins.PluginManager;

import static net.sourceforge.ganttproject.export.ExportFileWizardImplKt.getOurLastSelectedExporter;


/**
 * @author bard
 */
public class ExportFileWizardImpl {

  private final IGanttProject myProject;


  private static List<Exporter> ourExporters;
  private final ExportWizardModel wizardModel = new ExportWizardModel(
    "wizard.export", InternationalizationCoreKt.getRootLocalizer().formatText("exportWizard.dialog.title"));

  public ExportFileWizardImpl(UIFacade uiFacade, IGanttProject project, Preferences pluginPreferences) {
    final Preferences exportNode = pluginPreferences.node("/instance/net.sourceforge.ganttproject/export");
    myProject = project;
    wizardModel.getPublishInWebOption().setValue(exportNode.getBoolean("publishInWeb", false));
    wizardModel.getPublishInWebOption().addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        exportNode.putBoolean("publishInWeb", wizardModel.getPublishInWebOption().getValue());
      }
    });
    if (ourExporters == null) {
      ourExporters = PluginManager.getExporters();
    }
    wizardModel.setExporter(getOurLastSelectedExporter() == null ? ourExporters.get(0) : getOurLastSelectedExporter());
    for (Exporter e : ourExporters) {
      e.setContext(project, uiFacade, pluginPreferences);
    }

    var fileChooserPage = new ExportFileChooserPage(wizardModel, myProject, exportNode);
    wizardModel.addPage(new ExporterChooserPageFx(ourExporters, wizardModel));
    wizardModel.addPage(fileChooserPage);
    wizardModel.setOnOk(() -> {
      onOkPressed();
      return Unit.INSTANCE;
    });
  }

  protected void onOkPressed() {
    SwingUtilities.invokeLater(() -> {
      try {
        ExportFinalizationJob finalizationJob = new ExportFinalizationJobImpl();
        wizardModel.getExporter().run(wizardModel.getFile(), finalizationJob);
      } catch (Exception e) {
        GPLogger.log(e);
      }
    });
  }

  public void show() {
    WizardImplFxKt.showWizard(wizardModel);
  }

  private class ExportFinalizationJobImpl implements ExportFinalizationJob {
    @Override
    public void run(File[] exportedFiles) {
      if (wizardModel.getPublishInWebOption().isChecked() && exportedFiles.length > 0) {
        WebPublisher publisher = new WebPublisher();
        publisher.run(exportedFiles, myProject.getDocumentManager().getFTPOptions());
      }
    }
  }

}