/*
Copyright 2003-2012 GanttProject Team

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
package org.ganttproject.chart.pert;

import java.util.Date;

import javax.swing.JPanel;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import biz.ganttproject.core.option.GPOptionGroup;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartSelectionListener;
import net.sourceforge.ganttproject.task.TaskManager;

public abstract class PertChart extends JPanel implements Chart {
  /** Task manager used to build PERT chart. It provides data. */
  protected TaskManager myTaskManager;

  public PertChart() {
  }

  @Override
  public void init(IGanttProject project) {
    myTaskManager = project.getTaskManager();
  }

  @Override
  public abstract String getName();

  /** Builds PERT chart. */
  protected abstract void buildPertChart();

  /** This method in not supported by this Chart. */
  @Override
  public Date getStartDate() {
    throw new UnsupportedOperationException();
  }

  /** This method in not supported by this Chart. */
  @Override
  public Date getEndDate() {
    throw new UnsupportedOperationException();
  }

  /** Sets the task manager. */
  public void setTaskManager(TaskManager taskManager) {
    myTaskManager = taskManager;
  }

  @Override
  public GPOptionGroup[] getOptionGroups() {
    return null;
  }

  @Override
  public Chart createCopy() {
    return null;
  }

  @Override
  public ChartSelection getSelection() {
    return ChartSelection.EMPTY;
  }

  @Override
  public IStatus canPaste(ChartSelection selection) {
    return Status.CANCEL_STATUS;
  }

  @Override
  public void paste(ChartSelection selection) {
  }

  @Override
  public void addSelectionListener(ChartSelectionListener listener) {
    // No listeners are implemented
  }

  @Override
  public void removeSelectionListener(ChartSelectionListener listener) {
    // No listeners are implemented
  }
}
