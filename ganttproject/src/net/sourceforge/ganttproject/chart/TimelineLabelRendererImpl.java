/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.TextMetrics;
import biz.ganttproject.core.chart.canvas.TextSelector;
import biz.ganttproject.core.chart.canvas.Canvas.HAlignment;
import biz.ganttproject.core.chart.canvas.Canvas.Label;
import biz.ganttproject.core.chart.canvas.Canvas.Text;
import biz.ganttproject.core.chart.canvas.Canvas.VAlignment;
import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.chart.grid.OffsetLookup;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;

/**
 * Renders labels on the timeline.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class TimelineLabelRendererImpl extends ChartRendererBase {
  private static final int MAX_TIMELINE_LABEL_WIDTH = 200;
  private OffsetLookup myOffsetLookup;
  private Canvas myLabelsLayer;
  private ChartModelApi myChartModel;

  /**
   * This class dependencies interface
   */
  protected static interface ChartModelApi {
    int getTimelineTopLineHeight();
    List<Offset> getDefaultUnitOffsets();
    Date getStartDate();
    Collection<Task> getTimelineTasks();
  }

  public TimelineLabelRendererImpl(ChartModelApi chartModel) {
    super(null);
    myChartModel = chartModel;
    myOffsetLookup = new OffsetLookup();
    getPrimitiveContainer().createLayers(4);
    myLabelsLayer = getPrimitiveContainer().getLayer(3);
  }

  @Override
  public void render() {
    List<Offset> offsets = myChartModel.getDefaultUnitOffsets();
    TaskActivity leadActivity = null;
    for (Task t : myChartModel.getTimelineTasks()) {
      for (TaskActivity activity : t.getActivities()) {
        if (activity.getIntensity() > 0f) {
          leadActivity = activity;
          break;
        }
      }
      if (leadActivity == null || leadActivity.getEnd().before(myChartModel.getStartDate())) {
        continue;
      }
      int[] bounds = myOffsetLookup.getBounds(leadActivity.getStart(), leadActivity.getEnd(), offsets);
      Canvas.Text timelineLabel = createTimelineLabel(bounds[0], t);
      timelineLabel.setAlignment(HAlignment.LEFT, VAlignment.BOTTOM);
      timelineLabel.setForegroundColor(t.getColor());
    }
  }

  public Text createTimelineLabel(int leftX, final Task task) {
    final Text text = myLabelsLayer.createText(leftX, myChartModel.getTimelineTopLineHeight(), "");
    text.setSelector(new TextSelector() {
      @Override
      public Label[] getLabels(TextMetrics textLengthCalculator) {
        int height = textLengthCalculator.getTextHeight(task.getName());
        int fullLength = textLengthCalculator.getTextLength(task.getName());
        Label result;
        if (fullLength <= MAX_TIMELINE_LABEL_WIDTH) {
          result = text.createLabel(task.getName(), fullLength, height);
        } else {
          int idLength = textLengthCalculator.getTextLength(String.valueOf(task.getTaskID()));
          result = text.createLabel("#" + String.valueOf(task.getTaskID()), idLength, height);
        }
        return new Label[] {result};
      }
    });
    text.setStyle("text.timeline.label");
    myLabelsLayer.bind(text, task);
    return text;
  }

  public Canvas getLabelLayer() {
    return myLabelsLayer;
  }
}
