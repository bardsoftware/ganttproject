/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010 Dmitry Barashev

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
package biz.ganttproject.core.chart.grid;

import java.util.ArrayList;

public class OffsetList extends ArrayList<Offset> {
  private int myStartPx;

  void setStartPx(int startPx) {
    myStartPx = startPx;
  }

  public int getStartPx() {
    return myStartPx;
  }

  public int getEndPx() {
    return get(size() - 1).getOffsetPixels();
  }

  @Override
  public void clear() {
    super.clear();
    myStartPx = 0;
  }

  public void shift(int shiftPixels) {
    for (Offset o : this) {
      o.shift(shiftPixels);
    }
    myStartPx += shiftPixels;
  }
}
