/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

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
package net.sourceforge.ganttproject.task.algorithm;

import biz.ganttproject.core.chart.scene.gantt.ChartBoundsAlgorithm;
import net.sourceforge.ganttproject.task.TaskManagerImpl;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class AlgorithmCollection {
  private final FindPossibleDependeesAlgorithm myFindPossibleDependeesAlgorithm;

  private final RecalculateTaskScheduleAlgorithm myRecalculateTaskScheduleAlgorithm;

  private final AdjustTaskBoundsAlgorithm myAdjustTaskBoundsAlgorithm;

  private final RecalculateTaskCompletionPercentageAlgorithm myCompletionPercentageAlgorithm;

  private final ChartBoundsAlgorithm myProjectBoundsAlgorithm;

  private final ShiftTaskTreeAlgorithm myShiftTaskTreeAlgorithm;

  private final CriticalPathAlgorithm myCriticalPathAlgorithm;

  private final SchedulerImpl myScheduler;

  public AlgorithmCollection(TaskManagerImpl taskManager,
      FindPossibleDependeesAlgorithm myFindPossibleDependeesAlgorithm,
      RecalculateTaskScheduleAlgorithm recalculateTaskScheduleAlgorithm,
      AdjustTaskBoundsAlgorithm adjustTaskBoundsAlgorithm,
      RecalculateTaskCompletionPercentageAlgorithm completionPercentageAlgorithm,
      ChartBoundsAlgorithm projectBoundsAlgorithm, CriticalPathAlgorithm criticalPathAlgorithm, SchedulerImpl scheduler) {
    myScheduler = scheduler;
    this.myFindPossibleDependeesAlgorithm = myFindPossibleDependeesAlgorithm;
    myRecalculateTaskScheduleAlgorithm = recalculateTaskScheduleAlgorithm;
    myAdjustTaskBoundsAlgorithm = adjustTaskBoundsAlgorithm;
    myCompletionPercentageAlgorithm = completionPercentageAlgorithm;
    myProjectBoundsAlgorithm = projectBoundsAlgorithm;
    myShiftTaskTreeAlgorithm = new ShiftTaskTreeAlgorithm(taskManager, recalculateTaskScheduleAlgorithm);
    myCriticalPathAlgorithm = criticalPathAlgorithm;
  }

  public FindPossibleDependeesAlgorithm getFindPossibleDependeesAlgorithm() {
    return myFindPossibleDependeesAlgorithm;
  }

  public RecalculateTaskScheduleAlgorithm getRecalculateTaskScheduleAlgorithm() {
    return myRecalculateTaskScheduleAlgorithm;
  }

  public AdjustTaskBoundsAlgorithm getAdjustTaskBoundsAlgorithm() {
    return myAdjustTaskBoundsAlgorithm;
  }

  public RecalculateTaskCompletionPercentageAlgorithm getRecalculateTaskCompletionPercentageAlgorithm() {
    return myCompletionPercentageAlgorithm;
  }

  public ChartBoundsAlgorithm getProjectBoundsAlgorithm() {
    return myProjectBoundsAlgorithm;
  }

  public ShiftTaskTreeAlgorithm getShiftTaskTreeAlgorithm() {
    return myShiftTaskTreeAlgorithm;
  }

  public CriticalPathAlgorithm getCriticalPathAlgorithm() {
    return myCriticalPathAlgorithm;
  }

  public AlgorithmBase getScheduler() {
    return myScheduler;
  }
}
