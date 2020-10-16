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

import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.chart.scene.CapacityHeatmapSceneBuilder;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.LoadDistribution.Load;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Renders resource load chart
 */
class ResourceLoadRenderer extends ChartRendererBase {
  private final ResourceChart myResourcechart;
  private final ChartModelResource myModel;


  public ResourceLoadRenderer(ChartModelResource model, ResourceChart resourceChart) {
    super(model);
    myResourcechart = resourceChart;
    myModel = model;
  }

  /**
   * Renders load distribution one by one, from top of the chart downwards If
   * some resource is expanded, calls rendering of the load details
   */
  @Override
  public void render() {
    CapacityHeatmapSceneBuilder capacityHeatmap = new CapacityHeatmapSceneBuilder(
            getCapacityHeatmapSceneInput(), getResources(), getPrimitiveContainer()
    );
    capacityHeatmap.build();
  }

  private CapacityHeatmapSceneBuilder.InputApi getCapacityHeatmapSceneInput() {
    return new CapacityHeatmapSceneBuilder.InputApi() {
      @NotNull
      @Override
      public List<Offset> getOffsets() {
        return getChartModel().getDefaultUnitOffsets();
      }

      @NotNull
      @Override
      public Date getChartEndDate() {
        return getChartModel().getBottomUnitOffsets().get(getChartModel().getBottomUnitOffsets().size() - 1).getOffsetEnd();
      }

      @NotNull
      @Override
      public Date getChartStartDate() {
        return getChartModel().getStartDate();
      }

      @Override
      public int getChartWidth() {
        return (int) getChartModel().getBounds().getWidth();
      }

      @Override
      public int getRowHeight() {
        return getConfig().getRowHeight();
      }

      @Override
      public int getYCanvasOffset() {
        return getConfig().getHeaderHeight() - myModel.getVerticalOffset();
      }
    };
  }

  private List<CapacityHeatmapSceneBuilder.Resource> getResources() {
    HumanResource[] humanResources = ((ChartModelResource) getChartModel()).getVisibleResources();
    List<CapacityHeatmapSceneBuilder.Resource> resources = new ArrayList<>();

    for (HumanResource humanResource : humanResources) {
      List<CapacityHeatmapSceneBuilder.Load> loads = new ArrayList<>();
      List<Load> tasksLoads = humanResource.getLoadDistribution().getTasksLoads();
      for (Load taskLoad : tasksLoads) {
        Integer taskId = null;
        if (taskLoad.refTask != null) {
          taskId = taskLoad.refTask.getTaskID();
        }
        CapacityHeatmapSceneBuilder.Load load = new CapacityHeatmapSceneBuilder.Load(taskLoad.startDate.getTime(), taskLoad.endDate.getTime(), taskLoad.load, taskId);
        loads.add(load);
      }
      boolean isExpanded = myResourcechart.isExpanded(humanResource);
      CapacityHeatmapSceneBuilder.Resource resource = new CapacityHeatmapSceneBuilder.Resource(loads, isExpanded);
      resources.add(resource);
    }

    return resources;
  }
}
