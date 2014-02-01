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
import java.util.List;

import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.option.GPOption;
import net.sf.mpxj.MPXJException;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.gui.NotificationChannel;
import net.sourceforge.ganttproject.importer.BufferProject;
import net.sourceforge.ganttproject.importer.Importer;
import net.sourceforge.ganttproject.importer.ImporterBase;
import net.sourceforge.ganttproject.importer.ImporterFromGanttFile;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceMerger;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

public class ImporterFromMsProjectFile extends ImporterBase implements Importer {
  private final HumanResourceMerger.MergeResourcesOption myMergeResourcesOption = new HumanResourceMerger.MergeResourcesOption();
  private final GPCalendar.ImportCalendarOption myImportCalendarOption = new GPCalendar.ImportCalendarOption();

  private final GPOption[] myOptions = new GPOption[] { myMergeResourcesOption, myImportCalendarOption };
  public ImporterFromMsProjectFile() {
    super("impex.msproject2");
    myMergeResourcesOption.loadPersistentValue(HumanResourceMerger.MergeResourcesOption.BY_ID);
    myImportCalendarOption.setSelectedValue(GPCalendar.ImportCalendarOption.Values.NO);
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
  public void run(File selectedFile) {
    try {
      BufferProject bufferProject = new BufferProject(getProject(), getUiFacade());
      List<String> errors = new ProjectFileImporter(bufferProject, getUiFacade().getTaskTree(), selectedFile).run();

      getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(false);
      getTaskManager().getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm().setEnabled(false);
      getTaskManager().getAlgorithmCollection().getScheduler().setEnabled(false);

      try {
        ImporterFromGanttFile.importBufferProject(getProject(), bufferProject, getUiFacade(), myMergeResourcesOption, myImportCalendarOption);
      } finally {

      }
      if (!errors.isEmpty()) {
        StringBuilder builder = new StringBuilder();
        for (String message : errors) {
          GPLogger.log(message);
          builder.append("<li>").append(message);
        }
        getUiFacade().showNotificationDialog(NotificationChannel.WARNING,
            GanttLanguage.getInstance().formatText("impex.msproject.importErrorReport", builder.toString()));
      }
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

  private TaskManager getTaskManager() {
    return getProject().getTaskManager();
  }
}
