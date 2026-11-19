/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.chart.gantt;

import biz.ganttproject.app.InternationalizationCoreKt;
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Polygon;
import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;
import biz.ganttproject.core.chart.grid.OffsetList;
import biz.ganttproject.core.chart.scene.gantt.*;
import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.customproperty.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttPreviousStateTask;
import net.sourceforge.ganttproject.chart.AbsenceRun;
import net.sourceforge.ganttproject.chart.AbsenceStripeKt;
import net.sourceforge.ganttproject.chart.StripeSpan;
import net.sourceforge.ganttproject.chart.ChartComparison;
import net.sourceforge.ganttproject.chart.ChartComparisonKt;
import net.sourceforge.ganttproject.chart.ComparisonResult;
import net.sourceforge.ganttproject.chart.DatesAndDurations;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.task.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

/**
 * Renders task rectangles, dependency lines and all task-related text strings
 * in the gantt chart
 */
public class GanttChartSceneBuilder {
  public interface InputApi {
    int getHeaderHeight();
    int getWidth();
    int getLabelsFontSize();
    int getVerticalOffset();
    OffsetList getTasksUnitOffsets();
    TimeUnit getProgressBarTimeUnit();
    VerticalPartitioning getVerticalPartitioning();
    List<ITask> getVisibleTasks();
    List<ITaskSceneTask> getVisibleTaskSceneTasks();
    List<ITaskSceneTask> getTasksInDocumentOrder();
    List<GanttPreviousStateTask> getBaseline();

    /** Days on which somebody assigned to the task is away, as disjoint ascending runs. */
    default List<AbsenceRun> getAbsenceRuns(int rowId) {
      return List.of();
    }
    /**
     * The tasks which a collapsed recurrence series draws in the row of its group, earliest first. Empty if the
     * row is not a collapsed series.
     */
    default List<ITaskSceneTask> getSeriesMembers(int rowId) {
      return List.of();
    }
    /** What the band underneath the task bars compares. */
    ChartComparison getComparison();
    /** The effort comparison of the task in the given row. */
    ComparisonResult getEffortComparison(int rowId);
    TaskActivitySceneBuilder.ChartApi getChartApi(TaskLabelSceneBuilder<ITaskSceneTask> labelsRenderer);
    GPCalendarCalc getCalendar();
    Date getStartDate();
    TimeDuration createLength(TimeUnit timeUnit, Date startDate, Date endDate);
    TimeDuration createLength(int duration);
    CustomPropertyManager getCustomPropertyManager();
  }

  private final Canvas canvas;
  private final InputApi input;
  private final TaskLabelSceneInput<ITaskSceneTask> taskLabelSceneApi;

  public final TaskLabelSceneBuilder<ITaskSceneTask> myLabelsRenderer;

  private final TaskActivitySceneBuilder.TaskApi<ITaskSceneTask, ITaskActivity<ITaskSceneTask>> myTaskApi = new TaskActivitySceneTaskApi();

  private final TaskActivitySceneBuilder<ITaskSceneTask, ITaskActivity<ITaskSceneTask>> myTaskActivityRenderer;
  private final TaskActivitySceneBuilder<ITaskSceneTask, ITaskActivity<ITaskSceneTask>> myBaselineActivityRenderer;

  private final Canvas myLabelsLayer;
  private final TaskActivitySceneBuilder.ChartApi myChartApi;
  private final TaskActivitySplitter mySplitter;

  public GanttChartSceneBuilder(InputApi input) {
    this(input, new Canvas());
  }

  public GanttChartSceneBuilder(InputApi input, Canvas canvas) {
    this.input = input;
    this.canvas = canvas;

    getPrimitiveContainer().setOffset(0, input.getHeaderHeight());
    getPrimitiveContainer().newLayer();
    getPrimitiveContainer().newLayer();
    getPrimitiveContainer().newLayer();
    myLabelsLayer = getPrimitiveContainer().newLayer();

    var topLabelOption = new TaskColumnEnumerationOption("taskLabelUp", input.getCustomPropertyManager().getDefinitions());
    var bottomLabelOption = new TaskColumnEnumerationOption("taskLabelDown", input.getCustomPropertyManager().getDefinitions());
    var leftLabelOption = new TaskColumnEnumerationOption("taskLabelLeft", input.getCustomPropertyManager().getDefinitions());
    var rightLabelOption = new TaskColumnEnumerationOption("taskLabelRight", input.getCustomPropertyManager().getDefinitions());
    var allOptions = List.of(topLabelOption, bottomLabelOption, leftLabelOption, rightLabelOption);
    allOptions.forEach(option -> option.setValueLocalizer(id -> {
      var column = option.pubStringToObject(id);
      if (column == null || column.getID().isEmpty()) {
        return "";
      }
      var defaultColumn = TaskDefaultColumn.find(column.getID());
      if (defaultColumn != null) {
        return defaultColumn.getName();
      }
      var customProperty = input.getCustomPropertyManager().getCustomPropertyDefinition(column.getID());
      if (customProperty != null) {
        return customProperty.getName();
      }
      return InternationalizationCoreKt.getRootLocalizer().formatText(OptionsPageBuilder.I18N.getCanonicalOptionValueLabelKey(id));
    }));

    input.getCustomPropertyManager().addListener(event -> {
      allOptions.forEach(option -> option.reload(input.getCustomPropertyManager().getDefinitions()));
    });

    taskLabelSceneApi = new TaskLabelSceneInput<>(
      topLabelOption, bottomLabelOption, leftLabelOption, rightLabelOption,
      input.getLabelsFontSize(), input.getBaseline() != null,
      ITaskSceneTask::getProperty
    );

    myLabelsRenderer = new TaskLabelSceneBuilder<>(taskLabelSceneApi, myLabelsLayer);
    // The label below the bar goes under the band: always in the comparison views, and in the dates view
    // when a baseline is shown, because then the baseline band lies right under the bar. Without a
    // baseline the dates view keeps the label where it was.
    myLabelsRenderer.setBandHeight(
        () -> input.getComparison().getNeedsBandRoomWithoutBaseline() || input.getBaseline() != null
            ? getRectangleHeight() : 0);
    myChartApi = input.getChartApi(myLabelsRenderer);
    this.mySplitter = new TaskActivitySplitter<ITask>(
      input::getStartDate,
      myChartApi::getEndDate,
      input::createLength
    );
    myTaskActivityRenderer = createTaskActivitySceneBuilder(getPrimitiveContainer(), myChartApi,
        new TaskActivitySceneBuilder.Style(0));
    myBaselineActivityRenderer = createTaskActivitySceneBuilder(
        getPrimitiveContainer().getLayer(2), myChartApi,
        new TaskActivitySceneBuilder.Style(getRectangleHeight()));
  }

  /** The vertical shift which render() puts on the canvas. */
  private int canvasDeltaY() {
    return input.getHeaderHeight() - input.getVerticalOffset();
  }

  public Canvas render() {
    getPrimitiveContainer().clear();
    getPrimitiveContainer().getLayer(0).clear();
    getPrimitiveContainer().getLayer(1).clear();
    getPrimitiveContainer().getLayer(2).clear();
    getPrimitiveContainer().setOffset(0, canvasDeltaY());
    getPrimitiveContainer().getLayer(2).setOffset(0, canvasDeltaY());

    VerticalPartitioning vp = input.getVerticalPartitioning();
    vp.build(input.getTasksInDocumentOrder());
    OffsetList defaultUnitOffsets = input.getTasksUnitOffsets();

    renderVisibleTasks(input.getVisibleTaskSceneTasks(), defaultUnitOffsets);
    renderTasksAboveAndBelowViewport(vp.getAboveViewport(), vp.getBelowViewport(), defaultUnitOffsets);
    renderDependencies();

    return getPrimitiveContainer();
  }

  public TaskLabelSceneInput getTaskLabelSceneApi() {
    return taskLabelSceneApi;
  }

  private Canvas getPrimitiveContainer() {
    return canvas;
  }

  private void renderDependencies() {
    DependencySceneBuilder.ChartApi chartApi = () -> getRectangleHeight();
    var taskApi = new DependencySceneTaskApi(input.getVisibleTasks(), mySplitter);
    DependencySceneBuilder<ITask, BarChartConnectorImpl> dependencyRenderer = new DependencySceneBuilder<>(
        getPrimitiveContainer(), getPrimitiveContainer().getLayer(1), taskApi, chartApi);
    dependencyRenderer.build();
  }

  private void renderTasksAboveAndBelowViewport(List<ITaskSceneTask> tasksAboveViewport, List<ITaskSceneTask> tasksBelowViewport,
      OffsetList defaultUnitOffsets) {
    for (ITaskSceneTask nextAbove : tasksAboveViewport) {
      List<ITaskActivity<ITaskSceneTask>> activities = /*nextAbove.isMilestone() ? Collections.<TaskActivity> singletonList(new MilestoneTaskFakeActivity(
          nextAbove)) : */nextAbove.getActivities();
      for (Canvas.Shape s : renderActivities(-1, nextAbove, activities, defaultUnitOffsets, false)) {
        s.setVisible(false);
      }
    }
    for (ITaskSceneTask nextBelow : tasksBelowViewport) {
      List<ITaskActivity<ITaskSceneTask>> activities = /*nextBelow.isMilestone() ? Collections.<TaskActivity> singletonList(new MilestoneTaskFakeActivity(
          nextBelow)) : */nextBelow.getActivities();
      List<Polygon> rectangles = renderActivities(input.getVisibleTasks().size() + 1, nextBelow, activities,
          defaultUnitOffsets, false);
      for (Polygon nextRectangle : rectangles) {
        nextRectangle.setVisible(false);
      }
    }
  }

  private void renderVisibleTasks(List<ITaskSceneTask> visibleTasks, OffsetList defaultUnitOffsets) {
    List<Polygon> boundPolygons = new ArrayList<>();
    int rowNum = 0;
    for (ITaskSceneTask t : visibleTasks) {
      boundPolygons.clear();
      List<ITaskSceneTask> seriesMembers = input.getSeriesMembers(t.getRowId());
      List<Polygon> rectangles;
      if (seriesMembers.isEmpty()) {
        List<ITaskActivity<ITaskSceneTask>> activities = t.getActivities();
        activities = mySplitter.split(activities, Integer.MAX_VALUE);
        rectangles = renderActivities(rowNum, t, activities, defaultUnitOffsets, true);
      } else {
        rectangles = renderSeriesRow(rowNum, seriesMembers, defaultUnitOffsets);
      }
      for (Polygon p : rectangles) {
        if (p.getModelObject() != null) {
          boundPolygons.add(p);
        }
      }
      renderLabels(boundPolygons);
      if (seriesMembers.isEmpty()) {
        renderComparisonBand(t, rowNum, defaultUnitOffsets);
      }
      rowNum++;
      Canvas.Line nextLine = getPrimitiveContainer().createLine(0, rowNum * getRowHeight(),
          input.getWidth(), rowNum * getRowHeight());
      nextLine.setForegroundColor(Color.GRAY);
    }
  }

  public int getRowHeight() {
    return myChartApi.getRowHeight();
  }

  /**
   * Compares a task with the state it had in the baseline and decides whether a baseline bar is
   * drawn at all and which styles it gets.
   *
   * <p>A bar is drawn whenever anything changed: the end date moved, or the duration did. The
   * colours keep saying what happened to the end date, which is what a schedule is judged by:
   * "later" when the task now ends later than it was planned to, "earlier" when it ends sooner. A
   * task which still ends on the planned day gets neither colour and is painted with the neutral
   * one.
   *
   * @return the styles to add to the baseline bar, or {@code null} if nothing changed and no
   * baseline bar is to be drawn.
   */
  static List<String> getBaselineStyles(boolean isMilestone, int baselineDuration, int currentDuration,
                                        Date baselineEnd, Date currentEnd) {
    ComparisonResult result = ChartComparisonKt.compareDates(baselineEnd, currentEnd, baselineDuration, currentDuration);
    if (result == ComparisonResult.NO_BAND) {
      return null;
    }
    List<String> styles = new ArrayList<String>();
    if (isMilestone) {
      styles.add("milestone");
    }
    if (result.getStyleName() != null) {
      styles.add(result.getStyleName());
    }
    return styles;
  }

  /** Draws the band underneath the task bar which the selected {@link ChartComparison} asks for. */
  private void renderComparisonBand(ITaskSceneTask t, int rowNum, OffsetList defaultUnitOffsets) {
    switch (input.getComparison()) {
      case EFFORT:
        renderEffortBand(t, rowNum, defaultUnitOffsets);
        break;
      case DURATIONS:
        renderDurationBand(t, rowNum, defaultUnitOffsets);
        break;
      case DATES_AND_DURATIONS:
        renderDatesAndDurationsBand(t, rowNum, defaultUnitOffsets);
        break;
      default:
        renderBaseline(t, rowNum, defaultUnitOffsets);
        break;
    }
  }

  private void renderBaseline(ITaskSceneTask t, int rowNum, OffsetList defaultUnitOffsets) {
    GanttPreviousStateTask taskBaseline = findBaselineTask(t);
    if (taskBaseline == null) {
      return;
    }
    Date startDate = taskBaseline.getStart().getTime();
    Date endDate = getBaselineEnd(taskBaseline);
    // The durations are compared as the whole units which the baseline stored, which is what
    // GanttPreviousState.createTasks() took from the task in the first place.
    List<String> styles = getBaselineStyles(
        t.isMilestone(), taskBaseline.getDuration(), t.getDuration().getLength(),
        endDate, t.getEnd().getTime());
    if (styles == null) {
      return;
    }
    paintBand(rowNum, defaultUnitOffsets, createBandActivities(t, startDate, endDate), styles);
  }

  /**
   * The durations band starts where the task bar starts and is as long as the task was in the baseline, so that it
   * sticks out by the change of the duration only. Without a baseline a neutral band lies under the bar.
   */
  private void renderDurationBand(ITaskSceneTask t, int rowNum, OffsetList defaultUnitOffsets) {
    List<ITaskActivity<ITaskSceneTask>> activities = t.getActivities();
    if (activities.isEmpty()) {
      return;
    }
    GanttPreviousStateTask taskBaseline = findBaselineTask(t);
    Integer baselineDuration = taskBaseline == null ? null : taskBaseline.getDuration();
    ComparisonResult result = ChartComparisonKt.compareDurations(baselineDuration, t.getDuration().getLength());
    if (result == ComparisonResult.NO_BAND) {
      return;
    }
    List<ITaskActivity<ITaskSceneTask>> band = taskBaseline == null
        ? mySplitter.split(activities, Integer.MAX_VALUE)
        : createDurationBandActivities(t, activities, baselineDuration);
    paintBand(rowNum, defaultUnitOffsets, band, getBandStyles(t, result, null));
  }

  /**
   * The dates comparison in the upper half of the band, anchored at the baseline start like the dates band, and the
   * durations comparison in the lower half, anchored at the task start like the durations band. A half with nothing
   * to show is not drawn. A summary task keeps only the dates comparison, at full height.
   */
  private void renderDatesAndDurationsBand(ITaskSceneTask t, int rowNum, OffsetList defaultUnitOffsets) {
    List<ITaskActivity<ITaskSceneTask>> activities = t.getActivities();
    if (activities.isEmpty()) {
      return;
    }
    GanttPreviousStateTask taskBaseline = findBaselineTask(t);
    boolean isSummary = t.getHasNestedTasks();
    if (taskBaseline == null) {
      // Without a baseline there is no dates half, and a summary task has nothing else to show.
      DatesAndDurations both = ChartComparisonKt.compareDatesAndDurations(
          null, t.getEnd().getTime(), null, t.getDuration().getLength());
      if (!isSummary && both.getDurations() != ComparisonResult.NO_BAND) {
        paintBand(rowNum, defaultUnitOffsets, mySplitter.split(activities, Integer.MAX_VALUE),
            getBandStyles(t, both.getDurations(), BAND_LOWER));
      }
      return;
    }
    Date baselineStart = taskBaseline.getStart().getTime();
    Date baselineEnd = getBaselineEnd(taskBaseline);
    DatesAndDurations both = ChartComparisonKt.compareDatesAndDurations(
        baselineEnd, t.getEnd().getTime(), taskBaseline.getDuration(), t.getDuration().getLength());
    if (both.getDates() != ComparisonResult.NO_BAND) {
      paintBand(rowNum, defaultUnitOffsets, createBandActivities(t, baselineStart, baselineEnd),
          getBandStyles(t, both.getDates(), isSummary ? null : BAND_UPPER));
    }
    if (!isSummary && both.getDurations() != ComparisonResult.NO_BAND) {
      paintBand(rowNum, defaultUnitOffsets, createDurationBandActivities(t, activities, taskBaseline.getDuration()),
          getBandStyles(t, both.getDurations(), BAND_LOWER));
    }
  }

  /** The effort band lies exactly under the task bar: only its color tells, the calendar has no say. */
  private void renderEffortBand(ITaskSceneTask t, int rowNum, OffsetList defaultUnitOffsets) {
    ComparisonResult result = input.getEffortComparison(t.getRowId());
    List<ITaskActivity<ITaskSceneTask>> activities = t.getActivities();
    if (result == ComparisonResult.NO_BAND || activities.isEmpty()) {
      return;
    }
    paintBand(rowNum, defaultUnitOffsets, mySplitter.split(activities, Integer.MAX_VALUE),
        getBandStyles(t, result, null));
  }

  private GanttPreviousStateTask findBaselineTask(ITaskSceneTask t) {
    List<GanttPreviousStateTask> baseline = input.getBaseline();
    if (baseline != null) {
      for (GanttPreviousStateTask taskBaseline : baseline) {
        if (taskBaseline.getId() == t.getRowId()) {
          return taskBaseline;
        }
      }
    }
    return null;
  }

  private Date getBaselineEnd(GanttPreviousStateTask taskBaseline) {
    TimeDuration duration = input.createLength(taskBaseline.getDuration());
    return input.getCalendar().shiftDate(taskBaseline.getStart().getTime(), duration);
  }

  private List<ITaskActivity<ITaskSceneTask>> createDurationBandActivities(
      ITaskSceneTask t, List<ITaskActivity<ITaskSceneTask>> activities, int baselineDuration) {
    Date startDate = activities.get(0).getStart();
    Date endDate = input.getCalendar().shiftDate(startDate, input.createLength(baselineDuration));
    return createBandActivities(t, startDate, endDate);
  }

  private List<ITaskActivity<ITaskSceneTask>> createBandActivities(ITaskSceneTask t, Date startDate, Date endDate) {
    List<ITaskActivity<ITaskSceneTask>> result = new ArrayList<ITaskActivity<ITaskSceneTask>>();
    if (t.isMilestone()) {
      result.add(new TaskSceneMilestoneActivity(t, startDate, endDate, input.createLength(1)));
    } else {
      new TaskActivitiesSceneAlgorithm(
        input.getCalendar(),
        (Date s, Date e) -> input.createLength(t.getDuration().getTimeUnit(), s, e)
      ).recalculateActivities(t, result, startDate, endDate);
    }
    return result;
  }

  private static List<String> getBandStyles(ITaskSceneTask t, ComparisonResult result, String half) {
    List<String> styles = new ArrayList<String>();
    if (t.isMilestone()) {
      styles.add("milestone");
    }
    if (result.getStyleName() != null) {
      styles.add(result.getStyleName());
    }
    if (half != null) {
      styles.add(half);
    }
    return styles;
  }

  private void paintBand(int rowNum, OffsetList defaultUnitOffsets,
                         List<ITaskActivity<ITaskSceneTask>> activities, List<String> styles) {
    List<Polygon> bandRectangles = myBaselineActivityRenderer.renderActivities(rowNum, activities,
        defaultUnitOffsets);
    for (int i = 0; i < bandRectangles.size(); i++) {
      Polygon r = bandRectangles.get(i);
      r.setStyle("previousStateTask");
      for (String s : styles) {
        r.addStyle(s);
      }
      if (i == 0) {
        r.addStyle("start");
      }
      if (i == bandRectangles.size() - 1) {
        r.addStyle("end");
      }
    }
  }

  /** Styles of the two halves of the dates and durations band, read by the painter. */
  public static final String BAND_UPPER = "band.upper";
  public static final String BAND_LOWER = "band.lower";

  private static Predicate<Polygon> REMOVE_SUPERTASK_ENDINGS = shape -> !shape.hasStyle("task.ending");

  /**
   * Draws every member of a collapsed series as a bar of its own in one row. Each bar has the progress of its
   * member, its own absence stripes and its own comparison band. Members which overlap are drawn over each other, the later one on top.
   *
   * @return the bars of the last member, which is the one that gets the labels
   */
  private List<Polygon> renderSeriesRow(int rowNum, List<ITaskSceneTask> members, OffsetList defaultUnitOffsets) {
    List<Polygon> last = new ArrayList<>();
    for (ITaskSceneTask member : members) {
      List<ITaskActivity<ITaskSceneTask>> activities = mySplitter.split(member.getActivities(), Integer.MAX_VALUE);
      last = myTaskActivityRenderer.renderActivities(rowNum, activities, defaultUnitOffsets);
      if (!myTaskApi.hasNestedTasks(member) && !member.isMilestone()) {
        renderAbsenceStripes(member, last, defaultUnitOffsets);
        renderProgressBar(last.stream().filter(REMOVE_SUPERTASK_ENDINGS).toList());
      }
      renderComparisonBand(member, rowNum, defaultUnitOffsets);
    }
    return last;
  }

  private List<Polygon> renderActivities(final int rowNum, ITaskSceneTask t, List<ITaskActivity<ITaskSceneTask>> activities,
      OffsetList defaultUnitOffsets, boolean areVisible) {
    List<Polygon> rectangles = myTaskActivityRenderer.renderActivities(rowNum, activities, defaultUnitOffsets);
    if (areVisible && !myTaskApi.hasNestedTasks(t) && !t.isMilestone() && !t.isProjectTask()) {
      renderAbsenceStripes(t, rectangles, defaultUnitOffsets);
      renderProgressBar(rectangles.stream().filter(REMOVE_SUPERTASK_ENDINGS).toList());
    }
    if (areVisible && myTaskApi.hasNotes(t)) {
      Rectangle notes = getPrimitiveContainer().createRectangle(input.getWidth() - 24, rowNum * getRowHeight() + getRowHeight()/2 - 8, 16, 16);
      notes.setStyle("task.notesMark");
      getPrimitiveContainer().bind(notes, t);
    }
    return rectangles;
  }

  /**
   * Draws a stripe over each part of a task bar on which somebody assigned to the task is away. The stripes are
   * created on the base canvas right after the bars, so they lie on the bars and under the progress bar and the
   * labels. They are not bound to the task, therefore clicks still hit the bar. Activities without work (weekends,
   * holidays) get no stripe.
   */
  private void renderAbsenceStripes(ITaskSceneTask t, List<Polygon> barRectangles, OffsetList offsets) {
    List<AbsenceRun> runs = input.getAbsenceRuns(t.getRowId());
    if (runs.isEmpty()) {
      return;
    }
    int deltaY = canvasDeltaY();
    for (Polygon bar : barRectangles) {
      if (!(bar instanceof Rectangle barRect) || !(bar.getModelObject() instanceof ITaskActivity<?> activity)) {
        continue;
      }
      if (activity.getIntensity() == 0f) {
        continue;
      }
      List<StripeSpan> spans = AbsenceStripeKt.absenceStripeSpans(
          runs, activity.getStart(), activity.getEnd(), barRect.getLeftX(), barRect.getRightX(), offsets);
      for (StripeSpan span : spans) {
        Rectangle stripe = getPrimitiveContainer().createRectangle(
            span.getLeftX(), barRect.getTopY() - deltaY, span.getWidth(), barRect.getHeight());
        stripe.setStyle(AbsenceStripeKt.STYLE_ABSENCE);
      }
    }
  }

  private void renderLabels(List<Polygon> rectangles) {
    if (!rectangles.isEmpty()) {
      myLabelsRenderer.renderLabels(rectangles);
    }
  }

  private void renderProgressBar(List<Polygon> rectangles) {
    if (rectangles.isEmpty()) {
      return;
    }
    final Canvas container = getPrimitiveContainer().getLayer(0);
    final TimeUnit timeUnit = input.getProgressBarTimeUnit();
    final ITaskSceneTask task = ((ITaskActivity<ITaskSceneTask>) rectangles.get(0).getModelObject()).getOwner();
    float length = task.getDuration().getLength(timeUnit);
    float completed = task.getCompletionPercentage() * length / 100f;
    Polygon lastProgressRectangle = null;

    for (Polygon nextRectangle : rectangles) {
      final ITaskActivity<ITaskSceneTask> nextActivity = (ITaskActivity<ITaskSceneTask>) nextRectangle.getModelObject();
      final float nextLength = nextActivity.getDuration().getLength(timeUnit);

      final int nextProgressBarLength;
      if (completed > nextLength || nextActivity.getIntensity() == 0f) {
        nextProgressBarLength = nextRectangle.getWidth();
        if (nextActivity.getIntensity() > 0f) {
          completed -= nextLength;
        }
      } else {
        nextProgressBarLength = (int) (nextRectangle.getWidth() * (completed / nextLength));
        completed = 0f;
      }

      final Rectangle nextProgressBar = container.createRectangle(nextRectangle.getLeftX(),
          nextRectangle.getMiddleY() - 1, nextProgressBarLength, 3);
      nextProgressBar.setStyle(completed == 0f ? "task.progress.end" : "task.progress");
      getPrimitiveContainer().getLayer(0).bind(nextProgressBar, task);
      if (completed == 0) {
        lastProgressRectangle = nextRectangle;
        break;
      }
    }
    if (lastProgressRectangle == null) {
      lastProgressRectangle = rectangles.get(rectangles.size() - 1);
    }
    // createDownSideText(lastProgressRectangle);
  }

  private int getRectangleHeight() {
    return myLabelsRenderer.getFontHeight();
  }

  public Canvas getLabelLayer() {
    return myLabelsLayer;
  }

  private TaskActivitySceneBuilder<ITaskSceneTask, ITaskActivity<ITaskSceneTask>> createTaskActivitySceneBuilder(
      Canvas canvas, TaskActivitySceneBuilder.ChartApi chartApi, TaskActivitySceneBuilder.Style style) {
    return new TaskActivitySceneBuilder<>(myTaskApi, chartApi, canvas, myLabelsRenderer, style);
  }
}
