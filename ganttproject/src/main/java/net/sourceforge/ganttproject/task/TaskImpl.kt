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

internal interface EventSender {
  fun enable()
  fun fireEvent()
}


internal class ProgressEventSender(private val taskManager: TaskManagerImpl, private val taskImpl: TaskImpl) : EventSender {
  private var myEnabled = false
  override fun fireEvent() {
    if (myEnabled) {
      myEnabled = false
      taskManager.fireTaskProgressChanged(taskImpl)
    }
    myEnabled = false
  }

  override fun enable() {
    myEnabled = true
  }
}

internal class PropertiesEventSender(private val taskManager: TaskManagerImpl, private val taskImpl: TaskImpl) : EventSender {
  private var myEnabled = false
  override fun fireEvent() {
    if (myEnabled) {
      taskManager.fireTaskPropertiesChanged(taskImpl)
    }
    myEnabled = false
  }

  override fun enable() {
    myEnabled = true
  }
}

internal class FieldChange {
  var myFieldValue: Any? = null
  public var myOldValue: Any? = null
  var myEventSender: EventSender? = null
  fun setValue(newValue: Any?) {
    myFieldValue = newValue
    if (hasChange()) {
      myEventSender!!.enable()
    }
  }

  fun setOldValue(oldValue: Any?) {
    myOldValue = oldValue
  }

  fun hasChange(): Boolean {
    return myOldValue != myFieldValue
  }
}

internal fun createMutatorFixingDuration(myManager: TaskManagerImpl, task: TaskImpl, taskUpdateBuilder: ProjectDatabase.TaskUpdateBuilder?): MutatorImpl {
  return object : MutatorImpl(myManager, task, taskUpdateBuilder) {
    override fun setStart(start: GanttCalendar) {
      super.setStart(start);
      task.myEnd = null
      taskUpdateBuilder?.setStart(start)
    }
  }
}

open internal class MutatorImpl(
  private val myManager: TaskManagerImpl,
  private val taskImpl: TaskImpl,
  protected val taskUpdateBuilder: ProjectDatabase.TaskUpdateBuilder?) : TaskMutator {

  private val myPropertiesEventSender: EventSender = PropertiesEventSender(myManager, taskImpl)
  private val myProgressEventSender: EventSender = ProgressEventSender(myManager, taskImpl)
  private var myCompletionPercentageChange: FieldChange? = null
  private var myStartChange: FieldChange? = null
  private var myEndChange: FieldChange? = null
  private var myThirdChange: FieldChange? = null
  private var myDurationChange: FieldChange? = null
  private var myActivities: List<TaskActivity>? = null
  private var myShiftChange: Pair<FieldChange, FieldChange>? = null
  private val myCommands: MutableList<Runnable> = ArrayList()
  var myIsolationLevel = 0
  val myException = Exception()
  override fun commit() {
    try {
      if (myStartChange != null) {
        val start = getStart()
        taskImpl.setStart(start)
      }
      if (myDurationChange != null) {
        val duration = getDuration()
        taskImpl.setDuration(duration)
        myEndChange = null
      }
      if (myCompletionPercentageChange != null && myCompletionPercentageChange!!.hasChange()) {
        val newValue = completionPercentage
        taskImpl.setCompletionPercentage(newValue)
      }
      if (myEndChange != null) {
        val end = end
        if (end!!.time.compareTo(taskImpl.getStart().getTime()) > 0) {
          taskImpl.setEnd(end)
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
    if (myStartChange != null && taskImpl.isSupertask()) {
      taskImpl.adjustNestedTasks()
    }
    if (myStartChange != null || myEndChange != null || myDurationChange != null || myShiftChange != null || myThirdChange != null && taskImpl.areEventsEnabled()) {
      val oldStart: GanttCalendar?
      oldStart = if (myStartChange != null) {
        myStartChange!!.myOldValue as GanttCalendar?
      } else if (myShiftChange != null) {
        myShiftChange!!.first().myOldValue as GanttCalendar?
      } else {
        taskImpl.getStart()
      }
      val oldEnd: GanttCalendar?
      oldEnd = if (myEndChange != null) {
        myEndChange!!.myOldValue as GanttCalendar?
      } else if (myShiftChange != null) {
        myShiftChange!!.second().myOldValue as GanttCalendar?
      } else {
        taskImpl.getEnd()
      }
      myManager.fireTaskScheduleChanged(taskImpl, oldStart, oldEnd)
    }
  }

  val third: GanttCalendar
    get() = if (myThirdChange == null) taskImpl.myThird else (myThirdChange!!.myFieldValue as GanttCalendar?)!!
  val activities: List<TaskActivity>?
    get() {
      if (myActivities == null && myStartChange != null || myDurationChange != null) {
        myActivities = ArrayList()
        TaskImpl.recalculateActivities(myManager.getConfig().getCalendar(), taskImpl, myActivities,
          getStart().time, taskImpl.getEnd().getTime())
      }
      return myActivities
    }

  override fun setName(name: String) {
    myCommands.add(object : Runnable {
      private val myFieldChange = FieldChange()
      override fun run() {
        myFieldChange.myEventSender = myPropertiesEventSender
        myFieldChange.setOldValue(taskImpl.getName())
        myFieldChange.setValue(name)
        taskImpl.setName(name)
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
    myCommands.add(Runnable { taskImpl.setMilestone(milestone) })
  }

  override fun setPriority(priority: Task.Priority) {
    myCommands.add(Runnable { taskImpl.setPriority(priority) })
  }

  override fun setStart(start: GanttCalendar) {
    assert(start != null)
    val currentStart = getStart()
    if (currentStart != null && start.equals(currentStart)) {
      return
    }
    if (myStartChange == null) {
      myStartChange = FieldChange()
      myStartChange!!.myEventSender = myPropertiesEventSender
    }
    myStartChange!!.setOldValue(taskImpl.myStart)
    myStartChange!!.setValue(start)
    myActivities = null
    if (taskUpdateBuilder != null && myStartChange!!.hasChange()) {
      taskUpdateBuilder.setStart(start)
    }
  }

  override fun setEnd(end: GanttCalendar) {
    if (myEndChange == null) {
      myEndChange = FieldChange()
      myEndChange!!.myEventSender = myPropertiesEventSender
    }
    myEndChange!!.setOldValue(taskImpl.myEnd)
    myEndChange!!.setValue(end)
    myActivities = null
  }

  override fun setThird(third: GanttCalendar, thirdDateConstraint: Int) {
    myCommands.add(Runnable { taskImpl.setThirdDateConstraint(thirdDateConstraint) })
    if (myThirdChange == null) {
      myThirdChange = FieldChange()
      myThirdChange!!.myEventSender = myPropertiesEventSender
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
      myDurationChange = FieldChange()
      myDurationChange!!.myEventSender = myPropertiesEventSender
      myDurationChange!!.setValue(length)
    } else {
      val currentLength = myDurationChange!!.myFieldValue as TimeDuration?
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
    myCommands.add(Runnable { taskImpl.setExpand(expand) })
  }

  override fun setCompletionPercentage(percentage: Int) {
    if (percentage != completionPercentage) {
      if (myCompletionPercentageChange == null) {
        myCompletionPercentageChange = FieldChange()
        myCompletionPercentageChange!!.myEventSender = myProgressEventSender
      }
      myCompletionPercentageChange!!.setOldValue(taskImpl.myCompletionPercentage)
      myCompletionPercentageChange!!.setValue(percentage)
    }
  }

  override fun setCritical(critical: Boolean) {
    myCommands.add(Runnable { taskImpl.setCritical(critical) })
  }

  override fun setShape(shape: ShapePaint) {
    myCommands.add(Runnable { taskImpl.setShape(shape) })
  }

  override fun setColor(color: Color) {
    myCommands.add(Runnable { taskImpl.setColor(color) })
  }

  override fun setWebLink(webLink: String) {
    myCommands.add(Runnable { taskImpl.setWebLink(webLink) })
  }

  override fun setNotes(notes: String) {
    myCommands.add(Runnable { taskImpl.setNotes(notes) })
  }

  override fun addNotes(notes: String) {
    myCommands.add(Runnable { taskImpl.addNotes(notes) })
  }

  override fun getCompletionPercentage(): Int {
    return if (myCompletionPercentageChange == null) taskImpl.myCompletionPercentage else (myCompletionPercentageChange!!.myFieldValue as Int?)!!.toInt()
  }

  fun getStart(): GanttCalendar {
    return if (myStartChange == null) taskImpl.myStart else (myStartChange!!.myFieldValue as GanttCalendar?)!!
  }

  val end: GanttCalendar?
    get() = if (myEndChange == null) null else myEndChange!!.myFieldValue as GanttCalendar?

  fun getDuration(): TimeDuration {
    return if (myDurationChange == null) taskImpl.myLength else (myDurationChange!!.myFieldValue as TimeDuration?)!!
  }

  override fun shift(unitCount: Float) {
    val result = getPrecomputedShift(unitCount) ?: taskImpl.shift(unitCount).also {
      cachePrecomputedShift(it, unitCount)
    }
    setStart(result.start)
    setDuration(result.duration)
    setEnd(result.end)
  }

  override fun shift(shift: TimeDuration) {
    if (myShiftChange == null) {
      myShiftChange = Pair.create(FieldChange(), FieldChange()).also {
        it.first().setOldValue(taskImpl.myStart)
        it.second().setOldValue(taskImpl.myEnd)
      }
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

  private fun cachePrecomputedShift(result: Task?, unitCount: Float) {
    // TODO Implement cache
  }

  private fun getPrecomputedShift(unitCount: Float): Task? {
    // TODO Use cache to grab value
    return null
  }

  override fun setTaskInfo(taskInfo: TaskInfo) {
    taskImpl.myTaskInfo = taskInfo
  }
}
