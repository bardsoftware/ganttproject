/*
 * This code is provided under the terms of GPL version 2.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Line;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.time.TimeUnitText;

/**
 * Renders chart timeline.
 */
class ChartHeaderImpl extends ChartRendererBase implements ChartHeader {

    private GraphicPrimitiveContainer myPrimitiveContainer;
    private final BooleanOption myRedlineOption;
    private final BooleanOption myProjectDatesOption;
    private GPOptionGroup myOptions;
    private GraphicPrimitiveContainer myTimelineContainer;

    ChartHeaderImpl(ChartModel model, final UIConfiguration projectConfig) {
        super(model);
        myRedlineOption = projectConfig.getRedlineOption();
        myProjectDatesOption= projectConfig.getProjectBoundariesOption();
        myOptions = new ChartOptionGroup(
                "ganttChartGridDetails",
                new GPOption[] {myRedlineOption, myProjectDatesOption, projectConfig.getWeekendAlphaRenderingOption()},
                model.getOptionEventDispatcher());
        myPrimitiveContainer = new GraphicPrimitiveContainer();
    }
    GPOptionGroup getOptions() {
        return myOptions;
    }

    public GraphicPrimitiveContainer getPrimitiveContainer() {
        return myPrimitiveContainer;
    }

    public void beforeProcessingTimeFrames() {
        myPrimitiveContainer = new GraphicPrimitiveContainer(0, 0);
        myPrimitiveContainer.newLayer();
        myPrimitiveContainer.newLayer();
        myTimelineContainer = myPrimitiveContainer.newLayer();
        createGreyRectangleWithNiceBorders();
    }

    private GraphicPrimitiveContainer getTimelineContainer() {
        return myTimelineContainer;
    }
    /** Draws the timeline box
     */
    private void createGreyRectangleWithNiceBorders() {
        int sizex = getWidth();
        final int spanningHeaderHeight = getChartModel().getChartUIConfiguration()
                .getSpanningHeaderHeight();
        final int headerHeight =  getChartModel().getChartUIConfiguration().getHeaderHeight();
        GraphicPrimitiveContainer container = getPrimitiveContainer().getLayer(0);
        GraphicPrimitiveContainer.Rectangle headerRectangle = container
                .createRectangle(0, 0, sizex, headerHeight);
        headerRectangle.setBackgroundColor(getChartModel()
                .getChartUIConfiguration().getSpanningHeaderBackgroundColor());

        GraphicPrimitiveContainer.Rectangle timeunitHeaderBorder = container
                .createRectangle(0, spanningHeaderHeight, sizex - 1,
                        spanningHeaderHeight);
        timeunitHeaderBorder.setForegroundColor(getChartModel()
                .getChartUIConfiguration().getHeaderBorderColor());
        //
//        GraphicPrimitiveContainer.Line middleGutter1 = getTimelineContainer()
//                .createLine(1, spanningHeaderHeight - 1, sizex - 2, spanningHeaderHeight - 1);
//        middleGutter1.setForegroundColor(getChartModel()
//                .getChartUIConfiguration().getHorizontalGutterColor1());
        //
        GraphicPrimitiveContainer.Line bottomGutter = getTimelineContainer()
                .createLine(0, headerHeight-1, sizex - 2, headerHeight-1);
        bottomGutter.setForegroundColor(getChartModel()
                .getChartUIConfiguration().getHorizontalGutterColor1());
    }

    public GraphicPrimitiveContainer paint() {
        return myPrimitiveContainer;
    }

    public void render() {
        beforeProcessingTimeFrames();
        renderTopUnits();
        renderBottomUnits();
    }

    /** Draws cells of the top line in the time line
     */
    private void renderTopUnits() {
        Date curDate = getChartModel().getStartDate();
        List<Offset> topOffsets = getChartModel().getTopUnitOffsets();
        int curX = topOffsets.get(0).getOffsetPixels();
        if (curX > 0) {
            curX = 0;
        }
        final int topUnitHeight = getChartModel().getChartUIConfiguration().getSpanningHeaderHeight();
        for (Offset nextOffset : topOffsets) {
            if (curX >= 0) {
                TimeUnitText timeUnitText = nextOffset.getOffsetUnit().format(curDate);
                String unitText = timeUnitText.getText(-1);
                int posY = topUnitHeight - 5;
                GraphicPrimitiveContainer.Text text = getTimelineContainer().createText(curX + 5, posY, unitText);
                getTimelineContainer().bind(text, timeUnitText);
                text.setMaxLength(nextOffset.getOffsetPixels() - curX -5 );
                text.setFont(getChartModel().getChartUIConfiguration().getSpanningHeaderFont());
                getTimelineContainer().createLine(curX, topUnitHeight-10, curX, topUnitHeight);
            }
            curX = nextOffset.getOffsetPixels();
            curDate = nextOffset.getOffsetEnd();
        }
    }

    private void renderLine(Date date, Color color) {
        final int topUnitHeight = getChartModel().getChartUIConfiguration().getSpanningHeaderHeight();
        //boolean firstWeekendDay = true;
//        Date now = new Date();
        OffsetLookup lookup = new OffsetLookup();
        int todayOffsetIdx = lookup.lookupOffsetByEndDate(date, getChartModel().getDefaultUnitOffsets());
        if (todayOffsetIdx < 0) {
            todayOffsetIdx = -todayOffsetIdx - 1;
        }
        Offset yesterdayOffset = todayOffsetIdx == 0 ? null : getChartModel().getDefaultUnitOffsets().get(todayOffsetIdx - 1);
        if (yesterdayOffset == null) {
            return;
        }
        int yesterdayEndPixel = yesterdayOffset.getOffsetPixels();
        Line line = getPrimitiveContainer().createLine(
            yesterdayEndPixel + 2, topUnitHeight*2,
            yesterdayEndPixel + 2, getHeight()+topUnitHeight*2);
        line.setForegroundColor(color);

    }
    /** Draws cells of the bottom line in the time line
     */
    private void renderBottomUnits() {
        BottomUnitLineRendererImpl bottomUnitLineRenderer =
            new BottomUnitLineRendererImpl(getChartModel(), getPrimitiveContainer().getLayer(1), getPrimitiveContainer());
        bottomUnitLineRenderer.setHeight(getHeight());
        bottomUnitLineRenderer.render();
        if (myRedlineOption.isChecked()) {
            renderLine(new Date(), Color.RED);
        }
        if (isProjectBoundariesOptionOn()) {
            renderLine(getChartModel().getTaskManager().getProjectStart(), Color.BLUE);
            renderLine(getChartModel().getTaskManager().getProjectEnd(), Color.BLUE);
        }
   }
    private boolean isProjectBoundariesOptionOn() {
        return myProjectDatesOption.isChecked();
    }
}