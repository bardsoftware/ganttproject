/*
Copyright 2010-2012 GanttProject Team

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
package biz.ganttproject.core.chart.scene.gantt;

import java.awt.Color;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Polygon;
import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;
import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.chart.grid.OffsetList;
import biz.ganttproject.core.chart.grid.OffsetLookup;
import biz.ganttproject.core.chart.render.AlphaRenderingOption;
import biz.ganttproject.core.chart.render.ShapeConstants;
import biz.ganttproject.core.chart.render.ShapePaint;
import biz.ganttproject.core.chart.scene.BarChartActivity;

/**
 * Renders task activity rectangles on the Gantt chart.
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class TaskActivitySceneBuilder<T, A extends BarChartActivity<T>> {
  private final Canvas myCanvas;
  private final TaskLabelSceneBuilder<T> myLabelsRenderer;
  private final Style myStyle;
  private final TaskApi<T, A> myTaskApi;
  private final ChartApi myChartApi;

  public static class Style {
    int marginTop;

    public Style(int marginTop) {
      this.marginTop = marginTop;
    }
  }

  public static interface TaskApi<T, A> {
    boolean isFirst(A activity);
    boolean isLast(A activity);
    boolean isVoid(A activity);

    boolean isCriticalTask(T task);
    boolean isProjectTask(T task);
    boolean isMilestone(T task);
    boolean hasNestedTasks(T task);
    boolean hasNotes(T task);
    Color getColor(T task);
  }

  public static interface ChartApi {
    Date getChartStartDate();
    Date getEndDate();
    OffsetList getBottomUnitOffsets();
    int getRowHeight();
    int getBarHeight();
    int getViewportWidth();
    AlphaRenderingOption getWeekendOpacityOption();
  }

  public TaskActivitySceneBuilder(TaskApi<T, A> taskApi, ChartApi chartApi, Canvas canvas,
      TaskLabelSceneBuilder<T> labelsRenderer, Style style) {
    myTaskApi = taskApi;
    myChartApi = chartApi;
    myStyle = style;
    myCanvas = canvas;
    myLabelsRenderer = labelsRenderer;
  }

  public List<Polygon> renderActivities(int rowNum, List<A> activities, OffsetList offsets) {
    List<Polygon> rectangles = Lists.newArrayList();
    for (A activity : activities) {
      if (myTaskApi.isFirst(activity) || myTaskApi.isLast(activity)) {
        if (myTaskApi.isVoid(activity)) {
          continue;
        }
      }
      final Polygon nextRectangle;
      if (activity.getEnd().compareTo(myChartApi.getChartStartDate()) <= 0) {
        nextRectangle = processActivityEarlierThanViewport(rowNum, activity);
      } else if (activity.getStart().compareTo(myChartApi.getEndDate()) >= 0) {
        nextRectangle = processActivityLaterThanViewport(rowNum, activity);
      } else {
        nextRectangle = processRegularActivity(rowNum, activity, offsets);
      }
      rectangles.add(nextRectangle);
    }
    return rectangles;
  }

  private Rectangle processActivityLaterThanViewport(int rowNum, BarChartActivity<T> nextActivity) {
    Canvas container = myCanvas;
    int startx = myChartApi.getBottomUnitOffsets().getEndPx() + 1;
    int topy = rowNum * getRowHeight() + 4;
    Rectangle rectangle = container.createRectangle(startx, topy, 1, getRowHeight());
    container.bind(rectangle, nextActivity);
    rectangle.setVisible(false);
    return rectangle;
  }

  private Rectangle processActivityEarlierThanViewport(int rowNum, BarChartActivity<T> nextActivity) {
    Canvas container = myCanvas;
    int startx = myChartApi.getBottomUnitOffsets().getStartPx() - 1;
    int topy = rowNum * getRowHeight() + 4;
    Rectangle rectangle = container.createRectangle(startx, topy, 1, getRowHeight());
    container.bind(rectangle, nextActivity);
    rectangle.setVisible(false);
    return rectangle;
  }

  private Polygon processRegularActivity(int rowNum, A activity, OffsetList offsets) {
    T nextTask = activity.getOwner();
    if (myTaskApi.isMilestone(nextTask) && !myTaskApi.isFirst(activity)) {
      return null;
    }
    java.awt.Rectangle nextBounds = getBoundingRectangle(rowNum, activity, offsets);
    myLabelsRenderer.stripVerticalLabelSpace(nextBounds);
    final int nextLength = nextBounds.width;
    final int topy = nextBounds.y + myStyle.marginTop;

    boolean nextHasNested = myTaskApi.hasNestedTasks(nextTask);
    Canvas container = myCanvas;

    Canvas.Polygon resultShape;
    
    if (myTaskApi.isMilestone(nextTask)) {
      //nextRectangle.setVisible(false);
      //System.err.println("milestone rect=" + nextRectangle);
      //container.bind(nextRectangle, activity);
      Canvas.Rectangle rect = container.createDetachedRectangle(nextBounds.x, topy, nextLength, getRectangleHeight());
      Canvas.Polygon p = container.createPolygon(rect.getLeftX() - 2, rect.getMiddleY(),
          rect.getLeftX() + 3, rect.getMiddleY() - 5,
          rect.getLeftX() + 8, rect.getMiddleY(),
          rect.getLeftX() + 3, rect.getMiddleY() + 5);
      p.setStyle("task.milestone");
      //container.bind(p, activity);
      resultShape = p;
    } else {
      Canvas.Rectangle nextRectangle = container.createRectangle(nextBounds.x, topy, nextLength, getRectangleHeight());
      resultShape = nextRectangle;
      if (myTaskApi.isProjectTask(nextTask)) {
        nextRectangle.setStyle("task.projectTask");
        if (myTaskApi.isFirst(activity)) {
          Canvas.Rectangle supertaskStart = container.createRectangle(nextRectangle.getLeftX(), topy,
              nextLength, getRectangleHeight());
          supertaskStart.setStyle("task.projectTask.start");
        }
        if (myTaskApi.isLast(activity)) {
          Canvas.Rectangle supertaskEnd = container.createRectangle(nextRectangle.getLeftX() - 1, topy,
              nextLength, getRectangleHeight());
          supertaskEnd.setStyle("task.projectTask.end");

        }
      } else if (nextHasNested) {
        nextRectangle.setStyle("task.supertask");
        if (myTaskApi.isFirst(activity)) {
          container.createPolygon(nextRectangle.getLeftX(), nextRectangle.getTopY(), 
              nextRectangle.getLeftX() + nextRectangle.getHeight(), nextRectangle.getTopY(), 
              nextRectangle.getLeftX(), nextRectangle.getBottomY()).setStyle("task.supertask.start");
        }
        if (myTaskApi.isLast(activity)) {
          container.createPolygon(nextRectangle.getRightX(), nextRectangle.getTopY(), 
              nextRectangle.getRightX() - nextRectangle.getHeight(), nextRectangle.getTopY(), 
              nextRectangle.getRightX(), nextRectangle.getBottomY()).setStyle("task.supertask.end");
        }
      } else {
        if (myTaskApi.isFirst(activity) && myTaskApi.isLast(activity)) {
          nextRectangle.setStyle("task.startend");
        } else if (false == myTaskApi.isFirst(activity) ^ myTaskApi.isLast(activity)) {
          nextRectangle.setStyle("task");
        } else if (myTaskApi.isFirst(activity)) {
          nextRectangle.setStyle("task.start");
        } else if (myTaskApi.isLast(activity)) {
          nextRectangle.setStyle("task.end");
        }
        if (myTaskApi.isVoid(activity)) {
          nextRectangle.setOpacity(myChartApi.getWeekendOpacityOption().getValueAsFloat());
        }
      }
    }
    
    if (!"task.holiday".equals(resultShape.getStyle()) && !"task.supertask".equals(resultShape.getStyle())) {
      resultShape.setBackgroundColor(myTaskApi.getColor(nextTask));
    }
    if (myTaskApi.isCriticalTask(activity.getOwner())) {
      if (myTaskApi.hasNestedTasks(activity.getOwner())) {
        resultShape.setBackgroundColor(Color.RED);
      } else if (resultShape instanceof Canvas.Rectangle){
        ((Canvas.Rectangle)resultShape).setBackgroundPaint(
            new ShapePaint(ShapeConstants.THICK_BACKSLASH, Color.BLACK, myTaskApi.getColor(activity.getOwner())));
      }
    }

    container.bind(resultShape, activity);    
    return resultShape;
  }

  private java.awt.Rectangle getBoundingRectangle(int rowNum, BarChartActivity<T> activity, List<Offset> offsets) {
    OffsetLookup offsetLookup = new OffsetLookup();
    int[] bounds = offsetLookup.getBounds(activity.getStart(), activity.getEnd(), offsets);
    int leftX = bounds[0];
    int rightX = bounds[1];
    int topY = rowNum * getRowHeight();
    return new java.awt.Rectangle(leftX, topY, rightX - leftX, getRowHeight());
  }

  private int getRectangleHeight() {
    return myChartApi.getBarHeight();
  }

  private int getRowHeight() {
    return myChartApi.getRowHeight();
  }
}
