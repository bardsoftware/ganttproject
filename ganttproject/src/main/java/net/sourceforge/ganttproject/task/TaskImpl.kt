package net.sourceforge.ganttproject.task

import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.storage.ProjectDatabaseException
import net.sourceforge.ganttproject.util.collect.Pair
import java.awt.Color

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

internal class FieldChange<T>(private val eventSender: EventSender, val oldValue: T) {
  var myFieldValue: T? = null
  fun setValue(newValue: T): Boolean {
    if (newValue == myFieldValue) {
      return false
    }
    myFieldValue = newValue
    return if (hasChange()) {
      eventSender.enable()
      true
    } else {
      false
    }
  }

  fun hasChange(): Boolean {
    return myFieldValue != null && oldValue != myFieldValue
  }

  fun newValueOrElse(supplier: ()->T): T =
    if (hasChange()) {
      myFieldValue!!
    } else {
      supplier()
    }

  fun clear() {
    myFieldValue = null
  }

}

private fun <T> (FieldChange<T>?).ifChanged(consumer: (T)->Unit) {
  if (this != null && this.hasChange()) {
    this.myFieldValue?.let { consumer(it) }
  }
}

internal fun createMutatorFixingDuration(myManager: TaskManagerImpl, task: TaskImpl, taskUpdateBuilder: ProjectDatabase.TaskUpdateBuilder?): MutatorImpl {
  return object : MutatorImpl(myManager, task, taskUpdateBuilder) {
    override fun setStart(start: GanttCalendar) {
      super.setStart(start)
      task.myEnd = null
    }


  }
}

internal open class MutatorImpl(
  private val myManager: TaskManagerImpl,
  private val taskImpl: TaskImpl,
  private val taskUpdateBuilder: ProjectDatabase.TaskUpdateBuilder?) : TaskMutator {

  private val myPropertiesEventSender: EventSender = PropertiesEventSender(myManager, taskImpl)
  private val myProgressEventSender: EventSender = ProgressEventSender(myManager, taskImpl)

  private val colorChange: FieldChange<Color?> = FieldChange(myPropertiesEventSender, taskImpl.color)
  private val myCompletionPercentageChange = FieldChange(myProgressEventSender, taskImpl.completionPercentage)
  private val criticalFlagChange = FieldChange(myPropertiesEventSender, taskImpl.isCritical)
  private val expansionChange = FieldChange(myPropertiesEventSender, taskImpl.expand)
  private val milestoneChange = FieldChange(myPropertiesEventSender, taskImpl.isMilestone)
  private val myNameChange = FieldChange(myPropertiesEventSender, taskImpl.name)
  private val notesChange = FieldChange<String?>(myPropertiesEventSender, taskImpl.notes)
  private val priorityChange = FieldChange(myPropertiesEventSender, taskImpl.priority)
  private val projectTaskChange = FieldChange(myPropertiesEventSender, taskImpl.isProjectTask)
  private val shapeChange = FieldChange<ShapePaint?>(myPropertiesEventSender, taskImpl.shape)
  private val webLinkChange = FieldChange<String?>(myPropertiesEventSender, taskImpl.webLink)

  private val myStartChange = FieldChange(myPropertiesEventSender, taskImpl.start)
  private val myEndChange: FieldChange<GanttCalendar?> = FieldChange(myPropertiesEventSender, taskImpl.myEnd)
  private val myThirdChange: FieldChange<GanttCalendar?> = FieldChange(myPropertiesEventSender, taskImpl.myThird)
  private val myDurationChange = FieldChange(myPropertiesEventSender, taskImpl.duration)

  private val hasDateFieldsChange: Boolean get() = myStartChange.hasChange() || myDurationChange.hasChange() || myEndChange.hasChange() || myThirdChange.hasChange() || milestoneChange.hasChange()
  //private var myActivities: List<TaskActivity>? = null
  private val myCommands: MutableList<Runnable> = ArrayList()
  private var isCommitted = false

  var myIsolationLevel = 0
  override fun commit() {
    if (isCommitted) {
      throw IllegalStateException("Mutator for task ${taskImpl.taskID} is commiting twice")
    }
    var hasActualDatesChange = false
    try {
      myStartChange.ifChanged {
        taskImpl.start = it
      }
      myDurationChange.ifChanged {
        taskImpl.duration = it
        myEndChange.clear()
      }
      myEndChange.ifChanged {
        if (it!!.time > taskImpl.start.time) {
          taskImpl.end = it
        }
      }
      myThirdChange.ifChanged {
        taskImpl.setThirdDate(it)
      }
      milestoneChange.ifChanged {
        taskImpl.isMilestone = it
        taskUpdateBuilder?.setMilestone(it)
      }
      if (hasDateFieldsChange) {
        hasActualDatesChange = taskImpl.start != myStartChange.oldValue || taskImpl.duration != myDurationChange.oldValue
        if (hasActualDatesChange && taskUpdateBuilder != null) {
          if (taskImpl.start != myStartChange.oldValue) {
            taskUpdateBuilder.setStart(taskImpl.start)
          }
          if (taskImpl.duration != myDurationChange.oldValue) {
            taskUpdateBuilder.setDuration(taskImpl.duration)
          }
        }
      }

      colorChange.ifChanged {
        taskImpl.color = it
        taskUpdateBuilder?.setColor(it)
      }
      myCompletionPercentageChange.ifChanged {completion ->
        taskImpl.completionPercentage = completion
        taskUpdateBuilder?.setCompletionPercentage(completion)
      }
      criticalFlagChange.ifChanged {
        taskImpl.isCritical = it
        taskUpdateBuilder?.setCritical(it)
      }
      expansionChange.ifChanged {
        taskImpl.expand = it
        // no call to the taskUpdateBuilder because we do not keep the expansion state in the DB
      }
      myNameChange.ifChanged {
        taskImpl.name = it
        taskUpdateBuilder?.setName(it)
      }
      notesChange.ifChanged {
        taskImpl.notes = it
        taskUpdateBuilder?.setNotes(it)
      }
      priorityChange.ifChanged {
        taskImpl.priority = it
        taskUpdateBuilder?.setPriority(it)
      }
      projectTaskChange.ifChanged {
        taskImpl.isProjectTask = it
        taskUpdateBuilder?.setProjectTask(it)
      }
      shapeChange.ifChanged {
        taskImpl.shape = it
        taskUpdateBuilder?.setShape(it)
      }
      webLinkChange.ifChanged {
        taskImpl.webLink = it
        taskUpdateBuilder?.setWebLink(it)
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
      isCommitted = true
    }
    if (taskImpl.isSupertask && hasActualDatesChange) {
      taskImpl.adjustNestedTasks()
    }
    if (hasActualDatesChange && taskImpl.areEventsEnabled()) {
      val oldStart: GanttCalendar = myStartChange.oldValue
      val oldEnd: GanttCalendar = myEndChange.oldValue!!
      myManager.fireTaskScheduleChanged(taskImpl, oldStart, oldEnd)
    }
  }

  val third: GanttCalendar? get() = myThirdChange.newValueOrElse { taskImpl.myThird }

  val activities: List<TaskActivity>?
    get() {
      return if (myStartChange.hasChange() || myDurationChange.hasChange()) {
        mutableListOf<TaskActivity>().also {
          TaskImpl.recalculateActivities(myManager.config.calendar, taskImpl, it,
            getStart().time, taskImpl.end.time)
        }
      } else null
    }

  override fun setName(name: String) { myNameChange.setValue(name) }

  override fun setProjectTask(projectTask: Boolean) { projectTaskChange.setValue(projectTask) }

  override fun setMilestone(milestone: Boolean) { milestoneChange.setValue(milestone) }

  override fun setPriority(priority: Task.Priority) { priorityChange.setValue(priority) }

  override fun setStart(start: GanttCalendar) { myStartChange.setValue(start) }

  override fun setEnd(end: GanttCalendar) { myEndChange.setValue(end) }

  override fun setThird(third: GanttCalendar, thirdDateConstraint: Int) { myThirdChange.setValue(third) }

  override fun setDuration(length: TimeDuration) {
    // If duration of task was set to 0 or less do not change it
    if (length.length <= 0) {
      return
    }
    if (myDurationChange.setValue(length)) {
      val newEnd = CalendarFactory.createGanttCalendar(taskImpl.shiftDate(getStart().time, length))
      setEnd(newEnd)
    }
  }

  override fun setExpand(expand: Boolean) { expansionChange.setValue(expand) }

  override fun setCompletionPercentage(percentage: Int) { myCompletionPercentageChange.setValue(percentage) }

  override fun setCritical(critical: Boolean) { criticalFlagChange.setValue(critical) }

  override fun setShape(shape: ShapePaint) { shapeChange.setValue(shape) }

  override fun setColor(color: Color) { colorChange.setValue(color) }

  override fun setWebLink(webLink: String) { webLinkChange.setValue(webLink) }

  override fun setNotes(notes: String) { notesChange.setValue(notes) }

  override fun getCompletionPercentage() = myCompletionPercentageChange.newValueOrElse { taskImpl.myCompletionPercentage }

  fun getStart() = myStartChange.newValueOrElse { taskImpl.myStart }

  val end: GanttCalendar? = if (myEndChange.hasChange()) myEndChange.myFieldValue else null

  fun getDuration() = myDurationChange.newValueOrElse { taskImpl.myLength }

  override fun shift(shift: TimeDuration) {
    taskImpl.shift(shift)
  }

  override fun setIsolationLevel(level: Int) {
    myIsolationLevel = level
  }
}
