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

import biz.ganttproject.core.calendar.GPCalendar.DayMask;
import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.TextGroup;
import biz.ganttproject.core.chart.canvas.TextMetrics;
import biz.ganttproject.core.chart.canvas.TextSelector;
import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.chart.grid.OffsetList;
import biz.ganttproject.core.chart.text.TimeFormatter;
import biz.ganttproject.core.chart.text.TimeUnitText;
import biz.ganttproject.core.chart.text.TimeUnitText.Position;
import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;

import java.util.Date;
import java.util.List;

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
    TimeFormatter formatter = null;
    TextGroup textGroup = null;

    for (Offset offset : bottomOffsets) {
      renderScaleMark(offset, prevOffset);
      if (formatter == null) {
        formatter = myInputApi.getFormatter(offset.getOffsetUnit(), TimeUnitText.Position.LOWER_LINE);
      }
      if (textGroup == null) {
        if (formatter.getTextCount() == 1) {
          textGroup = getCanvas().createTextGroup(0, getLineTopPosition(),
              myInputApi.getTopLineHeight(), "timeline.bottom.label");
        } else {
          textGroup = getCanvas().createTextGroup(0, getLineTopPosition(),
              myInputApi.getTopLineHeight(), "timeline.bottom.major_label", "timeline.bottom.minor_label");
        }
      }
      renderLabel(textGroup, xpos, offset.getOffsetStart(), offset, formatter);
      prevOffset = offset;
      xpos = prevOffset.getOffsetPixels();
    }
  }

  private void renderLabel(TextGroup textGroup, int curX, Date curDate, Offset curOffset, TimeFormatter formatter) {
    final int maxWidth = curOffset.getOffsetPixels() - curX;
    TimeUnitText[] texts = formatter.format(curOffset);
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

  // This method renders short ticks separating time unit offsets in the bottom line
  private void renderScaleMark(Offset offset, Offset prevOffset) {
    if (prevOffset == null) {
      return;
    }
    if (offset.getOffsetUnit() == GPTimeUnitStack.DAY) {
      // We do not want to paint tick around non-working days
      if ((offset.getDayMask() & DayMask.WORKING) == 0 || (prevOffset.getDayMask() & DayMask.WORKING) == 0) {
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
