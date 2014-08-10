/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.chart.mouse;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;

import net.sourceforge.ganttproject.chart.TaskChartModelFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;

/**
 * Helper class for converting screen pixels into task completion percentage
 * value when changing the latter with a mouse. It operates with pixel offsets
 * from the chart viewport origin.
 *
 * @author Dmitry Barashev
 */
class ChangeTaskProgressRuler {
  private final Task myTask;
  private final TaskChartModelFacade myTaskChartFacade;
  private final SortedMap<Integer, Integer> myPixel2progress = new TreeMap<Integer, Integer>();
  private int myMinPx;

  /**
   * Creates and initializes the ruler
   */
  ChangeTaskProgressRuler(Task task, TaskChartModelFacade taskChartFacade) {
    myTask = task;
    myTaskChartFacade = taskChartFacade;

    float totalDuration = task.getDuration().getValue();
    int visiblePixels = -1;
    float accumulatedDuration = 0f;
    List<Rectangle> taskRectangles = myTaskChartFacade.getTaskRectangles(myTask);
    myMinPx = taskRectangles.get(0).getLeftX();
    myPixel2progress.put(myMinPx, 0);
    for (Rectangle r : taskRectangles) {
      TaskActivity activity = (TaskActivity) r.getModelObject();
      if (r.isVisible()) {
        visiblePixels = r.getRightX();
      }
      if (activity.getIntensity() > 0f) {
        accumulatedDuration += activity.getDuration().getValue();
      }
      myPixel2progress.put(visiblePixels, (int) (accumulatedDuration * 100 / totalDuration));
    }
  }

  /**
   * @return progress value corresponding to the given {@code pixels} value.
   */
  int getProgress(int pixels) {
    if (pixels < myMinPx) {
      return 0;
    }
    SortedMap<Integer, Integer> tailMap = myPixel2progress.tailMap(pixels);
    if (tailMap.isEmpty()) {
      return 100;
    }
    if (tailMap.firstKey().intValue() == pixels) {
      return tailMap.get(pixels);
    }

    SortedMap<Integer, Integer> headMap = myPixel2progress.headMap(pixels);
    int lowerPx = headMap.isEmpty() ? 0 : headMap.lastKey();
    int lowerProgress = headMap.isEmpty() ? 0 : headMap.get(lowerPx);
    int upperPx = tailMap.firstKey();
    int upperProgress = tailMap.get(upperPx);

    float diffProgress = (upperProgress - lowerProgress) * ((float) (pixels - lowerPx) / (float) (upperPx - lowerPx));
    return (int) (lowerProgress + diffProgress);
  }
}
