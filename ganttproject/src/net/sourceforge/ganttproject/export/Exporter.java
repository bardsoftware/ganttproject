/*
 * Created on 02.05.2005
 */
package net.sourceforge.ganttproject.export;

import java.awt.Component;
import java.io.File;
import java.util.List;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;

import org.osgi.service.prefs.Preferences;

/**
 * @author bard
 */
public interface Exporter {
    String getFileTypeDescription();

    GPOptionGroup getOptions();

    List<GPOptionGroup> getSecondaryOptions();

    String getFileNamePattern();

    void run(File outputFile, ExportFinalizationJob finalizationJob) throws Exception;

    // File proposeOutputFile(IGanttProject project);
    String proposeFileExtension();

    String[] getFileExtensions();

    String[] getCommandLineKeys();

    void setContext(IGanttProject project, UIFacade uiFacade, Preferences prefs);

    Component getCustomOptionsUI();
}
