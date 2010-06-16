package net.sourceforge.ganttproject.export;

import java.awt.Component;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import javax.swing.filechooser.FileFilter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.prefs.Preferences;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.export.ExportFileWizardImpl.State;
import net.sourceforge.ganttproject.filter.ExtensionBasedFileFilter;
import net.sourceforge.ganttproject.gui.FileChooserPageBase;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.projectwizard.WizardImpl;
import net.sourceforge.ganttproject.language.GanttLanguage;

class FileChooserPage extends FileChooserPageBase {

    private final State myState;

    private final IGanttProject myProject;

    private final GPOptionGroup myWebPublishingGroup;

    FileChooserPage(State state, IGanttProject project, WizardImpl wizardImpl, Preferences prefs) {
        super(wizardImpl, prefs, false);
        myState = state;
        myProject = project;
        myWebPublishingGroup = new GPOptionGroup("exporter.webPublishing", new GPOption[]{state.getPublishInWebOption()});
        myWebPublishingGroup.setTitled(false);
    }

    protected String getFileChooserTitle() {
        return GanttLanguage.getInstance().getText("selectFileToExport");
    }

    public String getTitle() {
        return GanttLanguage.getInstance().getText("selectFileToExport");
    }

    protected void loadPreferences() {
        super.loadPreferences();
        if (getPreferences().get(PREF_SELECTED_FILE, null) == null) {
            getChooser().setFile(proposeOutputFile(myProject, myState.getExporter()));
        } else {
            String proposedExtension = myState.getExporter().proposeFileExtension();
            if(proposedExtension != null) {
                String selectedFile = getPreferences().get(PREF_SELECTED_FILE, null);
                int lastDot = selectedFile.lastIndexOf('.');
                String extension = lastDot >=0 ? selectedFile.substring(lastDot + 1) : "";
                if (!extension.equals(proposedExtension)) {
                    getChooser().setFile(new File(selectedFile.substring(0, lastDot+1) + proposedExtension));
                    return;
                }
            }
            getChooser().setFile(new File(getPreferences().get(PREF_SELECTED_FILE, null)));
        }
    }

    protected void onSelectedUrlChange(URL selectedUrl) {
        myState.setUrl(selectedUrl);
        super.onSelectedUrlChange(selectedUrl);
    }

    protected IStatus onSelectedFileChange(File file) {
        if (file.exists() && !file.canWrite()) {
            return new Status(IStatus.ERROR, "foo", "Can't write to file");
        }
        if (!file.exists() && !file.getParentFile().canWrite()) {
            return new Status(IStatus.ERROR, "foo", "Can't write to directory");
        }
        IStatus result = new Status(IStatus.OK, "foo", "");
        String proposedExtension = myState.getExporter().proposeFileExtension();
        if(proposedExtension != null) {
            if (false == file.getName().toLowerCase().endsWith(proposedExtension)) {
                result = new Status(IStatus.OK, "foo", MessageFormat.format("Note that the extension is not {0}", proposedExtension));
            }
        }
        IStatus setStatus = setSelectedFile(file);
        return setStatus.isOK() ? result : setStatus;
    }

    protected Component createSecondaryOptionsPanel() {
        Component customUI = myState.getExporter().getCustomOptionsUI();
        return customUI == null ? super.createSecondaryOptionsPanel() : customUI;
    }

    private static File proposeOutputFile(IGanttProject project, Exporter exporter) {
        String proposedExtension = exporter.proposeFileExtension();
        if (proposedExtension == null) {
            return null;
        }
        File userHome = new File(System.getProperty("user.home"));
        File result = new File(userHome, project.getProjectName() + "."
                + proposedExtension);
        Document projectDocument = project.getDocument();
        if (projectDocument != null) {
            File localFile = new File(projectDocument.getFilePath());
            if (localFile.exists()) {
                String name = localFile.getAbsolutePath();
                int lastDot = name.lastIndexOf('.');
                name = name.substring(0, lastDot) + "." + proposedExtension;
                result = new File(name);
            } else {
                File directory = localFile.getParentFile();
                if (directory.exists()) {
                    result = new File(directory, project.getProjectName() + "."
                            + proposedExtension);
                }
            }
        }
        return result;
    }

    protected FileFilter createFileFilter() {
        return new ExtensionBasedFileFilter(
                myState.getExporter().getFileNamePattern(), myState.getExporter()
                        .getFileTypeDescription());
    }

    protected GPOptionGroup[] getOptionGroups() {
        GPOptionGroup[] exporterOptions = null;
        if (myState.getExporter()!=null) {
            exporterOptions = myState.getExporter().getSecondaryOptions();
        }
        if (exporterOptions==null) {
            return new GPOptionGroup[] {myWebPublishingGroup};
        }
        GPOptionGroup[] result = new GPOptionGroup[exporterOptions.length+1];
        result[0] = myWebPublishingGroup;
        System.arraycopy(exporterOptions, 0, result, 1, exporterOptions.length);
        return result;
    }
}
