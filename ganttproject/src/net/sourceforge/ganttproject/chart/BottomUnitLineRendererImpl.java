/*
 * Created on 13.11.2004
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import net.sourceforge.ganttproject.calendar.CalendarFactory;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.chart.ChartModelBase.Offset;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Line;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitText;
import net.sourceforge.ganttproject.time.gregorian.FramerImpl;

/**
 * @author bard
 */
public class BottomUnitLineRendererImpl extends ChartRendererBase {
    private final GraphicPrimitiveContainer myPrimitiveContainer;
    private final FramerImpl myDayFramer = new FramerImpl(Calendar.DAY_OF_MONTH);
    private final BooleanOption myRedlineOption;
    private final BooleanOption myProjectDatesOption;
    private Date myToday;

    public BottomUnitLineRendererImpl(ChartModelBase model, UIConfiguration projectConfig) {
    	this(model, new GraphicPrimitiveContainer(), projectConfig);
    }
    public BottomUnitLineRendererImpl(ChartModelBase model, GraphicPrimitiveContainer primitiveContainer, UIConfiguration projectConfig) {
        super(model);
        myPrimitiveContainer = primitiveContainer;
        myRedlineOption = projectConfig.getRedlineOption();
        myProjectDatesOption= projectConfig.getProjectBoundariesOption();
    }

    public GraphicPrimitiveContainer getPrimitiveContainer() {
        return myPrimitiveContainer;
    }

    public void render() {
        myToday = myDayFramer.adjustLeft(CalendarFactory.newCalendar().getTime());
    	getPrimitiveContainer().clear();
        int curX = 0;
        Date curDate = getChartModel().getStartDate();
        final int topUnitHeight = getLineTopPosition();
        boolean firstWeekendDay = true;
        List offsets = getOffsets();
        for (int i=0; i<offsets.size(); i++) {
            Offset nextOffset = (Offset) offsets.get(i);
            if (nextOffset.getDayType() == GPCalendar.DayType.WEEKEND ||
            		nextOffset.getDayType() == GPCalendar.DayType.HOLIDAY) {
                GraphicPrimitiveContainer.Rectangle r =
                    getPrimitiveContainer().createRectangle(
                            curX,
                            getLineBottomPosition()+1,
                            nextOffset.getOffsetPixels() - curX,
                            getHeight());
                Color background = nextOffset.getDayType()==GPCalendar.DayType.WEEKEND ?
                		getConfig().getHolidayTimeBackgroundColor() :
                        getConfig().getPublicHolidayTimeBackgroundColor();
                r.setBackgroundColor(background);
                r.setStyle("calendar.holiday");
                getPrimitiveContainer().bind(r, nextOffset.getDayType());
//                if (firstWeekendDay) {
//                    getPrimitiveContainer().createLine(
//                            curX, getLineTopPosition(), curX, getLineTopPosition()+10);
//                    firstWeekendDay = false;
//                }
            }
//            else {
//                TimeUnitText timeUnitText = nextOffset.getOffsetUnit().format(curDate);
//                String unitText = timeUnitText.getText(-1);
//                int posY = getTextBaselinePosition();
//                GraphicPrimitiveContainer.Text text = getPrimitiveContainer().createText(
//                        curX + 2, posY, unitText);
//                getPrimitiveContainer().bind(text, timeUnitText);
//                text.setMaxLength(nextOffset.getOffsetPixels() - curX);
//                text.setFont(getChartModel().getChartUIConfiguration().getSpanningHeaderFont());
//                getPrimitiveContainer().createLine(
//                        curX, getLineTopPosition(), curX, getLineTopPosition()+10);
//                firstWeekendDay = true;
//            }

            if (curDate.equals(myToday) && myRedlineOption.isChecked()) {
                Line redLine = getPrimitiveContainer().createLine(
                        curX+2, getLineBottomPosition()+1, curX+2, getHeight());
                redLine.setForegroundColor(Color.RED);
            }
            if ((curDate.equals(getChartModel().getTaskManager().getProjectStart()) ||
                    curDate.equals(getChartModel().getTaskManager().getProjectEnd())) &&
                isProjectBoundariesOptionOn()) {
                Line blueLine = getPrimitiveContainer().createLine(
                        curX, getLineBottomPosition()+1, curX, getHeight());
                blueLine.setForegroundColor(Color.BLUE);
            }

            curX = nextOffset.getOffsetPixels();
            curDate = nextOffset.getOffsetEnd();
            //System.err.println("curDate="+curDate+" curX="+curX);
        }
    }

    protected int getLineTopPosition() {
        return getChartModel().getChartUIConfiguration().getSpanningHeaderHeight();
    }

    protected int getLineBottomPosition() {
        return 0;
    }

    protected int getLineHeight() {
        return getLineTopPosition();
    }

    protected int getTextBaselinePosition() {
        return getLineBottomPosition() - 5;
    }

    protected List getOffsets() {
        return getChartModel().getDefaultUnitOffsets();
    }

    private boolean isProjectBoundariesOptionOn() {
        return myProjectDatesOption.isChecked();
    }

}
