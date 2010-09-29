/*
 * This code is provided under the terms of GPL version 2.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.chart.ChartModelBase.Offset;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * Renders task rectangles, dependency lines and all task-related text strings
 * in the gantt chart
 */
public class TaskRendererImpl2 extends ChartRendererBase {
    private boolean isVisible[] = { false, false, false, false, true, false,
            true };

    private ChartModelImpl myModel;

    private GPOptionGroup[] myOptionGroups;

    private TaskLabelsRendererImpl myLabelsRenderer;

    public TaskRendererImpl2(ChartModelImpl model) {
        super(model);
        this.myModel = model;
        getPrimitiveContainer().setOffset(0, model.getChartUIConfiguration().getHeaderHeight());
        getPrimitiveContainer().newLayer();
        getPrimitiveContainer().newLayer();

        myLabelsRenderer = new TaskLabelsRendererImpl(model, getPrimitiveContainer());
        myOptionGroups = new GPOptionGroup[] {myLabelsRenderer.getOptionGroup()};
    }

    private List<Task> getVisibleTasks() {
        return ((ChartModelImpl) getChartModel()).getVisibleTasks();
    }

    public void render() {
        getPrimitiveContainer().clear();
        getPrimitiveContainer().getLayer(0).clear();
        getPrimitiveContainer().getLayer(1).clear();
        getPrimitiveContainer().setOffset(0, myModel.getChartUIConfiguration().getHeaderHeight()-myModel.getVerticalOffset());

        List<Task> tasksAboveViewport = new ArrayList<Task>();
        List<Task> tasksBelowViewport = new ArrayList<Task>();

        collectTasksAboveAndBelowViewport(getVisibleTasks(), tasksAboveViewport, tasksBelowViewport);
        List<Offset> defaultUnitOffsets = getChartModel().getDefaultUnitOffsets();

        renderVisibleTasks(getVisibleTasks(), defaultUnitOffsets);
        renderTasksAboveAndBelowViewport(tasksAboveViewport, tasksBelowViewport, defaultUnitOffsets);
        renderDependencies();
    }


    private void renderDependencies() {
        createDependencyLines();
    }

    private void renderTasksAboveAndBelowViewport(List<Task> tasksAboveViewport, List<Task> tasksBelowViewport, List<Offset> defaultUnitOffsets) {
        for (Task nextAbove : tasksAboveViewport) {
            TaskActivity[] activities = nextAbove.isMilestone() ?
                    new TaskActivity[] {new MilestoneTaskFakeActivity(nextAbove)} :
                    nextAbove.getActivities();
            List<Rectangle> rectangles = renderActivities(-1, activities, defaultUnitOffsets);
            for (Rectangle nextRectangle : rectangles) {
                nextRectangle.setVisible(false);
            }
        }
        for (Task nextBelow : tasksBelowViewport) {
            TaskActivity[] activities = nextBelow.isMilestone() ?
                    new TaskActivity[] {new MilestoneTaskFakeActivity(nextBelow)} :
                    nextBelow.getActivities();
            List<Rectangle> rectangles = renderActivities(getVisibleTasks().size()+1, activities, defaultUnitOffsets);
            for (Rectangle nextRectangle : rectangles) {
                nextRectangle.setVisible(false);
            }
        }
    }

    private void renderVisibleTasks(List<Task> visibleTasks, List<Offset> defaultUnitOffsets) {
        int rowNum = 0;
        for (Task nextTask : visibleTasks) {
            TaskActivity[] activities = nextTask.isMilestone() ?
                    new TaskActivity[] {new MilestoneTaskFakeActivity(nextTask)} :
                    nextTask.getActivities();
            renderActivities(rowNum++, activities, defaultUnitOffsets);
            GraphicPrimitiveContainer.Line nextLine = getPrimitiveContainer().createLine(
                    0, rowNum*getRowHeight(), (int) getChartModel().getBounds().getWidth(), rowNum*getRowHeight());
            nextLine.setForegroundColor(Color.GRAY);
        }
    }

    private List<Rectangle> renderActivities(final int rowNum, TaskActivity[] activities, List<Offset> defaultUnitOffsets) {
        List<Rectangle> rectangles = new ArrayList<Rectangle>(activities.length);
        for (TaskActivity nextActivity : activities) {
            if (nextActivity.isFirst() || nextActivity.isLast()) {
                if (nextActivity.getIntensity() == 0f) {
                    continue;
                }
            }
            final Rectangle nextRectangle;
            if (nextActivity.getEnd().compareTo(getChartModel().getStartDate()) <= 0) {
                nextRectangle = processActivityEarlierThanViewport(rowNum, nextActivity);
            }
            else if (nextActivity.getStart().compareTo(getChartModel().getEndDate()) >= 0) {
                nextRectangle = processActivityLaterThanViewport(rowNum, nextActivity);
            }
            else {
                nextRectangle = processRegularActivity(rowNum, nextActivity, defaultUnitOffsets);
            }
            rectangles.add(nextRectangle);
        }
        if (!rectangles.isEmpty()) {
            Rectangle lastRectangle = rectangles.get(rectangles.size()-1);
            if (lastRectangle.myLeftX < getWidth()) {
                myLabelsRenderer.createRightSideText(lastRectangle);
                myLabelsRenderer.createDownSideText(lastRectangle);
                myLabelsRenderer.createUpSideText(lastRectangle);
            }
            Rectangle firstRectangle = rectangles.get(0);
            myLabelsRenderer.createLeftSideText(firstRectangle);
        }
        if (!getChartModel().getTaskManager().getTaskHierarchy().hasNestedTasks(activities[0].getTask())) {
            renderProgressBar(rectangles);
        }
        return rectangles;
    }

    private void renderProgressBar(List<Rectangle> rectangles) {
        final GraphicPrimitiveContainer container = getPrimitiveContainer().getLayer(0);
        final TimeUnit timeUnit = getChartModel().getTimeUnitStack().getDefaultTimeUnit();
        final Task task = ((TaskActivity)rectangles.get(0).getModelObject()).getTask();
        float length = task.getDuration().getLength(timeUnit);
        float completed = task.getCompletionPercentage()*length/100f;
        Rectangle lastProgressRectangle = null;

        for (Rectangle nextRectangle : rectangles) {
            final TaskActivity nextActivity = (TaskActivity) nextRectangle.getModelObject();
            final float nextLength = nextActivity.getDuration().getLength(timeUnit);

            final int nextProgressBarLength;
            if (completed > nextLength || nextActivity.getIntensity()==0f) {
                nextProgressBarLength = nextRectangle.myWidth;
                if (nextActivity.getIntensity()>0f) {
                    completed -= nextLength;
                }
            }
            else {
                nextProgressBarLength = (int)(nextRectangle.myWidth*(completed/nextLength));
                completed = 0f;
            }
            final Rectangle nextProgressBar = container.createRectangle(
                    nextRectangle.myLeftX,
                    nextRectangle.getMiddleY()-1,
                    nextProgressBarLength, 3);
            nextProgressBar.setStyle(completed==0f ? "task.progress.end" : "task.progress");
            getPrimitiveContainer().getLayer(0).bind(nextProgressBar, task);
            if (completed == 0) {
                lastProgressRectangle = nextRectangle;
                break;
            }
        }
        if (lastProgressRectangle==null) {
            lastProgressRectangle = rectangles.get(rectangles.size()-1);
        }
        //createDownSideText(lastProgressRectangle);
    }

    private Rectangle processActivityLaterThanViewport(int rowNum, TaskActivity nextActivity) {
        GraphicPrimitiveContainer container = getContainerFor(nextActivity.getTask());
        int startx = getWidth()+1;
        int topy = rowNum*getRowHeight()+4;
        Rectangle rectangle = container.createRectangle(startx, topy, 1, getRowHeight());
        container.bind(rectangle, nextActivity);
        rectangle.setVisible(false);
        return rectangle;
    }

    private Rectangle processActivityEarlierThanViewport(int rowNum, TaskActivity nextActivity) {
        GraphicPrimitiveContainer container = getContainerFor(nextActivity.getTask());
        int startx = -10;
        int topy = rowNum*getRowHeight()+4;
        Rectangle rectangle = container.createRectangle(startx, topy, 1, getRowHeight());
        container.bind(rectangle, nextActivity);
        rectangle.setVisible(false);
        return rectangle;
    }

    private void collectTasksAboveAndBelowViewport(
            List<Task> visibleTasks,
            List<Task> tasksAboveViewport,
            List<Task> tasksBelowViewport) {
        TaskContainmentHierarchyFacade containment = getChartModel().getTaskManager().getTaskHierarchy();
        List<Task> tasksInDocumentOrder = containment.getTasksInDocumentOrder();
        final Task firstVisible = visibleTasks.isEmpty() ? null : visibleTasks.get(0);
        final Task lastVisible = visibleTasks.isEmpty() ? null : visibleTasks.get(visibleTasks.size()-1);
        List<Task> addTo = tasksAboveViewport;
        for (Task nextTask : tasksInDocumentOrder) {
            if (addTo == null) {
                if (nextTask.equals(lastVisible)) {
                    addTo = tasksBelowViewport;
                }
                continue;
            }
            if (!nextTask.equals(firstVisible)) {
                addTo.add(nextTask);
            }
            else {
                addTo = null;
            }
        }
    }

    private int findOffset(Date date, int start, int end, List<Offset> offsets) {
        for (int compare = date.compareTo(offsets.get(start).getOffsetEnd()); compare != 0; compare = date.compareTo(offsets.get(start).getOffsetEnd())) {
            if (end == start) {
                if (start!=0 && end!=offsets.size()-1) {
                    throw new IllegalStateException("end="+end+" start="+start+" date="+date+" offset="+offsets.get(start)+" #offsets="+offsets.size());
                }
                break;
            }
            if (end < start) {
                throw new IllegalStateException("end="+end+" start="+start+" date="+date+" offset="+offsets.get(start));
            }
            int diff = end - start;
            if (compare == 1) {
                start += diff == 1 ? 1 : diff/2;
            }
            else {
                end = start;
                start -= diff == 1 ? 1 : diff/2;
            }
        }
        return start;
    }

    private java.awt.Rectangle getBoundingRectangle(int rowNum, TaskActivity activity, List<Offset> offsets) {
        int end = offsets.size()-1;
        int start = 0;

        if (activity.getStart().compareTo(offsets.get(start).getOffsetEnd()) > 0) {
            start = findOffset(activity.getStart(), start, end, offsets);
        }
        int leftX = offsets.get(start).getOffsetPixels();

        end = offsets.size()-1;
        if (activity.getEnd().compareTo(offsets.get(end).getOffsetEnd()) < 0) {
            end = findOffset(activity.getEnd(), 0, end, offsets);
        }
        int rightX = offsets.get(end).getOffsetPixels();
        if (activity.getTask().isMilestone()) {
            rightX += 10;
        }
        int topY = rowNum*getRowHeight();
        return new java.awt.Rectangle(leftX, topY, rightX - leftX, getRowHeight());
    }

    private Rectangle processRegularActivity(int rowNum, TaskActivity nextStarted, List<Offset> offsets) {
        Task nextTask = nextStarted.getTask();
        if (nextTask.isMilestone() && !nextStarted.isFirst()) {
            return null;
        }
        java.awt.Rectangle nextBounds = getBoundingRectangle(rowNum, nextStarted, offsets);
        myLabelsRenderer.stripVerticalLabelSpace(nextBounds);
        final int nextLength = (int) nextBounds.width;
        final int topy = nextBounds.y;

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
                // CodeReview: why 12, not 15?
                GraphicPrimitiveContainer.Rectangle supertaskStart = container.createRectangle(
                        nextRectangle.myLeftX, topy, (int) nextLength, getRectangleHeight());
                supertaskStart.setStyle("task.supertask.start");
            }
            if (nextStarted.isLast()) {
                // CodeReview: why 12, not 15?
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

    private void createDependencyLines() {
        List<DependencyDrawData> dependencyDrawData = prepareDependencyDrawData();
        drawDependencies(dependencyDrawData);
    }

    /**
     * @param dependencyDrawData
     */
    private void drawDependencies(List<DependencyDrawData> dependencyDrawData) {
        // if(dependencyDrawData.size() == 0)
        // System.out.println("VIDE");

        GraphicPrimitiveContainer primitiveContainer = getPrimitiveContainer().getLayer(1);
        int arrowLength = 7;
        for (int i = 0; i < dependencyDrawData.size(); i++) {
            DependencyDrawData next = dependencyDrawData.get(i);
            if (next.myDependeeVector
                    .reaches(next.myDependantVector.getPoint())) {
                // when dependee.end <= dependant.start && dependency.type is
                // any
                // or dependee.end <= dependant.end && dependency.type==FF
                // or dependee.start >= dependant.end && dependency.type==SF
                int ysign = signum(next.myDependantVector.getPoint().y
                        - next.myDependeeVector.getPoint().y);
                Point first = new Point(next.myDependeeVector.getPoint().x,
                        next.myDependeeVector.getPoint().y);
                Point second = new Point(next.myDependantVector.getPoint(-3).x,
                        next.myDependeeVector.getPoint().y);
                Point third = new Point(next.myDependantVector.getPoint(-3).x,
                        next.myDependantVector.getPoint().y);
                java.awt.Rectangle arrowBoundary = null;
                String style;
                if (next.myDependantVector.reaches(third)) {
                    second.x += arrowLength;
                    third.x += arrowLength;
                    Point forth = next.myDependantVector.getPoint();
                    primitiveContainer.createLine(third.x, third.y, forth.x,
                            forth.y);
                    arrowBoundary = new java.awt.Rectangle(forth.x,
                            forth.y - 3, arrowLength, 6);
                    style = "dependency.arrow.left";
                } else {
                    third.y -= ysign * next.myDependantRectangle.myHeight / 2;
                    arrowBoundary = new java.awt.Rectangle(third.x - 3, third.y
                            - (ysign > 0 ? ysign * arrowLength : 0), 6,
                            arrowLength);
                    style = ysign > 0 ? "dependency.arrow.down"
                            : "dependency.arrow.up";
                }
                primitiveContainer.createLine(first.x, first.y, second.x,
                        second.y);
                primitiveContainer.createLine(second.x, second.y, third.x,
                        third.y);
                if (arrowBoundary != null) {
                    Rectangle arrow = primitiveContainer.createRectangle(
                            arrowBoundary.x, arrowBoundary.y,
                            arrowBoundary.width, arrowBoundary.height);
                    arrow.setStyle(style);
                }
            } else {
                Point first = next.myDependeeVector.getPoint(3);
                if (next.myDependantVector.reaches(first)) {
                    Point second = new Point(first.x, next.myDependantVector
                            .getPoint().y);
                    primitiveContainer.createLine(next.myDependeeVector
                            .getPoint().x, next.myDependeeVector.getPoint().y,
                            first.x, first.y);
                    primitiveContainer.createLine(first.x, first.y, second.x,
                            second.y);
                    primitiveContainer.createLine(second.x, second.y,
                            next.myDependantVector.getPoint().x,
                            next.myDependantVector.getPoint().y);
                    int xsign = signum(next.myDependantVector.getPoint().x
                            - second.x);
                    java.awt.Rectangle arrowBoundary = new java.awt.Rectangle(
                            next.myDependantVector.getPoint(7).x,
                            next.myDependantVector.getPoint().y - 3, xsign * 7,
                            6);
                    Rectangle arrow = primitiveContainer.createRectangle(
                            arrowBoundary.x, arrowBoundary.y,
                            arrowBoundary.width, arrowBoundary.height);
                    arrow.setStyle(xsign < 0 ? "dependency.arrow.left"
                            : "dependency.arrow.right");
                } else {
                    Point forth = next.myDependantVector.getPoint(3);
                    Point second = new Point(first.x, (first.y + forth.y) / 2);
                    Point third = new Point(forth.x, (first.y + forth.y) / 2);
                    primitiveContainer.createLine(next.myDependeeVector
                            .getPoint().x, next.myDependeeVector.getPoint().y,
                            first.x, first.y);
                    primitiveContainer.createLine(first.x, first.y, second.x,
                            second.y);
                    primitiveContainer.createLine(second.x, second.y, third.x,
                            third.y);
                    primitiveContainer.createLine(third.x, third.y, forth.x,
                            forth.y);
                    primitiveContainer.createLine(forth.x, forth.y,
                            next.myDependantVector.getPoint().x,
                            next.myDependantVector.getPoint().y);
                }
            }
        }
    }

    private final int signum(int value) {
        if (value == 0) {
            return 0;
        }
        return value < 0 ? -1 : 1;
    }

    /**
     * @return
     */
    private List<DependencyDrawData> prepareDependencyDrawData() {
        List<DependencyDrawData> result = new ArrayList<DependencyDrawData>();
        List<Task> visibleTasks = ((ChartModelImpl) getChartModel()).getVisibleTasks();
        for (Task nextTask : visibleTasks) {
            if (nextTask != null) {
                prepareDependencyDrawData(nextTask, result);
            }
        }
        return result;
    }

    private void prepareDependencyDrawData(Task task, List<DependencyDrawData> result) {
        TaskDependency[] deps = task.getDependencies().toArray();
        for (int i = 0; i < deps.length; i++) {
            TaskDependency next = deps[i];
            TaskDependency.ActivityBinding activityBinding = next
                    .getActivityBinding();
            TaskActivity dependant = activityBinding.getDependantActivity();
            if (dependant.getTask().isMilestone()) {
                dependant = new MilestoneTaskFakeActivity(dependant.getTask());
            }
            GraphicPrimitiveContainer dependantContainer = getContainerFor(dependant.getTask());
            GraphicPrimitiveContainer.Rectangle dependantRectangle = (Rectangle)dependantContainer
                    .getPrimitive(dependant);
            if (dependantRectangle == null) {

                //System.out.println("dependantRectangle == null");
                continue;
            }
            TaskActivity dependee = activityBinding.getDependeeActivity();
            if (dependee.getTask().isMilestone()) {
                dependee = new MilestoneTaskFakeActivity(dependee.getTask());
            }
            GraphicPrimitiveContainer dependeeContainer = getContainerFor(dependee.getTask());
            GraphicPrimitiveContainer.Rectangle dependeeRectangle = (Rectangle)dependeeContainer
                    .getPrimitive(dependee);
            if (dependeeRectangle == null) {
                //System.out.println("dependeeRectangle == null");
                continue;
            }
            Date[] bounds = activityBinding.getAlignedBounds();
            PointVector dependantVector;
            if (bounds[0].equals(dependant.getStart())) {
                dependantVector = new WestPointVector(new Point(
                        dependantRectangle.myLeftX, dependantRectangle
                                .getMiddleY()));
            } else if (bounds[0].equals(dependant.getEnd())) {
                dependantVector = new EastPointVector(new Point(
                        dependantRectangle.getRightX(), dependantRectangle
                                .getMiddleY()));
            } else {
                throw new RuntimeException();
            }
            //
            PointVector dependeeVector;
            if (bounds[1].equals(dependee.getStart())) {
                dependeeVector = new WestPointVector(new Point(
                        dependeeRectangle.myLeftX, dependeeRectangle
                                .getMiddleY()));
            } else if (bounds[1].equals(dependee.getEnd())) {
                dependeeVector = new EastPointVector(new Point(
                        dependeeRectangle.getRightX(), dependeeRectangle
                                .getMiddleY()));
            } else {
                throw new RuntimeException("bounds: "+Arrays.asList(bounds)+" dependee="+dependee+" dependant="+dependant);
            }
            //System.err.println("dependant rectangle="+dependantRectangle+"\ndependeeREctangle="+dependeeRectangle+"\ndependantVector="+dependantVector+"\ndependeeVector="+dependeeVector);
            DependencyDrawData data = new DependencyDrawData(next,
                    dependantRectangle, dependeeRectangle, dependantVector,
                    dependeeVector);
            result.add(data);
        }
    }

    private int getRowHeight() {
        return myModel.getRowHeight();
    }

    private static class DependencyDrawData {
        final GraphicPrimitiveContainer.Rectangle myDependantRectangle;

        final GraphicPrimitiveContainer.Rectangle myDependeeRectangle;

        final TaskDependency myDependency;

        final PointVector myDependantVector;

        final PointVector myDependeeVector;

        public DependencyDrawData(TaskDependency dependency,
                GraphicPrimitiveContainer.GraphicPrimitive dependantPrimitive,
                GraphicPrimitiveContainer.GraphicPrimitive dependeePrimitive,
                PointVector dependantVector, PointVector dependeeVector) {
            myDependency = dependency;
            myDependantRectangle = (GraphicPrimitiveContainer.Rectangle) dependantPrimitive;
            myDependeeRectangle = (GraphicPrimitiveContainer.Rectangle) dependeePrimitive;
            myDependantVector = dependantVector;
            myDependeeVector = dependeeVector;
        }

        public String toString() {
            return "From activity="
                    + myDependency.getActivityBinding().getDependantActivity()
                    + " (vector=" + myDependantVector + ")\n to activity="
                    + myDependency.getActivityBinding().getDependeeActivity()
                    + " (vector=" + myDependeeVector;
        }
    }

    private static abstract class PointVector {
        private final Point myPoint;

        protected PointVector(Point point) {
            myPoint = point;
        }

        Point getPoint() {
            return myPoint;
        }

        abstract boolean reaches(Point targetPoint);

        abstract Point getPoint(int i);
    }

    private static class WestPointVector extends PointVector {
        protected WestPointVector(Point point) {
            super(point);
        }

        boolean reaches(Point targetPoint) {
            return targetPoint.x <= getPoint().x;
        }

        Point getPoint(int diff) {
            return new Point(getPoint().x - diff, getPoint().y);
        }

        public String toString() {
            return "<=" + getPoint().toString();
        }

    }

    private static class EastPointVector extends PointVector {
        protected EastPointVector(Point point) {
            super(point);
        }

        boolean reaches(Point targetPoint) {
            return targetPoint.x >= getPoint().x;
        }

        Point getPoint(int diff) {
            return new Point(getPoint().x + diff, getPoint().y);
        }

        public String toString() {
            return ">=" + getPoint().toString();
        }

    }

    public boolean isVisible(int index) {
        return isVisible[index];
    }

    public GPOptionGroup[] getOptionGroups() {
        return myOptionGroups;
    }

    public Rectangle getPrimitive(TaskActivity activity) {
        return (Rectangle) getContainerFor(activity.getTask()).getPrimitive(
                activity);
    }

    private GraphicPrimitiveContainer getContainerFor(Task task) {
        return getPrimitiveContainer();
    }

    int calculateRowHeight() {
        return myLabelsRenderer.calculateRowHeight();
    }

    private int getRectangleHeight() {
        return myLabelsRenderer.getFontHeight();
    }

}