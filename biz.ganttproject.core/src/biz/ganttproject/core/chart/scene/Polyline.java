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
package biz.ganttproject.core.chart.scene;

import java.awt.Dimension;
import java.awt.Point;

/**
 * Represents a line connecting two bars.
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class Polyline {
  
  /**
   * Vector is an origin point + direction
   */
  public static class Vector {
    public static final Dimension WEST = new Dimension(-1, 0);
    public static final Dimension EAST = new Dimension(1, 0);
    private final Point myPoint;
    private final Dimension myUnitVector;
    private final Vector myHProjection;

    public Vector(Point point, Dimension unitVector) {
      myPoint = point;
      myUnitVector = unitVector;
      myHProjection = unitVector.height == 0 && point.y == 0 ? null : 
          new Vector(new Point(point.x, 0), new Dimension(unitVector.width, 0));
    }

    public Point getPoint() {
      return myPoint;
    }

    /**
     * @return {@code true} if target point either equals to this vector origin or 
     *         resides in a quarter-plane where this vector direction points to
     */
    public boolean reaches(Point targetPoint) {
      return myPoint.equals(targetPoint) 
          || (Integer.signum(targetPoint.x - myPoint.x) == Integer.signum(myUnitVector.width) 
              && Integer.signum(targetPoint.y - myPoint.y) == Integer.signum(myUnitVector.height)); 
    }

    public Point getPoint(int units) {
      return new Point(myPoint.x + myUnitVector.width * units, myPoint.y + myUnitVector.height * units);
    }

    /**
     * @return horizontal projection of this vector, with origin's y coordinate and moving direction 
     *         set to zero
     */
    public Vector getHProjection() {
      return myHProjection == null ? this : myHProjection;
    }
  }

  private final Vector myStart;
  private final Vector myEnd;
  private final String myStyle;
  
  public Polyline(Vector start, Vector end, String style) {
    myStart = start;
    myEnd = end;
    myStyle = style;
  }
  
  public Vector getStart() {
    return myStart;
  }
  
  public Vector getEnd() {
    return myEnd;
  }
  
  public String getStyleName() {
    return myStyle;
  }
}
