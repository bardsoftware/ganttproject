/*
Copyright 2014 BarD Software s.r.o

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
package net.sourceforge.ganttproject.chart;

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.render.ShapePaint;
import biz.ganttproject.core.chart.scene.IdentifiableRow;
import biz.ganttproject.core.option.DefaultFontOption;
import biz.ganttproject.core.option.DefaultIntegerOption;
import biz.ganttproject.core.option.FontSpec;
import biz.ganttproject.core.time.GanttCalendar;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeDurationImpl;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import com.google.common.collect.Lists;
import kotlin.jvm.functions.Function2;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.chart.gantt.*;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.test.task.TaskTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public class TaskRendererImplTest extends TaskTestCase {
  private List<Task> createTasks(TaskManager taskManager, int count) {
    List<Task> result = Lists.newArrayList();
    for (int i = 0; i < count; i++) {
      result.add(taskManager.newTaskBuilder().withId(i).build());
    }
    return result;
  }

  private List<ITaskSceneTask> toSceneTask(List<Task> tasks) {
    return tasks.stream().map(TaskSceneTask::new).collect(Collectors.toList());
  }

  public void testVerticalPartitioningNoCollapsed() {
    TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder().build();
    Function2<ITaskSceneTask, ITaskSceneTask, Boolean> areUnrelated = (t1, t2) ->
      taskManager.getTaskHierarchy().areUnrelated(taskManager.getTask(t1.getRowId()), taskManager.getTask(t2.getRowId()));
    List<ITaskSceneTask> allTasks = toSceneTask(createTasks(taskManager, 10));
    {
      VerticalPartitioning partitioning = new VerticalPartitioning(allTasks.subList(0, 10), areUnrelated);
      partitioning.build(allTasks);
      assertTrue(partitioning.getAboveViewport().isEmpty());
      assertTrue(partitioning.getBelowViewport().isEmpty());
    }
    {
      VerticalPartitioning partitioning = new VerticalPartitioning(allTasks.subList(0, 5), areUnrelated);
      partitioning.build(allTasks);
      assertTrue(partitioning.getAboveViewport().isEmpty());
      assertEquals(5, partitioning.getBelowViewport().size());
    }
    {
      VerticalPartitioning partitioning = new VerticalPartitioning(allTasks.subList(5, 10), areUnrelated);
      partitioning.build(allTasks);
      assertEquals(5, partitioning.getAboveViewport().size());
      assertTrue(partitioning.getBelowViewport().isEmpty());
    }
    {
      VerticalPartitioning partitioning = new VerticalPartitioning(allTasks.subList(3, 7), areUnrelated);
      partitioning.build(allTasks);
      assertEquals(3, partitioning.getAboveViewport().size());
      assertEquals(3, partitioning.getBelowViewport().size());
    }
  }

  public void testVerticalPartitioningWithCollapsedTasks() {
    TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder().build();
    Function2<ITaskSceneTask, ITaskSceneTask, Boolean> areUnrelated = (t1, t2) ->
      taskManager.getTaskHierarchy().areUnrelated(taskManager.getTask(t1.getRowId()), taskManager.getTask(t2.getRowId()));
    List<Task> allTasks = createTasks(taskManager, 10);
    List<ITaskSceneTask> sceneTasks = toSceneTask(allTasks);
    allTasks.get(2).move(allTasks.get(1));
    allTasks.get(1).move(allTasks.get(0));
    allTasks.get(0).setExpand(false);
    {
      VerticalPartitioning partitioning = new VerticalPartitioning(sceneTasks.subList(4, 10), areUnrelated);
      partitioning.build(sceneTasks);
      assertEquals(2, partitioning.getAboveViewport().size());
      assertEquals(0, partitioning.getAboveViewport().get(0).getRowId());
      assertEquals(3, partitioning.getAboveViewport().get(1).getRowId());
      assertTrue(partitioning.getBelowViewport().isEmpty());
    }
    allTasks.get(7).move(allTasks.get(6));
    allTasks.get(9).move(allTasks.get(8));
    allTasks.get(6).setExpand(false);
    allTasks.get(8).setExpand(false);
    {
      VerticalPartitioning partitioning = new VerticalPartitioning(sceneTasks.subList(4, 6), areUnrelated);
      partitioning.build(sceneTasks);
      assertEquals(2, partitioning.getAboveViewport().size());
      assertEquals(0, partitioning.getAboveViewport().get(0).getRowId());
      assertEquals(3, partitioning.getAboveViewport().get(1).getRowId());
      assertEquals(2, partitioning.getBelowViewport().size());
      assertEquals(6, partitioning.getBelowViewport().get(0).getRowId());
      assertEquals(8, partitioning.getBelowViewport().get(1).getRowId());
    }
  }

  // Tests algorithm which partitions task activities into "before viewport", "inside viewport"
  // and "after viewport" parts
  public void testSplitOnBounds() {
    Task task = createTask(TestSetupHelper.newFriday(), 5);
    List<ITaskActivity<ITaskSceneTask>> taskActivities = task.getActivities().stream()
      .map((a) -> new TaskActivityDataImpl<ITaskSceneTask>(
        a.isFirst(), a.isLast(), a.getIntensity(), new TaskSceneTask(task), a.getStart(), a.getEnd(), a.getDuration()
      ))
      .collect(Collectors.toList());
    {
      // Split on frame with frame start = task start
      List<ITaskActivity<ITaskSceneTask>> activities = getSplitter(
        TestSetupHelper.newFriday().getTime(), TestSetupHelper.newMonday().getTime()
      ).split(taskActivities);
      assertEquals(2, activities.size());
      assertTrue(activities.get(0).isFirst());
      assertEquals(TestSetupHelper.newFriday().getTime(), activities.get(0).getStart());
      assertEquals(TestSetupHelper.newMonday().getTime(), activities.get(0).getEnd());

      assertTrue(activities.get(1).isLast());
      assertEquals(TestSetupHelper.newMonday().getTime(), activities.get(1).getStart());
      assertEquals(TestSetupHelper.newWendesday().getTime(), activities.get(1).getEnd());
    }
    {
      // Task crosses both frame borders
      List<ITaskActivity<ITaskSceneTask>> activities = getSplitter(
        TestSetupHelper.newSaturday().getTime(), TestSetupHelper.newTuesday().getTime()
      ).split(taskActivities);
      assertEquals(3, activities.size());
      assertTrue(activities.get(0).isFirst());
      assertEquals(TestSetupHelper.newFriday().getTime(), activities.get(0).getStart());
      assertEquals(TestSetupHelper.newSaturday().getTime(), activities.get(0).getEnd());

      assertFalse(activities.get(1).isFirst());
      assertFalse(activities.get(1).isLast());
      assertEquals(TestSetupHelper.newSaturday().getTime(), activities.get(1).getStart());
      assertEquals(TestSetupHelper.newTuesday().getTime(), activities.get(1).getEnd());

      assertTrue(activities.get(2).isLast());
      assertEquals(TestSetupHelper.newTuesday().getTime(), activities.get(2).getStart());
      assertEquals(TestSetupHelper.newWendesday().getTime(), activities.get(2).getEnd());
    }
    {
      // Split on frame with frame end = task end
      List<ITaskActivity<ITaskSceneTask>> activities = getSplitter(
        TestSetupHelper.newMonday().getTime(), TestSetupHelper.newWendesday().getTime()
      ).split(taskActivities);
      assertEquals(2, activities.size());
      assertTrue(activities.get(0).isFirst());
      assertEquals(TestSetupHelper.newFriday().getTime(), activities.get(0).getStart());
      assertEquals(TestSetupHelper.newMonday().getTime(), activities.get(0).getEnd());

      assertTrue(activities.get(1).isLast());
      assertEquals(TestSetupHelper.newMonday().getTime(), activities.get(1).getStart());
      assertEquals(TestSetupHelper.newWendesday().getTime(), activities.get(1).getEnd());
    }
  }

  public void testGetTaskRectangles() {
    Task t = createTask(TestSetupHelper.newMonday());
    {
      t.setColor(Color.RED);
      t.setDuration(getTaskManager().createLength(4));
    }
    ChartModelImpl chartModel;
    {
      // Setup chart with start date on Wednesday, size 200x200, weeks as big unit, days as small unit
      UIConfiguration projectConfig = new UIConfiguration(Color.BLACK, false);
      projectConfig.setChartFontOption(new DefaultFontOption("foo", new FontSpec("Foo", FontSpec.Size.HUGE), Collections.<String>emptyList()));
      projectConfig.setDpiOption(new DefaultIntegerOption("bar", 96));
      chartModel = new ChartModelImpl(getTaskManager(), new GPTimeUnitStack(), projectConfig);
      chartModel.setStartDate(TestSetupHelper.newWendesday().getTime());
      chartModel.setVisibleTasks(Lists.newArrayList(t));
      chartModel.setBounds(new Dimension(200, 200));
      chartModel.setTopTimeUnit(GPTimeUnitStack.WEEK);
      chartModel.setBottomTimeUnit(GPTimeUnitStack.DAY);
      chartModel.setBottomUnitWidth(20);
    }
    TaskRendererImpl2 renderer = new TaskRendererImpl2(chartModel);
    chartModel.addRenderer(renderer);
    renderer.render();

    // We expect that renderer will create two rectangles for our task, one "invisible" (before chart start date)
    // and one visible
    List<Canvas.Rectangle> rectangles = TaskRendererImpl2.getTaskRectangles(t, chartModel);
    assertEquals(2, rectangles.size());
    ITaskActivity<IdentifiableRow> part1 = (ITaskActivity<IdentifiableRow>) rectangles.get(0).getModelObject();
    assertNotNull(part1);
    assertEquals(TestSetupHelper.newMonday().getTime(), part1.getStart());
    assertEquals(TestSetupHelper.newWendesday().getTime(), part1.getEnd());

    ITaskActivity<IdentifiableRow> part2 = (ITaskActivity<IdentifiableRow>) rectangles.get(1).getModelObject();
    assertNotNull(part2);
    assertEquals(TestSetupHelper.newWendesday().getTime(), part2.getStart());
    assertEquals(2, part2.getDuration().getLength());
  }

  private TaskActivitySplitter<ITaskSceneTask> getSplitter(Date start, Date end) {
    return new TaskActivitySplitter<ITaskSceneTask>(
      () -> start,
      () -> end,
      (u, s, e) -> new TimeDurationImpl(u, TimeUnit.DAYS.convert(s.getTime() - e.getTime(), TimeUnit.MILLISECONDS))
    );
  }

  private static class TaskSceneTask implements ITaskSceneTask {
    private final Task task;

    TaskSceneTask(Task task) {
      this.task = task;
    }

    @Override
    public int getRowId() {
      return task.getRowId();
    }

    @Override
    public boolean isCritical() {
      return false;
    }

    @Override
    public boolean isProjectTask() {
      return false;
    }

    @Override
    public boolean getHasNestedTasks() {
      return false;
    }

    @NotNull
    @Override
    public Color getColor() {
      return null;
    }

    @Nullable
    @Override
    public ShapePaint getShape() {
      return null;
    }

    @Nullable
    @Override
    public String getNotes() {
      return null;
    }

    @Override
    public boolean isMilestone() {
      return false;
    }

    @NotNull
    @Override
    public GanttCalendar getEnd() {
      return null;
    }

    @NotNull
    @Override
    public List<ITaskActivity<ITaskSceneTask>> getActivities() {
      return null;
    }

    @Override
    public boolean getExpand() {
      return task.getExpand();
    }

    @NotNull
    @Override
    public TimeDuration getDuration() {
      return null;
    }

    @Override
    public int getCompletionPercentage() {
      return 0;
    }

    @Nullable
    @Override
    public Object getProperty(@Nullable String propertyID) {
      return null;
    }
  }
}
