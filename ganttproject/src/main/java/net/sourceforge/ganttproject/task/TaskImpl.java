/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject Team

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
package net.sourceforge.ganttproject.task;

import biz.ganttproject.core.calendar.AlwaysWorkingTimeCalendarImpl;
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.chart.render.ShapePaint;
import biz.ganttproject.core.time.GanttCalendar;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeDurationImpl;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import biz.ganttproject.customproperty.CustomColumnsException;
import biz.ganttproject.customproperty.CustomColumnsValues;
import biz.ganttproject.customproperty.CustomPropertyHolder;
import com.google.common.collect.ImmutableList;
import kotlin.Unit;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.chart.MilestoneTaskFakeActivity;
import net.sourceforge.ganttproject.document.AbstractURLDocument;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmCollection;
import net.sourceforge.ganttproject.task.algorithm.CostAlgorithmImpl;
import net.sourceforge.ganttproject.task.dependency.*;
import net.sourceforge.ganttproject.task.hierarchy.TaskHierarchyItem;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

/**
 * @author bard
 */
public class TaskImpl implements Task {
  private final String myUid;
  private final int myID;

  final TaskManagerImpl myManager;

  private String myName;

  private String myWebLink = "";

  private boolean isMilestone;

  boolean isProjectTask;

  private Priority myPriority;

  GanttCalendar myStart;

  GanttCalendar myEnd;

  GanttCalendar myThird;

  private int myThirdDateConstraint;

  int myCompletionPercentage;

  TimeDuration myLength;

  private final List<TaskActivity> myActivities = new ArrayList<>();

  private boolean bExpand;

  // private final TaskDependencyCollection myDependencies = new
  // TaskDependencyCollectionImpl();
  private final ResourceAssignmentCollectionImpl myAssignments;

  private final TaskDependencySlice myDependencySlice;

  private final TaskDependencySlice myDependencySliceAsDependant;

  private final TaskDependencySlice myDependencySliceAsDependee;

  private boolean myEventsEnabled;

  final TaskHierarchyItem myTaskHierarchyItem;

  private ShapePaint myShape;

  private Color myColor;

  private String myNotes;

  MutatorBase myMutator;

  private final CustomColumnsValues customValues;

  private boolean critical;

  private List<TaskActivity> myMilestoneActivity;

  private final CostImpl myCost = new CostImpl();

  private boolean isUnplugged = false;

  public final static int NONE = 0;

  public final static int EARLIESTBEGIN = 1;

  public static final GPCalendarCalc RESTLESS_CALENDAR = new AlwaysWorkingTimeCalendarImpl();

  private static final TimeDuration EMPTY_DURATION = new TimeDurationImpl(GPTimeUnitStack.DAY, 0);
  private boolean isDeleted;

  protected TaskImpl(@NotNull TaskManagerImpl taskManager, int taskID, @NotNull String taskUid) {
    myManager = taskManager;
    myID = taskID;
    myUid = taskUid;

    myAssignments = new ResourceAssignmentCollectionImpl(this, () -> myManager.getConfig().getResourceManager());
    myDependencySlice = new TaskDependencySliceImpl(this, myManager.getDependencyCollection(), TaskDependencySlice.COMPLETE_SLICE_FXN);
    myDependencySliceAsDependant = new TaskDependencySliceAsDependant(this, myManager.getDependencyCollection());
    myDependencySliceAsDependee = new TaskDependencySliceAsDependee(this, myManager.getDependencyCollection());
    myPriority = DEFAULT_PRIORITY;
    myTaskHierarchyItem = myManager.getHierarchyManager().createItem(this);
    myNotes = "";
    bExpand = true;
    myColor = null;

    customValues = new CustomColumnsValues(myManager.getCustomPropertyManager(), event -> {
      myManager.fireTaskPropertiesChanged(this);
      return Unit.INSTANCE;
    });
  }

  protected TaskImpl(@NotNull TaskManagerImpl manager, @NotNull TaskImpl copy, boolean isUnplugged, int taskId, @NotNull String taskUid) {
    this.isUnplugged = isUnplugged;
    myManager = manager;
    // Use a new (unique) ID for the cloned task
    myID = taskId;
    myUid = taskUid;

    if (!isUnplugged) {
      myTaskHierarchyItem = myManager.getHierarchyManager().createItem(this);
    } else {
      myTaskHierarchyItem = copy.myTaskHierarchyItem;
    }
    myAssignments = new ResourceAssignmentCollectionImpl(this, () -> myManager.getConfig().getResourceManager());
    myAssignments.importData(copy.getAssignmentCollection());
    myName = copy.myName;
    myWebLink = copy.myWebLink;
    isMilestone = copy.isMilestone;
    isProjectTask = copy.isProjectTask;
    myPriority = copy.myPriority;
    myStart = copy.myStart;
    myEnd = copy.myEnd;
    myThird = copy.myThird;
    myThirdDateConstraint = copy.myThirdDateConstraint;
    myCompletionPercentage = copy.myCompletionPercentage;
    myLength = copy.myLength;
    myShape = copy.myShape;
    myColor = copy.myColor;
    myNotes = copy.myNotes;
    bExpand = copy.bExpand;
    myCost.setValue(copy.myCost);

    myDependencySlice = new TaskDependencySliceImpl(this, myManager.getDependencyCollection(), TaskDependencySlice.COMPLETE_SLICE_FXN);
    myDependencySliceAsDependant = new TaskDependencySliceAsDependant(this, myManager.getDependencyCollection());
    myDependencySliceAsDependee = new TaskDependencySliceAsDependee(this, myManager.getDependencyCollection());

    customValues = copy.getCustomValues().copyOf();

    recalculateActivities();
  }

  @Override
  public Task unpluggedClone() {
    return new TaskImpl(TaskImpl.this.myManager, TaskImpl.this, true, TaskImpl.this.myID, TaskImpl.this.myUid) {
      @Override
      public boolean isSupertask() {
        return false;
      }
    };
  }

  @Override
  public int getRowId() {
    return getTaskID();
  }

  @Override
  public TaskMutator createMutator() {
    if (myMutator != null) {
      return myMutator.reentrance();
    }
    myMutator = new MutatorImpl(myManager, this, getManager().createTaskUpdateBuilder(this));
    return myMutator;
  }

  @Override
  public ShiftMutator createShiftMutator() {
    return new ShiftMutatorImpl(this);
  }

  @Override
  public TaskMutator createMutatorFixingDuration() {
    if (myMutator != null) {
      return myMutator.reentrance();
    }
    myMutator = TaskImplKt.createMutatorFixingDuration(myManager, this, getManager().createTaskUpdateBuilder(this));
    return myMutator;
  }
  // main properties
  @Override
  public int getTaskID() {
    return myID;
  }

  @Override
  public String getUid() {
    return myUid;
  }

  @Override
  public String getName() {
    return myName;
  }

  public String getWebLink() {
    return myWebLink;
  }

  @Override
  public List<Document> getAttachments() {
    if (getWebLink() != null && !"".equals(getWebLink())) {
      return Collections.singletonList(new AbstractURLDocument() {
        @Override
        public boolean canRead() {
          return true;
        }

        @Override
        public IStatus canWrite() {
          return Status.CANCEL_STATUS;
        }

        @Override
        public String getFileName() {
          return null;
        }

        @Override
        public InputStream getInputStream() {
          return null;
        }

        @Override
        public OutputStream getOutputStream() {
          return null;
        }

        @Override
        public String getPath() {
          return null;
        }

        @Override
        public URI getURI() {
          try {
            return new URI(new URL(getWebLink()).toString());
          } catch (URISyntaxException e) {
            // Do nothing
          } catch (MalformedURLException e) {
            File f = new File(getWebLink());
            if (f.exists()) {
              return f.toURI();
            }
          }
          try {
            URL context = myManager.getProjectDocument();
            if (context == null) {
              return null;
            }
            URL relative = new URL(context, getWebLink());
            return new URI(URLEncoder.encode(relative.toString(), StandardCharsets.UTF_8));
          } catch (URISyntaxException | MalformedURLException e) {
            // Do nothing
          }
          return null;
        }

        @Override
        public boolean isLocal() {
          return false;
        }

        @Override
        public boolean isValidForMRU() {
          return false;
        }

        @Override
        public void write() {
        }
      });
    }
    return Collections.emptyList();
  }

  @Override
  public boolean isMilestone() {
    return isMilestone && Boolean.TRUE == myManager.isZeroMilestones();
  }

  public boolean isLegacyMilestone() {
    return isMilestone;
  }
  @Override
  public Priority getPriority() {
    return myPriority;
  }

  @Override
  public GanttCalendar getStart() {
    if (myMutator != null && myMutator.getMyIsolationLevel() == TaskMutator.READ_UNCOMMITED) {
      return myMutator.getStart();
    }
    return myStart;
  }

  @Override
  public GanttCalendar getEnd() {
    GanttCalendar result = null;
    if (myMutator != null && myMutator.getMyIsolationLevel() == TaskMutator.READ_UNCOMMITED) {
      result = myMutator.getEnd();
    }
    if (result == null) {
      if (myEnd == null) {
        myEnd = calculateEnd();
      }
      result = myEnd;
    }
    return result;
  }

  @Override
  public GanttCalendar getDisplayEnd() {
    GanttCalendar modelEnd = getEnd();
    if (modelEnd.equals(getStart())) {
      boolean allMilestones = true;
      Task[] deepNestedTasks = getManager().getTaskHierarchy().getDeepNestedTasks(this);
      for (Task t : deepNestedTasks) {
        if (!t.isSupertask() && !t.isMilestone()) {
          allMilestones = false;
          break;
        }
      }
      if (!allMilestones) {
        GPLogger.getLogger(Task.class).warning(String.format(
            "This is probably a bug. Task #%d (%s) has end date=%s equal to start date." +
            "It could be possible if all child tasks were milestones, however they are not. Child tasks: %s",
            getTaskID(), getName(), modelEnd, Arrays.asList(deepNestedTasks)));
      }
      return modelEnd;
    }
    return isMilestone ? modelEnd : modelEnd.getDisplayValue();
  }

  GanttCalendar calculateEnd() {
    GanttCalendar result = getStart().clone();
    Date newEnd = shiftDate(result.getTime(), getDuration());
    result.setTime(newEnd);
    return result;
  }

  @Override
  public GanttCalendar getThird() {
    if (myMutator != null && myMutator.getMyIsolationLevel() == TaskMutator.READ_UNCOMMITED) {
      return myMutator.getThird();
    }
    return myThird;
  }

  @Override
  public int getThirdDateConstraint() {
    return myThirdDateConstraint;
  }

  @Override
  public List<TaskActivity> getActivities() {
    if (isMilestone) {
      return myMilestoneActivity;
    }
    List<TaskActivity> activities = myMutator == null ? null : myMutator.getActivities();
    if (activities == null) {
      activities = myActivities;
    }
    return ImmutableList.copyOf(activities);
  }

  @Override
  public TimeDuration getDuration() {
    if (isMilestone()) {
      return EMPTY_DURATION;
    }
    return (myMutator != null && myMutator.getMyIsolationLevel() == TaskMutator.READ_UNCOMMITED) ? myMutator.getDuration()
        : myLength;
  }

  @Override
  public int getCompletionPercentage() {
    return (myMutator != null && myMutator.getMyIsolationLevel() == TaskMutator.READ_UNCOMMITED) ? myMutator.getCompletionPercentage()
        : myCompletionPercentage;
  }

  @Override
  public boolean getExpand() {
    return bExpand;
  }

  @Override
  public ShapePaint getShape() {
    return myShape;
  }

  @Override
  public Color getColor() {
    Color result = myColor;
    if (result == null) {
      if (isMilestone() || myManager.getTaskHierarchy().hasNestedTasks(this)) {
        result = Color.BLACK;
      } else {
        result = myManager.getConfig().getDefaultColor();
      }
    }
    return result;
  }

  @Override
  public String getNotes() {
    return myNotes;
  }

  @Override
  public ResourceAssignment[] getAssignments() {
    return myAssignments.getAssignments();
  }

  @Override
  public ResourceAssignmentCollection getAssignmentCollection() {
    return myAssignments;
  }

  @Override
  public Task getSupertask() {
    TaskHierarchyItem container = myTaskHierarchyItem.getContainerItem();
    return container == null ? null : container.getTask();
  }

  @Override
  public Task[] getNestedTasks() {
    TaskHierarchyItem[] nestedItems = myTaskHierarchyItem.getNestedItems();
    Task[] result = new Task[nestedItems.length];
    for (int i = 0; i < nestedItems.length; i++) {
      result[i] = nestedItems[i].getTask();
    }
    return result;
  }

  @Override
  public void move(Task targetSupertask) {
    move(targetSupertask, -1);
  }

  @Override
  public void move(Task targetSupertask, int position) {
    TaskImpl supertaskImpl = (TaskImpl) targetSupertask;
    TaskHierarchyItem targetItem = supertaskImpl.myTaskHierarchyItem;
    myTaskHierarchyItem.delete();
    targetItem.addNestedItem(myTaskHierarchyItem, position);
    myManager.onTaskMoved(this);
  }

  @Override
  public boolean isDeleted() { return this.isDeleted; }
  @Override
  public void delete() {
    isDeleted = true;
    getDependencies().clear();
    getAssignmentCollection().clear();
    myTaskHierarchyItem.delete();
  }

  @Override
  public TaskDependencySlice getDependencies() {
    return myDependencySlice;
  }

  @Override
  public TaskDependencySlice getDependenciesAsDependant() {
    return myDependencySliceAsDependant;
  }

  @Override
  public TaskDependencySlice getDependenciesAsDependee() {
    return myDependencySliceAsDependee;
  }

  @Override
  public TaskManager getManager() {
    return myManager;
  }



  @Override
  public void setName(String name) {
    myName = (name == null ? null : name.trim());
  }

  @Override
  public void setWebLink(String webLink) {
    myWebLink = webLink;
  }

  @Override
  public void setMilestone(boolean milestone) {
    isMilestone = milestone;
    if (milestone) {
      setEnd(null);
    }
  }

  @Override
  public void setPriority(Priority priority) {
    myPriority = priority;
  }

  @Override
  public void setStart(GanttCalendar start) {
    Date closestWorkingStart = myManager.findClosestWorkingTime(start.getTime());
    start.setTime(closestWorkingStart);
    myStart = start;
    recalculateActivities();
    adjustNestedTasks();
  }

  void adjustNestedTasks() {
    assert myManager != null;
    try {
      AlgorithmCollection algorithmCollection = myManager.getAlgorithmCollection();
      if (algorithmCollection != null) {
        algorithmCollection.getAdjustTaskBoundsAlgorithm().adjustNestedTasks(this);
      }
    } catch (TaskDependencyException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
    }
  }

  @Override
  public boolean isSupertask() {
    //return myManager.getTaskHierarchy().hasNestedTasks(this);
    return myTaskHierarchyItem.hasNested();
  }

  @Override
  public void setEnd(GanttCalendar end) {
    myEnd = end;
    recalculateActivities();
  }

  @Override
  public void setThirdDate(GanttCalendar third) {
    Date closestWorkingStart = myManager.findClosestWorkingTime(third.getTime());
    third.setTime(closestWorkingStart);
    myThird = third;
  }

  @Override
  public void setThirdDateConstraint(int thirdDateConstraint) {
    myThirdDateConstraint = thirdDateConstraint;
  }
  @Override
  public void setDuration(TimeDuration length) {
    assert length.getLength() >= 0 : "An attempt to set length=" + length + " to task=" + this;

    myLength = length;
    myEnd = null;
    recalculateActivities();
  }

  Date shiftDate(Date input, TimeDuration duration) {
    return myManager.getConfig().getCalendar().shiftDate(input, duration);
  }

  private void recalculateActivities() {
    if (myLength == null || myManager == null) {
      return;
    }
    if (isMilestone) {
      myMilestoneActivity = ImmutableList.of(new MilestoneTaskFakeActivity(this));
      return;
    }

    final Date startDate = myStart.getTime();
    final Date endDate = getEnd().getTime();

    myActivities.clear();
    if (startDate.equals(endDate)) {
      myActivities.add(new MilestoneTaskFakeActivity(this));
      return;
    }

    recalculateActivities(myManager.getConfig().getCalendar(), this, myActivities, startDate, endDate);
    int length = 0;
    for (TaskActivity activity : myActivities) {
      if (activity.getIntensity() > 0) {
        length += activity.getDuration().getLength(getDuration().getTimeUnit());
      }
    }
    myLength = getManager().createLength(myLength.getTimeUnit(), length);
  }

  static void recalculateActivities(GPCalendarCalc calendar, Task task, List<TaskActivity> output, Date startDate,
      Date endDate) {
    TaskActivitiesAlgorithm alg = new TaskActivitiesAlgorithm(calendar);
    alg.recalculateActivities(task, output, startDate, endDate);
  }

  @Override
  public void setCompletionPercentage(int percentage) {
    if (percentage != myCompletionPercentage) {
      myCompletionPercentage = percentage;
    }
  }

  @Override
  public void setShape(ShapePaint shape) {
    myShape = shape;
  }

  @Override
  public void setColor(Color color) {
    myColor = color;
  }

  @Override
  public void setNotes(String notes) {
    myNotes = notes;
  }

  @Override
  public void setExpand(boolean expand) {
    bExpand = expand;
  }

  protected void enableEvents(boolean enabled) {
    myEventsEnabled = enabled;
  }

  protected boolean areEventsEnabled() {
    return myEventsEnabled && myManager.areEventsEnabled();
  }

  /**
   * Determines whether a special shape is defined for this task.
   *
   * @return true, if this task has its own shape defined.
   */
  public boolean shapeDefined() {
    return (myShape != null);
  }

  /**
   * Determines whether a special color is defined for this task.
   *
   * @return true, if this task has its own color defined.
   */
  public boolean colorDefined() {
    return (myColor != null);
  }

  @Override
  public String toString() {
    return getName();
  }

  public boolean isUnplugged() {
    return this.isUnplugged;
  }

  /** @return The CustomColumnValues. */
  @Override
  public CustomColumnsValues getCustomValues() {
    return customValues;
  }

  @Override
  public void setCritical(boolean critical) {
    this.critical = critical;
  }

  @Override
  public boolean isCritical() {
    return this.critical;
  }

  @Override
  public boolean isProjectTask() {
    return isProjectTask;
  }

  @Override
  public void setProjectTask(boolean projectTask) {
    isProjectTask = projectTask;
  }

  private class CostImpl implements Cost {
    private BigDecimal myValue = BigDecimal.ZERO;
    private boolean isCalculated = true;

    @Override
    public BigDecimal getValue() {
      return (isCalculated) ? getCalculatedValue() : getManualValue();
    }

    @Override
    public BigDecimal getManualValue() {
      return myValue;
    }

    @Override
    public BigDecimal getCalculatedValue() {
      return new CostAlgorithmImpl().getCalculatedCost(TaskImpl.this);
    }

//    public void setValue(BigDecimal value) {
//      myValue = value;
//    }

    public void setValue(Cost copy) {
      myValue = copy.getValue();
      isCalculated = copy.isCalculated();
    }

    @Override
    public boolean isCalculated() {
      return isCalculated;
    }

  }

  @Override
  public Cost getCost() {
    return myCost;
  }

  @Override
  public void setCost(Cost cost) {
    myCost.setValue(cost);
  }

  @Override
  public void setCustomProperties(CustomPropertyHolder customProperties) throws CustomColumnsException {
    customValues.importFrom(customProperties);
  }
}
