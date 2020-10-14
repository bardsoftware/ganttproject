/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010 Dmitry Barashev

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
package biz.ganttproject.impex.msproject2;

import java.io.File;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.google.common.collect.Lists;

import biz.ganttproject.core.calendar.ImportCalendarOption;
import biz.ganttproject.core.option.GPOption;
import net.sf.mpxj.MPXJException;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.gui.NotificationChannel;
import net.sourceforge.ganttproject.importer.BufferProject;
import net.sourceforge.ganttproject.importer.Importer;
import net.sourceforge.ganttproject.importer.ImporterBase;
import net.sourceforge.ganttproject.importer.ImporterFromGanttFile;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceMerger;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.util.collect.Pair;

public class ImporterFromMsProjectFile extends ImporterBase implements Importer {
  private final HumanResourceMerger.MergeResourcesOption myMergeResourcesOption = new HumanResourceMerger.MergeResourcesOption();
  private final ImportCalendarOption myImportCalendarOption = new ImportCalendarOption();

  private final GPOption[] myOptions = new GPOption[] { myMergeResourcesOption, myImportCalendarOption };
  public ImporterFromMsProjectFile() {
    super("impex.msproject2");
    myMergeResourcesOption.loadPersistentValue(HumanResourceMerger.MergeResourcesOption.BY_ID);
    myImportCalendarOption.setSelectedValue(ImportCalendarOption.Values.NO);
  }

  @Override
  protected GPOption[] getOptions() {
    return myOptions;
  }

  @Override
  public String getFileNamePattern() {
    return "mpp|mpx|xml";
  }

  @Override
  public void run() {
    try {
      File selectedFile = getFile();
      BufferProject bufferProject = new BufferProject(getProject(), getUiFacade());
      ProjectFileImporter importer = new ProjectFileImporter(bufferProject, getUiFacade().getTaskTree(), selectedFile);
      importer.run();

      List<Pair<Level, String>> errors = importer.getErrors();
      getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(false);
      getTaskManager().getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm().setEnabled(false);
      getTaskManager().getAlgorithmCollection().getScheduler().setEnabled(false);

      Map<Task, Task> buffer2realTask = ImporterFromGanttFile.importBufferProject(getProject(), bufferProject, getUiFacade(), myMergeResourcesOption, myImportCalendarOption);
      Map<GanttTask, Date> originalDates = importer.getOriginalStartDates();

      findChangedDates(originalDates, buffer2realTask, errors);
      reportErrors(errors, "MSProject");
    } catch (MPXJException e) {
      getUiFacade().showErrorDialog(e);
    } finally {
      getTaskManager().getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm().setEnabled(true);
      getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(true);
      getTaskManager().getAlgorithmCollection().getScheduler().setEnabled(true);
    }
    try {
      getTaskManager().getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm().run();
      getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
    } catch (TaskDependencyException e) {
      getUiFacade().showErrorDialog(e);
    }
  }

  private void findChangedDates(Map<GanttTask, Date> originalDates, Map<Task, Task> buffer2realTask,
      List<Pair<Level, String>> errors) {
    List<Pair<Level, String>> dateChangeMessages = Lists.newArrayList();
    for (Task bufferTask : originalDates.keySet()) {
      Date startPerMsProject = originalDates.get(bufferTask);
      if (startPerMsProject == null) {
        continue;
      }
      Task realTask = buffer2realTask.get(bufferTask);
      if (realTask == null) {
        continue;
      }
      Date startPerGanttProject = realTask.getStart().getTime();
      if (!startPerMsProject.equals(startPerGanttProject)) {
        dateChangeMessages.add(Pair.create(Level.WARNING, GanttLanguage.getInstance().formatText(
            "impex.msproject.warning.taskDateChanged", realTask.getName(), startPerMsProject, startPerGanttProject)));
      }
    }
    if (!dateChangeMessages.isEmpty()) {
      errors.add(Pair.create(Level.INFO, GanttLanguage.getInstance().formatText(
          "impex.msproject.warning.taskDateChanged.heading", dateChangeMessages.size(), originalDates.size())));
      errors.addAll(dateChangeMessages);
    }
  }

  private TaskManager getTaskManager() {
    return getProject().getTaskManager();
  }
}
