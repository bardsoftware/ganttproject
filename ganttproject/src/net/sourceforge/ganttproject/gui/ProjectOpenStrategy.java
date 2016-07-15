/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2016 GanttProject team

This file is part of GanttProject.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskImpl;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmCollection;

import org.jdesktop.swingx.JXRadioGroup;

import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.time.TimeDuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * When we open a file, we need to complete a number of steps in order to be sure
 * that should task dates change after the first scheduler run, user is informed about that.
 *
 * The steps are chained so that client can't invoke them in wrong order. Strategy is auto-closeable
 * and should be closed after using.
 *
 * @author bard
 */
class ProjectOpenStrategy implements AutoCloseable {
  private static enum ConvertMilestones {
    UNKNOWN, TRUE, FALSE
  }
  private static final DefaultEnumerationOption<ConvertMilestones> ourConvertMilestonesOption = new DefaultEnumerationOption<ConvertMilestones>(
      "milestones_to_zero", ConvertMilestones.values());
  static DefaultEnumerationOption<ConvertMilestones> getMilestonesOption() {
    return ourConvertMilestonesOption;
  }

  private final UIFacade myUiFacade;
  private final IGanttProject myProject;
  private final ProjectOpenDiagnosticImpl myDiagnostics;
  private final List<AutoCloseable> myCloseables = Lists.newArrayList();
  private final AutoCloseable myEnableAlgorithmsCmd;
  private final AlgorithmCollection myAlgs;
  private final List<Runnable> myTasks = Lists.newArrayList();
  private TimeDuration myOldDuration;
  private final GanttLanguage i18n = GanttLanguage.getInstance();
  private boolean myResetModifiedState = true;

  ProjectOpenStrategy(IGanttProject project, UIFacade uiFacade) {
    myProject = Preconditions.checkNotNull(project);
    myUiFacade = Preconditions.checkNotNull(uiFacade);
    myDiagnostics = new ProjectOpenDiagnosticImpl(myUiFacade);
    myAlgs = myProject.getTaskManager().getAlgorithmCollection();
    myEnableAlgorithmsCmd = new AutoCloseable() {
      public void close() throws Exception {
        myAlgs.getScheduler().setEnabled(true);
        myAlgs.getRecalculateTaskScheduleAlgorithm().setEnabled(true);
        myAlgs.getAdjustTaskBoundsAlgorithm().setEnabled(true);
      }
    };
  }

  public void close() {
    for (AutoCloseable c : myCloseables) {
      try {
        c.close();
      } catch (Exception e) {
        GPLogger.log(e);
      }
    }
  }

  // First we open file "as is", that is, without running any algorithms which
  // change task dates.
  Step1 openFileAsIs(Document document) throws Exception {
    myCloseables.add(myEnableAlgorithmsCmd);
    myAlgs.getScheduler().setEnabled(false);
    myAlgs.getRecalculateTaskScheduleAlgorithm().setEnabled(false);
    myAlgs.getAdjustTaskBoundsAlgorithm().setEnabled(false);
    myAlgs.getScheduler().setDiagnostic(myDiagnostics);
    try {
      myProject.open(document);
    } finally {
      myAlgs.getScheduler().setDiagnostic(null);
    }
    if (document.getPortfolio() != null) {
      Document defaultDocument = document.getPortfolio().getDefaultDocument();
      myProject.open(defaultDocument);
    }
    myOldDuration = myProject.getTaskManager().getProjectLength();
    return new Step1();
  }

  // This step checks if there are legacy 1-day milestones in the project.
  // If there are legacy milestones, we ask the user what shall we do with them.
  // This involves interaction with Swing thread and later task of patching
  // milestones.
  class Step1 {
    Step2 checkLegacyMilestones() {
      final TaskManager taskManager = myProject.getTaskManager();
      boolean hasLegacyMilestones = false;
      for (Task t : taskManager.getTasks()) {
        if (((TaskImpl)t).isLegacyMilestone()) {
          hasLegacyMilestones = true;
          break;
        }
      }

      if (hasLegacyMilestones && taskManager.isZeroMilestones() == null) {
        ConvertMilestones option = ourConvertMilestonesOption.getSelectedValue() == null
            ? ConvertMilestones.UNKNOWN
            : ourConvertMilestonesOption.getSelectedValue();
        switch (option) {
        case UNKNOWN:
          myTasks.add(new Runnable() {
            @Override
            public void run() {
              try {
                myProject.getTaskManager().getAlgorithmCollection().getScheduler().setDiagnostic(myDiagnostics);
                tryPatchMilestones(myProject, taskManager);
              } finally {
                myProject.getTaskManager().getAlgorithmCollection().getScheduler().setDiagnostic(null);
              }
            }
          });
          break;
        case TRUE:
          taskManager.setZeroMilestones(true);
          myResetModifiedState = false;
          break;
        case FALSE:
          taskManager.setZeroMilestones(false);
          break;
        }
      }
      return new Step2();
    }

    // Asks user what shall we do with milestones and updates milestones if user
    // decides to convert them. This code is executed by Step3.runUiTasks
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
      myUiFacade.createDialog(result, new Action[] {new OkAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          taskManager.setZeroMilestones(buttonConvert.isSelected());
          if (remember.isSelected()) {
            ourConvertMilestonesOption.setSelectedValue(buttonConvert.isSelected() ? ConvertMilestones.TRUE : ConvertMilestones.FALSE);
          }
          adjustTasks(taskManager);
          project.setModified(true);
        }
      }}, i18n.getText("legacyMilestones.title")).show();
    }

    private void adjustTasks(TaskManager taskManager) {
      try {
        taskManager.getAlgorithmCollection().getScheduler().run();
      } catch (Exception e) {
        GPLogger.logToLogger(e);
      }
    }
  }

  // This step runs the scheduler and checks if there are tasks with earliest start constraints
  // which changed their dates. Such tasks will be reported in the dialog.
  class Step2 {
    Step3 checkEarliestStartConstraints() throws Exception {
      myAlgs.getScheduler().setDiagnostic(myDiagnostics);
      try {
        // This actually runs the scheduler by enabling it
        myEnableAlgorithmsCmd.close();
        // We enabled algoritmhs so we don't need to keep them in the list of closeables
        myCloseables.remove(myEnableAlgorithmsCmd);
      } finally {
        myAlgs.getScheduler().setDiagnostic(null);
      }
      // Analyze earliest start dates
      for (Task t : myProject.getTaskManager().getTasks()) {
        if (t.getThird() != null && myDiagnostics.myModifiedTasks.containsKey(t)) {
          myDiagnostics.addReason(t, "scheduler.warning.table.reason.earliestStart");
        }
      }

      TimeDuration newDuration = myProject.getTaskManager().getProjectLength();
      if (!myDiagnostics.myModifiedTasks.isEmpty()) {
        // Some tasks have been modified, so let's add introduction text to the dialog
        myDiagnostics.info(i18n.getText("scheduler.warning.summary.item0"));
        if (newDuration.getLength() != myOldDuration.getLength()) {
          myDiagnostics.info(i18n.formatText("scheduler.warning.summary.item1", myOldDuration, newDuration));
        }
      }
      return new Step3();
    }
  }

  // This step runs the collected UI tasks. First (optional) task is legacy milestones question;
  // the remaining are added here.
  class Step3 {
    void runUiTasks() {
      myTasks.add(new Runnable() {
        @Override
        public void run() {
          if (!myDiagnostics.myMessages.isEmpty()) {
            myDiagnostics.showDialog();
          }
        }
      });
      if (myDiagnostics.myMessages.isEmpty() && myResetModifiedState) {
        myTasks.add(new Runnable() {
          @Override
          public void run() {
            myProject.setModified(false);
          }
        });
      }
      myTasks.add(new Runnable() {
        @Override
        public void run() {
          try {
            ProjectOpenStrategy.this.close();
          } catch (Exception e) {
            GPLogger.log(e);
          }
        }
      });
      processTasks(myTasks);
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
  }
}
