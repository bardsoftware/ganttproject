/*
 * This code is provided under the terms of GPL version 3.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.chart;

import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.TextGroup;
import net.sourceforge.ganttproject.chart.TimeUnitText;
import net.sourceforge.ganttproject.chart.timeline.TimeFormatters;
import net.sourceforge.ganttproject.chart.timeline.TimeFormatters.Position;
import net.sourceforge.ganttproject.util.TextLengthCalculator;

/**
 * Renders chart timeline.
 */
class ChartHeaderImpl extends ChartRendererBase implements ChartHeader {

  private GraphicPrimitiveContainer myPrimitiveContainer;
  private GraphicPrimitiveContainer myTimelineContainer;
  private GraphicPrimitiveContainer myBackgroundContainer;

  ChartHeaderImpl(ChartModelBase model) {
    super(model);
    myPrimitiveContainer = getPrimitiveContainer();
    myBackgroundContainer = myPrimitiveContainer.newLayer();
    myPrimitiveContainer.newLayer();
    myPrimitiveContainer.newLayer();
    myTimelineContainer = myPrimitiveContainer.newLayer();
  }

  public void beforeProcessingTimeFrames() {
    myPrimitiveContainer.clear();
    createGreyRectangleWithNiceBorders();
  }

  public GraphicPrimitiveContainer getTimelineContainer() {
    return myTimelineContainer;
  }

  /**
   * Draws the timeline box
   */
  private void createGreyRectangleWithNiceBorders() {
    int sizex = getWidth();
    final int spanningHeaderHeight = getChartModel().getChartUIConfiguration().getSpanningHeaderHeight();
    final int headerHeight = getChartModel().getChartUIConfiguration().getHeaderHeight();
    GraphicPrimitiveContainer container = myTimelineContainer;
    GraphicPrimitiveContainer.Rectangle headerRectangle = container.createRectangle(0, 0, sizex, headerHeight);
    headerRectangle.setBackgroundColor(getChartModel().getChartUIConfiguration().getSpanningHeaderBackgroundColor());

    GraphicPrimitiveContainer.Rectangle timeunitHeaderBorder = container.createRectangle(0, spanningHeaderHeight,
        sizex - 1, spanningHeaderHeight);
    timeunitHeaderBorder.setForegroundColor(getChartModel().getChartUIConfiguration().getHeaderBorderColor());
    //
    // GraphicPrimitiveContainer.Line middleGutter1 = getTimelineContainer()
    // .createLine(1, spanningHeaderHeight - 1, sizex - 2, spanningHeaderHeight
    // - 1);
    // middleGutter1.setForegroundColor(getChartModel()
    // .getChartUIConfiguration().getHorizontalGutterColor1());
    //
    GraphicPrimitiveContainer.Line bottomGutter = getTimelineContainer().createLine(0, headerHeight - 1, sizex - 2,
        headerHeight - 1);
    bottomGutter.setForegroundColor(getChartModel().getChartUIConfiguration().getHorizontalGutterColor1());
  }

  public GraphicPrimitiveContainer paint() {
    return myPrimitiveContainer;
  }

  @Override
  public void render() {
    beforeProcessingTimeFrames();
    renderTopUnits();
    renderBottomUnits();
  }

  /**
   * Draws cells of the top line in the time line
   */
  private void renderTopUnits() {
    Date curDate = getChartModel().getStartDate();
    List<Offset> topOffsets = getChartModel().getTopUnitOffsets();
    int curX = topOffsets.get(0).getOffsetPixels();
    if (curX > 0) {
      curX = 0;
    }
    final int topUnitHeight = getChartModel().getChartUIConfiguration().getSpanningHeaderHeight();
    TextGroup textGroup = myTimelineContainer.createTextGroup(0, 0, topUnitHeight, "timeline.top");
    for (Offset nextOffset : topOffsets) {
      if (curX >= 0) {
        TimeUnitText[] texts = TimeFormatters.getFormatter(nextOffset.getOffsetUnit(), Position.UPPER_LINE).format(
            nextOffset.getOffsetUnit(), curDate);
        final int maxWidth = nextOffset.getOffsetPixels() - curX - 5;
        final TimeUnitText timeUnitText = texts[0];
        textGroup.addText(curX + 5, 0, new TextSelector() {
          @Override
          public GraphicPrimitiveContainer.Label[] getLabels(TextLengthCalculator textLengthCalculator) {
            return timeUnitText.getLabels(maxWidth, textLengthCalculator);
          }
        });
        getTimelineContainer().createLine(curX, topUnitHeight - 10, curX, topUnitHeight);
      }
      curX = nextOffset.getOffsetPixels();
      curDate = nextOffset.getOffsetEnd();
    }
  }

  /**
   * Draws cells of the bottom line in the time line
   */
  private void renderBottomUnits() {
    BottomUnitLineRendererImpl bottomUnitLineRenderer = new BottomUnitLineRendererImpl(getChartModel(),
        myTimelineContainer, getPrimitiveContainer());
    bottomUnitLineRenderer.setHeight(getHeight());
    bottomUnitLineRenderer.render();
  }
}