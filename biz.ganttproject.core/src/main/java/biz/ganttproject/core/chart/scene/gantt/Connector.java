/*
Copyright 2012 GanttProject Team

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

/**
 * Represents a line connecting two bars.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class Connector {

  /**
   * Vector is an origin point + direction
   */
  public static class Vector {
    public static final Dimension WEST = new Dimension(-1, 0);
    public static final Dimension EAST = new Dimension(1, 0);
    private final Point myPoint;
    private final Dimension myUnitVector;
    private final Vector myHProjection;

    Vector(Point point, Dimension unitVector) {
      myPoint = point;
      myUnitVector = unitVector;
      myHProjection = unitVector.height == 0 && point.y == 0 ? null :
          new Vector(new Point(point.x, 0), new Dimension(unitVector.width, 0));
    }

    Point getPoint() {
      return myPoint;
    }

    /**
     * @return {@code true} if target point either equals to this vector origin or
     *         resides in a quarter-plane where this vector direction points to
     */
    boolean reaches(Point targetPoint) {
      return myPoint.equals(targetPoint)
          || (Integer.signum(targetPoint.x - myPoint.x) == Integer.signum(myUnitVector.width)
              && Integer.signum(targetPoint.y - myPoint.y) == Integer.signum(myUnitVector.height));
    }

    Point getPoint(int units) {
      return new Point(myPoint.x + myUnitVector.width * units, myPoint.y + myUnitVector.height * units);
    }

    /**
     * @return horizontal projection of this vector, with origin's y coordinate and moving direction
     *         set to zero
     */
    Vector getHProjection() {
      return myHProjection == null ? this : myHProjection;
    }

    @Override
    public String toString() {
      final StringBuffer sb = new StringBuffer("Vector{");
      sb.append("myPoint=").append(myPoint);
      sb.append(", myVector=").append(myUnitVector);
      sb.append('}');
      return sb.toString();
    }
  }

  private final Vector myStart;
  private final Vector myEnd;
  private final String myStyle;

  Connector(Vector start, Vector end, String style) {
    myStart = start;
    myEnd = end;
    myStyle = style;
  }

  Vector getStart() {
    return myStart;
  }

  Vector getEnd() {
    return myEnd;
  }

  String getStyleName() {
    return myStyle;
  }
}
