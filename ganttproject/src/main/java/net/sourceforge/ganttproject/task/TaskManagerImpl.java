/*
 * Created on 05.07.2003
 *
 */
package net.sourceforge.ganttproject.task;

import biz.ganttproject.app.Barrier;
import biz.ganttproject.app.BarrierEntrance;
import biz.ganttproject.core.calendar.AlwaysWorkingTimeCalendarImpl;
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.GPCalendarListener;
import biz.ganttproject.core.chart.scene.BarChartActivity;
import biz.ganttproject.core.chart.scene.gantt.ChartBoundsAlgorithm;
import biz.ganttproject.core.chart.scene.gantt.ChartBoundsAlgorithm.Result;
import biz.ganttproject.core.model.task.ConstraintType;
import biz.ganttproject.core.option.*;
import biz.ganttproject.core.time.*;
import biz.ganttproject.customproperty.*;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.ProjectEventListener;
import net.sourceforge.ganttproject.gui.NotificationChannel;
import net.sourceforge.ganttproject.gui.NotificationItem;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.storage.ProjectDatabase.TaskUpdateBuilder;
import net.sourceforge.ganttproject.task.algorithm.*;
import net.sourceforge.ganttproject.task.dependency.*;
import net.sourceforge.ganttproject.task.dependency.TaskDependency.Hardness;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.StartFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.StartStartConstraintImpl;
import net.sourceforge.ganttproject.task.event.*;
import net.sourceforge.ganttproject.task.hierarchy.TaskHierarchyManagerImpl;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author bard
 */
public class TaskManagerImpl implements TaskManager {
  private static final GPCalendarCalc RESTLESS_CALENDAR = new AlwaysWorkingTimeCalendarImpl();

  private final TaskHierarchyManagerImpl myHierarchyManager;

  private final TaskDependencyCollectionImpl myDependencyCollection;

  private final AlgorithmCollection myAlgorithmCollection;

  private final List<TaskListener> myListeners = new ArrayList<>();

  private final AtomicInteger myMaxID = new AtomicInteger(0);

  private final TaskImpl myRoot;

  private final TaskManagerConfig myConfig;

  private final TaskNamePrefixOption myTaskNamePrefixOption = new TaskNamePrefixOption();

  private final StringOption myTaskCopyNamePrefixOption = new DefaultStringOption("taskCopyNamePrefix", GanttLanguage.getInstance().getText("task.copy.prefix"));

  private final EnumerationOption myDependencyHardnessOption = new DefaultEnumerationOption<Object>(
      "dependencyDefaultHardness", new String[] { "Strong", "Rubber" }) {
    {
      resetValue("Strong", true);
    }
  };

  private final TaskContainmentHierarchyFacade.Factory myFacadeFactory;

  private final Supplier<TaskContainmentHierarchyFacade> myHierarchySupplier = this::getTaskHierarchy;
  private final DependencyGraph myDependencyGraph = new DependencyGraph(myHierarchySupplier, new DependencyGraph.Logger() {
    @Override
    public void logDependencyLoop(String title, String message) {
      if (myConfig.getNotificationManager() != null) {
        myConfig.getNotificationManager().addNotifications(NotificationChannel.WARNING,
            ImmutableList.of(new NotificationItem("Dependency loop detected", message, NotificationManager.DEFAULT_HYPERLINK_LISTENER)));
      }
      GPLogger.log(title + "\n" + message);
    }
  });

  private final AlgorithmBase myScheduler;

  private boolean areEventsEnabled = true;

  private static class TaskMap {
    private final Map<Integer, Task> myId2task = new HashMap<>();
    private final TaskDocumentOrderComparator myComparator;
    private boolean isModified = true;
    private Task[] myArray;

    TaskMap(TaskManagerImpl taskManager) {
      myComparator = new TaskDocumentOrderComparator(taskManager);
    }

    void addTask(Task task) {
      myId2task.put(task.getTaskID(), task);
      isModified = true;
    }

    Task getTask(int id) {
      return myId2task.get(id);
    }

    public Task[] getTasks() {
      if (isModified) {
        myArray = myId2task.values().stream().filter(t -> !t.isDeleted())
          .sorted(myComparator).toArray(Task[]::new);
        isModified = false;
      }
      return myArray;
    }

    public void clear() {
      myId2task.clear();
      isModified = true;
    }

    public int size() {
      return myId2task.size();
    }

    public boolean isEmpty() {
      return myId2task.isEmpty();
    }

    void setDirty() {
      isModified = true;
    }

    public void removeAllTasks(Iterable<Task> tasks) {
      tasks.forEach(t -> myId2task.remove(t.getTaskID()));
      isModified = true;
    }
  }

  private final TaskMap myTaskMap = new TaskMap(this);

  private final CustomPropertyListenerImpl myCustomPropertyListener;

  private final CustomColumnsManager myCustomColumnsManager;

  private final TaskUpdateBuilder.Factory myTaskUpdateBuilderFactory;
  private Boolean isZeroMilestones = true;

  public TaskManagerImpl(TaskContainmentHierarchyFacade.Factory containmentFacadeFactory, TaskManagerConfig config) {
    this(containmentFacadeFactory, config, null);
  }

  public TaskManagerImpl(TaskContainmentHierarchyFacade.Factory containmentFacadeFactory, TaskManagerConfig config,
                         TaskUpdateBuilder.Factory taskUpdateBuilderFactory) {
    myFacadeFactory = containmentFacadeFactory == null ? new FacadeFactoryImpl() : containmentFacadeFactory;
    myTaskUpdateBuilderFactory = taskUpdateBuilderFactory;
    myCustomPropertyListener = new CustomPropertyListenerImpl(this);
    myCustomColumnsManager = new CustomColumnsManager();
    myCustomColumnsManager.addListener(getCustomPropertyListener());

    myConfig = config;
    myScheduler = new SchedulerOptional(
        config.getSchedulerDisabledOption(),
        new SchedulerImpl(myDependencyGraph, myHierarchySupplier)
    );
    myDependencyGraph.addListener(() -> {
      if (areEventsEnabled) {
        myScheduler.run();
      }
    });
    myHierarchyManager = new TaskHierarchyManagerImpl();
    EventDispatcher dispatcher = new EventDispatcher() {
      @Override
      public void fireDependencyAdded(TaskDependency dep) {
        TaskManagerImpl.this.fireDependencyAdded(dep);
      }
      @Override
      public void fireDependencyRemoved(TaskDependency dep) {
        TaskManagerImpl.this.fireDependencyRemoved(dep);
      }
      @Override
      public void fireDependencyChanged(TaskDependency dep) {
        TaskManagerImpl.this.fireDependencyChanged(dep);
      }
    };
    myDependencyCollection = new TaskDependencyCollectionImpl(myFacadeFactory, dispatcher) {
      @Override
      protected TaskContainmentHierarchyFacade getTaskHierarchy() {
        return TaskManagerImpl.this.getTaskHierarchy();
      }

      @Override
      protected Hardness getDefaultHardness() {
        String optionValue = getDependencyHardnessOption().getValue();
        return optionValue == null ? super.getDefaultHardness() : Hardness.parse(optionValue);
      }

    };
    // clear();
    myRoot = createRootTask();

    FindPossibleDependeesAlgorithm alg1 = new FindPossibleDependeesAlgorithmImpl() {
      @Override
      protected TaskContainmentHierarchyFacade createContainmentFacade() {
        return TaskManagerImpl.this.getTaskHierarchy();
      }

    };
    AdjustTaskBoundsAlgorithm alg3 = new AdjustTaskBoundsAlgorithm() {
      @Override
      protected TaskContainmentHierarchyFacade createContainmentFacade() {
        return TaskManagerImpl.this.getTaskHierarchy();
      }
    };
    RecalculateTaskScheduleAlgorithm alg2 = new RecalculateTaskScheduleAlgorithm(alg3) {
      @Override
      protected TaskContainmentHierarchyFacade createContainmentFacade() {
        return TaskManagerImpl.this.getTaskHierarchy();
      }
    };
    RecalculateTaskCompletionPercentageAlgorithm alg4 = new RecalculateTaskCompletionPercentageAlgorithm() {
      @Override
      protected TaskContainmentHierarchyFacade createContainmentFacade() {
        return TaskManagerImpl.this.getTaskHierarchy();
      }
    };
    ChartBoundsAlgorithm alg5 = new ChartBoundsAlgorithm();
    var algCriticalPath = new CriticalPathAlgorithmImpl(this, getCalendar());
    myAlgorithmCollection = new AlgorithmCollection(this, alg1, alg2, alg3, alg4, alg5, algCriticalPath, myScheduler);
    addTaskListener(new TaskListenerAdapter() {
      @Override
      public void dependencyChanged(@NotNull TaskDependencyEvent e) {
        if (areEventsEnabled) {
          myScheduler.run();
        }
      }

      @Override
      public void taskScheduleChanged(@NotNull TaskScheduleEvent e) {
        processCriticalPath(getRootTask());
      }

      @Override
      public void dependencyAdded(@NotNull TaskDependencyEvent e) {
        processCriticalPath(getRootTask());
      }

      @Override
      public void dependencyRemoved(@NotNull TaskDependencyEvent e) {
        processCriticalPath(getRootTask());
      }

      @Override
      public void taskAdded(@NotNull TaskHierarchyEvent e) {
        processCriticalPath(getRootTask());
      }

      @Override
      public void taskRemoved(@NotNull TaskHierarchyEvent e) {
        processCriticalPath(getRootTask());
      }

      @Override
      public void taskMoved(@NotNull TaskHierarchyEvent e) {
        processCriticalPath(getRootTask());
      }

      @Override
      public void taskPropertiesChanged(@NotNull TaskPropertyEvent e) {}

      @Override
      public void taskProgressChanged(@NotNull TaskPropertyEvent e) {}

      @Override
      public void taskModelReset() {}
    });
  }

  private CustomPropertyListener getCustomPropertyListener() {
    return myCustomPropertyListener;
  }

  @Override
  public GanttTask getTask(int taskId) {
    return (GanttTask) myTaskMap.getTask(taskId);
  }

  @Override
  public Task getRootTask() {
    return myRoot;
  }

  @Override
  public Task[] getTasks() {
    return myTaskMap.getTasks();
  }

  private TaskImpl createRootTask() {
    Calendar c = CalendarFactory.newCalendar();
    Date today = c.getTime();
    TaskImpl root = new GanttTask(null, CalendarFactory.createGanttCalendar(today), 1, this, -1, "");
    root.setStart(CalendarFactory.createGanttCalendar(today));
    root.setDuration(createLength(getConfig().getTimeUnitStack().getDefaultTimeUnit(), 1));
    root.setExpand(true);
    root.setName("root");
    return root;
  }

  private void projectClosed() {
    myDependencyGraph.clear();
    myTaskMap.clear();
    myMaxID.set(0);
    myDependencyCollection.clear();
    myRoot.myTaskHierarchyItem.clearChildren();
    // createRootTask();
  }

  private void projectOpened() {
    fireTaskModelReset();
    processCriticalPath(getRootTask());
    myAlgorithmCollection.getRecalculateTaskCompletionPercentageAlgorithm().run();
  }

  @Override
  public void deleteTask(Task tasktoRemove) {
    Task[] nestedTasks = getTaskHierarchy().getDeepNestedTasks(tasktoRemove);
    for (Task t : nestedTasks) {
      t.delete();
    }
    myTaskMap.removeAllTasks(Arrays.asList(nestedTasks));
    Task container = getTaskHierarchy().getContainer(tasktoRemove);
    myTaskMap.removeAllTasks(Collections.singleton(tasktoRemove));
    tasktoRemove.delete();
    fireTaskRemoved(container, tasktoRemove);
  }

  @Override
  public GanttTask createTask() {
    return (GanttTask) newTaskBuilder().build();
  }

  @Override
  public GanttTask createTask(int id) {
    return (GanttTask) newTaskBuilder().withId(id).build();
  }

  @Override
  public TaskBuilder newTaskBuilder() {
    return new TaskBuilder(getConfig().getDefaultColor()) {
      @Override
      public Task build() {
        if (myPrototype != null) {
          myId = myPrototype.getTaskID();
        }
        if (myId == null || myTaskMap.getTask(myId) != null) {
          myId = getAndIncrementId();
        }

        var startDate = myStartDate != null
          ? CalendarFactory.createGanttCalendar(myStartDate)
          : (myPrototype == null ? null : myPrototype.getStart());
        var taskUid = Strings.isNullOrEmpty(myUid) ? UUID.randomUUID().toString().replace("-", "") : myUid;
        TaskImpl task = new GanttTask("", startDate == null ? CalendarFactory.createGanttCalendar() : startDate, 1, TaskManagerImpl.this, myId, taskUid);
        TaskManagerImplKt.setupNewTask(this, task, TaskManagerImpl.this);
        registerTask(task);

        if (myPrevSibling != null && myPrevSibling != getRootTask()) {
          int position = getTaskHierarchy().getTaskIndex(myPrevSibling) + 1;
          Task parentTask = getTaskHierarchy().getContainer(myPrevSibling);
          getTaskHierarchy().move(task, parentTask, position);
        } else {
          Task parentTask = myParent == null ? getRootTask() : myParent;
          getTaskHierarchy().move(task, parentTask);
        }

        if (isLegacyMilestone) {
          task.setMilestone(isLegacyMilestone);
        }
        fireTaskAdded(task, this.mySource);
        return task;
      }
    };
  }

  protected TimeUnitStack getTimeUnitStack() {
    return getConfig().getTimeUnitStack();
  }

  int getAndIncrementId() {
    return myMaxID.getAndIncrement();
  }

  @Override
  public void registerTask(Task task) {
    int taskID = task.getTaskID();
    assert myTaskMap.getTask(taskID) == null : "There is a task that already has the ID " + taskID;
    myTaskMap.addTask(task);
    myMaxID.set(Math.max(taskID + 1, myMaxID.get()));
    myDependencyGraph.addTask(task);
  }

  boolean isRegistered(TaskImpl task) {
    return myTaskMap.getTask(task.getTaskID()) != null;
  }

  @Override
  public int getTaskCount() {
    return myTaskMap.size();
  }

  private static Iterable<BarChartActivity<?>> tasksToActivities(Task[] tasks) {
    return Arrays.stream(tasks).map(task -> new BarChartActivity<Task>() {
      @Override
      public Date getStart() {
        return task.getStart().getTime();
      }

      @Override
      public Date getEnd() {
        return task.getEnd().getTime();
      }

      @Override
      public TimeDuration getDuration() {
        return task.getDuration();
      }

      @Override
      public Task getOwner() {
        return task;
      }
    }).collect(Collectors.toList());
  }

  @Override
  public TimeDuration getProjectLength() {
    if (myTaskMap.isEmpty()) {
      return createLength(getConfig().getTimeUnitStack().getDefaultTimeUnit(), 0);
    }
    Result result = getAlgorithmCollection().getProjectBoundsAlgorithm().getBounds(tasksToActivities(myTaskMap.getTasks()));
    return createLength(getConfig().getTimeUnitStack().getDefaultTimeUnit(), result.lowerBound, result.upperBound);
  }

  @Override
  public Date getProjectStart() {
    if (myTaskMap.isEmpty()) {
      return myRoot.getStart().getTime();
    }
    Result result = getAlgorithmCollection().getProjectBoundsAlgorithm().getBounds(tasksToActivities(myTaskMap.getTasks()));
    return result.lowerBound;
  }

  @Override
  public Date getProjectEnd() {
    if (myTaskMap.isEmpty()) {
      return myRoot.getStart().getTime();
    }
    Result result = getAlgorithmCollection().getProjectBoundsAlgorithm().getBounds(tasksToActivities(myTaskMap.getTasks()));
    return result.upperBound;
  }

  @Override
  public int getProjectCompletion() {
    return myRoot.getCompletionPercentage();
  }

  @Override
  public String encode(TimeDuration taskLength) {
    StringBuilder result = new StringBuilder(String.valueOf(taskLength.getLength()));
    result.append(myConfig.getTimeUnitStack().encode(taskLength.getTimeUnit()));
    return result.toString();
  }

  @Override
  public TimeDuration createLength(String lengthAsString) throws DurationParsingException {
    int state = 0;
    StringBuilder valueBuffer = new StringBuilder();
    Integer currentValue = null;
    TimeDuration currentLength = null;
    lengthAsString += " ";
    for (int i = 0; i < lengthAsString.length(); i++) {
      char nextChar = lengthAsString.charAt(i);
      if (Character.isDigit(nextChar)) {
        switch (state) {
        case 0:
          if (currentValue != null) {
            throw new DurationParsingException();
          }
          state = 1;
          valueBuffer.setLength(0);
        case 1:
          valueBuffer.append(nextChar);
          break;
        case 2:
          TimeUnit timeUnit = findTimeUnit(valueBuffer.toString());
          if (timeUnit == null) {
            throw new DurationParsingException(valueBuffer.toString());
          }
          assert currentValue != null;
          TimeDuration localResult = createLength(timeUnit, currentValue.floatValue());
          if (currentLength == null) {
            currentLength = localResult;
          } else {
            if (currentLength.getTimeUnit().isConstructedFrom(timeUnit)) {
              float recalculatedLength = currentLength.getLength(timeUnit);
              currentLength = createLength(timeUnit, localResult.getValue() + recalculatedLength);
            } else {
              throw new DurationParsingException();
            }
          }
          state = 1;
          currentValue = null;
          valueBuffer.setLength(0);
          valueBuffer.append(nextChar);
          break;
        }
      } else if (Character.isWhitespace(nextChar)) {
        switch (state) {
        case 0:
          break;
        case 1:
          currentValue = Integer.valueOf(valueBuffer.toString());
          state = 0;
          break;
        case 2:
          TimeUnit timeUnit = findTimeUnit(valueBuffer.toString());
          if (timeUnit == null) {
            throw new DurationParsingException(valueBuffer.toString());
          }
          assert currentValue != null;
          TimeDuration localResult = createLength(timeUnit, currentValue.floatValue());
          if (currentLength == null) {
            currentLength = localResult;
          } else {
            if (currentLength.getTimeUnit().isConstructedFrom(timeUnit)) {
              float recalculatedLength = currentLength.getLength(timeUnit);
              currentLength = createLength(timeUnit, localResult.getValue() + recalculatedLength);
            } else {
              throw new DurationParsingException();
            }
          }
          state = 0;
          currentValue = null;
          break;
        }
      } else {
        switch (state) {
        case 1:
          currentValue = Integer.valueOf(valueBuffer.toString());
        case 0:
          if (currentValue == null) {
            throw new DurationParsingException("Failed to parse value=" + lengthAsString);
          }
          state = 2;
          valueBuffer.setLength(0);
        case 2:
          valueBuffer.append(nextChar);
          break;
        }
      }
    }
    if (currentValue != null) {
      currentValue = Integer.valueOf(valueBuffer.toString());
      TimeUnit dayUnit = findTimeUnit("d");
      currentLength = createLength(dayUnit, currentValue.floatValue());
    }
    return currentLength;
  }

  private TimeUnit findTimeUnit(String code) {
    return myConfig.getTimeUnitStack().findTimeUnit(code);
  }

  @Override
  public TimeDuration createLength(TimeUnit unit, float length) {
    return new TimeDurationImpl(unit, length);
  }

  @Override
  public TimeDuration createLength(long count) {
    return new TimeDurationImpl(getConfig().getTimeUnitStack().getDefaultTimeUnit(), count);
  }

  @Override
  public TimeDuration createLength(TimeUnit timeUnit, Date startDate, Date endDate) {
    return getConfig().getTimeUnitStack().createDuration(timeUnit, startDate, endDate);
  }

  @Override
  public Date shift(Date original, TimeDuration duration) {
    return RESTLESS_CALENDAR.shiftDate(original, duration);
  }

  @Override
  public TaskDependencyCollection getDependencyCollection() {
    return myDependencyCollection;
  }

  @Override
  public AlgorithmCollection getAlgorithmCollection() {
    return myAlgorithmCollection;
  }

  public TaskHierarchyManagerImpl getHierarchyManager() {
    return myHierarchyManager;
  }

  @Override
  public TaskDependencyConstraint createConstraint(final ConstraintType type) {
    TaskDependencyConstraint result;
    switch (type) {
    case finishstart:
      result = new FinishStartConstraintImpl();
      break;
    case finishfinish:
      result = new FinishFinishConstraintImpl();
      break;
    case startfinish:
      result = new StartFinishConstraintImpl();
      break;
    case startstart:
      result = new StartStartConstraintImpl();
      break;
    default:
      throw new IllegalArgumentException("Unknown constraint type=" + type);
    }
    return result;
  }

  @Override
  public void addTaskListener(TaskListener listener) {
    myListeners.add(listener);
  }

  @Override
  public GPCalendarCalc getCalendar() {
    return getConfig().getCalendar();
  }

  public ProjectEventListener getProjectListener() {
    return new ProjectEventListener.Stub() {
      @Override
      public void projectClosed() {
        TaskManagerImpl.this.projectClosed();
      }

      @Override
      public void projectOpened(BarrierEntrance barrierRegistry, Barrier<IGanttProject> barrier) {
        TaskManagerImpl.this.projectOpened();
      }
    };
  }

  public GPCalendarListener getCalendarListener() {
    return () -> {
      for (Task t : getTasks()) {
        t.setEnd(null);
      }
      myScheduler.run();
    };
  }

  @Override
  public TaskUpdateBuilder createTaskUpdateBuilder(Task task) {
    if (task == getRootTask()) {
      return null;
    }
    if (myTaskUpdateBuilderFactory != null) {
      return myTaskUpdateBuilderFactory.createTaskUpdateBuilder(task);
    }
    return null;
  }

  public void fireTaskProgressChanged(Task changedTask) {
    if (areEventsEnabled) {
      getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm().run();
      TaskPropertyEvent e = new TaskPropertyEvent(changedTask);
      for (TaskListener next : myListeners) {
        next.taskProgressChanged(e);
      }
    }
  }

  void fireTaskScheduleChanged(Task changedTask, GanttCalendar oldStartDate, GanttCalendar oldFinishDate) {
    myScheduler.run();
    if (areEventsEnabled) {
      getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm().run();
      TaskScheduleEvent e = new TaskScheduleEvent(changedTask, oldStartDate, oldFinishDate, changedTask.getStart(),
          changedTask.getEnd());
      // List copy = new ArrayList(myListeners);
      // myListeners.clear();
      for (TaskListener next : myListeners) {
        next.taskScheduleChanged(e);
      }
    }
  }

  private void fireDependencyAdded(TaskDependency newDependency) {
    myDependencyGraph.addDependency(newDependency);
    if (areEventsEnabled) {
      TaskDependencyEvent e = new TaskDependencyEvent(getDependencyCollection(), newDependency);
      for (TaskListener next : myListeners) {
        next.dependencyAdded(e);
      }
    }
  }

  private void fireDependencyRemoved(TaskDependency dep) {
    myDependencyGraph.removeDependency(dep);
    TaskDependencyEvent e = new TaskDependencyEvent(getDependencyCollection(), dep);
    for (TaskListener next : myListeners) {
      next.dependencyRemoved(e);
    }
  }

  private void fireDependencyChanged(TaskDependency dep) {
    TaskDependencyEvent e = new TaskDependencyEvent(getDependencyCollection(), dep);
    for (TaskListener next : myListeners) {
      next.dependencyChanged(e);
    }
  }

  private void fireTaskAdded(Task task, EventSource source) {
    if (areEventsEnabled) {
      getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm().run();
      var newContainer = getTaskHierarchy().getContainer(task);
      TaskHierarchyEvent e = new TaskHierarchyEvent(source, task, null, newContainer, getTaskHierarchy().getTaskIndex(task));
      for (TaskListener next : myListeners) {
        next.taskAdded(e);
      }
    }
  }

  private void fireTaskRemoved(Task container, Task task) {
    myDependencyGraph.removeTask(task);
    if (areEventsEnabled) {
      getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm().run();
      TaskHierarchyEvent e = new TaskHierarchyEvent(EventSource.UNDEFINED, task, container, null, -1);
      for (TaskListener l : myListeners) {
        l.taskRemoved(e);
      }
    }
  }

  void fireTaskMoved(Task task, Task movedFrom, Task movedTo) {
    if (areEventsEnabled) {
      TaskHierarchyEvent e = new TaskHierarchyEvent(EventSource.UNDEFINED, task, movedFrom, movedTo, getTaskHierarchy().getTaskIndex(task));
      for (TaskListener l : myListeners) {
        l.taskMoved(e);
      }
    }
  }
  void fireTaskPropertiesChanged(Task task) {
    if (areEventsEnabled) {
      TaskPropertyEvent e = new TaskPropertyEvent(task);
      for (TaskListener next : myListeners) {
        next.taskPropertiesChanged(e);
      }
    }
  }

  private void fireTaskModelReset() {
      for (TaskListener next : myListeners) {
        next.taskModelReset();
      }
  }

  public TaskManagerConfig getConfig() {
    return myConfig;
  }


  private class FacadeFactoryImpl implements TaskContainmentHierarchyFacade.Factory {
    // private final Task myRoot;
    //
    // FacadeFactoryImpl(Task root) {
    // myRoot = root;
    // }

    @Override
    public TaskContainmentHierarchyFacade createFacade() {
      return new FacadeImpl(TaskManagerImpl.this, myRoot);
    }
  }

  @Override
  public TaskContainmentHierarchyFacade getTaskHierarchy() {
    // if (myTaskContainment==null) {
    return myFacadeFactory.createFacade();
    // }
    // return myTaskContainment;
  }

  @Override
  public TaskManager emptyClone() {
    TaskManagerImpl result = new TaskManagerImpl(null, myConfig, null);
    result.myDependencyHardnessOption.setValue(this.myDependencyHardnessOption.getValue());
    return result;
  }

  @Override
  public Map<Task, Task> importData(TaskManager taskManager,
      Map<CustomPropertyDefinition, CustomPropertyDefinition> customPropertyMapping) {
    Task importRoot = taskManager.getRootTask();
    Map<Task, Task> original2imported = new LinkedHashMap<>();
    importData(importRoot, getRootTask(), customPropertyMapping, original2imported);
    TaskDependency[] deps = taskManager.getDependencyCollection().getDependencies();
    for (TaskDependency dep : deps) {
      Task nextDependant = dep.getDependant();
      Task nextDependee = dep.getDependee();
      Task importedDependant = original2imported.get(nextDependant);
      Task importedDependee = original2imported.get(nextDependee);
      try {
        TaskDependency dependency = getDependencyCollection().createDependency(importedDependant, importedDependee,
          new FinishStartConstraintImpl());
        dependency.setConstraint(TaskDependencyConstraint.Util.copyConstraint(dep.getConstraint()));
        dependency.setDifference(dep.getDifference());
        dependency.setHardness(dep.getHardness());
      } catch (TaskDependencyException e) {
        if (!GPLogger.log(e)) {
          e.printStackTrace(System.err);
        }
      }
    }
    fireTaskModelReset();
    return original2imported;
  }

  private void importData(Task importRoot, Task root,
      Map<CustomPropertyDefinition, CustomPropertyDefinition> customPropertyMapping, Map<Task, Task> original2imported) {
    Task[] nested = importRoot.getManager().getTaskHierarchy().getNestedTasks(importRoot);
    for (Task task : nested) {
      TaskBuilder builder = newTaskBuilder();
      GanttTask that = (GanttTask) task;
      if (getTask(that.getTaskID()) == null) {
        builder = builder.withId(that.getTaskID());
      }
      var nextImported = builder
        .withName(that.getName())
        .withStartDate(that.getStart().getTime())
        .withDuration(that.getDuration())
        .withColor(that.getColor())
        .withNotes(that.getNotes())
        .withWebLink(that.getWebLink())
        .withPriority(that.getPriority())
        .withParent(root)
        .build();

      nextImported.setShape(task.getShape());
      nextImported.setCompletionPercentage(task.getCompletionPercentage());
      nextImported.setExpand(task.getExpand());
      nextImported.setMilestone(task.isMilestone());
      nextImported.getCost().setValue(that.getCost());
      if (task.getThird() != null) {
        nextImported.setThirdDate(task.getThird().clone());
        nextImported.setThirdDateConstraint(task.getThirdDateConstraint());
      }

      CustomColumnsValues customValues = task.getCustomValues();
      for (CustomPropertyDefinition thatDef : importRoot.getManager().getCustomPropertyManager().getDefinitions()) {
        CustomPropertyDefinition thisDef = customPropertyMapping.get(thatDef);
        if (thisDef != null) {
          Object value = customValues.getValue(thatDef);
          if (value != null) {
            try {
              nextImported.getCustomValues().setValue(thisDef, value);
            } catch (CustomColumnsException e) {
              if (!GPLogger.log(e)) {
                e.printStackTrace(System.err);
              }
            }
          }
        } else {
          GPLogger.log("Can't find custom property definition matching "+thatDef);
        }
      }
      original2imported.put(task, nextImported);
      importData(task, nextImported, customPropertyMapping, original2imported);
    }
  }

  public Date findClosestWorkingTime(Date time) {
    return getCalendar().findClosestWorkingTime(time);
  }

  @Override
  public void processCriticalPath(Task root) {
    if (myAlgorithmCollection.getCriticalPathAlgorithm().isEnabled()) {
      try {
        myAlgorithmCollection.getRecalculateTaskScheduleAlgorithm().run();
      } catch (TaskDependencyException e) {
        if (!GPLogger.log(e)) {
          e.printStackTrace(System.err);
        }
      }
      Task[] tasks = myAlgorithmCollection.getCriticalPathAlgorithm().getCriticalTasks();
      resetCriticalPath();
      for (Task task : tasks) {
        var mutator = task.createMutator();
        mutator.setCritical(true);
        mutator.commit();
      }
    }
  }

  private void resetCriticalPath() {
    Task[] allTasks = getTasks();
    for (Task t : allTasks) {
      var mutator = t.createMutator();
      mutator.setCritical(false);
      mutator.commit();
    }
  }

  @Override
  public void importAssignments(TaskManager importedTaskManager, HumanResourceManager hrManager,
      Map<Task, Task> original2importedTask, Map<HumanResource, HumanResource> original2importedResource) {
    Task[] tasks = importedTaskManager.getTasks();
    for (Task value : tasks) {
      ResourceAssignment[] assignments = value.getAssignments();
      for (ResourceAssignment resourceAssignment : assignments) {
        Task task = getTask(original2importedTask.get(value).getTaskID());
        ResourceAssignment assignment = task.getAssignmentCollection().addAssignment(
          original2importedResource.get(resourceAssignment.getResource()));
        assignment.setLoad(resourceAssignment.getLoad());
        assignment.setCoordinator(resourceAssignment.isCoordinator());
      }
    }
  }

  void onTaskMoved(TaskImpl task) {
    if (!isRegistered(task)) {
      registerTask(task);
    }
    myDependencyGraph.move(task, getTaskHierarchy().getContainer(task));
    myTaskMap.setDirty();
  }

  public void setEventsEnabled(boolean enabled) {
    areEventsEnabled = enabled;
  }

  boolean areEventsEnabled() {
    return areEventsEnabled;
  }

  @Override
  public CustomPropertyManager getCustomPropertyManager() {
    return myCustomColumnsManager;
  }

  public URL getProjectDocument() {
    return myConfig.getProjectDocumentURL();
  }

  private static class TaskNamePrefixOption extends DefaultStringOption implements GP1XOptionConverter {
    public TaskNamePrefixOption() {
      super("taskNamePrefix");
      resetValue(GanttLanguage.getInstance().getText("defaultTaskPrefix"), true);
    }

    @Override
    public String getTagName() {
      return "task-name";
    }

    @Override
    public String getAttributeName() {
      return "prefix";
    }

    @Override
    public void loadValue(String legacyValue) {
      resetValue(legacyValue, true);
    }
  }

  @Override
  public StringOption getTaskNamePrefixOption() {
    return myTaskNamePrefixOption;
  }

  @Override
  public StringOption getTaskCopyNamePrefixOption() {
    return myTaskCopyNamePrefixOption;
  }

  @Override
  public ColorOption getTaskDefaultColorOption() {
    return myConfig.getDefaultColorOption();
  }

  @Override
  public EnumerationOption getDependencyHardnessOption() {
    return myDependencyHardnessOption;
  }

  @Override
  public void setZeroMilestones(Boolean b) {
    isZeroMilestones = b;
    if (Boolean.TRUE == isZeroMilestones) {
      List<Task> milestones = Lists.newArrayList();
      for (Task t : getTasks()) {
        if (t.isMilestone()) {
          t.setMilestone(true);
          milestones.add(t);
        }
      }
      getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().run(milestones);
      try {
        getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run(milestones);
      } catch (TaskDependencyException e) {
        GPLogger.log(e);
      }
    }
  }

  @Override
  public Boolean isZeroMilestones() {
    return isZeroMilestones;
  }

  @Override
  public DependencyGraph getDependencyGraph() {
    return myDependencyGraph;
  }
}
