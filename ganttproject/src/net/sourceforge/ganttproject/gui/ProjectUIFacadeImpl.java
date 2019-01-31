/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

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
package net.sourceforge.ganttproject.gui;

import biz.ganttproject.core.option.GPOptionGroup;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.filter.GanttXMLFileFilter;
import net.sourceforge.ganttproject.gui.projectwizard.NewProjectWizard;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.undo.GPUndoManager;
import net.sourceforge.ganttproject.util.FileUtil;
import org.eclipse.core.runtime.IStatus;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ProjectUIFacadeImpl implements ProjectUIFacade {
  final UIFacade myWorkbenchFacade;
  private final GanttLanguage i18n = GanttLanguage.getInstance();
  private final DocumentManager myDocumentManager;
  private final GPUndoManager myUndoManager;

  private final GPOptionGroup myConverterGroup = new GPOptionGroup("convert", ProjectOpenStrategy.getMilestonesOption());
  public ProjectUIFacadeImpl(UIFacade workbenchFacade, DocumentManager documentManager, GPUndoManager undoManager) {
    myWorkbenchFacade = workbenchFacade;
    myDocumentManager = documentManager;
    myUndoManager = undoManager;
  }

  @Override
  public void saveProject(IGanttProject project) {
    if (project.getDocument() == null) {
      saveProjectAs(project);
      return;
    }
    Document document = project.getDocument();
    saveProjectTryWrite(project, document);
  }

  private boolean saveProjectTryWrite(final IGanttProject project, final Document document) {
    IStatus canWrite = document.canWrite();
    if (!canWrite.isOK()) {
      GPLogger.getLogger(Document.class).log(Level.INFO, canWrite.getMessage(), canWrite.getException());
      String message = formatWriteStatusMessage(document, canWrite);
      List<Action> actions = new ArrayList<Action>();
      actions.add(new GPAction("project.saveas") {
        @Override
        public void actionPerformed(ActionEvent e) {
          saveProjectAs(project);
        }
      });
      if (canWrite.getCode() == Document.ErrorCode.LOST_UPDATE.ordinal()) {
        actions.add(new GPAction("document.overwrite") {
          @Override
          public void actionPerformed(ActionEvent e) {
            saveProjectTryLock(project, document);
          }
        });
      }
      actions.add(CancelAction.EMPTY);
      myWorkbenchFacade.showOptionDialog(JOptionPane.ERROR_MESSAGE, message, actions.toArray(new Action[0]));

      return false;
    }
    return saveProjectTryLock(project, document);
  }

  private boolean saveProjectTryLock(IGanttProject project, Document document) {
    return saveProjectTrySave(project, document);
  }

  private boolean saveProjectTrySave(IGanttProject project, Document document) {
    try {
      saveProject(document);
      afterSaveProject(project);
      return true;
    } catch (Throwable e) {
      myWorkbenchFacade.showErrorDialog(e);
      return false;
    }
  }

  private String formatWriteStatusMessage(Document doc, IStatus canWrite) {
    assert canWrite.getCode() >= 0 && canWrite.getCode() < Document.ErrorCode.values().length;
    Document.ErrorCode errorCode = Document.ErrorCode.values()[canWrite.getCode()];
    String key = "document.error.write." + errorCode.name().toLowerCase();
    return MessageFormat.format(i18n.getText(key), doc.getPath(), canWrite.getMessage());
  }

  private void afterSaveProject(IGanttProject project) {
    Document document = project.getDocument();
    myDocumentManager.addToRecentDocuments(document);
    String title = i18n.getText("appliTitle") + " [" + document.getFileName() + "]";
    myWorkbenchFacade.setWorkbenchTitle(title);
    if (document.isLocal()) {
      URI url = document.getURI();
      if (url != null) {
        File file = new File(url);
        myDocumentManager.changeWorkingDirectory(file.getParentFile());
      }
    }
    project.setModified(false);
  }

  private void saveProject(Document document) throws IOException {
    myWorkbenchFacade.setStatusText(GanttLanguage.getInstance().getText("saving") + " " + document.getPath());
    document.write();
  }

  @Override
  public void saveProjectAs(IGanttProject project) {
    /*
     * if (project.getDocument() instanceof AbstractURLDocument) {
     * saveProjectRemotely(project); return; }
     */
    JFileChooser fc = new JFileChooser(myDocumentManager.getWorkingDirectory());
    FileFilter ganttFilter = new GanttXMLFileFilter();
    fc.addChoosableFileFilter(ganttFilter);

    // Remove the possibility to use a file filter for all files
    FileFilter[] filefilters = fc.getChoosableFileFilters();
    for (int i = 0; i < filefilters.length; i++) {
      if (filefilters[i] != ganttFilter) {
        fc.removeChoosableFileFilter(filefilters[i]);
      }
    }

    try {
      for (;;) {
        int userChoice = fc.showSaveDialog(myWorkbenchFacade.getMainFrame());
        if (userChoice != JFileChooser.APPROVE_OPTION) {
          break;
        }
        File projectfile = fc.getSelectedFile();
        String extension = FileUtil.getExtension(projectfile).toLowerCase();
        if (!"gan".equals(extension) && !"xml".equals(extension)) {
          projectfile = FileUtil.appendExtension(projectfile, "gan");
        }

        if (projectfile.exists()) {
          UIFacade.Choice overwritingChoice = myWorkbenchFacade.showConfirmationDialog(
              projectfile + "\n" + i18n.getText("msg18"), i18n.getText("warning"));
          if (overwritingChoice != UIFacade.Choice.YES) {
            continue;
          }
        }

        Document document = myDocumentManager.getDocument(projectfile.getAbsolutePath());
        saveProject(document);
        project.setDocument(document);
        afterSaveProject(project);
        break;
      }
    } catch (Throwable e) {
      myWorkbenchFacade.showErrorDialog(e);
    }
  }

  /**
   * Check if the project has been modified, before creating or opening another
   * project
   *
   * @return true when the project is <b>not</b> modified or is allowed to be
   *         discarded
   */
  @Override
  public boolean ensureProjectSaved(IGanttProject project) {
    if (project.isModified()) {
      UIFacade.Choice saveChoice = myWorkbenchFacade.showConfirmationDialog(i18n.getText("msg1"),
          i18n.getText("warning"));
      if (UIFacade.Choice.CANCEL == saveChoice) {
        return false;
      }
      if (UIFacade.Choice.YES == saveChoice) {
        try {
          saveProject(project);
          // If all those complex save procedures complete successfully and project gets saved
          // then its modified state becomes false
          // Otherwise it remains true which means we have not saved and can't continue
          return !project.isModified();
        } catch (Exception e) {
          myWorkbenchFacade.showErrorDialog(e);
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public void openProject(final IGanttProject project) throws IOException, DocumentException {
    if (false == ensureProjectSaved(project)) {
      return;
    }
    JFileChooser fc = new JFileChooser(myDocumentManager.getWorkingDirectory());
    FileFilter ganttFilter = new GanttXMLFileFilter();

    // Remove the possibility to use a file filter for all files
    FileFilter[] filefilters = fc.getChoosableFileFilters();
    for (int i = 0; i < filefilters.length; i++) {
      fc.removeChoosableFileFilter(filefilters[i]);
    }
    fc.addChoosableFileFilter(ganttFilter);

    int returnVal = fc.showOpenDialog(myWorkbenchFacade.getMainFrame());
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      Document document = getDocumentManager().getDocument(fc.getSelectedFile().getAbsolutePath());
      openProject(document, project);
    }
  }

  @Override
  public void openProject(final Document document, final IGanttProject project) throws IOException, DocumentException {
    beforeClose();
    project.close();

    try (ProjectOpenStrategy strategy = new ProjectOpenStrategy(project, myWorkbenchFacade)) {
      strategy.openFileAsIs(document)
        .checkLegacyMilestones()
        .checkEarliestStartConstraints()
        .runUiTasks();
    } catch (Exception e) {
      throw new DocumentException("Can't open document " + document, e);
    }
  }

  private void beforeClose() {
    myWorkbenchFacade.setWorkbenchTitle(i18n.getText("appliTitle"));
    getUndoManager().die();
  }

  @Override
  public void createProject(final IGanttProject project) {
    if (false == ensureProjectSaved(project)) {
      return;
    }
    beforeClose();
    project.close();
    myWorkbenchFacade.setStatusText(i18n.getText("project.new.description"));
    showNewProjectWizard(project);
  }

  private void showNewProjectWizard(IGanttProject project) {
    NewProjectWizard wizard = new NewProjectWizard();
    wizard.createNewProject(project, myWorkbenchFacade);
  }

  @Override
  public GPOptionGroup[] getOptionGroups() {
    return new GPOptionGroup[] { myConverterGroup };
  }

  private GPUndoManager getUndoManager() {
    return myUndoManager;
  }

  private DocumentManager getDocumentManager() {
    return myDocumentManager;
  }
}
