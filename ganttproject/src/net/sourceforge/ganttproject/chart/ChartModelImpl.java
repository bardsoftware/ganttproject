/*
 * Created on 17.06.2004
 *
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.chart.item.TaskBoundaryChartItem;
import net.sourceforge.ganttproject.chart.item.TaskProgressChartItem;
import net.sourceforge.ganttproject.chart.item.TaskRegularAreaChartItem;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.ColorOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultColorOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.TimeUnitStack;

/**
 * @author bard
 */
public class ChartModelImpl extends ChartModelBase implements ChartModel {

    private java.util.List/* <Task> */myVisibleTasks;

    private final TaskRendererImpl myTaskRendererImpl;

    private TaskContainmentHierarchyFacade myTaskContainment;

    private final TaskGridRendererImpl myTaskGridRendererImpl;

    //private final ResourcesRendererImpl myResourcesRendererImpl;

    // private final TaskProgressRendererImpl myTaskProgressRendererImpl;
    private final TaskManager taskManager;

    private boolean isOnlyDown = false;

    private boolean isOnlyUp = false;

    private boolean isPreviousState = false;

    private int rowHeight = 20;

    private final EnumerationOption myDependencyHardnessOption;
    private final GPOptionGroup myDependencyOptions;

    private final ColorOption myTaskDefaultColorOption;
    private final ChartOptionGroup myDefaultColorOptions;

    private final ColorOption myTaskAheadOfScheduleColor;
    private final ColorOption myTaskBehindScheduleColor;
    private final ColorOption myTaskOnScheduleColor;
//
//
    private final ChartOptionGroup myStateDiffOptions;

    private Set myHiddenTasks;

    public static class TuningOptions {
        private final boolean renderProgress;

        private final boolean renderDependencies;

        public TuningOptions(boolean renderProgress, boolean renderDependencies) {
            this.renderProgress = renderProgress;
            this.renderDependencies = renderDependencies;
        }

        public static final TuningOptions DEFAULT = new TuningOptions(true,
                true);
    }

    public ChartModelImpl(TaskManager taskManager, TimeUnitStack timeUnitStack,
            final UIConfiguration projectConfig) {
        super(taskManager, timeUnitStack, projectConfig);
        this.taskManager = taskManager;
        myTaskRendererImpl = new TaskRendererImpl(this);
        myTaskGridRendererImpl = new TaskGridRendererImpl(this);
        //myResourcesRendererImpl = new ResourcesRendererImpl(this);
        // myTaskProgressRendererImpl = new TaskProgressRendererImpl(this);
        myTimeUnitVisitors.add(myTaskGridRendererImpl);
        myTimeUnitVisitors.add(myTaskRendererImpl);

        class NewTaskColorOption extends DefaultColorOption implements GP1XOptionConverter {
            private NewTaskColorOption() {
                super("newTaskDefaultColor");
            }
            public String getTagName() {
                return "colors";
            }

            public String getAttributeName() {
                return "tasks";
            }

            public void loadValue(String legacyValue) {
                lock();
                loadPersistentValue(legacyValue);
                commit();
            }
            public void commit() {
                super.commit();
                projectConfig.setTaskColor(getValue());
            }

        };
        myTaskDefaultColorOption = new NewTaskColorOption();

        myDependencyHardnessOption = new DefaultEnumerationOption("dependencyDefaultHardness", new String[] {
           "Strong", "Rubber"
        });
        myDependencyHardnessOption.lock();
        myDependencyHardnessOption.setValue("Strong");
        myDependencyHardnessOption.commit();
        myDependencyOptions = new GPOptionGroup("dependency", new GPOption[] {myDependencyHardnessOption});
        myDependencyOptions.setTitled(true);
        myDependencyOptions.setI18Nkey(
                new OptionsPageBuilder.I18N().getCanonicalOptionGroupLabelKey(myDependencyOptions),
                "link");
        myDependencyOptions.setI18Nkey(
                new OptionsPageBuilder.I18N().getCanonicalOptionLabelKey(myDependencyHardnessOption),
                "hardness");
        myDependencyOptions.setI18Nkey(
                OptionsPageBuilder.I18N.getCanonicalOptionValueLabelKey("Strong"),
                "hardness.strong");
        myDependencyOptions.setI18Nkey(
                OptionsPageBuilder.I18N.getCanonicalOptionValueLabelKey("Rubber"),
                "hardness.rubber");
        myDefaultColorOptions = new ChartOptionGroup("ganttChartDefaultColors", new GPOption[] {myTaskDefaultColorOption, projectConfig.getWeekendAlphaRenderingOption()}, getOptionEventDispatcher());
        {
            myTaskAheadOfScheduleColor = new DefaultColorOption(
                    "ganttChartStateDiffColors.taskAheadOfScheduleColor") {
                public void commit() {
                    super.commit();
                    projectConfig.setEarlierPreviousTaskColor(getValue());
                }
            };
            myTaskAheadOfScheduleColor.lock();
            myTaskAheadOfScheduleColor.setValue(new Color(50, 229, 50));
            myTaskAheadOfScheduleColor.commit();
            //
            myTaskBehindScheduleColor = new DefaultColorOption(
                    "ganttChartStateDiffColors.taskBehindScheduleColor") {
                public void commit() {
                    super.commit();
                    projectConfig.setLaterPreviousTaskColor(getValue());
                }
            };
            myTaskBehindScheduleColor.lock();
            myTaskBehindScheduleColor.setValue(new Color(229, 50, 50));
            myTaskBehindScheduleColor.commit();
            //
            myTaskOnScheduleColor = new DefaultColorOption(
                    "ganttChartStateDiffColors.taskOnScheduleColor") {
                public void commit() {
                    super.commit();
                    projectConfig.setPreviousTaskColor(getValue());
                }
            };
            myTaskOnScheduleColor.lock();
            myTaskOnScheduleColor.setValue(Color.LIGHT_GRAY);
            myTaskOnScheduleColor.commit();
            //
            myStateDiffOptions = new ChartOptionGroup(
                    "ganttChartStateDiffColors", new GPOption[] {
                            myTaskOnScheduleColor, myTaskAheadOfScheduleColor,
                            myTaskBehindScheduleColor },
                    getOptionEventDispatcher());
        }
        // myTimeUnitVisitors.add(myResourcesRendererImpl);
        // myTimeUnitVisitors.add(myTaskProgressRendererImpl);
    }

    protected void enableRenderers1() {
        super.enableRenderers1();
        myTaskRendererImpl.setEnabled(true);
    }

    protected void enableRenderers2() {
        super.enableRenderers2();
        myTaskRendererImpl.setEnabled(false);
    }


    public void paint(Graphics g) {
        super.paint(g);
        if (getTopUnit().isConstructedFrom(myBottomUnit)) {
            myFrameWidthFunction = myRegularFrameWidthFunction;
            for (int i = 0; i < myTimeUnitVisitors.size(); i++) {
                ((TimeUnitVisitor) myTimeUnitVisitors.get(i)).setEnabled(true);
            }
            paintRegularTimeFrames(g, getTimeFrames(null));
        } else {
            myFrameWidthFunction = mySkewedFrameWidthFunction;
            mySkewedFrameWidthFunction.initialize();
            paintSkewedTimeFrames(g);
        }
    }

    protected void paintMainArea(Graphics mainArea, Painter p) {
        super.paintMainArea(mainArea, p);
        mainArea.translate(0, -getVerticalOffset());
        myTaskRendererImpl.getPrimitiveContainer().paint(p, mainArea);
        myTaskGridRendererImpl.getPrimitiveContainer().paint(p, mainArea);
        myTaskRendererImpl.getPrimitiveContainer().getLayer(1).paint(p, mainArea);
        myTaskRendererImpl.getPrimitiveContainer().getLayer(2).paint(p, mainArea);
//
//        myTaskRendererImpl.getPrimitiveContainer().paint(p, mainArea);
//        myTaskRendererImpl.getPrimitiveContainer().getLayer(1).paint(p,
//                mainArea);
//        super.paintMainArea(mainArea, p);
//        myTaskRendererImpl.getPrimitiveContainer().getLayer(2).paint(p,
//                mainArea);
//        myTaskGridRendererImpl.getPrimitiveContainer().paint(p, mainArea);
    }

    public void setVisibleTasks(java.util.List/* <Task> */visibleTasks) {
        myVisibleTasks = visibleTasks;
    }

    public void setExplicitlyHiddenTasks(Set hiddenTasks) {
        myHiddenTasks = hiddenTasks;
    }

    public Task findTaskWithCoordinates(int x, int y) {
        y = y + getVerticalOffset();
        GraphicPrimitiveContainer.GraphicPrimitive primitive = myTaskRendererImpl
                .getPrimitiveContainer().getPrimitive(x,
                        y - getChartUIConfiguration().getHeaderHeight());
        if (primitive instanceof GraphicPrimitiveContainer.Rectangle) {
            TaskActivity activity = (TaskActivity) primitive.getModelObject();
            return activity == null ? null : activity.getTask();
        }
        return null;
    }

    public ChartItem getChartItemWithCoordinates(int x, int y) {
        y = y + getVerticalOffset();
        ChartItem result = findTaskProgressItem(x, y);
        if (result == null) {
            result = findTaskBoundaryItem(x, y);
        }
        return result;
    }

    private ChartItem findTaskProgressItem(int x, int y) {
        boolean foundResult = false;
        GraphicPrimitiveContainer.GraphicPrimitive primitive = myTaskRendererImpl
                .getPrimitiveContainer().getLayer(1).getPrimitive(x, 4,
                        y - getChartUIConfiguration().getHeaderHeight(), 0);
        if (primitive instanceof GraphicPrimitiveContainer.Rectangle
                && "task.progress.end".equals(primitive.getStyle())) {
            GraphicPrimitiveContainer.Rectangle rect = (GraphicPrimitiveContainer.Rectangle) primitive;
            GraphicPrimitiveContainer.Rectangle taskRect = (GraphicPrimitiveContainer.Rectangle) myTaskRendererImpl
                .getPrimitiveContainer().getLayer(0).getPrimitive(x, 4,
                    y - getChartUIConfiguration().getHeaderHeight(), 0);;
            if(taskRect.myLeftX + 4 >= rect.getRightX()) {
                // Task completion bar is near left boundary
                if (rect.getRightX() >= x - 10 && rect.getRightX() <= x - 2) {
                    foundResult = true;
                }
            } else if(taskRect.getRightX() - 4 <= rect.getRightX()) {
                // Task completion bar is near right boundary
                if (rect.getRightX() >= x + 2 && rect.getRightX() <= x + 10) {
                    foundResult = true;
                }
            } else {
                // Progress bar is away from left and right boundaries
                if (rect.getRightX() >= x - 4 && rect.getRightX() <= x + 4) {
                    foundResult = true;
                }
            }
        }
        if(foundResult) {
            return new TaskProgressChartItem(x, getBottomUnitWidth(),
                    getBottomUnit(), (Task) primitive.getModelObject());
        }
        return null;
    }

    private ChartItem findTaskBoundaryItem(int x, int y) {
        ChartItem result = null;
        GraphicPrimitiveContainer.GraphicPrimitive primitive = myTaskRendererImpl
                .getPrimitiveContainer().getPrimitive(x,
                        y - getChartUIConfiguration().getHeaderHeight());
        if (primitive==null) {
            primitive = myTaskRendererImpl.getPrimitiveContainer().getLayer(2).getPrimitive(x, y-getChartUIConfiguration().getHeaderHeight());
        }
        if (primitive instanceof GraphicPrimitiveContainer.Rectangle) {
            GraphicPrimitiveContainer.Rectangle rect = (Rectangle) primitive;
            TaskActivity activity = (TaskActivity) primitive.getModelObject();
            if (activity != null) {
                if (activity.isFirst() && rect.myLeftX - 2 <= x
                        && rect.myLeftX + 2 >= x) {
                    result = new TaskBoundaryChartItem(activity.getTask(), true);
                }
                if (result == null && activity.isLast()
                        && rect.myLeftX + rect.myWidth - 2 <= x
                        && rect.myLeftX + rect.myWidth + 2 >= x) {
                    result = new TaskBoundaryChartItem(activity.getTask(),
                            false);
                }
                if (result == null) {
                    result = new TaskRegularAreaChartItem(activity.getTask());
                }
            }
        }
        return result;
    }

    public java.awt.Rectangle getBoundingRectangle(Task task) {
        java.awt.Rectangle result = null;
        TaskActivity[] activities = task.getActivities();
        for (int i = 0; i < activities.length; i++) {
            GraphicPrimitiveContainer.Rectangle nextRectangle = myTaskRendererImpl
                    .getPrimitive(activities[i]);
            if (nextRectangle != null) {
                java.awt.Rectangle nextAwtRectangle = new java.awt.Rectangle(
                        nextRectangle.myLeftX, nextRectangle.myTopY,
                        nextRectangle.myWidth, nextRectangle.myHeight);
                if (result == null) {
                    result = nextAwtRectangle;
                } else {
                    result = result.union(nextAwtRectangle);
                }
            }
        }
        return result;
    }

    GraphicPrimitiveContainer.Rectangle[] getTaskActivityRectangles(Task task) {
        List result = new ArrayList();
        TaskActivity[] activities = task.getActivities();
        for (int i = 0; i < activities.length; i++) {
            GraphicPrimitiveContainer.Rectangle nextRectangle = myTaskRendererImpl
                    .getPrimitive(activities[i]);
            if (nextRectangle!=null) {
                result.add(nextRectangle);
            }
        }
        return (Rectangle[]) result.toArray(new GraphicPrimitiveContainer.Rectangle[0]);
    }

    java.util.List/* <Task> */getVisibleTasks() {
        return myVisibleTasks;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sourceforge.ganttproject.chart.ChartModel#setTaskContainment(net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade)
     */
    public void setTaskContainment(
            TaskContainmentHierarchyFacade taskContainment) {
        myTaskContainment = taskContainment;
    }

    TaskContainmentHierarchyFacade getTaskContainment() {
        return myTaskContainment;
    }

    public void setTuningOptions(TuningOptions tuningOptions) {
        myTaskRendererImpl
                .setProgressRenderingEnabled(tuningOptions.renderProgress);
        myTaskRendererImpl
                .setDependenciesRenderingEnabled(tuningOptions.renderDependencies);
    }

    public int setRowHeight() {
        boolean textUP = false;
        boolean textDOWN = false;
        isOnlyDown = false;
        isOnlyUp = false;
        // GPOption[] options = myTaskRendererImpl.getOptionGroups()[0]
        //                .getOptions();// HACK: we assume there is at least one option
        // group which is label options group


//        for (int i = 1; i < options.length; i++) {
//            EnumerationOption nextOption = (EnumerationOption) options[i];
//            if ((LabelPositionOptionImpl.UP.equals(nextOption.getValue()))) {
//                textUP = true;
//            }
//            if ((LabelPositionOptionImpl.DOWN.equals(nextOption.getValue()))) {
//                textDOWN = true;
//            }
//        }

        textUP = myTaskRendererImpl.isTextUp();
        textDOWN =myTaskRendererImpl.isTextDown();


        if (textUP && textDOWN) {
            rowHeight = 40;
        } else if (textUP) {
            rowHeight = 30;
            isOnlyUp = true;
        } else if (textDOWN) {
            rowHeight = 30;
            isOnlyDown = true;
        } else {
            rowHeight = 20;
        }
        if (isPreviousState) {
            rowHeight = rowHeight + 8;
        }
        return rowHeight;
    }

    public boolean isOnlyUp() {
        return isOnlyUp;
    }

    public boolean isOnlyDown() {
        return isOnlyDown;
    }

    public boolean isSelected(int index) {
        return myTaskRendererImpl.isVisible(index);
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public GPOptionGroup[] getChartOptionGroups() {
        GPOptionGroup[] superGroups = super.getChartOptionGroups();
        GPOptionGroup[] rendererGroups = myTaskRendererImpl.getOptionGroups();
        List result = new ArrayList();
        result.addAll(Arrays.asList(superGroups));
        result.addAll(Arrays.asList(rendererGroups));
        result.add(myDependencyOptions);
        result.add(myDefaultColorOptions);
        result.add(myStateDiffOptions);
        return (GPOptionGroup[]) result.toArray(new GPOptionGroup[result.size()]);
    }


    public int setPreviousStateTasks(ArrayList tasks) {
        if (tasks == null) {
            isPreviousState = false;
        } else {
            isPreviousState = true;
        }
        myTaskRendererImpl.setPreviousStateTasks(tasks);
        return setRowHeight();
    }

    public boolean isPrevious() {
        return isPreviousState;
    }

    public ChartModelBase createCopy() {
        ChartModelImpl result = new ChartModelImpl(getTaskManager(), getTimeUnitStack(), getProjectConfig());
        result.setTaskContainment(getTaskContainment());
        super.setupCopy(result);
        GPOptionGroup[] originalOptions = this.getChartOptionGroups();
        GPOptionGroup[] copyOptions = result.getChartOptionGroups();
        for (int i=0; i<copyOptions.length; i++) {
            copyOptions[i].copyFrom(originalOptions[i]);
        }
        result.setRowHeight();
        return result;

    }

    public boolean isExplicitlyHidden(Task task) {
        return myHiddenTasks==null ? false : myHiddenTasks.contains(task);
    }

    public int calculateRowHeight() {
        return myTaskRendererImpl.calculateRowHeight();
    }

    protected int getRowCount() {
        return getVisibleTasks().size();
    }

    public EnumerationOption getDependencyHardnessOption() {
        return myDependencyHardnessOption;
    }

}
