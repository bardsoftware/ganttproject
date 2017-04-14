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

import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.FontOption;
import biz.ganttproject.core.option.FontSpec;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.IntegerOption;
import com.google.common.base.Preconditions;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartSelectionListener;
import net.sourceforge.ganttproject.task.TaskManager;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

import static net.sourceforge.ganttproject.gui.UIFacade.DEFAULT_DPI;

public abstract class PertChart extends JPanel implements Chart {
  /** Task manager used to build PERT chart. It provides data. */
  TaskManager myTaskManager;
  private IntegerOption myDpi;
  private FontOption myChartFontOption;
  private Font myBaseFont;
  private Font myBoldFont;

  PertChart() {
  }

  @Override
  public void init(IGanttProject project, IntegerOption dpiOption, FontOption chartFontOption) {
    myTaskManager = project.getTaskManager();
    myDpi = Preconditions.checkNotNull(dpiOption);
    myChartFontOption = chartFontOption;
    myChartFontOption.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        updateFonts();
      }
    });
    updateFonts();
  }

  private void updateFonts() {
    FontSpec fontSpec = myChartFontOption.getValue();
    float scaleFactor = fontSpec.getSize().getFactor() * getDpi();
    myBaseFont = new Font(fontSpec.getFamily(), Font.PLAIN, (int)(10*scaleFactor));
    myBoldFont = myBaseFont.deriveFont(Font.BOLD);
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

  float getDpi() {
    return myDpi.getValue().floatValue() / DEFAULT_DPI;
  }

  Font getBaseFont() {
    return myBaseFont;
  }

  Font getBoldFont() {
    return myBoldFont;
  }
}
