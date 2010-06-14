package net.sourceforge.ganttproject.export;

import java.awt.Component;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.filechooser.FileFilter;

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

    private static URL ourLastSelectedUrl;

    private final State myState;

    private final IGanttProject myProject;

    private final GPOptionGroup myWebPublishingGroup;

    FileChooserPage(State state, IGanttProject project, WizardImpl wizardImpl, Preferences prefs) {
        super(wizardImpl, prefs, false);
        myState = state;
        myProject = project;
        myWebPublishingGroup = new GPOptionGroup("exporter.webPublishing", new GPOption[]{state.getPublishInWebOption()});
        myWebPublishingGroup.setTitled(false);
        //myOptionsBuilder = new OptionsPageBuilder();
    }

    protected String getFileChooserTitle() {
        return GanttLanguage.getInstance().getText("selectFileToExport");
    }

    public String getTitle() {
        return GanttLanguage.getInstance().getText("selectFileToExport");
    }

    protected void onFileChosen(File file) {
        String proposedExtension = myState.getExporter()
                .proposeFileExtension();
        if(proposedExtension != null) {
            if (false == file.getName().toLowerCase().endsWith(
                    proposedExtension)) {
                file = new File(file.getAbsolutePath() + "."
                        + proposedExtension);
            }
        }
        try {
            myState.setUrl(file.toURI().toURL());
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        super.onFileChosen(file);
    }

    public void setActive(boolean b) {
        if (b == false) {
            URL url = getSelectedUrl();
            myState.setUrl(url);
            FileChooserPage.ourLastSelectedUrl = url;
            super.setActive(b);
        } else {
            super.setActive(b);
            URL proposedUrl = proposeOutputUrl(myProject);
            if (proposedUrl == null) {
                setSelectedUrl(myState.getUrl());
                return;
            }
            if (false == proposedUrl.equals(getSelectedUrl())) {
                setSelectedUrl(proposedUrl);
                myState.setUrl(proposedUrl);
            }
        }
    }

    protected Component createSecondaryOptionsPanel() {
        Component customUI = myState.getExporter().getCustomOptionsUI();
        return customUI == null ? super.createSecondaryOptionsPanel() : customUI;
    }

    public URL proposeOutputUrl(IGanttProject project) {
        return FileChooserPage.proposeOutputUrl(project, myState.getExporter());
    }

    static URL proposeOutputUrl(IGanttProject project, Exporter exporter) {
        String proposedExtension = exporter.proposeFileExtension();
        if (proposedExtension == null) {
            return null;
        }
        if (FileChooserPage.ourLastSelectedUrl != null) {
            String name = FileChooserPage.ourLastSelectedUrl.getPath();
            int lastDot = name.lastIndexOf('.');
            String extension = lastDot >=0 ? name.substring(lastDot + 1) : "";
            if (extension.equals(proposedExtension)) {
                return FileChooserPage.ourLastSelectedUrl;
            }
        }
        try {
            return FileChooserPage.proposeOutputFile(project, exporter).toURI().toURL();
        } catch (MalformedURLException e) {
            return null;
        }

    }

    static File proposeOutputFile(IGanttProject project, Exporter exporter) {
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
