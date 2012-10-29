/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2003-2012 GanttProject Team

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
package biz.ganttproject.core.chart.canvas;

/**
 * Spatial index which associates rectangular areas with values
 * and supports search by points.
 *
 * X-coordinates grom from the left to the right, Y-coordinates grom from the top to the bottom,
 * just like in Swing graphics model.
 *
 * @author dbarashev (Dmitry Barashev)
 *
 * @param <T> value type
 */
public interface SpatialIndex<T> {
  /**
   * Inserts a rectangle into the index and associates it with a value.
   *
   * @param value value object
   * @param leftX left edge of a rectangle
   * @param bottomY bottom (bigger) edge of a rectangle
   * @param width rectangle width
   * @param height rectangle height
   */
  void put(T value, int leftX, int bottomY, int width, int height);

  /**
   * Returns value object if there is a rectangle which contains the given point
   *
   * @param x search X-coordinate
   * @param y search Y-coordinate
   * @return value object containing the given point or {@code null} otherwise
   */
  T get(int x, int y);
  
  T get(int x, int xpadding, int y, int ypadding);
}
