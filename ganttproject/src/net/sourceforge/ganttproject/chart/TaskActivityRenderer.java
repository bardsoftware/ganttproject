/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2010 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;

/**
 * Renders task activity rectangles on the Gantt chart.
 */
class TaskActivityRenderer {
    private final ChartModelImpl myChartModel;
    private final GraphicPrimitiveContainer myGraphicPrimitiveContainer;
    private final TaskLabelsRendererImpl myLabelsRenderer;
    private final Style myStyle;

    static class Style {
        int marginTop;
        int height;
        
        Style(int marginTop, int height) {
            this.marginTop = marginTop;
            this.height = height;
        }
    }
    TaskActivityRenderer(ChartModelImpl chartModel, GraphicPrimitiveContainer primitiveContainer, 
            TaskLabelsRendererImpl labelsRenderer, Style style) {
        myChartModel = chartModel;
        myStyle = style;
        myGraphicPrimitiveContainer = primitiveContainer;
        myLabelsRenderer = labelsRenderer;
    }
    
    List<Rectangle> renderActivities(int rowNum, List<TaskActivity> activities, List<Offset> offsets) {
        List<Rectangle> rectangles = new ArrayList<Rectangle>();
        for (TaskActivity nextActivity : activities) {
            if (nextActivity.isFirst() || nextActivity.isLast()) {
                if (nextActivity.getIntensity() == 0f) {
                    continue;
                }
            }
            final Rectangle nextRectangle;
            if (nextActivity.getEnd().compareTo(getChartModel().getOffsetAnchorDate()) <= 0) {
                nextRectangle = processActivityEarlierThanViewport(rowNum, nextActivity, offsets);
            }
            else if (nextActivity.getStart().compareTo(getChartModel().getEndDate()) >= 0) {
                nextRectangle = processActivityLaterThanViewport(rowNum, nextActivity, offsets);
            }
            else {
                nextRectangle = processRegularActivity(rowNum, nextActivity, offsets);
            }
            rectangles.add(nextRectangle);
        }
        return rectangles;
    }

    private Rectangle processActivityLaterThanViewport(int rowNum, TaskActivity nextActivity, List<Offset> offsets) {
        GraphicPrimitiveContainer container = getContainerFor(nextActivity.getTask());
        int startx = getChartModel().getBottomUnitOffsets().getEndPx()+1;
        int topy = rowNum*getRowHeight()+4;
        Rectangle rectangle = container.createRectangle(startx, topy, 1, getRowHeight());
        container.bind(rectangle, nextActivity);
        rectangle.setVisible(false);
        return rectangle;
    }

    private GraphicPrimitiveContainer getContainerFor(Task task) {
        return myGraphicPrimitiveContainer;
    }

    private Rectangle processActivityEarlierThanViewport(int rowNum, TaskActivity nextActivity, List<Offset> offsets) {
        GraphicPrimitiveContainer container = getContainerFor(nextActivity.getTask());
        int startx = getChartModel().getBottomUnitOffsets().getStartPx() - 1;
        int topy = rowNum*getRowHeight()+4;
        Rectangle rectangle = container.createRectangle(startx, topy, 1, getRowHeight());
        container.bind(rectangle, nextActivity);
        rectangle.setVisible(false);
        return rectangle;
    }

    private Rectangle processRegularActivity(int rowNum, TaskActivity nextStarted, List<Offset> offsets) {
        Task nextTask = nextStarted.getTask();
        if (nextTask.isMilestone() && !nextStarted.isFirst()) {
            return null;
        }
        java.awt.Rectangle nextBounds = getBoundingRectangle(rowNum, nextStarted, offsets);
        myLabelsRenderer.stripVerticalLabelSpace(nextBounds);
        final int nextLength = (int) nextBounds.width;
        final int topy = nextBounds.y + myStyle.marginTop;

        GraphicPrimitiveContainer.Rectangle nextRectangle;
        boolean nextHasNested = ((ChartModelImpl) getChartModel())
                .getTaskContainment().hasNestedTasks(nextTask); // JA Switch to
        GraphicPrimitiveContainer container = getContainerFor(nextTask);
        nextRectangle = container.createRectangle(
                nextBounds.x, topy, (int) nextLength, getRectangleHeight());
        if (nextStarted.getTask().isMilestone()) {
            nextRectangle.setStyle("task.milestone");
        } else if (nextTask.isProjectTask()) {
            nextRectangle.setStyle("task.projectTask");
            if (nextStarted.isFirst()) {
                GraphicPrimitiveContainer.Rectangle supertaskStart = container.createRectangle(
                        nextRectangle.myLeftX, topy, (int) nextLength, getRectangleHeight());
                supertaskStart.setStyle("task.projectTask.start");
            }
            if (nextStarted.isLast()) {
                GraphicPrimitiveContainer.Rectangle supertaskEnd = container.createRectangle(
                        nextRectangle.myLeftX - 1, topy, (int) nextLength, getRectangleHeight());
                supertaskEnd.setStyle("task.projectTask.end");

            }
        } else if (nextHasNested) {
            nextRectangle.setStyle("task.supertask");
            if (nextStarted.isFirst()) {
                GraphicPrimitiveContainer.Rectangle supertaskStart = container.createRectangle(
                        nextRectangle.myLeftX, topy, (int) nextLength, getRectangleHeight());
                supertaskStart.setStyle("task.supertask.start");
            }
            if (nextStarted.isLast()) {
                GraphicPrimitiveContainer.Rectangle supertaskEnd = container.createRectangle(
                        nextRectangle.myLeftX, topy, (int) nextLength, getRectangleHeight());
                supertaskEnd.setStyle("task.supertask.end");

            }
        } else if (nextStarted.getIntensity() == 0f) {
            nextRectangle.setStyle("task.holiday");
        } else {
            if (nextStarted.isFirst() && nextStarted.isLast()) {
                nextRectangle.setStyle("task.startend");
            }
            else if (false==nextStarted.isFirst() ^ nextStarted.isLast()) {
                nextRectangle.setStyle("task");
            }
            else if (nextStarted.isFirst()) {
                nextRectangle.setStyle("task.start");
            }
            else if (nextStarted.isLast()) {
                nextRectangle.setStyle("task.end");
            }
        }
        if (!"task.holiday".equals(nextRectangle.getStyle())
                && !"task.supertask".equals(nextRectangle.getStyle())) {
            nextRectangle.setBackgroundColor(nextStarted.getTask().getColor());
        }
        container.bind(nextRectangle, nextStarted);
        return nextRectangle;
    }

    private java.awt.Rectangle getBoundingRectangle(int rowNum, TaskActivity activity, List<Offset> offsets) {
        OffsetLookup offsetLookup = new OffsetLookup();
        int[] bounds = offsetLookup.getBounds(activity.getStart(), activity.getEnd(), offsets);
        int leftX = bounds[0];
        int rightX = bounds[1];
        if (activity.getTask().isMilestone()) {
            rightX += 10;
        }
        int topY = rowNum*getRowHeight();
        return new java.awt.Rectangle(leftX, topY, rightX - leftX, getRowHeight());
    }

    private int getRectangleHeight() {
        return myStyle.height;
    }

    private ChartModelImpl getChartModel() {
        return myChartModel;
    }
    
    private int getRowHeight() {
        return getChartModel().getRowHeight();
    }
}
