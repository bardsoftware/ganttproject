/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import net.sourceforge.ganttproject.GanttOptions;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
import net.sourceforge.ganttproject.gui.projectwizard.WizardImpl;
import net.sourceforge.ganttproject.plugins.PluginManager;

/**
 * @author bard
 */
public class ExportFileWizardImpl extends WizardImpl {

    private final IGanttProject myProject;

    private final GanttOptions myOptions;

    private final State myState;

    private static Exporter ourLastSelectedExporter;
    private static List<Exporter> ourExporters;

    public ExportFileWizardImpl(UIFacade uiFacade, IGanttProject project,
            GanttOptions options) {
        this(uiFacade, project, uiFacade.getGanttChart(),
                uiFacade.getResourceChart(), uiFacade.getActiveChart(), options);
    }

    public ExportFileWizardImpl(UIFacade uiFacade,
            IGanttProject project, Chart ganttChart, Chart resourceChart,
            Chart visibleChart, GanttOptions options) {
        super(uiFacade, language.getText("exportWizard.dialog.title"));
        myProject = project;
        myOptions = options;
        myState = new State(project.getDocument());
        if (ourExporters == null) {
            ourExporters = PluginManager.getExporters();
        }
        myState.setExporter(ourLastSelectedExporter == null ?
                ourExporters.get(0) : ourLastSelectedExporter);
        for (Exporter e : ourExporters) {
            e.setContext(project, uiFacade, myOptions.getPluginPreferences());
            if (e instanceof LegacyOptionsClient) {
                ((LegacyOptionsClient)e).setOptions(myOptions);
            }
        }
        addPage(new ExporterChooserPage(ourExporters, myState));
        addPage(new FileChooserPage(
                myState,
                myProject,
                this,
                options.getPluginPreferences().node("/instance/net.sourceforge.ganttproject/export")));
    }

    @Override
    protected boolean canFinish() {
        return myState.getExporter() != null
            && myState.myUrl != null
            && "file".equals(myState.getUrl().getProtocol());
    }

    @Override
    protected void onOkPressed() {
        super.onOkPressed();
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                try {
                    ExportFinalizationJob finalizationJob = new ExportFinalizationJobImpl();
                    if ("file".equals(myState.getUrl().getProtocol())) {
                        String path = URLDecoder.decode(myState.getUrl().getPath(), "utf-8");
                        myState.getExporter().run(new File(path), finalizationJob);
                    }
                } catch (Exception e) {
                    getUIFacade().showErrorDialog(e);
                }
            }
        });
    }

    private class ExportFinalizationJobImpl implements ExportFinalizationJob {
        public void run(File[] exportedFiles) {
            if (myState.getPublishInWebOption().isChecked() && exportedFiles.length>0) {
                WebPublisher publisher = new WebPublisher();
                publisher.run(exportedFiles, myProject.getDocumentManager().getFTPOptions(), getUIFacade());
            }
        }
    }

    static class State {
        //final Document myProjectDocument;

        private Exporter myExporter;

        private final BooleanOption myPublishInWebOption = new DefaultBooleanOption("exporter.publishInWeb");

        private URL myUrl;

        State(Document projectDocument) {
            //myProjectDocument = projectDocument;
        }

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

    interface LegacyOptionsClient {
        void setOptions(GanttOptions options);
    }
}