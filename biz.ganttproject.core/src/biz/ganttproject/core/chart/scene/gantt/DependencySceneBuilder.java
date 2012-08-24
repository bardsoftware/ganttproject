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
package biz.ganttproject.core.chart.scene.gantt;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Line;
import biz.ganttproject.core.chart.canvas.Canvas.Line.Arrow;
import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;
import biz.ganttproject.core.chart.scene.BarChartActivity;
import biz.ganttproject.core.chart.scene.BarChartConnector;
import biz.ganttproject.core.chart.scene.Polyline;

/**
 * Renders dependency lines between tasks.
 *
 * @author Dmitry Barashev
 */
public class DependencySceneBuilder<T, D extends BarChartConnector<T, D>> {
  private final Canvas myTaskCanvas;
  private final Canvas myOutputCanvas;
  private final ChartApi myChartApi;
  private final TaskApi<T, D> myTaskApi;

  public static interface TaskApi<T, D> {
    boolean isMilestone(T task);
    Dimension getUnitVector(BarChartActivity<T> activity, D dependency);
    String getStyle(D dependency);
    Iterable<D> getConnectors(T task);
    List<T> getTasks();
  }

  public static interface ChartApi {
    int getBarHeight();
  }

  public DependencySceneBuilder(Canvas taskCanvas, Canvas outputCanvas, TaskApi<T, D> taskApi, ChartApi chartApi) {
    myTaskApi = taskApi;
    myChartApi = chartApi;
    //myVisibleTasks = visibleTasks;
    myTaskCanvas = taskCanvas;
    myOutputCanvas = outputCanvas;
  }

  public void build() {
    List<Polyline> dependencyDrawData = prepareDependencyDrawData();
    drawDependencies(dependencyDrawData);
  }


  public void drawDependencies(Collection<Polyline> connectors) {
    Canvas primitiveContainer = myOutputCanvas;
    for (Polyline connector : connectors) {
      Polyline.Vector dependantVector = connector.getEnd();
      Polyline.Vector dependeeVector = connector.getStart();
      // Determine the line style (depending on type of dependency)
      String lineStyle = connector.getStyleName();

      if (dependeeVector.getHProjection().reaches(dependantVector.getHProjection().getPoint())) {
        // when dependee.end <= dependant.start && dependency.type is
        // any
        // or dependee.end <= dependant.end && dependency.type==FF
        // or dependee.start >= dependant.end && dependency.type==SF
        int ysign = Integer.signum(dependantVector.getPoint().y - dependeeVector.getPoint().y);
        Point first = new Point(dependeeVector.getPoint().x, dependeeVector.getPoint().y);
        Point second = new Point(dependantVector.getPoint(-3).x, dependeeVector.getPoint().y);
        Point third = new Point(dependantVector.getPoint(-3).x, dependantVector.getPoint().y - ysign * myChartApi.getBarHeight() / 2);
        primitiveContainer.createLine(first.x, first.y, second.x, second.y).setStyle(lineStyle);
        Line secondLine = primitiveContainer.createLine(second.x, second.y, third.x, third.y);
        secondLine.setStyle(lineStyle);
        secondLine.setArrow(Arrow.FINISH);
      } else if (dependantVector.getHProjection().reaches(dependeeVector.getHProjection().getPoint(3))) {
        Point first = dependeeVector.getPoint(3);
        Point second = new Point(first.x, dependantVector.getPoint().y);
        primitiveContainer.createLine(dependeeVector.getPoint().x, dependeeVector.getPoint().y, first.x,
            first.y).setStyle(lineStyle);
        primitiveContainer.createLine(first.x, first.y, second.x, second.y).setStyle(lineStyle);
        Line line = primitiveContainer.createLine(second.x, second.y, dependantVector.getPoint().x,
            dependantVector.getPoint().y);
        line.setStyle(lineStyle);
        line.setArrow(Line.Arrow.FINISH);
      } else {
        Point first = dependeeVector.getPoint(3);
        Point forth = dependantVector.getPoint(3);
        Point second = new Point(first.x, (first.y + forth.y) / 2);
        Point third = new Point(forth.x, (first.y + forth.y) / 2);
        primitiveContainer.createLine(dependeeVector.getPoint().x, dependeeVector.getPoint().y, first.x,
            first.y).setStyle(lineStyle);
        primitiveContainer.createLine(first.x, first.y, second.x, second.y).setStyle(lineStyle);
        primitiveContainer.createLine(second.x, second.y, third.x, third.y).setStyle(lineStyle);
        primitiveContainer.createLine(third.x, third.y, forth.x, forth.y).setStyle(lineStyle);
        primitiveContainer.createLine(forth.x, forth.y, dependantVector.getPoint().x,
            dependantVector.getPoint().y).setStyle(lineStyle);
      }
    }
  }

  private List<Polyline> prepareDependencyDrawData() {
    List<Polyline> result = new ArrayList<Polyline>();
    for (T t : myTaskApi.getTasks()) {
      if (t != null) {
        for (D td : myTaskApi.getConnectors(t)) {
          prepareDependencyDrawData(td, result);
        }
      }
    }
    return result;
  }

  private void prepareDependencyDrawData(D connector, List<Polyline> result) {
    BarChartActivity<T> dependant = connector.getEnd();
    BarChartActivity<T> dependee = connector.getStart();

    Canvas graphicPrimitiveContainer = myTaskCanvas;
    Canvas.Rectangle dependantRectangle = (Rectangle) graphicPrimitiveContainer.getPrimitive(dependant);
    if (dependantRectangle == null) {
      return;
    }
    Canvas.Rectangle dependeeRectangle = (Rectangle) graphicPrimitiveContainer.getPrimitive(dependee);
    if (dependeeRectangle == null) {
      return;
    }
    if (!dependantRectangle.isVisible() && !dependeeRectangle.isVisible()) {
      return;
    }
    Dimension dependantDirection = myTaskApi.getUnitVector(dependant, connector.getImpl());
    int dependantOriginX = (dependantDirection == Polyline.Vector.WEST) ? dependantRectangle.getLeftX() : dependantRectangle.getRightX();
    Point dependantOrigin = new Point(
        myTaskApi.isMilestone(dependant.getOwner()) ? dependantRectangle.getMiddleX() : dependantOriginX,
        dependantRectangle.getMiddleY());
    Polyline.Vector dependantVector = new Polyline.Vector(dependantOrigin, dependantDirection);

    Dimension dependeeDirection = myTaskApi.getUnitVector(dependee, connector.getImpl());
    int dependeeOriginX = (dependeeDirection == Polyline.Vector.WEST) ? dependeeRectangle.getLeftX() : dependeeRectangle.getRightX();
    Point dependeeOrigin = new Point(
        myTaskApi.isMilestone(dependee.getOwner()) ? dependeeRectangle.getMiddleX() : dependeeOriginX,
        dependeeRectangle.getMiddleY());
    Polyline.Vector dependeeVector = new Polyline.Vector(dependeeOrigin, dependeeDirection);
    result.add(new Polyline(dependeeVector, dependantVector, myTaskApi.getStyle(connector.getImpl())));
  }
}
