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

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.HAlignment;
import biz.ganttproject.core.chart.canvas.Canvas.Label;
import biz.ganttproject.core.chart.canvas.Canvas.Text;
import biz.ganttproject.core.chart.canvas.Canvas.VAlignment;
import biz.ganttproject.core.chart.canvas.TextMetrics;
import biz.ganttproject.core.chart.canvas.TextSelector;
import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.chart.grid.OffsetLookup;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;

import java.util.Collection;
import java.util.Date;
import java.util.List;

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
  protected interface ChartModelApi {
    int getTimelineTopLineHeight();
    List<Offset> getDefaultUnitOffsets();
    Date getStartDate();
    Collection<Task> getTimelineTasks();
  }

  TimelineLabelRendererImpl(ChartModelApi chartModel) {
    super(null);
    myChartModel = chartModel;
    myOffsetLookup = new OffsetLookup();
    getPrimitiveContainer().createLayers(5);
    myLabelsLayer = getPrimitiveContainer().getLayer(4);
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

  private Text createTimelineLabel(int leftX, final Task task) {
    final Text text = myLabelsLayer.createText(leftX, myChartModel.getTimelineTopLineHeight(), "");
    text.setSelector(new LabelTextSelector(task, text));
    text.setStyle("myText.timeline.label");
    myLabelsLayer.bind(text, task);
    return text;
  }

  Canvas getLabelLayer() {
    return myLabelsLayer;
  }

  public static class LabelTextSelector implements TextSelector {
    private final Text myText;
    private final Task myTask;

    public LabelTextSelector(Task task, Text text) {
      this.myTask = task;
      this.myText = text;
    }
    private Label createMaxWidthLabel(TextMetrics textLengthCalculator, String taskName, int maxWidth) {
      int stepSize = taskName.length() / 2;
      int upperBound = taskName.length();
      String substring = "";
      int textLength = 0;
      while (stepSize > 0) {
        substring = taskName.substring(0, upperBound);
        if (upperBound < taskName.length()) {
          substring = substring + "...";
        }
        textLength = textLengthCalculator.getTextLength(substring);
        if (textLength <= maxWidth) {
          if (upperBound < taskName.length()) {
            upperBound += stepSize;
          } else {
            break;
          }
        } else {
          upperBound -= stepSize;
        }
        stepSize /= 2;
        assert upperBound <= taskName.length();
      }
      int height = textLengthCalculator.getTextHeight(taskName);
      return myText.createLabel(substring, textLength, height);
    }

    @Override
    public Label[] getLabels(TextMetrics textLengthCalculator) {
      return new Label[] {createMaxWidthLabel(textLengthCalculator, myTask.getName(), MAX_TIMELINE_LABEL_WIDTH)};
    }
  }
}
