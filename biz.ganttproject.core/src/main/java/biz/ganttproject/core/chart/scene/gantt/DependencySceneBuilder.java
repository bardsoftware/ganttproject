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

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Line;
import biz.ganttproject.core.chart.scene.BarChartActivity;
import biz.ganttproject.core.chart.scene.BarChartConnector;
import biz.ganttproject.core.chart.scene.IdentifiableRow;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Renders dependency lines between tasks.
 *
 * @author Dmitry Barashev
 */
public class DependencySceneBuilder<T extends IdentifiableRow, D extends BarChartConnector<T, D>> {
  private final Canvas myTaskCanvas;
  private final Canvas myOutputCanvas;
  private final ChartApi myChartApi;
  private final TaskApi<T, D> myTaskApi;
  private int myBarHeight;
  private Canvas.Arrow myFinishArrow;

  public interface TaskApi<T extends IdentifiableRow, D> {
    boolean isMilestone(T task);
    Dimension getUnitVector(BarChartActivity<T> activity, D dependency);
    String getStyle(D dependency);
    Iterable<D> getConnectors(T task);
    List<T> getTasks();
  }

  public interface ChartApi {
    int getBarHeight();
  }

  public DependencySceneBuilder(Canvas taskCanvas, Canvas outputCanvas, TaskApi<T, D> taskApi, ChartApi chartApi) {
    myTaskApi = taskApi;
    myChartApi = chartApi;
    //myVisibleTasks = visibleTasks;
    myTaskCanvas = taskCanvas;
    myOutputCanvas = outputCanvas;
    myFinishArrow = Canvas.Arrow.FINISH;
    myBarHeight = -1;
  }

  public void build() {
    List<Connector> dependencyDrawData = prepareDependencyDrawData();
    drawDependencies(dependencyDrawData);
  }


  public void drawDependencies(Collection<Connector> connectors) {
    if (myChartApi.getBarHeight() != myBarHeight) {
      myFinishArrow = new Canvas.Arrow((int)(0.7f * myChartApi.getBarHeight()), (int)(0.3f*myChartApi.getBarHeight()));
      myBarHeight = myChartApi.getBarHeight();
    }
    Canvas primitiveContainer = myOutputCanvas;
    for (Connector connector : connectors) {
      Connector.Vector dependantVector = connector.getEnd();
      Connector.Vector dependeeVector = connector.getStart();
      // Determine the line style (depending on type of dependency)
      String lineStyle = connector.getStyleName();

      if (dependeeVector.getHProjection().reaches(dependantVector.getHProjection().getPoint())) {
        // when dependee.end <= dependant.start && dependency.type is
        // any
        // or dependee.end <= dependant.end && dependency.type==FF
        // or dependee.start >= dependant.end && dependency.type==SF
        Point first = new Point(dependeeVector.getPoint().x, dependeeVector.getPoint().y);

        int xEntry = dependantVector.getPoint().x;
        int yEntry = dependantVector.getPoint().y;
        Point second = new Point(xEntry, dependeeVector.getPoint().y);
        Point third = new Point(xEntry, yEntry);

        primitiveContainer.createLine(first.x, first.y, second.x, second.y).setStyle(lineStyle);
        Line secondLine = primitiveContainer.createLine(second.x, second.y, third.x, third.y);
        secondLine.setStyle(lineStyle);
        secondLine.setArrow(myFinishArrow);
      } else if (dependantVector.getHProjection().reaches(dependeeVector.getHProjection().getPoint(3))) {
        Point first = dependeeVector.getPoint(3);
        Point second = new Point(first.x, dependantVector.getPoint().y);
        primitiveContainer.createLine(dependeeVector.getPoint().x, dependeeVector.getPoint().y, first.x,
            first.y).setStyle(lineStyle);
        primitiveContainer.createLine(first.x, first.y, second.x, second.y).setStyle(lineStyle);
        Line line = primitiveContainer.createLine(second.x, second.y, dependantVector.getPoint().x,
            dependantVector.getPoint().y);
        line.setStyle(lineStyle);
        line.setArrow(myFinishArrow);
      } else {
        Point first = dependeeVector.getPoint(10);
        Point forth = dependantVector.getPoint(10);
        Point second = new Point(first.x, (first.y + forth.y) / 2);
        Point third = new Point(forth.x, (first.y + forth.y) / 2);
        primitiveContainer.createLine(dependeeVector.getPoint().x, dependeeVector.getPoint().y, first.x,
            first.y).setStyle(lineStyle);
        primitiveContainer.createLine(first.x, first.y, second.x, second.y).setStyle(lineStyle);
        primitiveContainer.createLine(second.x, second.y, third.x, third.y).setStyle(lineStyle);
        primitiveContainer.createLine(third.x, third.y, forth.x, forth.y).setStyle(lineStyle);
        Line lastLine = primitiveContainer.createLine(forth.x, forth.y, dependantVector.getPoint().x,
            dependantVector.getPoint().y);
        lastLine.setStyle(lineStyle);
        lastLine.setArrow(myFinishArrow);
      }
    }
  }

  private List<Connector> prepareDependencyDrawData() {
    List<Connector> result = new ArrayList<Connector>();
    for (T t : myTaskApi.getTasks()) {
      if (t != null) {
        for (D td : myTaskApi.getConnectors(t)) {
          prepareDependencyDrawData(td, result);
        }
      }
    }
    return result;
  }

  private void prepareDependencyDrawData(D connector, List<Connector> result) {
    BarChartActivity<T> dependant = connector.getEnd();
    BarChartActivity<T> dependee = connector.getStart();

    Canvas graphicPrimitiveContainer = myTaskCanvas;
    Canvas.Polygon dependantRectangle = (Canvas.Polygon) graphicPrimitiveContainer.getPrimitive(dependant);
    if (dependantRectangle == null) {
      return;
    }
    Canvas.Polygon dependeeRectangle = (Canvas.Polygon) graphicPrimitiveContainer.getPrimitive(dependee);
    if (dependeeRectangle == null) {
      return;
    }
    if (!dependantRectangle.isVisible() && !dependeeRectangle.isVisible()) {
      return;
    }
    if (!dependeeRectangle.isVisible() && dependantRectangle.getWidth() == 0) {
      return;
    }

    Connector c = createConnector(connector, dependant, dependee, dependantRectangle, dependeeRectangle, ConnectorEndArrow.VERTICAL);
    if (!c.getStart().getHProjection().reaches(c.getEnd().getHProjection().getPoint()) &&
        !c.getEnd().getHProjection().reaches(c.getStart().getHProjection().getPoint(3))) {
      c = createConnector(connector, dependant, dependee, dependantRectangle, dependeeRectangle, ConnectorEndArrow.HORIZONTAL);
    }
    result.add(c);
  }

  private enum ConnectorEndArrow { VERTICAL, HORIZONTAL }
  private Connector createConnector(D connector, BarChartActivity<T> dependant, BarChartActivity<T> dependee,
                                    Canvas.Polygon dependantRectangle, Canvas.Polygon dependeeRectangle, ConnectorEndArrow endArrrow) {
    final int ysign = Integer.signum(dependeeRectangle.getMiddleY() - dependantRectangle.getMiddleY());

    final int yDantEntry = endArrrow == ConnectorEndArrow.VERTICAL
        ? (ysign > 0 ? dependantRectangle.getBottomY() : dependantRectangle.getTopY())
        : dependantRectangle.getMiddleY();
    final Dimension dependantDirection = myTaskApi.getUnitVector(dependant, connector);
    int xDantEntry;
    if (myTaskApi.isMilestone(dependant.getOwner())) {
      xDantEntry = dependantRectangle.getMiddleX();
    } else if (dependantDirection == Connector.Vector.WEST) {
      xDantEntry = endArrrow == ConnectorEndArrow.VERTICAL ? dependantRectangle.getLeftX() + 3 : dependantRectangle.getLeftX();
    } else if (dependantDirection == Connector.Vector.EAST) {
      xDantEntry = endArrrow == ConnectorEndArrow.VERTICAL ? dependantRectangle.getRightX() - 3 : dependantRectangle.getRightX();
    } else {
      xDantEntry = dependantRectangle.getMiddleX();
    }
    Connector.Vector dependantVector = new Connector.Vector(new Point(xDantEntry, yDantEntry), dependantDirection);
    Dimension dependeeDirection = myTaskApi.getUnitVector(dependee, connector);
    int xDeeExit;
    int yDeeExit;
    if (myTaskApi.isMilestone(dependee.getOwner()) && xDantEntry == dependeeRectangle.getMiddleX()) {
      xDeeExit = xDantEntry;
      yDeeExit = ysign > 0 ? dependeeRectangle.getTopY() : dependeeRectangle.getBottomY();
    } else {
      yDeeExit = dependeeRectangle.getMiddleY();
      if (dependeeDirection == Connector.Vector.WEST) {
        xDeeExit = dependeeRectangle.getLeftX();
      } else if (dependeeDirection == Connector.Vector.EAST) {
        xDeeExit = dependeeRectangle.getRightX();
      } else {
        xDeeExit = dependeeRectangle.getMiddleX();
      }
    }
    Connector.Vector dependeeVector = new Connector.Vector(new Point(xDeeExit, yDeeExit), dependeeDirection);
    return new Connector(dependeeVector, dependantVector, myTaskApi.getStyle(connector.getImpl()));
  }

}
