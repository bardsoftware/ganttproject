/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2012 GanttProject Team

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

import java.awt.Color;
import java.util.Date;
import java.util.List;

import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.calendar.GPCalendar.DayMask;
import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Line;
import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;
import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.chart.grid.OffsetList;
import biz.ganttproject.core.chart.grid.OffsetLookup;
import biz.ganttproject.core.option.BooleanOption;

/**
 * Builds a scene consisting of vertical columns on the charts, such as weekend days, today line and
 * project boundaries.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class DayGridSceneBuilder extends AbstractSceneBuilder {
  private final BooleanOption myRedlineOption;
  private final BooleanOption myProjectDatesOption;
  private final Canvas myTimelineCanvas;
  private final InputApi myInputApi;

  public static interface InputApi {
    BooleanOption getRedlineOption();
    BooleanOption getProjectDatesOption();
    int getTopLineHeight();
    Color getWeekendColor();
    Color getHolidayColor(Date holiday);

    Date getProjectStart();
    Date getProjectEnd();

    OffsetList getAtomUnitOffsets();
    boolean hasEvent(Date offsetStart);
  }

  public DayGridSceneBuilder(InputApi inputApi, Canvas timelineCanvas) {
    myInputApi = inputApi;
    myRedlineOption = inputApi.getRedlineOption();
    myProjectDatesOption = inputApi.getProjectDatesOption();
    myTimelineCanvas = timelineCanvas;
  }

  @Override
  public void build() {
    if (myRedlineOption.isChecked()) {
      renderLine(new Date(), "timeline.today", 2, OffsetLookup.BY_END_DATE);
    }
    if (isProjectBoundariesOptionOn()) {
      renderLine(myInputApi.getProjectStart(), "timeline.project_start", -2, OffsetLookup.BY_START_DATE);
      renderLine(myInputApi.getProjectEnd(), "timeline.project_end", 2, OffsetLookup.BY_START_DATE);
    }
    renderNonWorkingDayColumns();
  }

  private void renderLine(Date date, String style, int marginPx, OffsetLookup.ComparatorBy<Date> dateComparator) {
    final int topUnitHeight = myInputApi.getTopLineHeight();
    OffsetLookup lookup = new OffsetLookup();
    int todayOffsetIdx = lookup.lookupOffsetBy(date, myInputApi.getAtomUnitOffsets(), dateComparator);
    if (todayOffsetIdx < 0) {
      todayOffsetIdx = -todayOffsetIdx - 1;
    }
    Offset yesterdayOffset = todayOffsetIdx == 0 ? null : myInputApi.getAtomUnitOffsets().get(
        todayOffsetIdx - 1);
    if (yesterdayOffset == null) {
      return;
    }
    int yesterdayEndPixel = yesterdayOffset.getOffsetPixels();
    Line line = getCanvas().createLine(yesterdayEndPixel + marginPx, topUnitHeight * 2,
        yesterdayEndPixel + marginPx, getHeight() + topUnitHeight * 2);
    line.setStyle(style);
  }

  // This method creates colored vertical stripes on the chart which correspond 
  // to weekend days and holidays. It is not necessary that colored stripe is 
  // a non-working day, though: e.g. we may show weekends but count them as working days.
  private void renderNonWorkingDayColumns() {
    List<Offset> defaultOffsets = myInputApi.getAtomUnitOffsets();
    int curX = defaultOffsets.get(0).getOffsetPixels();
    if (curX > 0) {
      curX = 0;
    }
    for (Offset offset : defaultOffsets) {
      int dayMask = offset.getDayMask();
      if ((dayMask & (DayMask.HOLIDAY | DayMask.WEEKEND)) != 0 || myInputApi.hasEvent(offset.getOffsetStart())) {
        // Create a holiday/weekend day bar in the main area
        renderNonWorkingDay(curX, offset);
        // And expand it to the timeline area.
        Rectangle r = myTimelineCanvas.createRectangle(curX, getLineTopPosition(), offset.getOffsetPixels()
            - curX, getLineBottomPosition() - getLineTopPosition());
        applyRectangleStyle(r, offset);
      }
      curX = offset.getOffsetPixels();
    }
  }

  private void renderNonWorkingDay(int curX, Offset curOffset) {
    Canvas.Rectangle r = getCanvas().createRectangle(curX, getLineBottomPosition(),
        curOffset.getOffsetPixels() - curX, getHeight());
    applyRectangleStyle(r, curOffset);
  }

  private void applyRectangleStyle(Rectangle r, Offset offset) {
    Color customColor = myInputApi.getHolidayColor(offset.getOffsetStart());
    if (customColor != null) {
      r.setBackgroundColor(customColor);
    }
    if ((offset.getDayMask() & DayMask.HOLIDAY) == DayMask.HOLIDAY) {
      r.setStyle("calendar.holiday");
      return;
    }
    if ((offset.getDayMask() & DayMask.WEEKEND) == DayMask.WEEKEND) {
      r.setStyle("calendar.weekend");
    }
  }

  private boolean isProjectBoundariesOptionOn() {
    return myProjectDatesOption.isChecked();
  }

  private int getLineTopPosition() {
    return myInputApi.getTopLineHeight();
  }

  private int getLineBottomPosition() {
    return getLineTopPosition() + getLineHeight();
  }

  private int getLineHeight() {
    return getLineTopPosition();
  }
}
