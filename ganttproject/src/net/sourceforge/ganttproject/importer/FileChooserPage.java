/*
 * Created on 01.05.2005
 */
package net.sourceforge.ganttproject.importer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.osgi.service.prefs.Preferences;

import net.sourceforge.ganttproject.filter.ExtensionBasedFileFilter;
import net.sourceforge.ganttproject.gui.FileChooserPageBase;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.projectwizard.WizardImpl;
import net.sourceforge.ganttproject.importer.ImportFileWizardImpl.State;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author bard
 */
class FileChooserPage extends FileChooserPageBase {

    private final State myState;

    public FileChooserPage(WizardImpl wizardImpl, Preferences prefs, State state) {
        super(wizardImpl, prefs);
        myState = state;
    }

    protected String getFileChooserTitle() {
        return GanttLanguage.getInstance().getText("importerFileChooserPageTitle");
    }

    protected int getFileChooserSelectionMode() {
        return JFileChooser.FILES_ONLY;
    }

    public String getTitle() {
        return GanttLanguage.getInstance().getText("importerFileChooserPageTitle");
    }

    protected FileFilter createFileFilter() {
        return new ExtensionBasedFileFilter(
                myState.myImporter.getFileNamePattern(), myState.myImporter.getFileTypeDescription());
    }

    protected GPOptionGroup[] getOptionGroups() {
        return myState.myImporter==null ? new GPOptionGroup[0] : myState.myImporter.getSecondaryOptions();
    }

    protected void onSelectedUrlChange(URL selectedUrl) {
        myState.setUrl(selectedUrl);
        super.onSelectedUrlChange(selectedUrl);
    }


}
