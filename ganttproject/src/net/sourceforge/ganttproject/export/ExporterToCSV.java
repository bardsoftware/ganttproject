/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev

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

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import net.sourceforge.ganttproject.GanttOptions;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.io.GanttCSVExport;
import net.sourceforge.ganttproject.language.GanttLanguage;

import org.osgi.service.prefs.Preferences;

public class ExporterToCSV implements Exporter, ExportFileWizardImpl.LegacyOptionsClient {

    private static String[] FILE_EXTENSIONS = new String[] {"csv"};
    private IGanttProject myProject;
    private GanttOptions myOptions;

    @Override
    public String getFileTypeDescription() {
        return GanttLanguage.getInstance().getText("impex.csv.description");
    }

    @Override
    public GPOptionGroup getOptions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<GPOptionGroup> getSecondaryOptions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Component getCustomOptionsUI() {
        return null;
    }

    @Override
    public String getFileNamePattern() {
        return ExporterToCSV.FILE_EXTENSIONS[0];
    }

    @Override
    public void run(File outputFile, ExportFinalizationJob finalizationJob) throws Exception {
        outputFile.createNewFile();
        GanttCSVExport legacyExporter = new GanttCSVExport(myProject, myOptions.getCSVOptions());
        legacyExporter.save(new FileOutputStream(outputFile));
        finalizationJob.run(new File[] {outputFile});
    }

    @Override
    public String proposeFileExtension() {
        return ExporterToCSV.FILE_EXTENSIONS[0];
    }

    @Override
    public String[] getFileExtensions() {
        return ExporterToCSV.FILE_EXTENSIONS;
    }

    @Override
    public String[] getCommandLineKeys() {
        return ExporterToCSV.FILE_EXTENSIONS;
    }

    @Override
    public void setContext(IGanttProject project, UIFacade uiFacade, Preferences prefs) {
        myProject = project;
    }

    @Override
    public void setOptions(GanttOptions options) {
        myOptions = options;
    }
}
