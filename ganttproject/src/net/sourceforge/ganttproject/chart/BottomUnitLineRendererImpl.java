/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2004-2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.chart;

import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Text;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.TextGroup;
import net.sourceforge.ganttproject.chart.timeline.TimeFormatter;
import net.sourceforge.ganttproject.chart.timeline.TimeFormatters;
import net.sourceforge.ganttproject.chart.timeline.TimeFormatters.Position;
import net.sourceforge.ganttproject.time.TimeUnitText;
import net.sourceforge.ganttproject.time.gregorian.GPTimeUnitStack;
import net.sourceforge.ganttproject.util.TextLengthCalculator;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public class BottomUnitLineRendererImpl extends ChartRendererBase {
    private GraphicPrimitiveContainer myTimelineContainer;

    public BottomUnitLineRendererImpl(ChartModel model, GraphicPrimitiveContainer primitiveContainer) {
        this(model, primitiveContainer, primitiveContainer);
    }

    public BottomUnitLineRendererImpl(
            ChartModel model,
            GraphicPrimitiveContainer timelineContainer,
            GraphicPrimitiveContainer primitiveContainer) {
        super(model);
        myTimelineContainer = timelineContainer;
    }

    @Override
    public GraphicPrimitiveContainer getPrimitiveContainer() {
        return myTimelineContainer;
    }

    @Override
    public void render() {
        Offset prevOffset = null;
        List<Offset> bottomOffsets = getBottomUnitOffsets();
        int xpos = bottomOffsets.get(0).getOffsetPixels();
        if (xpos > 0) {
            xpos = 0;
        }
        TextGroup textGroup = myTimelineContainer.createTextGroup(
                0, getLineTopPosition(), getConfig().getSpanningHeaderHeight(), "timeline.bottom.major_label", "timeline.bottom.minor_label");
        for (Offset offset : bottomOffsets) {
            renderScaleMark(offset, prevOffset);
            renderLabel(textGroup, xpos, offset.getOffsetStart(), offset);
            prevOffset = offset;
            xpos = prevOffset.getOffsetPixels();
        }
    }

    private void renderLabel(TextGroup textGroup, int curX, Date curDate, Offset curOffset) {
        final int maxWidth = curOffset.getOffsetPixels() - curX;
        TimeFormatter formatter = TimeFormatters.getFormatter(curOffset.getOffsetUnit(), Position.LOWER_LINE);
        TimeUnitText[] texts = formatter.format(curOffset.getOffsetUnit(), curDate);
        for (int i = 0; i < texts.length; i++) {
            final TimeUnitText timeUnitText = texts[i];
            GraphicPrimitiveContainer.Text text = new Text(curX + 2, i, new TextSelector() {
                @Override
                public GraphicPrimitiveContainer.Label[] getLabels(TextLengthCalculator textLengthCalculator) {
                    return timeUnitText.getLabels(maxWidth, textLengthCalculator);
                }
            });
            textGroup.addText(text);
        }
    }

    private void renderScaleMark(Offset offset, Offset prevOffset) {
        if (prevOffset == null) {
            return;
        }
        if (offset.getOffsetUnit() == GPTimeUnitStack.DAY) {
            if (offset.getDayType() != GPCalendar.DayType.WORKING
                    || prevOffset.getDayType() != GPCalendar.DayType.WORKING) {
                return;
            }
        }
        myTimelineContainer.createLine(
                prevOffset.getOffsetPixels(), getLineTopPosition(),
                prevOffset.getOffsetPixels(), getLineTopPosition()+10);
    }

    private int getLineTopPosition() {
        return getChartModel().getChartUIConfiguration().getSpanningHeaderHeight();
    }

    private List<Offset> getBottomUnitOffsets() {
        return getChartModel().getBottomUnitOffsets();
    }
}