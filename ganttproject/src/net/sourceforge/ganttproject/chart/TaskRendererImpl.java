package net.sourceforge.ganttproject.chart;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.methods.GetMethod;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttPreviousStateTask;
import net.sourceforge.ganttproject.Mediator;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.chart.ChartModelBase.Offset;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.GraphicPrimitive;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.HAlignment;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Text;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.VAlignment;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.BlankLineNode;
import net.sourceforge.ganttproject.task.CustomColumEvent;
import net.sourceforge.ganttproject.task.CustomColumsListener;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskProperties;
import net.sourceforge.ganttproject.task.algorithm.SortTasksAlgorithm;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class TaskRendererImpl extends ChartRendererBase implements
        TimeUnitVisitor {

    private List/* <TaskActivity> */myVisibleActivities;

    private static final SortTasksAlgorithm ourAlgorithm = new SortTasksAlgorithm();

    private List/* <TaskActivity> */myCurrentlyProcessed = new ArrayList();

    private Map/* <TaskActivity,Integer> */myActivity2ordinalNumber = new HashMap();

    private Map/* <Task, Integer> */myTask_WorkingRectanglesLength = new HashMap();

    private int myPosX;

    private TimeFrame myCurrentTimeFrame;

    private TimeUnit myCurrentUnit;

    private Date myUnitStart;

    private boolean myProgressRenderingEnabled = true;

    private boolean myDependenciesRenderingEnabled = true;

    private GanttLanguage lang = GanttLanguage.getInstance();

    private boolean isVisible[] = { false, false, false, false, true, false,
            true };

    private ChartModelImpl myModel;

    private GPOption[] myDetailsOptions;

    private EnumerationOption[] myLabelOptions;

    private GPOptionGroup[] myOptionGroups;

    public static final int UP = 0;

    public static final int DOWN = 1;

    public static final int LEFT = 2;

    public static final int RIGHT = 3;

    private ArrayList myTasks;

    private ArrayList myPreviousStateTasks;

    private List myPreviousStateCurrentlyProcessed = new ArrayList();

    private static List ourInfoList;

    static {
        ourInfoList = new ArrayList();
        ourInfoList.add("");
        ourInfoList.add("id");
        ourInfoList.add("taskDates");
        ourInfoList.add("name");
        ourInfoList.add("length");
        ourInfoList.add("advancement");
        ourInfoList.add("coordinator");
        ourInfoList.add("resources");
        ourInfoList.add("predecessors");

    }

    public TaskRendererImpl(ChartModelImpl model) {
        super(model);
        this.myModel = model;
        getPrimitiveContainer().newLayer();
        getPrimitiveContainer().newLayer();

        DefaultEnumerationOption deo0 = new DefaultEnumerationOption("taskLabelUp",
                ourInfoList);
        DefaultEnumerationOption deo1 = new DefaultEnumerationOption("taskLabelDown",
                ourInfoList);
        DefaultEnumerationOption deo2 = new DefaultEnumerationOption("taskLabelLeft",
                ourInfoList);
        DefaultEnumerationOption deo3 = new DefaultEnumerationOption("taskLabelRight",
                ourInfoList);

        Mediator.addChangeValueDispatcher(deo0);
        Mediator.addChangeValueDispatcher(deo1);
        Mediator.addChangeValueDispatcher(deo2);
        Mediator.addChangeValueDispatcher(deo3);

        myLabelOptions = new EnumerationOption[] { deo0, deo1, deo2, deo3 };
        myOptionGroups = new GPOptionGroup[1];
        updateOptions();

    }

    private void addOption(String name) {
        ourInfoList.add(name);
        updateOptions();
    }

    private void removeOption(String name) {
        ourInfoList.remove(name);
        updateOptions();
    }

    private void updateOptions() {
        myDetailsOptions = new GPOption[myLabelOptions.length];
        System.arraycopy(myLabelOptions, 0, myDetailsOptions, 0,
                myLabelOptions.length);
        myOptionGroups[0] = new ChartOptionGroup("ganttChartDetails",
                myDetailsOptions, myModel.getOptionEventDispatcher());
    }

    public boolean isTextUp() {
        return myLabelOptions[UP].getValue() != null
                && myLabelOptions[UP].getValue().length() != 0;
    }

    public boolean isTextDown() {
        return myLabelOptions[DOWN].getValue() != null
                && myLabelOptions[DOWN].getValue().length() != 0;
    }

    public void beforeProcessingTimeFrames() {
        getPrimitiveContainer().clear();
        getPrimitiveContainer().getLayer(1).clear();
        getPrimitiveContainer().getLayer(2).clear();
        myActivity2ordinalNumber.clear();
        myTask_WorkingRectanglesLength.clear();
        myActivitiesOutOfView.clear();
        myVisibleActivities = getSortedTaskActivities();
        if (myTasks != null) {
            myPreviousStateTasks = (ArrayList) myTasks.clone();
        }
        myCurrentlyProcessed.clear();
        myPosX = 0;
    }

    public void afterProcessingTimeFrames() {
        for (int i=0; i<myActivitiesOutOfView.size(); i++) {
            TaskActivity next = (TaskActivity) myActivitiesOutOfView.get(i);
            Integer nextOrdNumber = (Integer) myActivity2ordinalNumber.get(next);
            int topy = nextOrdNumber.intValue() * getRowHeight() + 4; // JA Added
            GraphicPrimitiveContainer container = getContainerFor(next.getTask());
            Rectangle rectangle = container.createRectangle(-10, topy, 1, getRowHeight());
            container.bind(rectangle, next);
        }
        for (int i=0; i<myVisibleActivities.size(); i++) {
            TaskActivity next = (TaskActivity) myVisibleActivities.get(i);
            Integer nextOrdNumber = (Integer) myActivity2ordinalNumber.get(next);
            int topy = nextOrdNumber.intValue() * getRowHeight() + 4; // JA Added
            GraphicPrimitiveContainer container = getContainerFor(next.getTask());
            Rectangle rectangle = container.createRectangle(getWidth()+10, topy, 1, getRowHeight());
            container.bind(rectangle, next);
        }
        
        if (myDependenciesRenderingEnabled) {
            createDependencyLines();
        }
    }

    private boolean isPathExpanded(Task task) {
        boolean result = true;
        TaskContainmentHierarchyFacade taskHierarchy = getChartModel().getTaskManager().getTaskHierarchy();
        for (Task supertask = taskHierarchy.getContainer(task); supertask!=null && supertask!=getChartModel().getTaskManager().getRootTask(); supertask = taskHierarchy.getContainer(supertask)) {
            if (!supertask.getExpand()) {
                result = false;
                break;
            }
        }
        return result;
    }
    
    private List/* <Task> */getSortedTaskActivities() {
        List visibleTasks = ((ChartModelImpl) getChartModel())
                .getVisibleTasks();
        List visibleActivities = new ArrayList();
        myActivity2ordinalNumber.clear();
        for (int i = 0; i < visibleTasks.size(); i++) {
            if (visibleTasks.get(i).equals(BlankLineNode.BLANK_LINE))
                continue; // todo a revoir...
            Task nextTask = (Task) visibleTasks.get(i);

            Integer nextOrdinal = new Integer(i);
            if (nextTask == null) {
                continue; // case of space
            }
            TaskActivity[] activities = nextTask.getActivities();
            //System.err.println("[TaskRendererImpl]task="+nextTask+"\nactivities="+java.util.Arrays.asList(activities));
            float visibleWorkingLength = 0;
            float totalWorkingLength = 0;
            for (int j = 0; j < activities.length; j++) {
            	final TaskActivity nextActivity = activities[j];
                myActivity2ordinalNumber.put(nextActivity, nextOrdinal);
                visibleActivities.add(nextActivity);
                if (nextActivity.getIntensity() == 0f) {
                    continue;
                }
                final float nextChunk = 
                    nextActivity.getDuration().getLength(getChartModel().getBottomUnit()) 
                    * getChartModel().getBottomUnitWidth(); 
                totalWorkingLength += nextChunk; 
                if (!nextActivity.getEnd().before(getChartModel().getStartDate())) {
                	visibleWorkingLength += nextChunk;
                }
            }
            final float invisibleWorkingLength = totalWorkingLength - visibleWorkingLength;
            final long progressBarLength = 
                (long)(totalWorkingLength * nextTask.getCompletionPercentage() / 100 
                        - invisibleWorkingLength); 
            myTask_WorkingRectanglesLength.put(nextTask, new Long(progressBarLength));
        }
        Set hashedVisible = new HashSet(visibleActivities);
        Integer maxOrdinal = new Integer(hashedVisible.size()+1);
        Integer minOrdinal = new Integer(-2);
        for (int i=0; i<visibleTasks.size(); i++) {
            Task next = (Task)visibleTasks.get(i);
            TaskDependency[] dependencies = next.getDependenciesAsDependant().toArray();
            for (int j=0; j<dependencies.length; j++) {
                TaskDependency nextDependency = dependencies[j];
                TaskActivity dependeeActivity = nextDependency.getActivityBinding().getDependeeActivity();
                if (hashedVisible.contains(dependeeActivity)) {
                    continue;
                }
                Task dependeeTask = dependeeActivity.getTask();
                if (false==getChartModel().getTaskManager().getTaskHierarchy().contains(dependeeTask)) {
                    continue;
                }
                if (false==isPathExpanded(dependeeTask)) {
                    continue;
                }
                int diff = getChartModel().getTaskManager().getTaskHierarchy().compareDocumentOrder(next, dependeeTask);
                assert diff!=0;
                Integer dependeePosition = diff<0 ? maxOrdinal : minOrdinal;
                myActivity2ordinalNumber.put(dependeeActivity, dependeePosition);
                visibleActivities.add(dependeeActivity);
                hashedVisible.add(dependeeActivity);
            }
            dependencies = next.getDependenciesAsDependee().toArray();
            for (int j=0; j<dependencies.length; j++) {
                TaskDependency nextDependency = dependencies[j];
                TaskActivity dependantActivity = nextDependency.getActivityBinding().getDependantActivity();
                if (hashedVisible.contains(dependantActivity)) {
                    continue;
                }
                Task dependantTask = dependantActivity.getTask();
                if (false==getChartModel().getTaskManager().getTaskHierarchy().contains(dependantTask)) {
                    continue;
                }
                if (false==isPathExpanded(dependantTask)) {
                    continue;
                }
                int diff = getChartModel().getTaskManager().getTaskHierarchy().compareDocumentOrder(next, dependantTask);
                assert diff!=0;
                Integer dependantPosition = diff<0 ? maxOrdinal : minOrdinal;
                myActivity2ordinalNumber.put(dependantActivity, dependantPosition);
                visibleActivities.add(dependantActivity);
                hashedVisible.add(dependantActivity);
            }
        }
        ourAlgorithm.sortByStartDate(visibleActivities);
        return visibleActivities;
    }

    public void startTimeFrame(TimeFrame timeFrame) {
        myCurrentTimeFrame = timeFrame;
    }

    public void endTimeFrame(TimeFrame timeFrame) {
        myCurrentTimeFrame = null;
    }

    public void startUnitLine(TimeUnit timeUnit) {
        if (myCurrentTimeFrame.getBottomUnit() == timeUnit) {
            myCurrentUnit = timeUnit;
        }
    }

    public void endUnitLine(TimeUnit timeUnit) {
        myCurrentUnit = null;
    }

    public void nextTimeUnit(int unitIndex) {
        if (myCurrentUnit != null) {
            Date unitStart = myCurrentTimeFrame.getUnitStart(myCurrentUnit,
                    unitIndex);
            Date unitFinish = myCurrentTimeFrame.getUnitFinish(myCurrentUnit,
                    unitIndex);
            myUnitStart = unitStart;
            pullQueue(unitStart, unitFinish);
            // System.err.println("[TaskRendererImpl] nextTimeUnit():
            // unitStart="+unitStart+" posX="+myPosX);
            // if (!myCurrentlyProcessed.isEmpty()) {
            // System.err.println("[TaskRendererImpl] nextTimeUnit():
            // processing:"+myCurrentlyProcessed);
            // }
            for (Iterator startedActivities = myCurrentlyProcessed.iterator(); startedActivities
                    .hasNext(); startedActivities.remove()) {
                TaskActivity nextStarted = (TaskActivity) startedActivities
                        .next();
                processActivity(nextStarted);
            }
            if (myModel.isPrevious()) {
                for (int i = 0; i < myPreviousStateCurrentlyProcessed.size(); i++) {
                    Object next = myPreviousStateCurrentlyProcessed.get(i);
                    // System.out.println (next + " : " + i);
                    if (next != null) {
                        GanttPreviousStateTask previousTask = (GanttPreviousStateTask) next;
                        drawPreviousStateTask(previousTask, i);
                    }
                }
                myPreviousStateCurrentlyProcessed = new ArrayList();
            }
            myPosX += getChartModel().getBottomUnitWidth();
        }
    }

    private void processActivity(TaskActivity nextStarted) {
        if (nextStarted.isLast()) {
            processLastActivity(nextStarted);
        } else if (nextStarted.isFirst()) {
            processFirstActivity(nextStarted);
        } else {
            processRegularActivity(nextStarted);
        }
    }

    private Rectangle processRegularActivity(TaskActivity nextStarted) {
        Task nextTask = nextStarted.getTask();
        if (nextTask.isMilestone() && !nextStarted.isFirst()) {
            return null;
        }
        java.awt.Rectangle nextBounds = getBoundingRectangle(nextStarted);
        int nextLength = (int) nextBounds.width;
        int topy = nextBounds.y;
        topy = topy + (getRowHeight() - 20) / 2;
        if (myModel.isOnlyDown())
            topy = topy - 6;
        else if (myModel.isOnlyUp())
            topy = topy + 6;
        if (myModel.isPrevious())
            topy = topy - 5;

//        int posX = myPosX;
        GraphicPrimitiveContainer.Rectangle nextRectangle;
//        // if (nextStarted.getStart().compareTo(myUnitStart)>=0) {
//        TaskLength deltaLength = nextTask.getManager().createLength(
//                getChartModel().getTimeUnitStack().getDefaultTimeUnit(),
//                myUnitStart, nextStarted.getStart());
//
//        int deltaX = (int) (deltaLength.getLength(myCurrentUnit) * getChartModel()
//                .getBottomUnitWidth());
//        posX += deltaX;
        // System.err.println("[TaskRendererImpl] myUnitStart="+myUnitStart+"
        // nextActivity="+nextStarted+" deltaX="+deltaX+"
        // deltaLength="+deltaLength.getLength(myCurrentUnit));
        // }
        // else {
        // nextRectangle =
        // getPrimitiveContainer().createRectangle(myPosX+getChartModel().getBottomUnitWidth()-nextLength,
        // topy, nextLength, getRowHeight()*3/5);
        // }

        boolean nextHasNested = ((ChartModelImpl) getChartModel())
                .getTaskContainment().hasNestedTasks(nextTask); // JA Switch to
        GraphicPrimitiveContainer container = getContainerFor(nextTask);
        nextRectangle = container.createRectangle(nextBounds.x, topy, (int) nextLength,
                12); // CodeReview: why 12, not 15?
        // System.err.println("task="+nextStarted.getTask()+" nested tasks
        // length="+nextStarted.getTask().getNestedTasks().length);
        if (nextStarted.getTask().isMilestone()) {
            nextRectangle.setStyle("task.milestone");
        } else if (nextTask.isProjectTask()) {
            nextRectangle.setStyle("task.projectTask");
            if (nextStarted.isFirst()) {
                // CodeReview: why 12, not 15?
                GraphicPrimitiveContainer.Rectangle supertaskStart = container
                        .createRectangle(nextRectangle.myLeftX, topy,
                                (int) nextLength, 12);
                supertaskStart.setStyle("task.projectTask.start");
            }
            if (nextStarted.isLast()) {
                GraphicPrimitiveContainer.Rectangle supertaskEnd = container
                        .createRectangle(myPosX - 1, topy, (int) nextLength, 12);
                supertaskEnd.setStyle("task.projectTask.end");

            }
        } else if (nextHasNested) {
            nextRectangle.setStyle("task.supertask");
            if (nextStarted.isFirst()) {
                // CodeReview: why 12, not 15?
                GraphicPrimitiveContainer.Rectangle supertaskStart = container
                        .createRectangle(nextRectangle.myLeftX, topy,
                                (int) nextLength, 12);
                supertaskStart.setStyle("task.supertask.start");
            }
            if (nextStarted.isLast()) {
                // CodeReview: why 12, not 15?
                GraphicPrimitiveContainer.Rectangle supertaskEnd = container
                        .createRectangle(nextRectangle.myLeftX, topy, (int) nextLength, 12);
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
        if (myProgressRenderingEnabled && !nextTask.isMilestone() && !nextHasNested) {
            renderProgressBar(nextStarted, nextRectangle);
        }
        if (!"task.holiday".equals(nextRectangle.getStyle())
                && !"task.supertask".equals(nextRectangle.getStyle())) {
            nextRectangle.setBackgroundColor(nextStarted.getTask().getColor());
        }
        container.bind(nextRectangle, nextStarted);
        return nextRectangle;
    }

    private void renderProgressBar(TaskActivity nextStarted,
            GraphicPrimitiveContainer.Rectangle nextActivityRectangle) {
        if (nextStarted.getIntensity()==0) {
            return;
        }
        Task nextTask = nextStarted.getTask();
        int nextLength = nextActivityRectangle.myWidth;
        Long workingRectanglesLength = (Long) myTask_WorkingRectanglesLength
                .get(nextTask);
        if (workingRectanglesLength != null) {
            long nextProgressLength = nextLength;
            String style;
            if (workingRectanglesLength.longValue() > nextLength) {
                myTask_WorkingRectanglesLength.put(nextTask, new Long(
                        workingRectanglesLength.longValue() - nextLength));
                style = "task.progress";
            } else {
                nextProgressLength = workingRectanglesLength.longValue();
                myTask_WorkingRectanglesLength.remove(nextTask);
                style = "task.progress.end";
            }
            int nextMidY = nextActivityRectangle.getMiddleY();
            GraphicPrimitive nextProgressRect = getPrimitiveContainer()
                    .getLayer(1).createRectangle(nextActivityRectangle.myLeftX,
                            nextMidY - 1, (int) nextProgressLength, 3);
            nextProgressRect.setStyle(style);
            getPrimitiveContainer().getLayer(1)
                    .bind(nextProgressRect, nextTask);
        }
    }

    private void processFirstActivity(TaskActivity taskActivity) {
        boolean stop = taskActivity.getIntensity() == 0f;
        if (!stop) {
            processRegularActivity(taskActivity);
        }
    }

    private void createRightSideText(TaskActivity taskActivity) {
        java.awt.Rectangle bounds = getBoundingRectangle(taskActivity);
        String text = getTaskLabel(taskActivity.getTask(), RIGHT);
        if (text == null) {
            return;
        }

        int xText = (int) bounds.getMaxX() + 9;
        int yText = (int) myModel.getBoundingRectangle(taskActivity.getTask())
                .getMaxY() - 3;
        Text textPrimitive = processText(xText, yText, text);
    }

    private void createDownSideText(TaskActivity taskActivity) {
        String text = getTaskLabel(taskActivity.getTask(), DOWN);
        if (text == null) {
            return;
        }
        if (text.length() > 0) {
            java.awt.Rectangle taskRectangle = myModel
                    .getBoundingRectangle(taskActivity.getTask());
            int xOrigin = (int) taskRectangle.getMinX()
                    + (int) taskRectangle.getWidth() / 2;
            int yOrigin = (int) taskRectangle.getMaxY() + 2;
            Text textPrimitive = processText(xOrigin, yOrigin, text);
            textPrimitive.setAlignment(HAlignment.CENTER, VAlignment.TOP);
        }
    }

    private void createUpSideText(TaskActivity taskActivity) {
        String text = getTaskLabel(taskActivity.getTask(), UP);
        if (text == null) {
            return;
        }

        if (text.length() > 0) {
            java.awt.Rectangle taskRectangle = myModel
                    .getBoundingRectangle(taskActivity.getTask());
            int xOrigin = (int) taskRectangle.getMinX()
                    + (int) taskRectangle.getWidth() / 2;
            int yOrigin = (int) taskRectangle.getMinY() - 3;
            Text textPrimitive = processText(xOrigin, yOrigin, text);
            textPrimitive.setAlignment(HAlignment.CENTER, VAlignment.BOTTOM);
        }
    }

    private void createLeftSideText(TaskActivity taskActivity) {
        String text = getTaskLabel(taskActivity.getTask(), LEFT);
        if (text == null) {
            return;
        }

        if (text.length() > 0) {
            java.awt.Rectangle taskRectangle = myModel
                    .getBoundingRectangle(taskActivity.getTask());
            int xOrigin = (int) taskRectangle.getMinX() - 9;
            int yOrigin = (int) (taskRectangle.getMaxY() - 3);
            Text textPrimitive = processText(xOrigin, yOrigin, text);
            textPrimitive.setAlignment(HAlignment.RIGHT, VAlignment.BOTTOM);
        }
    }

    private void processLastActivity(TaskActivity taskActivity) {
        if (taskActivity.getIntensity() != 0f) {
            processRegularActivity(taskActivity);
        }
        createRightSideText(taskActivity);
        createDownSideText(taskActivity);
        createUpSideText(taskActivity);
        createLeftSideText(taskActivity);
        //
        //drawProjectBoundaries();
    }

    private Text processText(int xorigin, int yorigin, String text) {
        Text res = getPrimitiveContainer().getLayer(2).createText(xorigin,
                yorigin, text);
        res.setStyle("text.ganttinfo");
        return res;
    }

    // [dbarashev] This method violates the rule: rendering model knows (almost)
    // nothing about
    // specific rendering library (such as java.awt.*) and knows absolutely
    // nothing about
    // application framework (such as GanttGraphicArea)
    // I understand that it is nice to render coordinators with bold font and
    // linebreak. However,
    // there exist other ways of doing this
    /*
     * private void processText(int xInit, int yInit, String text) { String save =
     * text; boolean startNew = false; StringTokenizer st = new
     * StringTokenizer(text, "{}", true); while (st.hasMoreTokens()) { String
     * token = st.nextToken(); if (token.equals("{")) { startNew = true;
     * continue; } if (token.equals("}")) { startNew = false; continue; } if
     * (startNew) { Text t =
     * getPrimitiveContainer().getLayer(2).createText(xInit, yInit, token);
     * xInit += TextLengthCalculatorImpl.getTextLength(myModel
     * .getArea().getGraphics(), token); t.setFont(new Font(null, Font.BOLD,
     * 9)); continue; } getPrimitiveContainer().getLayer(2).createText(xInit,
     * yInit, token); xInit +=
     * TextLengthCalculatorImpl.getTextLength(myModel.getArea() .getGraphics(),
     * token); } }
     */

    private void drawPreviousStateTask(GanttPreviousStateTask task, int row) {
        int topy = (row * getRowHeight()) + getRowHeight() - 8;
        int posX = myPosX;
        if (task.getStart().getTime().compareTo(myUnitStart) >= 0) {
            TaskLength deltaLength = myModel.getTaskManager().createLength(
                    getChartModel().getTimeUnitStack().getDefaultTimeUnit(),
                    myUnitStart, task.getStart().getTime());

            int deltaX = (int) (deltaLength.getLength(myCurrentUnit) * getChartModel()
                    .getBottomUnitWidth());
            posX += deltaX;
        }
        int duration = task.getEnd(myModel.getTaskManager().getCalendar())
                .diff(task.getStart());

        TaskLength tl = myModel.getTaskManager().createLength(duration);
        int length = (int) (tl.getLength(myCurrentUnit) * getChartModel()
                .getBottomUnitWidth());

        Integer nextOrdNumber = (Integer) myActivity2ordinalNumber.get(task);
        GraphicPrimitiveContainer container = getPrimitiveContainer();

        Rectangle rect = container.createRectangle(posX, topy, length, 6);
        String style = "";
        if (task.isMilestone()) {
            style = "previousStateTask.milestone";
        } else if (task.hasNested()) {
            style = "previousStateTask.super";
            if (task.isAPart())
                style = "previousStateTask.super.apart";
        } else {
            style = "previousStateTask";
        }
        if (task.getState() == GanttPreviousStateTask.EARLIER)
            style = style + ".earlier";
        else if (task.getState() == GanttPreviousStateTask.LATER)
            style = style + ".later";
        rect.setStyle(style);
    }

    private String getTaskLabel(Task task, int position) {
        String propertyID = myLabelOptions[position].getValue(); 
        if (task.isMilestone() && !TaskProperties.ID_TASK_NAME.equals(propertyID)) {
            return null;
        }
        StringBuffer result = new StringBuffer();
        Object property = TaskProperties.getProperty(task, propertyID);
        if (property != null) {
            if (property instanceof Boolean)
                if (((Boolean) property).booleanValue())
                    result.append(lang.getText("yes"));
                else
                    result.append(lang.getText("no"));
            else
                result.append(property);
        }
        return result.toString();
    }

    private long getNegativeOffset(Date offsetDate, Date anchorDate) {
    	if (getChartModel().getTimeUnitStack().getDefaultTimeUnit().equals(myCurrentUnit)) {
    		TaskLength length = getChartModel().getTaskManager().createLength(myCurrentUnit, offsetDate, anchorDate);
    		return -length.getLength()*getChartModel().getBottomUnitWidth();
    	}
    	int length = 0;
    	while(true) {
    		ChartModelBase.Offset[] offsets = getChartModel().calculateOffsets(myCurrentTimeFrame, myCurrentUnit, offsetDate, getChartModel().getTimeUnitStack().getDefaultTimeUnit(), getChartModel().getBottomUnitWidth());
    		assert offsets.length>0;
    		Date lastOffsetEnd = offsets[offsets.length-1].getOffsetEnd(); 
    		if (lastOffsetEnd.before(anchorDate)) {
    			offsetDate = lastOffsetEnd;
    			length += offsets[offsets.length-1].getOffsetPixels();
    			continue;
    		}
			for (int i=offsets.length-1; i>=0; i--) {
				Offset offset = offsets[i];
				if (offset.getOffsetEnd().after(anchorDate)) {
					continue;
				}
				length+=offset.getOffsetPixels();
				break;
			}
			break;
    	}
    	return -length;
    }
    private long getPositiveOffset(Date offsetDate, Date anchorDate) {
        if (getChartModel().getTimeUnitStack().getDefaultTimeUnit().equals(myCurrentUnit)) {
            TaskLength length = getChartModel().getTaskManager().createLength(myCurrentUnit, anchorDate, offsetDate);
            return length.getLength()*getChartModel().getBottomUnitWidth();
        }
        int length = 0;
        while(true) {
            ChartModelBase.Offset[] offsets = getChartModel().calculateOffsets(
                    myCurrentTimeFrame, 
                    myCurrentUnit, 
                    anchorDate, 
                    getChartModel().getTimeUnitStack().getDefaultTimeUnit(), 
                    getChartModel().getBottomUnitWidth());
            if (offsets.length==0) {
                throw new IllegalStateException("Failed to calculate offsets for anchorDate="+anchorDate);
            }
            Date lastOffsetEnd = offsets[offsets.length-1].getOffsetEnd(); 
            //System.err.println("[TaskRendererImpl] getPositiveOffset(): |offsets|="+offsets.length+" last offset end="+lastOffsetEnd+" last offset pixels="+offsets[offsets.length-1].getOffsetPixels());
            if (lastOffsetEnd.before(offsetDate)) {
                anchorDate = lastOffsetEnd;
                length += offsets[offsets.length-1].getOffsetPixels();
                continue;
            }
            //int firstOffsetPixels = offsets[0].getOffsetPixels();
            for (int i=0; i<offsets.length; i++) {
                Offset offset = offsets[i];
                if (false==offset.getOffsetEnd().before(offsetDate)) {
                    length+=(offset.getOffsetPixels());
                    break;
                }
            }
            break;
        }
        return length;
        
    }
    private java.awt.Rectangle getBoundingRectangle(TaskActivity activity) {
        //System.err.println("[TaskRendererImpl] getBoundingRectangle():\nunit start="+myUnitStart+"\nactivity="+activity);
        int posX = myPosX;
        int length;
        if (false==activity.getStart().equals(myUnitStart)) {
            int deltaX = 0;
            if (activity.getStart().before(myUnitStart)) {
            	deltaX = (int) getNegativeOffset(activity.getStart(), myUnitStart);
            }
            else if (activity.getStart().after(myUnitStart)) {
                deltaX = (int) getPositiveOffset(activity.getStart(), myUnitStart);
            }
            posX += deltaX;
            length=(int) getPositiveOffset(activity.getEnd(), myUnitStart)-deltaX;
            //System.err.println("[TaskRendererImpl] getBoundingRectangle(): delta="+deltaX+" length="+length);
        }
        else {
            length = (int) (activity.getDuration().getLength(myCurrentUnit) * getChartModel()
                    .getBottomUnitWidth());
        }
        Integer nextOrdNumber = (Integer) myActivity2ordinalNumber
                .get(activity);
        int topy = nextOrdNumber.intValue() * getRowHeight() + 4; // JA Added
        // 4 so that
        // it draws
        // in middle
        // of row
        return new java.awt.Rectangle(posX, topy, length, getRowHeight());
    }

    private void createDependencyLines() {
        List/* <DependencyDrawData> */dependencyDrawData = prepareDependencyDrawData();
        drawDependencies(dependencyDrawData);
    }

    /**
     * @param dependencyDrawData
     */
    private void drawDependencies(List dependencyDrawData) {
        // if(dependencyDrawData.size() == 0)
        // System.out.println("VIDE");

        GraphicPrimitiveContainer primitiveContainer = getPrimitiveContainer()
                .getLayer(2);
        int arrowLength = 7;
        for (int i = 0; i < dependencyDrawData.size(); i++) {
            DependencyDrawData next = (DependencyDrawData) dependencyDrawData
                    .get(i);
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
                    Rectangle arrow = getPrimitiveContainer().createRectangle(
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
    private List prepareDependencyDrawData() {
        List result = new ArrayList();
        List/* <Task> */visibleTasks = ((ChartModelImpl) getChartModel())
                .getVisibleTasks();
        for (int i = 0; i < visibleTasks.size(); i++) {
            if (visibleTasks.get(i).equals(BlankLineNode.BLANK_LINE))
                continue; // todo a revoir...
            Task nextTask = (Task) visibleTasks.get(i);
            if (nextTask != null)
                prepareDependencyDrawData(nextTask, result);
        }
        return result;
    }

    private void prepareDependencyDrawData(Task task, List result) {
        TaskDependency[] deps = task.getDependencies().toArray();
        for (int i = 0; i < deps.length; i++) {
            TaskDependency next = deps[i];
            TaskDependency.ActivityBinding activityBinding = next
                    .getActivityBinding();
            TaskActivity dependant = activityBinding.getDependantActivity();
            GraphicPrimitiveContainer dependantContainer = getContainerFor(dependant.getTask());
            GraphicPrimitiveContainer.Rectangle dependantRectangle = (Rectangle)dependantContainer
                    .getPrimitive(dependant);
            if (dependantRectangle == null) {
                
                //System.out.println("dependantRectangle == null");
                continue;
            }
            TaskActivity dependee = activityBinding.getDependeeActivity();
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
                throw new RuntimeException();
            }
            DependencyDrawData data = new DependencyDrawData(next,
                    dependantRectangle, dependeeRectangle, dependantVector,
                    dependeeVector);
            result.add(data);
        }
    }

    private int getRowHeight() {
        return myModel.getRowHeight();
    }

    private List myActivitiesOutOfView = new ArrayList();
    
    private void pullQueue(Date unitStart, Date unitFinish) {
    	
        for (Iterator activities = myVisibleActivities.iterator(); activities
                .hasNext();) {
            TaskActivity next = (TaskActivity) activities.next();
            if (next.getEnd().before(getChartModel().getStartDate())) {
                myActivitiesOutOfView.add(next);
                activities.remove();
                continue;
            }
            if (next.getStart().before(getChartModel().getStartDate()) && next.getEnd().after(getChartModel().getEndDate())) {
                myCurrentlyProcessed.add(next);
                activities.remove();
                continue;                
            }
            if (next.getStart().after(unitFinish)) {
                break;
            }
            if (next.getStart().compareTo(unitStart) >= 0
                    && next.getStart().compareTo(unitFinish) < 0
                    || next.getEnd().compareTo(unitStart) >= 0
                    && next.getEnd().compareTo(unitFinish) < 0) {
                //System.err.println("pullQueue: \nnextActivity="+next+"\ntask="+next.getTask()+" \nunitStart="+unitStart+" unitFinish="+unitFinish);
                myCurrentlyProcessed.add(next);
                activities.remove();
            }
        }
        // initialize the myPreviousCurrentlyProcessed List
        // each index value matches with the row
        // null value means there is no previous task for this row or
        // the previous task is not between unitStart & unitFinish

        if (myModel.isPrevious()) {
            List visibleTasks = ((ChartModelImpl) getChartModel())
                    .getVisibleTasks();

            for (int i = 0; i < visibleTasks.size(); i++) {
                Task task = (Task) visibleTasks.get(i);
                int index = getPreviousStateTaskIndex(task);
                GPCalendar calendar = myModel.getTaskManager().getCalendar();
                if (index != -1) {
                    GanttPreviousStateTask previousStateTask = (GanttPreviousStateTask) myPreviousStateTasks
                            .get(index);
                    previousStateTask.setState(task, calendar);
                    if (previousStateTask.getStart().after(unitFinish)) {
                        myPreviousStateCurrentlyProcessed.add(null);
                    } else if (previousStateTask.getStart().getTime()
                            .compareTo(unitStart) >= 0
                            && previousStateTask.getStart().getTime()
                                    .compareTo(unitFinish) < 0)
                    // )|| previousStateTask.getEnd(
                    // calendar)
                    // .getTime().compareTo(unitStart) > 0
                    // && previousStateTask.getEnd(
                    // calendar)
                    // .getTime().compareTo(unitFinish) < 0)
                    {
                        myPreviousStateCurrentlyProcessed
                                .add(previousStateTask);
                        myPreviousStateTasks.remove(index);
                    }
                    // if just a part of the previous task is visible
                    else if (previousStateTask.getStart().getTime().compareTo(
                            unitStart) < 0
                            && (previousStateTask.getEnd(calendar).getTime()
                                    .compareTo(unitStart) > 0)) {
                        GanttCalendar newStart = new GanttCalendar(unitStart);
                        int id = previousStateTask.getId();
                        int duration = previousStateTask.getDuration()
                                - newStart.diff(previousStateTask.getStart());
                        int diff = newStart.diff(previousStateTask.getStart());
                        for (int j = 0; j < diff; j++) {
                            if (calendar.isNonWorkingDay(previousStateTask
                                    .getStart().newAdd(j).getTime())) {
                                duration++;
                            }
                        }
                        boolean isMilestone = previousStateTask.isMilestone();
                        boolean hasNested = previousStateTask.hasNested();
                        GanttPreviousStateTask partOfPreviousTask = new GanttPreviousStateTask(
                                id, newStart, duration, isMilestone, hasNested);
                        partOfPreviousTask.setState(task, calendar);
                        partOfPreviousTask.setIsAPart(true);
                        myPreviousStateCurrentlyProcessed
                                .add(partOfPreviousTask);
                        myPreviousStateTasks.remove(index);
                    } else
                        myPreviousStateCurrentlyProcessed.add(null);
                } else
                    myPreviousStateCurrentlyProcessed.add(null);
            }
        }
    }

    // return the index in the List tasks of the previous Task which have the
    // same ID as next
    // return -1 if there is no previous Task with the next ID
    private int getPreviousStateTaskIndex(Task task) {
        if (myPreviousStateTasks == null)
            return -1;
        for (int i = 0; i < myPreviousStateTasks.size(); i++) {
            if (task.getTaskID() == ((GanttPreviousStateTask) myPreviousStateTasks
                    .get(i)).getId())
                return i;
        }
        return -1;
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

    public void setProgressRenderingEnabled(boolean renderProgress) {
        myProgressRenderingEnabled = renderProgress;
    }

    public void setDependenciesRenderingEnabled(boolean renderDependencies) {
        myDependenciesRenderingEnabled = renderDependencies;
    }

    public boolean isVisible(int index) {
        return isVisible[index];
    }

    public GPOptionGroup[] getOptionGroups() {
        return myOptionGroups;
    }

    public void setPreviousStateTasks(ArrayList tasks) {
        myTasks = tasks;
    }

    public Rectangle getPrimitive(TaskActivity activity) {
        return (Rectangle) getContainerFor(activity.getTask()).getPrimitive(
                activity);
    }

    private GraphicPrimitiveContainer getContainerFor(Task task) {
        boolean hasNested = ((ChartModelImpl) getChartModel())
                .getTaskContainment().hasNestedTasks(task); // JA Switch to
        return hasNested ? getPrimitiveContainer().getLayer(2)
                : getPrimitiveContainer();
    }

    public int calculateRowHeight() {
        return getRowHeight();
    }

}
