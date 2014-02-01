/*
Copyright (C) 2004-2012 GanttProject Team

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

import java.util.Date;
import java.util.List;

import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.TextMetrics;
import biz.ganttproject.core.chart.canvas.TextSelector;
import biz.ganttproject.core.chart.canvas.Canvas.TextGroup;
import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.chart.grid.OffsetList;
import biz.ganttproject.core.chart.text.TimeFormatter;
import biz.ganttproject.core.chart.text.TimeUnitText;
import biz.ganttproject.core.chart.text.TimeUnitText.Position;
import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public class BottomUnitSceneBuilder extends AbstractSceneBuilder {

  public static interface InputApi {
    int getTopLineHeight();
    OffsetList getBottomUnitOffsets();
    TimeFormatter getFormatter(TimeUnit offsetUnit, Position lowerLine);
  }

  private final InputApi myInputApi;

  public BottomUnitSceneBuilder(Canvas timelineCanvas, InputApi inputApi) {
    super(timelineCanvas);
    myInputApi = inputApi;
  }

  @Override
  public void build() {
    Offset prevOffset = null;
    List<Offset> bottomOffsets = getBottomUnitOffsets();
    int xpos = bottomOffsets.get(0).getOffsetPixels();
    if (xpos > 0) {
      xpos = 0;
    }
    TextGroup textGroup = getCanvas().createTextGroup(0, getLineTopPosition(),
        myInputApi.getTopLineHeight(), "timeline.bottom.major_label", "timeline.bottom.minor_label");
    for (Offset offset : bottomOffsets) {
      renderScaleMark(offset, prevOffset);
      renderLabel(textGroup, xpos, offset.getOffsetStart(), offset);
      prevOffset = offset;
      xpos = prevOffset.getOffsetPixels();
    }
  }

  private void renderLabel(TextGroup textGroup, int curX, Date curDate, Offset curOffset) {
    final int maxWidth = curOffset.getOffsetPixels() - curX;
    TimeFormatter formatter = myInputApi.getFormatter(curOffset.getOffsetUnit(), TimeUnitText.Position.LOWER_LINE);
    TimeUnitText[] texts = formatter.format(curOffset.getOffsetUnit(), curDate);
    for (int i = 0; i < texts.length; i++) {
      final TimeUnitText timeUnitText = texts[i];
      textGroup.addText(curX + 2, i, new TextSelector() {
        @Override
        public Canvas.Label[] getLabels(TextMetrics textLengthCalculator) {
          return timeUnitText.getLabels(maxWidth, textLengthCalculator);
        }
      });
    }
  }

  private void renderScaleMark(Offset offset, Offset prevOffset) {
    if (prevOffset == null) {
      return;
    }
    if (offset.getOffsetUnit() == GPTimeUnitStack.DAY) {
      if (offset.getDayType() != GPCalendar.DayType.WORKING || prevOffset.getDayType() != GPCalendar.DayType.WORKING) {
        return;
      }
    }
    getCanvas().createLine(prevOffset.getOffsetPixels(), getLineTopPosition(), prevOffset.getOffsetPixels(),
        getLineTopPosition() + 10);
  }

  private int getLineTopPosition() {
    return myInputApi.getTopLineHeight();
  }

  private OffsetList getBottomUnitOffsets() {
    return myInputApi.getBottomUnitOffsets();
  }
}