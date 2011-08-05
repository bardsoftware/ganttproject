/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

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
package net.sourceforge.ganttproject.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.eclipse.core.runtime.IStatus;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.document.HttpDocument;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.filter.GanttXMLFileFilter;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.projectwizard.NewProjectWizard;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.undo.GPUndoManager;
import net.sourceforge.ganttproject.util.FileUtil;

public class ProjectUIFacadeImpl implements ProjectUIFacade {
    private final UIFacade myWorkbenchFacade;
    private final GanttLanguage i18n = GanttLanguage.getInstance();
    private final DocumentManager myDocumentManager;
    private final GPUndoManager myUndoManager;
    public ProjectUIFacadeImpl(UIFacade workbenchFacade, DocumentManager documentManager, GPUndoManager undoManager) {
        myWorkbenchFacade = workbenchFacade;
        myDocumentManager =documentManager;
        myUndoManager = undoManager;
    }

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
            actions.add(new GPAction("saveAsProject") {
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
            actions.add(new CancelAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                }
            });
            myWorkbenchFacade.showOptionDialog(JOptionPane.ERROR_MESSAGE, message, actions.toArray(new Action[0]));

            return false;
        }
        return saveProjectTryLock(project, document);
    }

    private boolean saveProjectTryLock(IGanttProject project, Document document) {
        if (!document.acquireLock()) {
            myWorkbenchFacade.showErrorDialog(i18n.getText("msg14"));
            saveProjectAs(project);
            return false;
        }
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
        String title = i18n.getText("appliTitle") + " ["
                + document.getFileName() + "]";
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
        myWorkbenchFacade.setStatusText(GanttLanguage.getInstance()
                    .getText("saving")
                    + " " + document.getPath());
        document.write();
    }
    public void saveProjectAs(IGanttProject project) {
        /*
        if (project.getDocument() instanceof AbstractURLDocument) {
            saveProjectRemotely(project);
            return;
        }
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
            for(;;) {
                int userChoice = fc.showSaveDialog(myWorkbenchFacade.getMainFrame());
                if (userChoice != JFileChooser.APPROVE_OPTION) {
                    break;
                }
                File projectfile = fc.getSelectedFile();
                String extension = FileUtil.getExtension(projectfile).toLowerCase();
                if (!"gan".equals(extension) && !"xml".equals(extension)) {
                    projectfile = FileUtil.replaceExtension(projectfile, "gan");
                }

                if (projectfile.exists()) {
                    UIFacade.Choice overwritingChoice = myWorkbenchFacade.showConfirmationDialog(projectfile + "\n" + i18n.getText("msg18"), i18n.getText("warning"));
                    if (overwritingChoice!=UIFacade.Choice.YES) {
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

    public void saveProjectRemotely(IGanttProject project) {
        Document document = showURLDialog(project, false);
        if (document != null) {
            project.setDocument(document);
            saveProject(project);
        }
    }

    /**
     * Check if the project has been modified, before creating or opening
     * another project
     *
     * @return true when the project is <b>not</b> modified or is allowed to be discarded
     */
    public boolean ensureProjectSaved(IGanttProject project) {
        if (project.isModified()) {
            UIFacade.Choice saveChoice = myWorkbenchFacade.showConfirmationDialog(i18n.getText("msg1"), i18n.getText("warning"));
            if (UIFacade.Choice.CANCEL == saveChoice) {
                return false;
            }
            if (UIFacade.Choice.YES == saveChoice) {
                try {
                    saveProject(project);
                } catch (Exception e) {
                    myWorkbenchFacade.showErrorDialog(e);
                    return false;
                }
                // Check if project indeed is saved
                return project.isModified() == false;
            }
        }
        return true;
    }

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
            Document document = getDocumentManager().getDocument(
                    fc.getSelectedFile().getAbsolutePath());
            openProject(document, project);
        }
    }

    public void openRemoteProject(final IGanttProject project) throws IOException, DocumentException {
        final Document document = showURLDialog(project, true);
        if (document != null) {
            openProject(document, project);
        }
    }

    public void openProject(final Document document, final IGanttProject project) throws IOException, DocumentException {
        beforeClose();
        project.close();
        project.open(document);
    }

    private void beforeClose() {
        myWorkbenchFacade.setWorkbenchTitle(i18n.getText("appliTitle"));
        getUndoManager().die();
    }

    public void createProject(final IGanttProject project) {
        if (false == ensureProjectSaved(project)) {
            return;
        }
        getUndoManager().undoableEdit("Init new Project", new Runnable() {
            public void run() {
                beforeClose();
                project.close();
                myWorkbenchFacade.setStatusText(i18n.getText("newProject2"));
                showNewProjectWizard(project);
            }
        });
    }

    private void showNewProjectWizard(IGanttProject project) {
        NewProjectWizard wizard = new NewProjectWizard();
        wizard.createNewProject(project, myWorkbenchFacade);
    }

    public GPOptionGroup getOptionGroup() {
        return myDocumentManager.getOptionGroup();
    }

    private GPUndoManager getUndoManager() {
        return myUndoManager;
    }

    private DocumentManager getDocumentManager() {
        return myDocumentManager;
    }

    private Document showURLDialog(IGanttProject project, boolean isOpenUrl) {
        Document document = project.getDocument();
        GanttURLChooser uc = new GanttURLChooser(myWorkbenchFacade,
            (null != document) ? document.getURLPath() : myDocumentManager.getLastWebDAVDocumentOption().getValue(),
            (null != document) ? document.getUsername() : null,
            (null != document) ? document.getPassword() : null,
            myDocumentManager.getWebDavLockTimeoutOption().getValue());
        uc.show(isOpenUrl);
        if (uc.getChoice() == UIFacade.Choice.OK) {
            if (!sameDocument(document, uc)) {
                document = myDocumentManager.getDocument(uc.getUrl(), uc.getUsername(), uc.getPassword());
            }
            myDocumentManager.getLastWebDAVDocumentOption().setValue(uc.getUrl());
            if (uc.isTimeoutEnabled()) {
                HttpDocument.setLockDAVMinutes(uc.getTimeout());
                myDocumentManager.getWebDavLockTimeoutOption().setValue(uc.getTimeout());
            } else {
                HttpDocument.setLockDAVMinutes(-1);
            }
        }
        else {
            document = null;
        }
        return document;
    }

    private boolean sameDocument(Document document, GanttURLChooser uc) {
        if (document == null) {
            return false;
        }
        return document.getURLPath().equals(uc.getUrl()) && document.getUsername().equals(uc.getUsername())
            && document.getPassword().equals(uc.getPassword());
    }
}
