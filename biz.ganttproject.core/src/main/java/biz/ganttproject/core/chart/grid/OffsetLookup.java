/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010-2011 Dmitry Barashev, GanttProject Team

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

import java.util.Date;
import java.util.List;

/**
 * Finds the bounds of the given date range in the given list of offsets.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class OffsetLookup {
  public static interface ComparatorBy<T> {
    int compare(T point, int offsetIdx, List<Offset> offsets);
  }

  static class ComparatorByStartDate implements ComparatorBy<Date> {
    @Override
    public int compare(Date point, int offsetIdx, List<Offset> offsets) {
      return point.compareTo(offsets.get(offsetIdx).getOffsetStart());
    }
  }

  static class ComparatorByEndDate implements ComparatorBy<Date> {
    @Override
    public int compare(Date point, int offsetIdx, List<Offset> offsets) {
      return point.compareTo(offsets.get(offsetIdx).getOffsetEnd());
    }
  }

  public static final ComparatorBy<Date> BY_START_DATE = new ComparatorByStartDate();
  public static final ComparatorBy<Date> BY_END_DATE = new ComparatorByEndDate();

  private <Type> int findOffset(Type point, ComparatorBy<Type> comparator, int start, int end, List<Offset> offsets) {
    if (comparator.compare(point, end, offsets) > 0) {
      return -end - 2;
    }
    if (comparator.compare(point, start, offsets) < 0) {
      return start;
    }

    for (int compare = comparator.compare(point, start, offsets); compare != 0; compare = comparator.compare(point,
        start, offsets)) {
      if (end == start) {
        start = -start - 1;
        break;
      }
      if (end < start) {
        throw new IllegalStateException("end=" + end + " start=" + start + " date=" + point + " offset="
            + offsets.get(start));
      }
      int diff = end - start;
      if (compare == 1) {
        start += diff == 1 ? 1 : diff / 2;
      } else {
        end = start;
        start -= diff == 1 ? 1 : diff / 2;
      }
    }
    return start;
  }

  public int[] getBounds(Date startDate, Date endDate, List<Offset> offsets) {
    int end = offsets.size() - 1;
    int start = 0;

    ComparatorByEndDate comparator = new ComparatorByEndDate();

    if (startDate.compareTo(offsets.get(start).getOffsetEnd()) > 0) {
      start = findOffset(startDate, comparator, start, end, offsets);
    }
    if (start < 0) {
      start = -start - 1;
    }
    int leftX = start == offsets.size() ? offsets.get(start - 1).getOffsetPixels()
        : offsets.get(start).getOffsetPixels();

    end = offsets.size() - 1;
    if (endDate.compareTo(offsets.get(end).getOffsetEnd()) < 0) {
      end = findOffset(endDate, comparator, 0, end, offsets);
    }
    if (end < 0) {
      end = -end - 1;
    }
    int rightX = end == offsets.size() ? offsets.get(end - 1).getOffsetPixels() : offsets.get(end).getOffsetPixels();
    return new int[] { leftX, rightX };
  }

  public int lookupOffsetBy(Date date, List<Offset> offsets, ComparatorBy<Date> comparator) {
    return findOffset(date, comparator, 0, offsets.size() - 1, offsets);
  }

  public int lookupOffsetByStartDate(Date startDate, List<Offset> offsets) {
    ComparatorByStartDate comparator = new ComparatorByStartDate();
    return findOffset(startDate, comparator, 0, offsets.size() - 1, offsets);
  }

  public int lookupOffsetByEndDate(Date endDate, List<Offset> offsets) {
    ComparatorByEndDate comparator = new ComparatorByEndDate();
    return findOffset(endDate, comparator, 0, offsets.size() - 1, offsets);
  }

  static class ComparatorByPixels implements ComparatorBy<Integer> {
    @Override
    public int compare(Integer point, int offsetIdx, List<Offset> offsets) {
      Offset offset = offsets.get(offsetIdx);
      if (offset.getOffsetPixels() > point) {
        return 1;
      }
      if (offsetIdx == offsets.size() - 1) {
        return 0;
      }
      Offset rightOffset = offsets.get(offsetIdx + 1);
      if (rightOffset.getOffsetPixels() > point) {
        return 0;
      }
      return -1;
    }
  }

  Date lookupDateByPixels(int pixels, List<Offset> offsets) {
    int offsetIdx = findOffset(pixels, new ComparatorByPixels(), 0, offsets.size() - 1, offsets);
    if (offsetIdx < 0) {
      offsetIdx = -offsetIdx - 1;
    }
    Offset offset = offsets.get(offsetIdx);
    return offset.getOffsetStart();
  }
}
