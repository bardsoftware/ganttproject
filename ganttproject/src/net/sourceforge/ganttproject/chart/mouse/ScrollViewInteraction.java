/*
GanttProject is an opensource project management tool. License: GPL2
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
package net.sourceforge.ganttproject.chart.mouse;

import java.awt.event.MouseEvent;

import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitFunctionOfDate;

public class ScrollViewInteraction extends MouseInteractionBase
        implements MouseInteraction {
    private final ScrollingManager myScrollingManager;
    private final TimeUnit myBottomUnit;
    private int myStartX;

    public ScrollViewInteraction(
            MouseEvent e, TimelineFacade chartDateGrid, ScrollingManager scrollingManager, TimeUnit bottomUnit) {
        super(chartDateGrid.getDateAt(e.getX()), chartDateGrid);
        myScrollingManager = scrollingManager;
        myBottomUnit = bottomUnit;
        myStartX = e.getX();
    }

    public void apply(MouseEvent event) {
    	TaskLength scrollInterval = getLengthDiff(event);
    	if (scrollInterval.getLength() == 0) {
    	    myScrollingManager.scrollBy(event.getX() - myStartX);
    	    myStartX = event.getX();
    		return;
    	}
    	TimeUnit bottomUnit = myBottomUnit;
    	if (bottomUnit instanceof TimeUnitFunctionOfDate) {
    	    bottomUnit = ((TimeUnitFunctionOfDate)bottomUnit).createTimeUnit(getChartDateGrid().getDateAt(event.getX()));
    	}
    	if (Math.abs(scrollInterval.getLength(bottomUnit)) >= 1) {
            myScrollingManager.scrollBy(scrollInterval.reverse());
            setStartDate(getChartDateGrid().getDateAt(event.getX()));
    	} else {
            myScrollingManager.scrollBy(event.getX() - myStartX);
            myStartX = event.getX();    	    
    	}
    }

    public void finish() {
    }

}