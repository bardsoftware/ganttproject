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
package net.sourceforge.ganttproject.chart;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Line;
import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;
import biz.ganttproject.core.chart.canvas.Canvas.Line.Arrow;
import biz.ganttproject.core.chart.scene.BarChartConnector;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;

/**
 * Renders dependency lines between tasks.
 *
 * @author Dmitry Barashev
 */
class TaskDependencyRenderer {
  private final List<Task> myVisibleTasks;
  private final Canvas myTaskCanvas;
  private final Canvas myOutputCanvas;

  public TaskDependencyRenderer(List<Task> visibleTasks, Canvas taskCanvas,
      Canvas outputCanvas) {
    myVisibleTasks = visibleTasks;
    myTaskCanvas = taskCanvas;
    myOutputCanvas = outputCanvas;
  }

  void createDependencyLines() {
    List<DependencyDrawData> dependencyDrawData = prepareDependencyDrawData();
    drawDependencies(dependencyDrawData);
  }

  private void drawDependencies(List<DependencyDrawData> dependencyDrawData) {
    Canvas primitiveContainer = myOutputCanvas;
    int arrowLength = 7;
    for (int i = 0; i < dependencyDrawData.size(); i++) {
      BarChartConnector.Vector dependantVector;
      BarChartConnector.Vector dependeeVector;
      DependencyDrawData next = dependencyDrawData.get(i);

      dependantVector = next.myDependantVector;
      dependeeVector = next.myDependeeVector;
      // Determine the line style (depending on type of dependency)
      Line line;
      String lineStyle;
      if (next.myDependency.getHardness() == TaskDependency.Hardness.RUBBER) {
        lineStyle = "dependency.line.rubber";
      } else {
        lineStyle = "dependency.line.hard";
      }

      if (dependeeVector.getHProjection().reaches(dependantVector.getHProjection().getPoint())) {
        // when dependee.end <= dependant.start && dependency.type is
        // any
        // or dependee.end <= dependant.end && dependency.type==FF
        // or dependee.start >= dependant.end && dependency.type==SF
        int ysign = Integer.signum(dependantVector.getPoint().y - dependeeVector.getPoint().y);
        Point first = new Point(dependeeVector.getPoint().x, dependeeVector.getPoint().y);
        Point second = new Point(dependantVector.getPoint(-3).x, dependeeVector.getPoint().y);
        Point third = new Point(dependantVector.getPoint(-3).x, dependantVector.getPoint().y);

        Line lastLine = null;
        if (dependantVector.getHProjection().reaches(new Point(third.x, 0))) {
          second.x += arrowLength;
          third.x += arrowLength;
          Point forth = dependantVector.getPoint();
          lastLine = primitiveContainer.createLine(third.x, third.y, forth.x, forth.y);
          lastLine.setStyle(lineStyle);
        } else {
          third.y -= ysign * next.myDependantRectangle.myHeight / 2;
        }
        primitiveContainer.createLine(first.x, first.y, second.x, second.y).setStyle(lineStyle);
        Line secondLine = primitiveContainer.createLine(second.x, second.y, third.x, third.y);
        secondLine.setStyle(lineStyle);
        if (lastLine == null) {
          lastLine = secondLine;
        }
        lastLine.setArrow(Arrow.FINISH);
      } else {
        if (dependantVector.getHProjection().reaches(dependeeVector.getHProjection().getPoint(3))) {
          Point first = dependeeVector.getPoint(3);
          Point second = new Point(first.x, dependantVector.getPoint().y);
          line = primitiveContainer.createLine(dependeeVector.getPoint().x, dependeeVector.getPoint().y, first.x,
              first.y);
          line.setStyle(lineStyle);
          line = primitiveContainer.createLine(first.x, first.y, second.x, second.y);
          line.setStyle(lineStyle);
          line = primitiveContainer.createLine(second.x, second.y, dependantVector.getPoint().x,
              dependantVector.getPoint().y);
          line.setStyle(lineStyle);
          line.setArrow(Line.Arrow.FINISH);
        } else {
          Point first = dependeeVector.getPoint(3);
          Point forth = dependantVector.getPoint(3);
          Point second = new Point(first.x, (first.y + forth.y) / 2);
          Point third = new Point(forth.x, (first.y + forth.y) / 2);
          line = primitiveContainer.createLine(dependeeVector.getPoint().x, dependeeVector.getPoint().y, first.x,
              first.y);
          line = primitiveContainer.createLine(first.x, first.y, second.x, second.y);
          line.setStyle(lineStyle);
          line = primitiveContainer.createLine(second.x, second.y, third.x, third.y);
          line.setStyle(lineStyle);
          line = primitiveContainer.createLine(third.x, third.y, forth.x, forth.y);
          line.setStyle(lineStyle);
          line = primitiveContainer.createLine(forth.x, forth.y, dependantVector.getPoint().x,
              dependantVector.getPoint().y);
          line.setStyle(lineStyle);
        }
      }
    }
  }

  private List<DependencyDrawData> prepareDependencyDrawData() {
    List<DependencyDrawData> result = new ArrayList<DependencyDrawData>();
    for (Task nextTask : myVisibleTasks) {
      if (nextTask != null) {
        prepareDependencyDrawData(nextTask, result);
      }
    }
    return result;
  }

  private void prepareDependencyDrawData(Task task, List<DependencyDrawData> result) {
    TaskDependency[] deps = task.getDependencies().toArray();
    for (int i = 0; i < deps.length; i++) {
      TaskDependency next = deps[i];
      TaskDependency.ActivityBinding activityBinding = next.getActivityBinding();
      TaskActivity dependant = activityBinding.getDependantActivity();
      if (dependant.getOwner().isMilestone()) {
        dependant = new MilestoneTaskFakeActivity(dependant.getOwner());
      }
      Canvas graphicPrimitiveContainer = myTaskCanvas;
      Canvas.Rectangle dependantRectangle = (Rectangle) graphicPrimitiveContainer.getPrimitive(dependant);
      if (dependantRectangle == null) {
        // System.out.println("dependantRectangle == null");
        continue;
      }
      TaskActivity dependee = activityBinding.getDependeeActivity();
      if (dependee.getOwner().isMilestone()) {
        dependee = new MilestoneTaskFakeActivity(dependee.getOwner());
      }
      Canvas.Rectangle dependeeRectangle = (Rectangle) graphicPrimitiveContainer.getPrimitive(dependee);
      if (dependeeRectangle == null) {
        // System.out.println("dependeeRectangle == null");
        continue;
      }
      if (!dependantRectangle.isVisible() && !dependeeRectangle.isVisible()) {
        continue;
      }
      Date[] bounds = activityBinding.getAlignedBounds();
      BarChartConnector.Vector dependantVector;
      if (bounds[0].equals(dependant.getStart())) {
        Point origin = new Point(
            dependant.getOwner().isMilestone() ? dependantRectangle.getMiddleX() : dependantRectangle.myLeftX,
            dependantRectangle.getMiddleY());
        dependantVector = new BarChartConnector.Vector(origin, BarChartConnector.Vector.WEST);
      } else if (bounds[0].equals(dependant.getEnd())) {
        Point origin = new Point(
            dependant.getOwner().isMilestone() ? dependantRectangle.getMiddleX() : dependantRectangle.getRightX(),
            dependantRectangle.getMiddleY());
        dependantVector = new BarChartConnector.Vector(origin, BarChartConnector.Vector.EAST);
      } else {
        throw new RuntimeException();
      }

      BarChartConnector.Vector dependeeVector;
      if (bounds[1].equals(dependee.getStart())) {
        Point origin = new Point(
            dependee.getOwner().isMilestone() ? dependeeRectangle.getMiddleX() : dependeeRectangle.myLeftX,
            dependeeRectangle.getMiddleY());
        dependeeVector = new BarChartConnector.Vector(origin, BarChartConnector.Vector.WEST);
      } else if (bounds[1].equals(dependee.getEnd())) {
        Point origin = new Point(
            dependee.getOwner().isMilestone() ? dependantRectangle.getMiddleX() : dependeeRectangle.getRightX(),
            dependeeRectangle.getMiddleY());
        dependeeVector = new BarChartConnector.Vector(origin, BarChartConnector.Vector.EAST);
      } else {
        throw new RuntimeException("bounds: " + Arrays.asList(bounds) + " dependee=" + dependee + " dependant="
            + dependant);
      }
      // System.err.println("dependant rectangle="+dependantRectangle+"\ndependeeREctangle="+dependeeRectangle+"\ndependantVector="+dependantVector+"\ndependeeVector="+dependeeVector);
      DependencyDrawData data = new DependencyDrawData(next, dependantRectangle, dependantVector, dependeeVector);
      result.add(data);
    }
  }

  private static class DependencyDrawData {
    final Canvas.Rectangle myDependantRectangle;

    final TaskDependency myDependency;

    final BarChartConnector.Vector myDependantVector;

    final BarChartConnector.Vector myDependeeVector;

    public DependencyDrawData(TaskDependency dependency, Canvas.Shape dependantPrimitive,
        BarChartConnector.Vector dependantVector, BarChartConnector.Vector dependeeVector) {
      myDependency = dependency;
      myDependantRectangle = (Canvas.Rectangle) dependantPrimitive;
      myDependantVector = dependantVector;
      myDependeeVector = dependeeVector;
    }

    @Override
    public String toString() {
      return "From activity=" + myDependency.getActivityBinding().getDependantActivity() + " (vector="
          + myDependantVector + ")\n to activity=" + myDependency.getActivityBinding().getDependeeActivity()
          + " (vector=" + myDependeeVector;
    }
  }
}
