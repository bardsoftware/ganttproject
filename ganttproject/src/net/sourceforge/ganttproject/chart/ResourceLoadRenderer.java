/*
 * This code is provided under the terms of GPL version 2.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.chart.ChartModelBase.Offset;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.resource.LoadDistribution;
import net.sourceforge.ganttproject.resource.ProjectResource;
import net.sourceforge.ganttproject.resource.LoadDistribution.Load;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;

/**
 * Renders resource load chart
 */
class ResourceLoadRenderer extends ChartRendererBase {

    private List myDistributions;

    private ResourceChart myResourcechart;

    public ResourceLoadRenderer(ChartModelResource model,
            ResourceChart resourceChart) {
        super(model);
        myResourcechart = resourceChart;
    }

    /**
     * Renders load distribution one by one, from top of the chart downwards
     * If some resource is expanded, calls rendering of the load details
     */
    public void render() {
       beforeProcessingTimeFrames();
       int ypos = 0 - getConfig().getYOffSet();
       for (int i=0; i<myDistributions.size(); i++) {
    	   LoadDistribution nextDistribution = (LoadDistribution) myDistributions.get(i);
           List loads = nextDistribution.getLoads();
           renderLoads(nextDistribution.getDaysOff(), ypos);
           renderLoads(loads, ypos);
           if (myResourcechart.isExpanded(nextDistribution.getResource())) {
               renderLoadDetails(nextDistribution, ypos);
               ypos += calculateGap(nextDistribution.getResource());
           }
           ypos += getConfig().getRowHeight();
           GraphicPrimitiveContainer.Line nextLine = getPrimitiveContainer().createLine(
        		   0, ypos,(int) getChartModel().getBounds().getWidth(), ypos);
           nextLine.setForegroundColor(Color.GRAY);
       }
    }

    /**
     * Renders resource load details, that is, tasks where the resource
     * is assigned to, with that resource load percentage
     */
    private void renderLoadDetails(LoadDistribution distribution, int ypos) {
        int yPos2 = ypos;
        Map/*<Task, List<Load>>*/ task2loads = distribution.getSeparatedTaskLoads();

        ResourceAssignment[] assignments = distribution.getResource().getAssignments();
        for (int i=0; i<assignments.length; i++) {
        	ResourceAssignment assignment = assignments[i];
            List nextLoads = (List) task2loads.get(assignment.getTask());
            yPos2 += getConfig().getRowHeight();
            if (nextLoads==null) {
                continue;
            }
            buildTasksLoadsRectangles(nextLoads, yPos2);
        }
    }

    /**
     * Renders the list of loads in a single chart row
     * Preconditions: loads come from the same distribution and are ordered by
     * their time offsets
     */
    private void renderLoads(List/*<Load>*/ loads, int ypos) {
        Load prevLoad = null;
        Load curLoad = null;
        LinkedList/*<Offset>*/ offsets = getOffsets();
        String suffix = "";
        for (int curIndex=1; curIndex<loads.size() && offsets.getFirst()!=null; curIndex++) {
            curLoad = (Load) loads.get(curIndex);
            prevLoad = (Load) loads.get(curIndex-1);
            if (prevLoad.load!=0) {
                renderLoads(prevLoad, curLoad, offsets, ypos, suffix);
                suffix = "";
            }
            else if (curLoad.load > 0) {
                suffix = ".first";
            }
        }
    }

    /**
     * Renders prevLoad, with curLoad serving as a load right border marker and style hint
     */
    private void renderLoads(
            Load prevLoad, Load curLoad, LinkedList/*<Offset>*/ offsets, int ypos, String suffix) {
        final Date prevEnd = curLoad.startDate;
        final Date prevStart = prevLoad.startDate;

        Rectangle nextRect = createRectangle(offsets, prevStart, prevEnd, ypos);
        if (nextRect==null) {
            return;
        }
        String style;
        if (prevLoad.isResourceUnavailable()) {
            style = "dayoff";
        }
        else {
            suffix += curLoad.load == 0 ? ".last" : "";

            if (prevLoad.load < 100f) {
                style = "load.underload";
            }
            else if (prevLoad.load > 100f) {
                style = "load.overload";
            }
            else {
                style = "load.normal";
            }
            style += suffix;
        }
        nextRect.setStyle(style);
        nextRect.setModelObject(new ResourceLoad(prevLoad.load));
    }

    /**
     * Renders a list of loads in a single chart row
     * Precondition: loads belong to the same pair (resource,task) and are ordered
     * by their time values
     */
    private void buildTasksLoadsRectangles(List/*<Load>*/ partition, int ypos) {
        LinkedList/*<Offset>*/ offsets = getOffsets();
        Iterator/*<Load>*/ loads = partition.iterator();
        while (loads.hasNext() && offsets.getFirst()!=null) {
            final Load nextLoad = (Load) loads.next();
            final Date nextStart = nextLoad.startDate;
            final Date nextEnd = nextLoad.endDate;

            Rectangle nextRect = createRectangle(offsets, nextStart, nextEnd, ypos);
            if (nextRect==null) {
                continue;
            }
            String style;
            if (nextLoad.load < 100f) {
                style = "load.underload";
            }
            else if (nextLoad.load > 100f) {
                style = "load.overload";
            }
            else {
                style = "load.normal";
            }
            style += ".first.last";
            nextRect.setStyle(style);
            nextRect.setModelObject(new ResourceLoad(nextLoad.load));
        }
    }

    private Rectangle createRectangle(LinkedList/*<Offset>*/ offsets, Date start, Date end, int ypos) {
        if (start.after(getChartEndDate()) || end.compareTo(getChartStartDate())<=0) {
            return null;
        }
        Date lastOffsetEnd = ((Offset)offsets.getLast()).getOffsetEnd();
        if (end.after(lastOffsetEnd)) {
        	end = lastOffsetEnd;
        }
        ArrayList copy = new ArrayList(offsets);
        Offset offsetBefore = null;
        Offset offsetAfter = null;

        LinkedList buffer = new LinkedList();
        while (offsets.getFirst()!=null) {
            Offset offset = (Offset) offsets.getFirst();
            if (offset.getOffsetEnd().compareTo(start)<=0) {
                offsetBefore = offset;
                buffer.clear();
            }
            if (offset.getOffsetEnd().compareTo(end)>=0) {
                offsetAfter = offset;
                if (offset.getOffsetEnd().after(end)) {
                	offsets.addAll(0, buffer);
                }
                break;
            }
            buffer.addLast(offset);
            offsets.removeFirst();
        }

        int rectStart;
        int rectEnd;
        if (offsetAfter==null) {
            rectEnd = getChartModel().getBounds().width;
        }
        else if (offsetAfter.getOffsetEnd().equals(end)) {
            rectEnd = offsetAfter.getOffsetPixels();
        }
        else {
            rectEnd = -1;
        }
        if (offsetBefore == null) {
            rectStart = 0;
        }
        else if (offsetBefore.getOffsetEnd().equals(start)) {
            rectStart = offsetBefore.getOffsetPixels();
        }
        else {
            rectStart = -1;
        }
        if (rectStart==-1 || rectEnd==-1) {
            return createRectangle(getDefaultOffsetsInRange(offsetBefore, offsetAfter), start, end, ypos);
        }
        Rectangle nextRect = getPrimitiveContainer().createRectangle(
                rectStart, ypos, rectEnd-rectStart, getConfig().getRowHeight());
        return nextRect;
    }

    private LinkedList/*<Offset>*/ getDefaultOffsetsInRange(Offset offsetBefore, Offset offsetAfter) {
        LinkedList/*<Offset>*/ result = new LinkedList/*<Offset>*/(
                getChartModel().getDefaultUnitOffsetsInRange(offsetBefore, offsetAfter));
        if (offsetBefore!=null) {
            result.addFirst(offsetBefore);
        }
        return result;
    }

    private Date getChartStartDate() {
        return getChartModel().getStartDate();
    }

    private Date getChartEndDate() {
        return ((Offset) getChartModel().getBottomUnitOffsets().get(getChartModel().getBottomUnitOffsets().size()-1)).getOffsetEnd();
    }

    private LinkedList/*<Offset>*/ getOffsets() {
        return new LinkedList/*<Offset>*/(getChartModel().getBottomUnitOffsets());
    }

    public void beforeProcessingTimeFrames() {
        myDistributions = new ArrayList/*<LoadDistribution>*/();
        getPrimitiveContainer().clear();
        ProjectResource[] resources = ((ChartModelResource) getChartModel())
                .getVisibleResources();
        for (int i = 0; i < resources.length; i++) {
            ProjectResource nextResource = resources[i];
            LoadDistribution nextDistribution = nextResource.getLoadDistribution();
            myDistributions.add(nextDistribution);
        }
    }

    /**
     * Class to use as Model object to display the load percentage in the
     * rectangle.
     *
     * @author bbaranne
     */
    static class ResourceLoad {
        private float load;

        ResourceLoad(float load) {
            this.load = load;
        }

        public float getLoad() {
            return load;
        }

        public String toString() {
            return Float.toString(load);
        }
    }

    private int calculateGap(ProjectResource resource) {
        return resource.getAssignments().length * getConfig().getRowHeight();
    }
}
