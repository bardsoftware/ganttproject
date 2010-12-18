/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2004-2010 Dmitry Barashev

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

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.chart.ChartModelBase.Offset;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.time.TimeUnitText;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public class BottomUnitLineRendererImpl extends ChartRendererBase {
    private final GraphicPrimitiveContainer myPrimitiveContainer;
    private GraphicPrimitiveContainer myTimelineContainer;

    public BottomUnitLineRendererImpl(ChartModel model, GraphicPrimitiveContainer primitiveContainer) {
        this(model, primitiveContainer, primitiveContainer);
    }

    public BottomUnitLineRendererImpl(
            ChartModel model,
            GraphicPrimitiveContainer timelineContainer,
            GraphicPrimitiveContainer primitiveContainer) {
        super(model);
        myPrimitiveContainer = primitiveContainer;
        myTimelineContainer = timelineContainer;
    }

    @Override
    public GraphicPrimitiveContainer getPrimitiveContainer() {
        return myPrimitiveContainer;
    }

    public void render() {
        Offset prevOffset = null;
        for (Offset offset : getBottomUnitOffsets()) {
            int xpos = prevOffset == null ? 0 : prevOffset.getOffsetPixels();
            if (offset.getDayType() == GPCalendar.DayType.WORKING) {
                renderWorkingDay(xpos, offset, prevOffset);
            }
            renderLabel(xpos, offset.getOffsetStart(), offset);
            prevOffset = offset;
        }
        renderNonWorkingDayColumns();
    }
    
    private void renderLabel(int curX, Date curDate, Offset curOffset) {
        TimeUnitText timeUnitText = curOffset.getOffsetUnit().format(curDate);
        String unitText = timeUnitText.getText(-1);
        int posY = getTextBaselinePosition();
        GraphicPrimitiveContainer.Text text = myTimelineContainer.createText(
                curX + 2, posY, unitText);
        myTimelineContainer.bind(text, timeUnitText);
        text.setMaxLength(curOffset.getOffsetPixels() - curX);
        text.setFont(getChartModel().getChartUIConfiguration().getSpanningHeaderFont());
    }

    private void renderNonWorkingDayColumns() {
        int curX = 0;
        for (Offset offset : getChartModel().getDefaultUnitOffsets()) {
            if (offset.getDayType() != GPCalendar.DayType.WORKING){
                renderNonWorkingDay(curX, offset);
                    Rectangle r = myTimelineContainer.createRectangle(
                            curX, getLineTopPosition() + 1, 
                            offset.getOffsetPixels() - curX, 
                            getLineBottomPosition() - getLineTopPosition() + 1);
                    applyRectangleStyle(r, offset.getDayType());
            }
            curX = offset.getOffsetPixels();
        }
    }

    private void renderNonWorkingDay(int curX, Offset curOffset) {
        GraphicPrimitiveContainer.Rectangle r =
            getPrimitiveContainer().createRectangle(
                    curX,
                    getLineBottomPosition(),
                    curOffset.getOffsetPixels() - curX,
                    getHeight());
        applyRectangleStyle(r, curOffset.getDayType());
        getPrimitiveContainer().bind(r, curOffset.getDayType());
    }

    private void applyRectangleStyle(Rectangle r, GPCalendar.DayType dayType) {
        if (dayType == GPCalendar.DayType.WEEKEND) {
            r.setBackgroundColor(getConfig().getHolidayTimeBackgroundColor());
        }
        else if (dayType == GPCalendar.DayType.HOLIDAY) {
            r.setBackgroundColor(getConfig().getPublicHolidayTimeBackgroundColor());
        }
        r.setStyle("calendar.holiday");        
    }
    private void renderWorkingDay(int curX, Offset offset, Offset prevOffset) {
        if (prevOffset != null && prevOffset.getDayType() == GPCalendar.DayType.WORKING) {
            myTimelineContainer.createLine(
                    prevOffset.getOffsetPixels(), getLineTopPosition(), 
                    prevOffset.getOffsetPixels(), getLineTopPosition()+10);
        }
    }

    protected int getLineTopPosition() {
        return getChartModel().getChartUIConfiguration().getSpanningHeaderHeight();
    }

    protected int getLineBottomPosition() {
        return getLineTopPosition() + getLineHeight();
    }

    protected int getLineHeight() {
        return getLineTopPosition();
    }

    protected int getTextBaselinePosition() {
        return getLineBottomPosition() - 5;
    }

    protected Iterable<Offset> getBottomUnitOffsets() {
        return getChartModel().getBottomUnitOffsets();
    }
}