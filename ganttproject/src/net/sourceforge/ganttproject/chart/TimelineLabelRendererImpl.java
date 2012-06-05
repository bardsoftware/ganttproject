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

import java.util.List;

import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.HAlignment;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Label;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Text;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.VAlignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.util.TextLengthCalculator;

/**
 * Renders labels on the timeline.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class TimelineLabelRendererImpl extends ChartRendererBase {
  private static final int MAX_TIMELINE_LABEL_WIDTH = 200;
  private OffsetLookup myOffsetLookup;
  private GraphicPrimitiveContainer myLabelsLayer;
  private ChartModelApi myChartModel;

  /**
   * Thsi class dependencies interface
   */
  protected static interface ChartModelApi {
    TaskManager getTaskManager();
    int getTimelineTopLineHeight();
    List<Offset> getDefaultUnitOffsets();
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
    for (Task t : myChartModel.getTaskManager().getTasks()) {
      if (t.isMilestone()) {
        TaskActivity activity = new MilestoneTaskFakeActivity(t);
        int[] bounds = myOffsetLookup.getBounds(activity.getStart(), activity.getEnd(), myChartModel.getDefaultUnitOffsets());
        GraphicPrimitiveContainer.Text timelineLabel = createTimelineLabel(bounds[0], t);
        timelineLabel.setAlignment(HAlignment.LEFT, VAlignment.BOTTOM);
        timelineLabel.setForegroundColor(t.getColor());
      }
    }
  }

  public Text createTimelineLabel(int leftX, final Task task) {
    final Text text = myLabelsLayer.createText(leftX, myChartModel.getTimelineTopLineHeight(), "");
    text.setSelector(new TextSelector() {
      @Override
      public Label[] getLabels(TextLengthCalculator textLengthCalculator) {
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

  public GraphicPrimitiveContainer getLabelLayer() {
    return myLabelsLayer;
  }
}
