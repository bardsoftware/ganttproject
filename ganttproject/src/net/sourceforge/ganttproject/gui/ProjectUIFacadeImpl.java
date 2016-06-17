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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import net.sourceforge.ganttproject.util.collect.Pair;

import org.eclipse.core.runtime.IStatus;
import org.jdesktop.swingx.JXRadioGroup;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.CalendarFactory;
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

  class DiagnosticImpl implements AlgorithmBase.Diagnostic {
    List<String> myMessages = Lists.newArrayList();
    LinkedHashMap<Task, Pair<Date, Date>> myModifiedTasks = new LinkedHashMap<>();
    Map<Task, String> myReasons = Maps.newHashMap();
    private boolean myHasOnlyEndDateChange = false;
    private GanttLanguage i18n = GanttLanguage.getInstance();

    void info(String message) {
      myMessages.add(message);
    }
    @Override
    public void addModifiedTask(Task t, Date newStart, Date newEnd) {
      Pair<Date, Date> entry = myModifiedTasks.get(t);
      if (entry == null) {
        entry = Pair.create(null, null);
      }
      if (newStart != null) {
        entry = Pair.create(newStart, entry.second());
      }
      if (newEnd != null) {
        entry = Pair.create(entry.first(), newEnd);
      }
      if (entry.first() == null && entry.second() != null) {
        myHasOnlyEndDateChange = true;
      }
      myModifiedTasks.put(t, entry);
    }
    void addReason(Task t, String reasonKey) {
      myReasons.put(t, reasonKey);
    }
    void showDialog() {
      String intro = Joiner.on("<br>").join(myMessages);
      String startDateChangeTable = buildStartDateChangeTable();
      String endDateChangeTable = myHasOnlyEndDateChange ? buildEndDateChangeTable() : "";
      String reasonTable = buildReasonTable();
      String msg = String.format("<html><p>%s</p><br>%s%s<br>%s</html>",
          intro,
          startDateChangeTable,
          endDateChangeTable,
          reasonTable);
      myWorkbenchFacade.showOptionDialog(JOptionPane.INFORMATION_MESSAGE, msg, new Action[] {CancelAction.CLOSE});
    }
    private String buildReasonTable() {
      List<String> rows = Lists.newArrayList();
      Set<String> uniqueReasons = new LinkedHashSet<>(myReasons.values());
      uniqueReasons.add("scheduler.warning.reason.other");
      for (String reasonKey : uniqueReasons) {
        rows.add(String.format("<p><b>%s</b>: %s<br></p>",
            i18n.getText(reasonKey + ".label"),
            i18n.getText(reasonKey + ".description")));
      }
      return String.format("<hr>%s", Joiner.on("<br>").join(rows));
    }
    private String buildStartDateChangeTable() {
      List<String> tableRows = Lists.newArrayList();
      for (Entry<Task, Pair<Date, Date>> entry : myModifiedTasks.entrySet()) {
        Task t = entry.getKey();
        Pair<Date,Date> changes = entry.getValue();
        if (changes.first() != null) {
          String row = String.format("<tr><td>%s</td><td>%s</td><td>%s</td></tr>",
              t.getName(),
              i18n.formatDate(CalendarFactory.createGanttCalendar(changes.first())),
              i18n.getText(Objects.firstNonNull(
                  myReasons.get(t),
                  "scheduler.warning.reason.other") + ".label")
           );
          tableRows.add(row);
        }
      }
      String rows =  Joiner.on('\n').join(tableRows);
      String table = String.format("<hr><b>%s</b><table><tr><th>%s</th><th>%s</th><th>%s</th></tr>%s</table>",
          i18n.getText("scheduler.warning.section.startDate"),
          i18n.getText("taskname"),
          i18n.getText("option.generic.startDate.label"),
          i18n.getText("scheduler.warning.reason"),
          rows);
      return table;
    }
    private String buildEndDateChangeTable() {
      List<String> tableRows = Lists.newArrayList();
      for (Entry<Task, Pair<Date, Date>> entry : myModifiedTasks.entrySet()) {
        Task t = entry.getKey();
        Pair<Date,Date> changes = entry.getValue();
        if (changes.first() == null) {
          String row = String.format("<br><tr><td>%s</td><td>%s</td><td>%s</td></tr>",
              t.getName(),
              i18n.formatDate(CalendarFactory.createGanttCalendar(changes.second())),
              i18n.getText(Objects.firstNonNull(
                  myReasons.get(t),
                  "scheduler.warning.reason.other") + ".label")
           );
          tableRows.add(row);
        }
      }
      String rows =  Joiner.on('\n').join(tableRows);
      String table = String.format("<b>%s</b><table><tr><th>%s</th><th>%s</th><th>%s</th></tr>%s</table>",
          i18n.getText("scheduler.warning.section.endDate"),
          i18n.getText("taskname"),
          i18n.getText("option.generic.endDate.label"),
          i18n.getText("scheduler.warning.reason"),
          rows);
      return table;
    }
  }

  @Override
  public void openProject(final Document document, final IGanttProject project) throws IOException, DocumentException {
    beforeClose();
    project.close();

    TimeDuration oldDuration = null;
    boolean resetModified = true;

    final DiagnosticImpl d = new DiagnosticImpl();
    AlgorithmBase scheduler = project.getTaskManager().getAlgorithmCollection().getScheduler();
    try {
      oldDuration = project.getTaskManager().getProjectLength();
      scheduler.setEnabled(false);
      scheduler.setDiagnostic(d);
      project.open(document);
      scheduler.setEnabled(true);
    } finally {
      scheduler.setDiagnostic(null);
    }
    if (document.getPortfolio() != null) {
      Document defaultDocument = document.getPortfolio().getDefaultDocument();
      project.open(defaultDocument);
    }


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

    GanttLanguage i18n = GanttLanguage.getInstance();
    // Analyze earliest start dates
    for (Task t : taskManager.getTasks()) {
      if (t.getThird() != null && d.myModifiedTasks.containsKey(t)) {
        d.addReason(t, "scheduler.warning.reason.earliestStart");
      }
    }

    TimeDuration newDuration = project.getTaskManager().getProjectLength();
    if (!d.myModifiedTasks.isEmpty()) {
      String part0 = i18n.getText("scheduler.warning.datesChanged.part0");
      String part1 = (newDuration.getLength() == oldDuration.getLength())
          ? "": i18n.formatText("scheduler.warning.datesChanged.part1", oldDuration, newDuration);
      String part2 = "";
      d.info(i18n.formatText("scheduler.warning.datesChanged.pattern", part0, part1, part2));
    }
    tasks.add(new Runnable() {
      @Override
      public void run() {
        if (!d.myMessages.isEmpty()) {
          d.showDialog();
//
//          GPLogger.logToLogger(Joiner.on('\n').join(d.myMessages));
//          myWorkbenchFacade.showNotificationDialog(NotificationChannel.WARNING, msg);
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
