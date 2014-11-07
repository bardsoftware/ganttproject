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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.filter.GanttXMLFileFilter;
import net.sourceforge.ganttproject.gui.projectwizard.NewProjectWizard;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskImpl;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmBase;
import net.sourceforge.ganttproject.undo.GPUndoManager;
import net.sourceforge.ganttproject.util.FileUtil;

import org.eclipse.core.runtime.IStatus;
import org.jdesktop.swingx.JXRadioGroup;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.TimeDuration;

public class ProjectUIFacadeImpl implements ProjectUIFacade {
  private final UIFacade myWorkbenchFacade;
  private final GanttLanguage i18n = GanttLanguage.getInstance();
  private final DocumentManager myDocumentManager;
  private final GPUndoManager myUndoManager;

  private static enum ConvertMilestones {
    UNKNOWN, TRUE, FALSE
  }
  private final DefaultEnumerationOption<ConvertMilestones> myConvertMilestonesOption = new DefaultEnumerationOption<ConvertMilestones>(
      "milestones_to_zero", ConvertMilestones.values());
  private final GPOptionGroup myConverterGroup = new GPOptionGroup("convert", myConvertMilestonesOption);
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
          projectfile = FileUtil.replaceExtension(projectfile, "gan");
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

    class DiagnosticImpl implements AlgorithmBase.Diagnostic {
      List<String> myMessages = Lists.newArrayList();
      @Override
      public void info(String message) {
        myMessages.add(message);
      }
    }
    final DiagnosticImpl d = new DiagnosticImpl();
    try {
      project.getTaskManager().getAlgorithmCollection().getScheduler().setDiagnostic(d);
      project.open(document);
    } finally {
      project.getTaskManager().getAlgorithmCollection().getScheduler().setDiagnostic(null);
    }
    if (document.getPortfolio() != null) {
      Document defaultDocument = document.getPortfolio().getDefaultDocument();
      project.open(defaultDocument);
    }

    final TimeDuration oldDuration = project.getTaskManager().getProjectLength();
    boolean resetModified = true;

    final TaskManager taskManager = project.getTaskManager();
    boolean hasLegacyMilestones = false;
    for (Task t : taskManager.getTasks()) {
      if (((TaskImpl)t).isLegacyMilestone()) {
        hasLegacyMilestones = true;
        break;
      }
    }

    List<Runnable> tasks = Lists.newArrayList();

    if (hasLegacyMilestones && taskManager.isZeroMilestones() == null) {
      ConvertMilestones option = myConvertMilestonesOption.getSelectedValue() == null ? ConvertMilestones.UNKNOWN : myConvertMilestonesOption.getSelectedValue();
      switch (option) {
      case UNKNOWN:
        tasks.add(new Runnable() {
          @Override
          public void run() {
            try {
              project.getTaskManager().getAlgorithmCollection().getScheduler().setDiagnostic(d);
              tryPatchMilestones(project, taskManager);
            } finally {
              project.getTaskManager().getAlgorithmCollection().getScheduler().setDiagnostic(null);
            }
          }
        });
        break;
      case TRUE:
        taskManager.setZeroMilestones(true);
        resetModified = false;
        break;
      case FALSE:
        taskManager.setZeroMilestones(false);
        break;
      }
    }

    tasks.add(new Runnable() {
      @Override
      public void run() {
        if (!d.myMessages.isEmpty()) {
          TimeDuration newDuration = project.getTaskManager().getProjectLength();
          GPLogger.logToLogger(Joiner.on('\n').join(d.myMessages));
          String part0 = GanttLanguage.getInstance().getText("scheduler.warning.datesChanged.part0");
          String part1 = (newDuration.getLength() == oldDuration.getLength())
              ? "": GanttLanguage.getInstance().formatText("scheduler.warning.datesChanged.part1", oldDuration, newDuration);
          String part2 = GanttLanguage.getInstance().getText("scheduler.warning.datesChanged.part2");
          String msg = GanttLanguage.getInstance().formatText("scheduler.warning.datesChanged.pattern", part0, part1, part2);
          myWorkbenchFacade.showNotificationDialog(NotificationChannel.WARNING, msg);
        }
      }
    });
    if (resetModified) {
      tasks.add(new Runnable() {
        @Override
        public void run() {
          project.setModified(false);
        }
      });
    }
    processTasks(tasks);
  }

  private void processTasks(final List<Runnable> tasks) {
    if (tasks.isEmpty()) {
      return;
    }
    final Runnable task = tasks.get(0);
    Runnable wrapper = new Runnable() {
      @Override
      public void run() {
        task.run();
        tasks.remove(0);
        processTasks(tasks);
      }
    };
    SwingUtilities.invokeLater(wrapper);
  }

  private void adjustTasks(TaskManager taskManager) {
//    try {
//      taskManager.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
//    } catch (TaskDependencyException e) {
//      GPLogger.logToLogger(e);
//    }
//    List<Task> leafTasks = Lists.newArrayList();
//    for (Task t : taskManager.getTasks()) {
//      if (taskManager.getTaskHierarchy().getNestedTasks(t).length == 0) {
//        leafTasks.add(t);
//      }
//    }
//    taskManager.getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().run(leafTasks);
    try {
      taskManager.getAlgorithmCollection().getScheduler().run();
    } catch (Exception e) {
      GPLogger.logToLogger(e);
    }
  }

  private void tryPatchMilestones(final IGanttProject project, final TaskManager taskManager) {
    final JRadioButton buttonConvert = new JRadioButton(i18n.getText("legacyMilestones.choice.convert"));
    final JRadioButton buttonKeep = new JRadioButton(i18n.getText("legacyMilestones.choice.keep"));
    buttonConvert.setSelected(true);
    JXRadioGroup<JRadioButton> group = JXRadioGroup.create(new JRadioButton[] {buttonConvert, buttonKeep});
    group.setLayoutAxis(BoxLayout.PAGE_AXIS);
    final JCheckBox remember = new JCheckBox(i18n.getText("legacyMilestones.choice.remember"));

    Box content = Box.createVerticalBox();
    JLabel question = new JLabel(i18n.getText("legacyMilestones.question"), SwingConstants.LEADING);
    question.setOpaque(true);
    question.setAlignmentX(0.5f);
    content.add(question);
    content.add(Box.createVerticalStrut(15));
    content.add(group);
    content.add(Box.createVerticalStrut(5));
    content.add(remember);

    Box icon = Box.createVerticalBox();
    icon.add(new JLabel(GPAction.getIcon("64", "dialog-question.png")));
    icon.add(Box.createVerticalGlue());

    JPanel result = new JPanel(new BorderLayout());
    result.add(content, BorderLayout.CENTER);
    result.add(icon, BorderLayout.WEST);
    result.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    myWorkbenchFacade.createDialog(result, new Action[] {new OkAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        taskManager.setZeroMilestones(buttonConvert.isSelected());
        if (remember.isSelected()) {
          myConvertMilestonesOption.setSelectedValue(buttonConvert.isSelected() ? ConvertMilestones.TRUE : ConvertMilestones.FALSE);
        }
        adjustTasks(taskManager);
        project.setModified(true);
      }
    }}, i18n.getText("legacyMilestones.title")).show();
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
