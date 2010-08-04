/*
 * Created on 02.05.2005
 */
package org.ganttproject.impex.msproject;

import java.awt.Component;
import java.io.File;
import java.util.Arrays;
import org.osgi.service.prefs.Preferences;

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
 * @author bard
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

    //private Locale myLocale;


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
        myLanguageOption.lock();
        myLanguageOption.setSelectedLocale(GanttLanguage.getInstance().getLocale());
        myLanguageOption.commit();
    }
    public void run(final File outputFile, ExportFinalizationJob finalizationJob) throws Exception {
        ClassLoader contextClassLoader = Thread.currentThread()
                .getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                    getClass().getClassLoader());
            myUIFacade.setStatusText("msproject-export");
            if (FILE_FORMAT_IDS[0].equals(myFileFormat)) {
                if (myLanguageOption.getSelectedLocale() != null) {
                    GanttMPXSaver saver = new GanttMPXSaver(myProject,
                            myLanguageOption.getSelectedLocale());
                    saver.save(outputFile);
                }
            }
            else if (FILE_FORMAT_IDS[1].equals(myFileFormat)) {
                GanttMSPDISaver saver = new GanttMSPDISaver(myProject);
                saver.save(outputFile);
            }
            finalizationJob.run(new File[] { outputFile });
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
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
