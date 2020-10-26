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
package net.sourceforge.ganttproject.chart;

import biz.ganttproject.core.option.FontOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.IntegerOption;
import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.export.ChartImageVisitor;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;

import java.awt.image.RenderedImage;
import java.util.Date;

public interface Chart extends IAdaptable {
  IGanttProject getProject();

  void init(IGanttProject project, IntegerOption dpiOption, FontOption chartFontOption);

  public void buildImage(GanttExportSettings settings, ChartImageVisitor imageVisitor);

  public RenderedImage getRenderedImage(GanttExportSettings settings);

  public Date getStartDate();

  void setStartDate(Date startDate);

  public Date getEndDate();

  void setDimensions(int height, int width);

  public String getName();

  /** Repaints the chart */
  public void reset();

  public GPOptionGroup[] getOptionGroups();

  public Chart createCopy();

  public ChartSelection getSelection();

  public IStatus canPaste(ChartSelection selection);

  public void paste(ChartSelection selection);

  public void addSelectionListener(ChartSelectionListener listener);

  public void removeSelectionListener(ChartSelectionListener listener);

}
