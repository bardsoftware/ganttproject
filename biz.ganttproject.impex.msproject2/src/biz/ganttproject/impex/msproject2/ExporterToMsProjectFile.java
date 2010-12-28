/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2010 Dmitry Barashev

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
package biz.ganttproject.impex.msproject2;

import java.awt.Component;
import java.io.File;
import java.util.Arrays;
import org.osgi.service.prefs.Preferences;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.mpx.MPXWriter;
import net.sf.mpxj.mspdi.MSPDIWriter;
import net.sf.mpxj.writer.ProjectWriter;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.export.ExportFinalizationJob;
import net.sourceforge.ganttproject.export.Exporter;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public class ExporterToMsProjectFile implements Exporter {

    private static final String[] FILE_FORMAT_IDS = new String[] {
            "impex.msproject.fileformat.mpx", 
            "impex.msproject.fileformat.mspdi" };

    private static final String[] FILE_EXTENSIONS = new String[] { "mpx","xml" };

    private String myFileFormat = FILE_FORMAT_IDS[0];

    private EnumerationOption myFileFormatOption = new DefaultEnumerationOption("impex.msproject.fileformat", FILE_FORMAT_IDS) {
        public void commit() {
            super.commit();
            ExporterToMsProjectFile.this.myFileFormat = getValue();
        }
    };

    private LocaleOption myLanguageOption = new LocaleOption();
    
    private GPOptionGroup myOptions = new GPOptionGroup("exporter.msproject",
            new GPOption[] { myFileFormatOption });

    private GPOptionGroup myMPXOptions = new GPOptionGroup("exporter.msproject.mpx", new GPOption[] {myLanguageOption});
    
    private IGanttProject myProject;

    private UIFacade myUIFacade;

    public ExporterToMsProjectFile() {
        myOptions.setTitled(false);
        myMPXOptions.setTitled(false);
        myFileFormatOption.lock();
        myFileFormatOption.setValue(FILE_FORMAT_IDS[0]);
        myFileFormatOption.commit();
    }

    public String getFileTypeDescription() {
        return GanttLanguage.getInstance().getText("impex.msproject.description");
    }

    public GPOptionGroup getOptions() {
        return myOptions;
    }

    public GPOptionGroup[] getSecondaryOptions() {
        return FILE_FORMAT_IDS[0].equals(myFileFormat) ? new GPOptionGroup[] {myMPXOptions} : null;
    }    
    
    public Component getCustomOptionsUI() {
        return null;
    }


    public String getFileNamePattern() {
        return myFileFormat;
    }

    public void setContext(IGanttProject project, UIFacade uiFacade, Preferences prefs) {
        myProject = project;
        myUIFacade = uiFacade;
        myLanguageOption = new LocaleOption();
        myMPXOptions = new GPOptionGroup("exporter.msproject.mpx", new GPOption[] {myLanguageOption});
        myLanguageOption.setSelectedLocale(GanttLanguage.getInstance().getLocale());
    }
    
    public void run(final File outputFile, ExportFinalizationJob finalizationJob) throws Exception {
        ProjectFile outProject = new ProjectFileExporter(myProject).run();
        ProjectWriter writer = createProjectWriter();
        writer.write(outProject, outputFile);
        finalizationJob.run(new File[] { outputFile });
    }

    private ProjectWriter createProjectWriter() {
        if (FILE_FORMAT_IDS[0].equals(myFileFormat)) {
        	MPXWriter result = new MPXWriter();
            if (myLanguageOption.getSelectedLocale() != null) {
            	result.setLocale(myLanguageOption.getSelectedLocale());
            }
            return result;
        }
        if (FILE_FORMAT_IDS[1].equals(myFileFormat)) {
        	return new MSPDIWriter();
        }
        assert false : "Should not be here";
        return null;
    }
    
    public String proposeFileExtension() {
        return getSelectedFormatExtension();
    }

    private String getSelectedFormatExtension() {
        for (int i = 0; i < FILE_FORMAT_IDS.length; i++) {
            if (myFileFormat.equals(FILE_FORMAT_IDS[i])) {
                return FILE_EXTENSIONS[i];
            }
        }
        throw new IllegalStateException("Selected format=" + myFileFormat
                + " has not been found in known formats:"
                + Arrays.asList(FILE_FORMAT_IDS));
    }
    
    public String[] getFileExtensions() {
        return FILE_EXTENSIONS;
    }
}
