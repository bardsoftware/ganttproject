package net.sourceforge.ganttproject.chart;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Line;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Line.Arrow;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;

public class TaskDependencyRenderer {
  private List<Task> myVisibleTasks;
  private GraphicPrimitiveContainer myTaskCanvas;
  private GraphicPrimitiveContainer myOutputCanvas;

  public TaskDependencyRenderer(List<Task> visibleTasks, GraphicPrimitiveContainer taskCanvas, GraphicPrimitiveContainer outputCanvas) {
    myVisibleTasks = visibleTasks;
    myTaskCanvas = taskCanvas;
    myOutputCanvas = outputCanvas;
  }

  void createDependencyLines() {
    List<DependencyDrawData> dependencyDrawData = prepareDependencyDrawData();
    drawDependencies(dependencyDrawData);
  }

  private void drawDependencies(List<DependencyDrawData> dependencyDrawData) {
    // if(dependencyDrawData.size() == 0)
    // System.out.println("VIDE");

    GraphicPrimitiveContainer primitiveContainer = myOutputCanvas;
    int arrowLength = 7;
    for (int i = 0; i < dependencyDrawData.size(); i++) {
      PointVector dependantVector;
      PointVector dependeeVector;
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

      if (dependeeVector.reaches(dependantVector.getPoint())) {
        // when dependee.end <= dependant.start && dependency.type is
        // any
        // or dependee.end <= dependant.end && dependency.type==FF
        // or dependee.start >= dependant.end && dependency.type==SF
        int ysign = signum(dependantVector.getPoint().y - dependeeVector.getPoint().y);
        Point first = new Point(dependeeVector.getPoint().x, dependeeVector.getPoint().y);
        Point second = new Point(dependantVector.getPoint(-3).x, dependeeVector.getPoint().y);
        Point third = new Point(dependantVector.getPoint(-3).x, dependantVector.getPoint().y);

        Line lastLine = null;
        if (dependantVector.reaches(third)) {
          second.x += arrowLength;
          third.x += arrowLength;
          Point forth = dependantVector.getPoint();
          lastLine = primitiveContainer.createLine(third.x, third.y, forth.x, forth.y);
          lastLine.setStyle(lineStyle);
//          arrowBoundary = new java.awt.Rectangle(forth.x, forth.y - 3, arrowLength, 6);
//          arrowStyle = "dependency.arrow.left";
        } else {
          third.y -= ysign * next.myDependantRectangle.myHeight / 2;

//          arrowBoundary = new java.awt.Rectangle(third.x - 3, third.y - (ysign > 0 ? ysign * arrowLength : 0), 6,
//              arrowLength);
//          arrowStyle = ysign > 0 ? "dependency.arrow.down" : "dependency.arrow.up";
        }
        primitiveContainer.createLine(first.x, first.y, second.x, second.y).setStyle(lineStyle);
        Line secondLine = primitiveContainer.createLine(second.x, second.y, third.x, third.y);
        secondLine.setStyle(lineStyle);
        if (lastLine == null) {
          lastLine = secondLine;
        }
        lastLine.setArrow(Arrow.FINISH);
//        Rectangle arrow = primitiveContainer.createRectangle(arrowBoundary.x, arrowBoundary.y, arrowBoundary.width,
//            arrowBoundary.height);
//        arrow.setStyle(arrowStyle);
      } else {
        Point first = dependeeVector.getPoint(3);
        if (dependantVector.reaches(first)) {
          Point second = new Point(first.x, dependantVector.getPoint().y);
          line = primitiveContainer.createLine(dependeeVector.getPoint().x, dependeeVector.getPoint().y,
              first.x, first.y);
          line.setStyle(lineStyle);
          line = primitiveContainer.createLine(first.x, first.y, second.x, second.y);
          line.setStyle(lineStyle);
          line = primitiveContainer.createLine(second.x, second.y, dependantVector.getPoint().x,
              dependantVector.getPoint().y);
          line.setStyle(lineStyle);
          int xsign = signum(dependantVector.getPoint().x - second.x);
          java.awt.Rectangle arrowBoundary = new java.awt.Rectangle(dependantVector.getPoint(7).x,
              dependantVector.getPoint().y - 3, xsign * 7, 6);
          Rectangle arrow = primitiveContainer.createRectangle(arrowBoundary.x, arrowBoundary.y, arrowBoundary.width,
              arrowBoundary.height);
          arrow.setStyle(xsign < 0 ? "dependency.arrow.left" : "dependency.arrow.right");
        } else {
          Point forth = dependantVector.getPoint(3);
          Point second = new Point(first.x, (first.y + forth.y) / 2);
          Point third = new Point(forth.x, (first.y + forth.y) / 2);
          line = primitiveContainer.createLine(dependeeVector.getPoint().x, dependeeVector.getPoint().y,
              first.x, first.y);
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

  private final int signum(int value) {
    if (value == 0) {
      return 0;
    }
    return value < 0 ? -1 : 1;
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
      if (dependant.getTask().isMilestone()) {
        dependant = new MilestoneTaskFakeActivity(dependant.getTask());
      }
      GraphicPrimitiveContainer graphicPrimitiveContainer = myTaskCanvas;
      GraphicPrimitiveContainer.Rectangle dependantRectangle = (Rectangle) graphicPrimitiveContainer.getPrimitive(dependant);
      if (dependantRectangle == null) {
        // System.out.println("dependantRectangle == null");
        continue;
      }
      TaskActivity dependee = activityBinding.getDependeeActivity();
      if (dependee.getTask().isMilestone()) {
        dependee = new MilestoneTaskFakeActivity(dependee.getTask());
      }
      GraphicPrimitiveContainer.Rectangle dependeeRectangle = (Rectangle) graphicPrimitiveContainer.getPrimitive(dependee);
      if (dependeeRectangle == null) {
        // System.out.println("dependeeRectangle == null");
        continue;
      }
      if (!dependantRectangle.isVisible() && !dependeeRectangle.isVisible()) {
        continue;
      }
      Date[] bounds = activityBinding.getAlignedBounds();
      PointVector dependantVector;
      if (bounds[0].equals(dependant.getStart())) {
        dependantVector = new WestPointVector(new Point(
            dependant.getTask().isMilestone() ? dependantRectangle.getMiddleX() : dependantRectangle.myLeftX, dependantRectangle.getMiddleY()));
      } else if (bounds[0].equals(dependant.getEnd())) {
        dependantVector = new EastPointVector(new Point(
            dependant.getTask().isMilestone() ? dependantRectangle.getMiddleX() : dependantRectangle.getRightX(), dependantRectangle.getMiddleY()));
      } else {
        throw new RuntimeException();
      }

      PointVector dependeeVector;
      if (bounds[1].equals(dependee.getStart())) {
        dependeeVector = new WestPointVector(new Point(
            dependee.getTask().isMilestone() ? dependeeRectangle.getMiddleX() : dependeeRectangle.myLeftX, dependeeRectangle.getMiddleY()));
      } else if (bounds[1].equals(dependee.getEnd())) {
        dependeeVector = new EastPointVector(new Point(
            dependee.getTask().isMilestone() ? dependantRectangle.getMiddleX() : dependeeRectangle.getRightX(), dependeeRectangle.getMiddleY()));
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
    final GraphicPrimitiveContainer.Rectangle myDependantRectangle;

    final TaskDependency myDependency;

    final PointVector myDependantVector;

    final PointVector myDependeeVector;

    public DependencyDrawData(TaskDependency dependency, GraphicPrimitiveContainer.GraphicPrimitive dependantPrimitive,
        PointVector dependantVector, PointVector dependeeVector) {
      myDependency = dependency;
      myDependantRectangle = (GraphicPrimitiveContainer.Rectangle) dependantPrimitive;
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

  private static abstract class PointVector {
    private final Point myPoint;

    protected PointVector(Point point) {
      myPoint = point;
    }

    Point getPoint() {
      return myPoint;
    }

    abstract PointVector moveOrigin(Point p);

    abstract boolean reaches(Point targetPoint);

    abstract Point getPoint(int i);
  }

  private static class WestPointVector extends PointVector {
    protected WestPointVector(Point point) {
      super(point);
    }

    @Override
    boolean reaches(Point targetPoint) {
      return targetPoint.x <= getPoint().x;
    }

    @Override
    Point getPoint(int diff) {
      return new Point(getPoint().x - diff, getPoint().y);
    }

    @Override
    public String toString() {
      return "<=" + getPoint().toString();
    }

    @Override
    PointVector moveOrigin(Point p) {
      return new WestPointVector(p);
    }
  }

  private static class EastPointVector extends PointVector {
    protected EastPointVector(Point point) {
      super(point);
    }

    @Override
    boolean reaches(Point targetPoint) {
      return targetPoint.x >= getPoint().x;
    }

    @Override
    Point getPoint(int diff) {
      return new Point(getPoint().x + diff, getPoint().y);
    }

    @Override
    public String toString() {
      return ">=" + getPoint().toString();
    }

    @Override
    PointVector moveOrigin(Point p) {
      return new EastPointVector(p);
    }
  }

}
