package net.sourceforge.ganttproject.importer;

import java.io.File;

import org.osgi.service.prefs.Preferences;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;

public interface Importer {
    String getFileTypeDescription();

    String getFileNamePattern();

    GPOptionGroup[] getSecondaryOptions();

    void run(File selectedFile);

    String EXTENSION_POINT_ID = "net.sourceforge.ganttproject.importer";

    void setContext(IGanttProject project, UIFacade uiFacade, Preferences pluginPreferences);
}
