package net.sourceforge.ganttproject.task

import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.storage.ProjectDatabaseException
import net.sourceforge.ganttproject.task.algorithm.AlgorithmException
import net.sourceforge.ganttproject.task.algorithm.ShiftTaskTreeAlgorithm
import net.sourceforge.ganttproject.util.collect.Pair
import java.awt.Color
import java.util.*

internal open class EventSender(private val taskImpl: TaskImpl, private val notify: (TaskImpl)->Unit) {
  private var enabled = false
  fun enable() {
    enabled = true
  }
  fun fireEvent() {
    if (enabled) {
      enabled = false
      notify(taskImpl)
    }
    enabled = false
  }
}

internal class ProgressEventSender(taskManager: TaskManagerImpl, taskImpl: TaskImpl)
  : EventSender(taskImpl, taskManager::fireTaskProgressChanged)

internal class PropertiesEventSender(taskManager: TaskManagerImpl, taskImpl: TaskImpl)
  : EventSender(taskImpl, taskManager::fireTaskPropertiesChanged)

internal class FieldChange<T>(private val eventSender: EventSender, val oldValue: T?) {
  var myFieldValue: T? = null
  fun setValue(newValue: T?) {
    myFieldValue = newValue
    if (hasChange()) {
      eventSender.enable()
    }
  }

  fun hasChange(): Boolean {
    return oldValue != myFieldValue
  }
}

internal fun createMutatorFixingDuration(myManager: TaskManagerImpl, task: TaskImpl, taskUpdateBuilder: ProjectDatabase.TaskUpdateBuilder?): MutatorImpl {
  return object : MutatorImpl(myManager, task, taskUpdateBuilder) {
    override fun setStart(start: GanttCalendar) {
      super.setStart(start)
      task.myEnd = null
      taskUpdateBuilder?.setStart(start)
    }
  }
}

internal open class MutatorImpl(
  private val myManager: TaskManagerImpl,
  private val taskImpl: TaskImpl,
  protected val taskUpdateBuilder: ProjectDatabase.TaskUpdateBuilder?) : TaskMutator {

  private val myPropertiesEventSender: EventSender = PropertiesEventSender(myManager, taskImpl)
  private val myProgressEventSender: EventSender = ProgressEventSender(myManager, taskImpl)
  private var myCompletionPercentageChange: FieldChange<Int>? = null
  private var myStartChange: FieldChange<GanttCalendar>? = null
  private var myEndChange: FieldChange<GanttCalendar>? = null
  private var myThirdChange: FieldChange<GanttCalendar>? = null
  private var myDurationChange: FieldChange<TimeDuration>? = null
  private var myActivities: List<TaskActivity>? = null
  private var myShiftChange: Pair<FieldChange<GanttCalendar>, FieldChange<GanttCalendar>>? = null
  private val myCommands: MutableList<Runnable> = ArrayList()
  var myIsolationLevel = 0
  override fun commit() {
    try {
      if (myStartChange != null) {
        val start = getStart()
        taskImpl.start = start
      }
      if (myDurationChange != null) {
        val duration = getDuration()
        taskImpl.duration = duration
        myEndChange = null
      }
      if (myCompletionPercentageChange != null && myCompletionPercentageChange!!.hasChange()) {
        val newValue = completionPercentage
        taskImpl.completionPercentage = newValue
      }
      if (myEndChange != null) {
        val end = end
        if (end!!.time > taskImpl.start.time) {
          taskImpl.end = end
        }
      }
      if (myThirdChange != null) {
        val third = third
        taskImpl.setThirdDate(third)
      }
      for (command in myCommands) {
        command.run()
      }
      myCommands.clear()
      myPropertiesEventSender.fireEvent()
      myProgressEventSender.fireEvent()
      if (taskUpdateBuilder != null) {
        try {
          taskUpdateBuilder.commit()
        } catch (e: ProjectDatabaseException) {
          GPLogger.log(e)
        }
      }
    } finally {
      taskImpl.myMutator = null
    }
    if (myStartChange != null && taskImpl.isSupertask) {
      taskImpl.adjustNestedTasks()
    }
    if (myStartChange != null || myEndChange != null || myDurationChange != null || myShiftChange != null || myThirdChange != null && taskImpl.areEventsEnabled()) {
      val oldStart: GanttCalendar? = if (myStartChange != null) {
        myStartChange!!.oldValue
      } else if (myShiftChange != null) {
        myShiftChange!!.first().oldValue
      } else {
        taskImpl.start
      }
      val oldEnd: GanttCalendar? = if (myEndChange != null) {
        myEndChange!!.oldValue
      } else if (myShiftChange != null) {
        myShiftChange!!.second().oldValue
      } else {
        taskImpl.end
      }
      myManager.fireTaskScheduleChanged(taskImpl, oldStart, oldEnd)
    }
  }

  val third: GanttCalendar
    get() = if (myThirdChange == null) taskImpl.myThird else myThirdChange!!.myFieldValue!!
  val activities: List<TaskActivity>?
    get() {
      if (myActivities == null && myStartChange != null || myDurationChange != null) {
        myActivities = ArrayList()
        TaskImpl.recalculateActivities(myManager.config.calendar, taskImpl, myActivities,
          getStart().time, taskImpl.end.time)
      }
      return myActivities
    }

  override fun setName(name: String) {
    myCommands.add(object : Runnable {
      private val myFieldChange = FieldChange(myPropertiesEventSender, taskImpl.name)
      override fun run() {
        myFieldChange.setValue(name)
        taskImpl.name = name
        if (taskUpdateBuilder != null && myFieldChange.hasChange()) {
          taskUpdateBuilder.setName(name)
        }
      }
    })
  }

  override fun setProjectTask(projectTask: Boolean) {
    myCommands.add(Runnable { taskImpl.setProjectTask(projectTask) })
  }

  override fun setMilestone(milestone: Boolean) {
    myCommands.add(Runnable { taskImpl.isMilestone = milestone })
  }

  override fun setPriority(priority: Task.Priority) {
    myCommands.add(Runnable { taskImpl.priority = priority })
  }

  override fun setStart(start: GanttCalendar) {
    val currentStart = getStart()
    if (currentStart != null && start.equals(currentStart)) {
      return
    }
    if (myStartChange == null) {
      myStartChange = FieldChange(myPropertiesEventSender, taskImpl.myStart)
    }
    myStartChange!!.setValue(start)
    myActivities = null
    if (taskUpdateBuilder != null && myStartChange!!.hasChange()) {
      taskUpdateBuilder.setStart(start)
    }
  }

  override fun setEnd(end: GanttCalendar) {
    if (myEndChange == null) {
      myEndChange = FieldChange(myPropertiesEventSender, taskImpl.myEnd)
    }
    myEndChange!!.setValue(end)
    myActivities = null
  }

  override fun setThird(third: GanttCalendar, thirdDateConstraint: Int) {
    myCommands.add(Runnable { taskImpl.thirdDateConstraint = thirdDateConstraint })
    if (myThirdChange == null) {
      myThirdChange = FieldChange(myPropertiesEventSender, taskImpl.myThird)
    }
    myThirdChange!!.setValue(third)
    myActivities = null
  }

  override fun setDuration(length: TimeDuration) {
    // If duration of task was set to 0 or less do not change it
    if (length.length <= 0) {
      return
    }
    if (myDurationChange == null) {
      myDurationChange = FieldChange(myPropertiesEventSender, taskImpl.myLength).also {
        it.setValue(length)
      }
    } else {
      val currentLength = myDurationChange!!.myFieldValue
      if (currentLength!!.length - length.length == 0) {
        return
      }
    }
    myDurationChange!!.setValue(length)
    val shifted: Date = taskImpl.shiftDate(getStart().time, length)
    val newEnd = CalendarFactory.createGanttCalendar(shifted)
    setEnd(newEnd)
    myActivities = null
  }

  override fun setExpand(expand: Boolean) {
    myCommands.add(Runnable { taskImpl.expand = expand })
  }

  override fun setCompletionPercentage(percentage: Int) {
    if (percentage != completionPercentage) {
      if (myCompletionPercentageChange == null) {
        myCompletionPercentageChange = FieldChange(myProgressEventSender, taskImpl.myCompletionPercentage)
      }
      myCompletionPercentageChange!!.setValue(percentage)
    }
  }

  override fun setCritical(critical: Boolean) {
    myCommands.add(Runnable { taskImpl.isCritical = critical })
  }

  override fun setShape(shape: ShapePaint) {
    myCommands.add(Runnable { taskImpl.shape = shape })
  }

  override fun setColor(color: Color) {
    myCommands.add(Runnable { taskImpl.color = color })
  }

  override fun setWebLink(webLink: String) {
    myCommands.add(Runnable { taskImpl.webLink = webLink })
  }

  override fun setNotes(notes: String) {
    myCommands.add(Runnable { taskImpl.notes = notes })
  }

  override fun addNotes(notes: String) {
    myCommands.add(Runnable { taskImpl.addNotes(notes) })
  }

  override fun getCompletionPercentage(): Int {
    return if (myCompletionPercentageChange == null) taskImpl.myCompletionPercentage else myCompletionPercentageChange!!.myFieldValue!!.toInt()
  }

  fun getStart(): GanttCalendar {
    return if (myStartChange == null) taskImpl.myStart else myStartChange!!.myFieldValue!!
  }

  val end: GanttCalendar?
    get() = if (myEndChange == null) null else myEndChange!!.myFieldValue

  fun getDuration(): TimeDuration {
    return if (myDurationChange == null) taskImpl.myLength else myDurationChange!!.myFieldValue!!
  }

  override fun shift(unitCount: Float) {
    val result = taskImpl.shift(unitCount)
    setStart(result.start)
    setDuration(result.duration)
    setEnd(result.end)
  }

  override fun shift(shift: TimeDuration) {
    if (myShiftChange == null) {
      myShiftChange = Pair.create(
        FieldChange(EventSender(taskImpl) {}, taskImpl.myStart),
        FieldChange(EventSender(taskImpl) {}, taskImpl.myEnd))
    }
    val shiftAlgorithm = ShiftTaskTreeAlgorithm(myManager, null)
    try {
      shiftAlgorithm.run(taskImpl, shift, ShiftTaskTreeAlgorithm.DEEP)
    } catch (e: AlgorithmException) {
      GPLogger.log(e)
    }
  }

  override fun setIsolationLevel(level: Int) {
    myIsolationLevel = level
  }

  override fun setTaskInfo(taskInfo: TaskInfo) {
    taskImpl.myTaskInfo = taskInfo
  }
}
