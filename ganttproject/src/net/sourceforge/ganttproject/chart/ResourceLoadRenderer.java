/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010-2011 Dmitry Barashev, GanttProject Team

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

package net.sourceforge.ganttproject.chart;

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.HAlignment;
import biz.ganttproject.core.chart.canvas.Canvas.Label;
import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;
import biz.ganttproject.core.chart.canvas.Canvas.Text;
import biz.ganttproject.core.chart.canvas.Canvas.VAlignment;
import biz.ganttproject.core.chart.canvas.TextMetrics;
import biz.ganttproject.core.chart.canvas.TextSelector;
import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.chart.grid.OffsetLookup;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.LoadDistribution;
import net.sourceforge.ganttproject.resource.LoadDistribution.Load;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Renders resource load chart
 */
class ResourceLoadRenderer extends ChartRendererBase {

  private List<LoadDistribution> myDistributions;

  private final ResourceChart myResourcechart;

  private final ChartModelResource myModel;

  private final Canvas myTextCanvas;

  public ResourceLoadRenderer(ChartModelResource model, ResourceChart resourceChart) {
    super(model);
    myResourcechart = resourceChart;
    myModel = model;
    myTextCanvas = getPrimitiveContainer().newLayer();
  }

  /**
   * Renders load distribution one by one, from top of the chart downwards If
   * some resource is expanded, calls rendering of the load details
   */
  @Override
  public void render() {
    getPrimitiveContainer().setOffset(0, getConfig().getHeaderHeight() - myModel.getVerticalOffset());
    beforeProcessingTimeFrames();
    int ypos = 0;
    for (LoadDistribution distribution : myDistributions) {
      List<Load> loads = distribution.getLoads();
      renderLoads(distribution.getDaysOff(), ypos);
      renderLoads(loads, ypos);
      if (myResourcechart.isExpanded(distribution.getResource())) {
        renderLoadDetails(distribution, ypos);
        ypos += calculateGap(distribution.getResource());
      }
      ypos += getConfig().getRowHeight();
      Canvas.Line nextLine = getPrimitiveContainer().createLine(0, ypos,
          (int) getChartModel().getBounds().getWidth(), ypos);
      nextLine.setForegroundColor(Color.GRAY);
    }
  }

  /**
   * Renders resource load details, that is, tasks where the resource is
   * assigned to, with that resource load percentage
   */
  private void renderLoadDetails(LoadDistribution distribution, int ypos) {
    int yPos2 = ypos;
    Map<Task, List<Load>> task2loads = distribution.getSeparatedTaskLoads();

    ResourceAssignment[] assignments = distribution.getResource().getAssignments();
    for (int i = 0; i < assignments.length; i++) {
      ResourceAssignment assignment = assignments[i];
      List<Load> nextLoads = task2loads.get(assignment.getTask());
      yPos2 += getConfig().getRowHeight();
      if (nextLoads == null) {
        continue;
      }
      buildTasksLoadsRectangles(nextLoads, yPos2);
    }
  }

  /**
   * Renders the list of loads in a single chart row Preconditions: loads come
   * from the same distribution and are ordered by their time offsets
   */
  private void renderLoads(List<Load> loads, int ypos) {
    Load prevLoad = null;
    Load curLoad = null;
    List<Offset> offsets = getDefaultOffsets();
    String suffix = "";
    for (int curIndex = 1; curIndex < loads.size(); curIndex++) {
      curLoad = loads.get(curIndex);
      prevLoad = loads.get(curIndex - 1);
      if (prevLoad.load != 0) {
        renderLoads(prevLoad, curLoad, offsets, ypos, suffix);
        suffix = "";
      } else if (curLoad.load > 0) {
        suffix = ".first";
      }
    }
  }

  /**
   * Renders prevLoad, with curLoad serving as a load right border marker and
   * style hint
   */
  private void renderLoads(Load prevLoad, Load curLoad, List<Offset> offsets, int ypos, String suffix) {
    final Date prevEnd = curLoad.startDate;
    final Date prevStart = prevLoad.startDate;

    Rectangle nextRect = createRectangle(offsets, prevStart, prevEnd, ypos);
    if (nextRect == null) {
      return;
    }
    String style;
    if (prevLoad.isResourceUnavailable()) {
      style = "dayoff";
    } else {
      suffix += curLoad.load == 0 ? ".last" : "";
      if (prevLoad.load < 100f) {
        style = "load.underload";
      } else if (prevLoad.load > 100f) {
        style = "load.overload";
      } else {
        style = "load.normal";
      }
      style += suffix;
    }
    nextRect.setStyle(style);
    nextRect.setModelObject(new ResourceLoad(prevLoad.load));
    if (!prevLoad.isResourceUnavailable()) {
      createLoadText(nextRect, prevLoad);
    }
  }

  /**
   * Renders a list of loads in a single chart row Precondition: loads belong to
   * the same pair (resource,task) and are ordered by their time values
   */
  private void buildTasksLoadsRectangles(List<Load> partition, int ypos) {
    List<Offset> offsets = getDefaultOffsets();
    Iterator<Load> loads = partition.iterator();
    while (loads.hasNext()) {
      final Load nextLoad = loads.next();
      final Date nextStart = nextLoad.startDate;
      final Date nextEnd = nextLoad.endDate;

      final Rectangle nextRect = createRectangle(offsets, nextStart, nextEnd, ypos);
      if (nextRect == null) {
        continue;
      }
      String style;
      if (nextLoad.load < 100f) {
        style = "load.underload";
      } else if (nextLoad.load > 100f) {
        style = "load.overload";
      } else {
        style = "load.normal";
      }
      style += ".first.last";
      nextRect.setStyle(style);
      nextRect.setModelObject(new ResourceLoad(nextLoad.load));
      createLoadText(nextRect, nextLoad);
    }
  }

  private void createLoadText(final Rectangle rect, final Load load) {
    final Text loadLabel = myTextCanvas.createText(rect.getMiddleX(), rect.getTopY(), "");
    loadLabel.setSelector(new TextSelector() {
      @Override
      public Label[] getLabels(TextMetrics textLengthCalculator) {
        int loadInt = Math.round(load.load);
        String loadStr = loadInt + "%";
        int emsLength = textLengthCalculator.getTextLength(loadStr);
        boolean displayLoad = (loadInt != 100 && emsLength <= rect.getWidth());
        return displayLoad ? new Label[] {loadLabel.createLabel(loadStr, rect.getWidth())} : new Label[0];
      }
    });
    loadLabel.setAlignment(HAlignment.CENTER, VAlignment.TOP);
    loadLabel.setStyle("text.resource.load");
  }

  private Rectangle createRectangle(List<Offset> offsets, Date start, Date end, int ypos) {
    if (start.after(getChartEndDate()) || end.compareTo(getChartStartDate()) <= 0) {
      return null;
    }
    OffsetLookup offsetLookup = new OffsetLookup();
    int[] bounds = offsetLookup.getBounds(start, end, offsets);
    return getPrimitiveContainer().createRectangle(bounds[0], ypos, bounds[1] - bounds[0], getConfig().getRowHeight());
  }

  private Date getChartStartDate() {
    return getChartModel().getStartDate();
  }

  private Date getChartEndDate() {
    return getChartModel().getBottomUnitOffsets().get(getChartModel().getBottomUnitOffsets().size() - 1).getOffsetEnd();
  }

  private List<Offset> getDefaultOffsets() {
    return getChartModel().getDefaultUnitOffsets();
  }

  public void beforeProcessingTimeFrames() {
    myDistributions = new ArrayList<LoadDistribution>();
    getPrimitiveContainer().clear();
    HumanResource[] resources = ((ChartModelResource) getChartModel()).getVisibleResources();
    for (HumanResource resource : resources) {
      LoadDistribution nextDistribution = resource.getLoadDistribution();
      myDistributions.add(nextDistribution);
    }
  }

  /**
   * Class to use as Model object to display the load percentage in the
   * rectangle.
   *
   * @author bbaranne
   */
  static class ResourceLoad {
    private float load;

    ResourceLoad(float load) {
      this.load = load;
    }

    public float getLoad() {
      return load;
    }

    @Override
    public String toString() {
      return Float.toString(load);
    }
  }

  private int calculateGap(HumanResource resource) {
    return resource.getAssignments().length * getConfig().getRowHeight();
  }
}
