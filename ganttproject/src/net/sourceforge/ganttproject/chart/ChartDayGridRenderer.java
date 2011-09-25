/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Dmitry Barashev, GanttProject Team

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

import java.awt.Color;
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Line;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;

/**
 * Renders vertical columns on the charts, such as weekend days, today line and project boundaries.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class ChartDayGridRenderer extends ChartRendererBase {
    private final BooleanOption myRedlineOption;
    private final BooleanOption myProjectDatesOption;
    private final GPOptionGroup myOptions;
    private final GraphicPrimitiveContainer myTimelineContainer;

    public ChartDayGridRenderer(ChartModel model, final UIConfiguration projectConfig,
            GraphicPrimitiveContainer timelineContainer) {
        myRedlineOption = projectConfig.getRedlineOption();
        myProjectDatesOption= projectConfig.getProjectBoundariesOption();
        myOptions = new ChartOptionGroup(
                "ganttChartGridDetails",
                new GPOption[] {myRedlineOption, myProjectDatesOption, projectConfig.getWeekendAlphaRenderingOption()},
                model.getOptionEventDispatcher());
        myTimelineContainer = timelineContainer;
    }

    GPOptionGroup getOptions() {
        return myOptions;
    }

    @Override
    public void render() {
        if (myRedlineOption.isChecked()) {
            renderLine(new Date(), Color.RED, 2, OffsetLookup.BY_END_DATE);
        }
        if (isProjectBoundariesOptionOn()) {
            renderLine(getChartModel().getTaskManager().getProjectStart(), Color.BLUE, -2, OffsetLookup.BY_START_DATE);
            renderLine(getChartModel().getTaskManager().getProjectEnd(), Color.BLUE, 2, OffsetLookup.BY_START_DATE);
        }
        renderNonWorkingDayColumns();
    }

    private void renderLine(Date date, Color color, int marginPx, OffsetLookup.ComparatorBy<Date> dateComparator) {
        final int topUnitHeight = getChartModel().getChartUIConfiguration().getSpanningHeaderHeight();
        OffsetLookup lookup = new OffsetLookup();
        int todayOffsetIdx = lookup.lookupOffsetBy(date, getChartModel().getDefaultUnitOffsets(), dateComparator);
        if (todayOffsetIdx < 0) {
            todayOffsetIdx = -todayOffsetIdx - 1;
        }
        Offset yesterdayOffset = todayOffsetIdx == 0 ? null : getChartModel().getDefaultUnitOffsets().get(todayOffsetIdx - 1);
        if (yesterdayOffset == null) {
            return;
        }
        int yesterdayEndPixel = yesterdayOffset.getOffsetPixels();
        Line line = getPrimitiveContainer().createLine(
            yesterdayEndPixel + marginPx, topUnitHeight*2,
            yesterdayEndPixel + marginPx, getHeight()+topUnitHeight*2);
        line.setForegroundColor(color);

    }

    private void renderNonWorkingDayColumns() {
        List<Offset> defaultOffsets = getChartModel().getDefaultUnitOffsets();
        int curX = defaultOffsets.get(0).getOffsetPixels();
        if (curX > 0) {
            curX = 0;
        }
        for (Offset offset : defaultOffsets) {
            if (offset.getDayType() != GPCalendar.DayType.WORKING){
                // Create a non-working day bar in the main area
                renderNonWorkingDay(curX, offset);
                // And expand it to the timeline area.
                Rectangle r = myTimelineContainer.createRectangle(
                    curX, getLineTopPosition() + 1,
                    offset.getOffsetPixels() - curX,
                    getLineBottomPosition() - getLineTopPosition() + 1);
                //System.err.println(offset.getDayType()+": " + r);
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

    private boolean isProjectBoundariesOptionOn() {
        return myProjectDatesOption.isChecked();
    }
    private int getLineTopPosition() {
        return getChartModel().getChartUIConfiguration().getSpanningHeaderHeight();
    }

    private int getLineBottomPosition() {
        return getLineTopPosition() + getLineHeight();
    }

    private int getLineHeight() {
        return getLineTopPosition();
    }
}
