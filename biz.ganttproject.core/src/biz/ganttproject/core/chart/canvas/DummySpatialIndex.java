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

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * Implements a simple index with get() method working in O(N)
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class DummySpatialIndex<T> implements SpatialIndex<T>{

  private static class Rect<T> {
    final T myObject;
    final int myBottomY;
    private int myWidth;
    private int myHeight;
    private int myLeftX;

    Rect(T object, int leftX, int bottomY, int width, int height) {
      myObject = object;
      myBottomY = bottomY;
      myLeftX = leftX;
      myWidth = width;
      myHeight = height;
    }

    @Override
    public String toString() {
      return "x=" + myLeftX + " y=" + myBottomY + " width=" + myWidth;
    }
  }

  private final List<Rect<T>> myRects = Lists.newArrayList();
  private final List<T> myValues = Lists.newArrayList();
  
  @Override
  public void put(T data, int x, int y, int width, int height) {
    myRects.add(new Rect<T>(data, x, y, width, height));
    myValues.add(data);
  }

  @Override
  public T get(int x, int y) {
    return get(x, 0, y, 0);
  }

  public T get(int x, int xpadding, int y, int ypadding) {
    for (Rect<T> r : myRects) {
      if (r.myLeftX <= x + xpadding && r.myLeftX + r.myWidth >= x - xpadding 
          && r.myBottomY >= y - ypadding && r.myBottomY - r.myHeight <= y + ypadding) {
        return r.myObject;
      }
    }
    return null;    
  }
  public void clear() {
    myRects.clear();
    myValues.clear();
  }

  public Collection<T> values() {
    return myValues;
  }
}
