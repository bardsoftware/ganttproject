package net.sourceforge.ganttproject.importer;

import org.osgi.service.prefs.Preferences;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class ImporterBase {
    private final String myID;
    private UIFacade myUiFacade;
    private IGanttProject myProject;
    private Preferences myPrefs;

    protected ImporterBase() {
        myID = "";
    }
    protected ImporterBase(String id) {
        myID = id;
    }

    public String getFileTypeDescription() {
        if (myID.length() == 0) {
            return null;
        }
        return GanttLanguage.getInstance().getText(myID);
    }

    public String getFileNamePattern() {
        return null;
    }

    public GPOptionGroup[] getSecondaryOptions() {
        GPOption[] options = getOptions();
        if (options == null) {
            return new GPOptionGroup[0];
        }
        return new GPOptionGroup[] {new GPOptionGroup("importer." + myID, options)};
    }

    protected GPOption[] getOptions() {
        return null;
    }
    public void setContext(IGanttProject project, UIFacade uiFacade, Preferences preferences) {
        myProject = project;
        myUiFacade = uiFacade;
        myPrefs = preferences;
    }

    protected UIFacade getUiFacade() {
        return myUiFacade;
    }

    protected IGanttProject getProject() {
        return myProject;
    }
}
