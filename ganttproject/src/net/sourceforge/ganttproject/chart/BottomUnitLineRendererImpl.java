/*
 * Created on 13.11.2004
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Date;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.chart.ChartModelBase.Offset;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Line;
import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitText;

/**
 * @author bard
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
        int curX = 0;
        Date curDate = getChartModel().getStartDate();
        boolean firstWeekendDay = true;
        for (Offset nextOffset : getOffsets()) {
            if (!getChartModel().getTaskManager().getCalendar().isNonWorkingDay(nextOffset.getOffsetStart())) {
                renderWorkingDay(curX, curDate, nextOffset);
                firstWeekendDay = true;
            }
            if (nextOffset.getDayType() != GPCalendar.DayType.WORKING){
                renderNonWorkingDay(curX, nextOffset);
                if (firstWeekendDay) {
                    myTimelineContainer.createLine(
                            curX, getLineTopPosition(), curX, getLineTopPosition()+10);
                    firstWeekendDay = false;
                }
            }
            curX = nextOffset.getOffsetPixels();
            curDate = nextOffset.getOffsetEnd();
        }
    }

    private void renderNonWorkingDay(int curX, Offset curOffset) {
        GraphicPrimitiveContainer.Rectangle r =
            getPrimitiveContainer().createRectangle(
                    curX,
                    getLineBottomPosition()+1,
                    curOffset.getOffsetPixels() - curX,
                    getHeight());
        if (curOffset.getDayType() == GPCalendar.DayType.WEEKEND) {
            r.setBackgroundColor(getConfig().getHolidayTimeBackgroundColor());
        }
        else if (curOffset.getDayType() == GPCalendar.DayType.HOLIDAY) {
            r.setBackgroundColor(getConfig().getPublicHolidayTimeBackgroundColor());
        }
        r.setStyle("calendar.holiday");
        getPrimitiveContainer().bind(r, curOffset.getDayType());
    }

    private void renderWorkingDay(int curX, Date curDate, Offset curOffset) {
        TimeUnitText timeUnitText = curOffset.getOffsetUnit().format(curDate);
        String unitText = timeUnitText.getText(-1);
        int posY = getTextBaselinePosition();
        GraphicPrimitiveContainer.Text text = myTimelineContainer.createText(
                curX + 2, posY, unitText);
        myTimelineContainer.bind(text, timeUnitText);
        text.setMaxLength(curOffset.getOffsetPixels() - curX);
        text.setFont(getChartModel().getChartUIConfiguration().getSpanningHeaderFont());
        myTimelineContainer.createLine(
                curX, getLineTopPosition(), curX, getLineTopPosition()+10);
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

    protected Iterable<Offset> getOffsets() {
        return getChartModel().getBottomUnitOffsets();
    }
}