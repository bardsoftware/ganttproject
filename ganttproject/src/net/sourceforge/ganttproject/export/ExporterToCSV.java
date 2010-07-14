/*
 * Created on 17.12.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.ganttproject.export;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;

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

	public String getFileTypeDescription() {
        return GanttLanguage.getInstance().getText("impex.csv.description");
	}

	public GPOptionGroup getOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	public GPOptionGroup[] getSecondaryOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	public Component getCustomOptionsUI() {
        return null;
    }

    public String getFileNamePattern() {
		return ExporterToCSV.FILE_EXTENSIONS[0];
	}

	public void run(File outputFile, ExportFinalizationJob finalizationJob) throws Exception {
		// TODO Auto-generated method stub
		if (!outputFile.exists()) {
			outputFile.createNewFile();
		}
		GanttCSVExport legacyExporter = new GanttCSVExport(myProject, myOptions.getCSVOptions());
		legacyExporter.save(new FileOutputStream(outputFile));
        finalizationJob.run(new File[] {outputFile});
	}

	public String proposeFileExtension() {
		return ExporterToCSV.FILE_EXTENSIONS[0];
	}

	public String[] getFileExtensions() {
		return ExporterToCSV.FILE_EXTENSIONS;
	}

	public void setContext(IGanttProject project, UIFacade uiFacade, Preferences prefs) {
		myProject = project;
	}

	public void setOptions(GanttOptions options) {
		myOptions = options;
	}

}
