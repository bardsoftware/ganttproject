/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 GanttProject Team

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
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;

import javax.swing.filechooser.FileFilter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.prefs.Preferences;

import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.export.ExportFileWizardImpl.State;
import net.sourceforge.ganttproject.filter.ExtensionBasedFileFilter;
import net.sourceforge.ganttproject.gui.FileChooserPageBase;
import net.sourceforge.ganttproject.gui.UIUtil;
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
    myWebPublishingGroup = new GPOptionGroup("exporter.webPublishing", new GPOption[] { state.getPublishInWebOption() });
    myWebPublishingGroup.setTitled(false);
  }

  @Override
  protected String getFileChooserTitle() {
    return GanttLanguage.getInstance().getText("selectFileToExport");
  }

  @Override
  public String getTitle() {
    return GanttLanguage.getInstance().getText("selectFileToExport");
  }

  @Override
  protected void loadPreferences() {
    super.loadPreferences();
    if (getPreferences().get(PREF_SELECTED_FILE, null) == null) {
      getChooser().setFile(proposeOutputFile(myProject, myState.getExporter()));
    } else {
      String proposedExtension = myState.getExporter().proposeFileExtension();
      if (proposedExtension != null) {
        File selectedFile = getChooser().getFile();
        String fileName = selectedFile.getName();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0) {
          // No extension available, add one
          fileName += ".";
          lastDot = selectedFile.getName().length();
        }
        String extension = fileName.substring(lastDot + 1);
        if (!extension.equals(proposedExtension)) {
          getChooser().setFile(
              new File(selectedFile.getParent(), fileName.substring(0, lastDot + 1) + proposedExtension));
        }
      }
    }
  }

  @Override
  protected void onSelectedUrlChange(URL selectedUrl) {
    myState.setUrl(selectedUrl);
    super.onSelectedUrlChange(selectedUrl);
  }

  @Override
  protected IStatus onSelectedFileChange(File file) {
    if (!file.exists()) {
      File parent = file.getParentFile();
      if (!parent.exists()) {
        return new Status(IStatus.ERROR, "foo", IStatus.ERROR,
            GanttLanguage.getInstance().formatText("fileChooser.error.directoryDoesNotExists", UIUtil.formatPathForLabel(parent)),
            null);
      }
      if (!parent.canWrite()) {
        return new Status(IStatus.ERROR, "foo", IStatus.ERROR,
            GanttLanguage.getInstance().formatText("fileChooser.error.directoryIsReadOnly", UIUtil.formatPathForLabel(parent)),
            null);
      }
    } else if (!file.canWrite()) {
      if (file.isDirectory()) {
        return new Status(IStatus.ERROR, "foo", IStatus.ERROR,
            GanttLanguage.getInstance().formatText("fileChooser.error.directoryIsReadOnly", UIUtil.formatPathForLabel(file)),
            null);
      } else {
        return new Status(IStatus.ERROR, "foo", IStatus.ERROR,
            GanttLanguage.getInstance().formatText("fileChooser.error.fileIsReadOnly", UIUtil.formatPathForLabel(file)),
            null);
      }
    }
    IStatus result = new Status(IStatus.OK, "foo", IStatus.OK, "", null);
    String proposedExtension = myState.getExporter().proposeFileExtension();
    if (proposedExtension != null) {
      if (false == file.getName().toLowerCase().endsWith(proposedExtension)) {
        result = new Status(IStatus.OK, "foo", IStatus.OK, MessageFormat.format("Note that the extension is not {0}",
            new Object[] { proposedExtension }), null);
      }
    }
    IStatus setStatus = setSelectedFile(file);
    return setStatus.isOK() ? result : setStatus;
  }

  @Override
  protected Component createSecondaryOptionsPanel() {
    Component customUI = myState.getExporter().getCustomOptionsUI();
    return customUI == null ? super.createSecondaryOptionsPanel() : customUI;
  }

  static File proposeOutputFile(IGanttProject project, Exporter exporter) {
    String proposedExtension = exporter.proposeFileExtension();
    if (proposedExtension == null) {
      return null;
    }

    File result = null;
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
          result = new File(directory, project.getProjectName() + "." + proposedExtension);
        }
      }
    }
    if (result == null) {
      File userHome = new File(System.getProperty("user.home"));
      result = new File(userHome, project.getProjectName() + "." + proposedExtension);
    }
    return result;
  }

  @Override
  protected FileFilter createFileFilter() {
    return new ExtensionBasedFileFilter(myState.getExporter().getFileNamePattern(),
        myState.getExporter().getFileTypeDescription());
  }

  @Override
  protected GPOptionGroup[] getOptionGroups() {
    GPOptionGroup[] exporterOptions = null;
    if (myState.getExporter() != null) {
      List<GPOptionGroup> options = myState.getExporter().getSecondaryOptions();
      exporterOptions = options == null ? null : options.toArray(new GPOptionGroup[0]);
    }
    if (exporterOptions == null) {
      return new GPOptionGroup[] { myWebPublishingGroup };
    }
    GPOptionGroup[] result = new GPOptionGroup[exporterOptions.length + 1];
    result[0] = myWebPublishingGroup;
    System.arraycopy(exporterOptions, 0, result, 1, exporterOptions.length);
    return result;
  }
}
