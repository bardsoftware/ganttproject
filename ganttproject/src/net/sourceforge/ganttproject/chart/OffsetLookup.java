/* LICENSE: GPL2
Copyright (C) 2010 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.chart;

import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.chart.ChartModelBase.Offset;

/**
 * Finds the bounds of the given date range in the given list of offsets.
 *  
 * @author dbarashev (Dmitry Barashev)
 */
class OffsetLookup {
    private int findOffset(Date date, int start, int end, List<Offset> offsets) {
        for (int compare = date.compareTo(offsets.get(start).getOffsetEnd()); compare != 0; compare = date.compareTo(offsets.get(start).getOffsetEnd())) {
            if (end == start) {
                if (start!=0 && end!=offsets.size()-1) {
                    throw new IllegalStateException("end="+end+" start="+start+" date="+date+" offset="+offsets.get(start)+" #offsets="+offsets.size());
                }
                break;
            }
            if (end < start) {
                throw new IllegalStateException("end="+end+" start="+start+" date="+date+" offset="+offsets.get(start));
            }
            int diff = end - start;
            if (compare == 1) {
                start += diff == 1 ? 1 : diff/2;
            }
            else {
                end = start;
                start -= diff == 1 ? 1 : diff/2;
            }
        }
        return start;
    }

    int[] getBounds(Date startDate, Date endDate, List<Offset> offsets) {
        int end = offsets.size()-1;
        int start = 0;

        if (startDate.compareTo(offsets.get(start).getOffsetEnd()) > 0) {
            start = findOffset(startDate, start, end, offsets);
        }
        int leftX = offsets.get(start).getOffsetPixels();

        end = offsets.size()-1;
        if (endDate.compareTo(offsets.get(end).getOffsetEnd()) < 0) {
            end = findOffset(endDate, 0, end, offsets);
        }
        int rightX = offsets.get(end).getOffsetPixels();
        return new int[] {leftX, rightX};
    }
}
