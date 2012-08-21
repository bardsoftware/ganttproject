/*
 * This code is provided under the terms of GPL version 3.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.util.Date;
import java.util.List;

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.TextMetrics;
import biz.ganttproject.core.chart.canvas.TextSelector;
import biz.ganttproject.core.chart.canvas.Canvas.TextGroup;
import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.chart.grid.OffsetList;
import biz.ganttproject.core.chart.scene.AbstractSceneBuilder;

import net.sourceforge.ganttproject.chart.TimeUnitText;
import net.sourceforge.ganttproject.chart.timeline.TimeFormatters;
import net.sourceforge.ganttproject.chart.timeline.TimeFormatters.Position;

/**
 * Renders chart timeline.
 */
class ChartHeaderImpl extends AbstractSceneBuilder implements ChartHeader {

  private Canvas myTimelineContainer;
  private final InputApi myInputApi;
  private final BottomUnitLineRendererImpl myBottomUnitLineRenderer;

  public static interface InputApi {
    int getViewportWidth();
    int getTopLineHeight();
    int getTimelineHeight();
    Color getTimelineBackgroundColor();
    Color getTimelineBorderColor();

    Date getViewportStartDate();
    OffsetList getTopUnitOffsets();
    OffsetList getBottomUnitOffsets();

  }
  ChartHeaderImpl(InputApi inputApi) {
    myInputApi = inputApi;
    getCanvas().newLayer();
    getCanvas().newLayer();
    getCanvas().newLayer();
    myTimelineContainer = getCanvas().newLayer();
    myBottomUnitLineRenderer = new BottomUnitLineRendererImpl(
        myTimelineContainer, new BottomUnitLineRendererImpl.InputApi() {
          @Override
          public int getTopLineHeight() {
            return myInputApi.getTopLineHeight();
          }

          @Override
          public OffsetList getBottomUnitOffsets() {
            return myInputApi.getBottomUnitOffsets();
          }
        });
  }

  public Canvas getTimelineContainer() {
    return myTimelineContainer;
  }

  @Override
  public void reset(int sceneHeight) {
    super.reset(sceneHeight);
    myBottomUnitLineRenderer.reset(getHeight());
  }

  /**
   * Draws the timeline box
   */
  private void renderUnderlay() {
    int sizex = myInputApi.getViewportWidth();
    final int spanningHeaderHeight = myInputApi.getTopLineHeight();
    final int headerHeight = myInputApi.getTimelineHeight();
    Canvas container = myTimelineContainer;
    Canvas.Rectangle headerRectangle = container.createRectangle(0, 0, sizex, headerHeight);
    headerRectangle.setBackgroundColor(myInputApi.getTimelineBackgroundColor());

    Canvas.Rectangle timeunitHeaderBorder = container.createRectangle(0, spanningHeaderHeight,
        sizex - 1, spanningHeaderHeight);
    timeunitHeaderBorder.setForegroundColor(myInputApi.getTimelineBorderColor());
    //
    // GraphicPrimitiveContainer.Line middleGutter1 = getTimelineContainer()
    // .createLine(1, spanningHeaderHeight - 1, sizex - 2, spanningHeaderHeight
    // - 1);
    // middleGutter1.setForegroundColor(getChartModel()
    // .getChartUIConfiguration().getHorizontalGutterColor1());
    //
    Canvas.Line bottomBorder = getTimelineContainer().createLine(0, headerHeight - 1, sizex - 2,
        headerHeight - 1);
    bottomBorder.setStyle("timeline.borderBottom");
  }

//  public Canvas paint() {
//    return myPrimitiveContainer;
//  }

  @Override
  public void build() {
    renderUnderlay();
    renderTopUnits();
    renderBottomUnits();
  }

  /**
   * Draws cells of the top line in the time line
   */
  private void renderTopUnits() {
    Date curDate = myInputApi.getViewportStartDate();
    List<Offset> topOffsets = myInputApi.getTopUnitOffsets();
    int curX = topOffsets.get(0).getOffsetPixels();
    if (curX > 0) {
      curX = 0;
    }
    final int topUnitHeight = myInputApi.getTopLineHeight();
    TextGroup textGroup = myTimelineContainer.createTextGroup(0, 0, topUnitHeight, "timeline.top");
    for (Offset nextOffset : topOffsets) {
      if (curX >= 0) {
        TimeUnitText[] texts = TimeFormatters.getFormatter(nextOffset.getOffsetUnit(), Position.UPPER_LINE).format(
            nextOffset.getOffsetUnit(), curDate);
        final int maxWidth = nextOffset.getOffsetPixels() - curX - 5;
        final TimeUnitText timeUnitText = texts[0];
        textGroup.addText(curX + 5, 0, new TextSelector() {
          @Override
          public Canvas.Label[] getLabels(TextMetrics textLengthCalculator) {
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
    myBottomUnitLineRenderer.build();
  }
}