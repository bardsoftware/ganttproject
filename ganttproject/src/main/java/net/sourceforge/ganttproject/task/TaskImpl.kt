/*
 * Copyright (C) 2022 Dmitry Barashev, BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.task

import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.customproperty.CustomPropertyHolder
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.storage.ProjectDatabaseException
import net.sourceforge.ganttproject.task.algorithm.ShiftTaskTreeAlgorithm
import java.awt.Color
import java.math.BigDecimal

class CostStub(private val value: BigDecimal, private val isCalculated: Boolean): Task.Cost {
  override fun getValue() = value

  override fun getManualValue() = value

  override fun getCalculatedValue() = value

  override fun setValue(copy: Task.Cost) {
    TODO("Not yet implemented")
  }

  override fun isCalculated() = this.isCalculated
}

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

internal abstract class MutatorBase(internal val taskImpl: TaskImpl): TaskMutator {

  var myIsolationLevel = 0
  abstract fun reentrance(): MutatorBase
  override fun setIsolationLevel(level: Int) {
    myIsolationLevel = level
  }

  abstract fun getStart(): GanttCalendar
  abstract fun getEnd(): GanttCalendar?
  abstract fun getThird(): GanttCalendar?
  abstract fun getDuration(): TimeDuration
  abstract fun getActivities(): List<TaskActivity>?
}

internal class MutatorReentered(taskImpl: TaskImpl, private val delegate: MutatorBase): MutatorBase(taskImpl), TaskMutator by delegate {
  override fun reentrance(): MutatorBase = this
  override fun getStart() = delegate.getStart()

  override fun getEnd() = delegate.getEnd()

  override fun getThird() = delegate.getThird()

  override fun getDuration() = delegate.getDuration()

  override fun getActivities() = delegate.getActivities()

  override fun commit() {
    // do nothing
  }

  override fun setIsolationLevel(level: Int) {
    delegate.setIsolationLevel(level)
  }
}

internal open class MutatorImpl(
  private val myManager: TaskManagerImpl,
  taskImpl: TaskImpl,
  private val taskUpdateBuilder: ProjectDatabase.TaskUpdateBuilder?) : MutatorBase(taskImpl) {

  private val myPropertiesEventSender: EventSender = PropertiesEventSender(myManager, taskImpl)
  private val myProgressEventSender: EventSender = ProgressEventSender(myManager, taskImpl)

  private val colorChange: FieldChange<Color?> = FieldChange(myPropertiesEventSender, taskImpl.color)
  private val myCompletionPercentageChange = FieldChange(myProgressEventSender, taskImpl.completionPercentage)
  private val costChange = FieldChange(myPropertiesEventSender, taskImpl.cost)
  private val criticalFlagChange = FieldChange(myPropertiesEventSender, taskImpl.isCritical)
  private val customPropertiesChange = FieldChange<CustomPropertyHolder>(myPropertiesEventSender, taskImpl.customValues.copyOf())
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
  private var isCommitted = false


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
        taskUpdateBuilder?.setMilestone(milestoneChange.oldValue, it)
      }
      if (hasDateFieldsChange) {
        hasActualDatesChange = taskImpl.start != myStartChange.oldValue || taskImpl.duration != myDurationChange.oldValue
        if (hasActualDatesChange && taskUpdateBuilder != null) {
          if (taskImpl.start != myStartChange.oldValue) {
            taskUpdateBuilder.setStart(myStartChange.oldValue, taskImpl.start)
          }
          if (taskImpl.duration != myDurationChange.oldValue) {
            taskUpdateBuilder.setDuration(myDurationChange.oldValue, taskImpl.duration)
          }
        }
      }

      colorChange.ifChanged {
        taskImpl.color = it
        taskUpdateBuilder?.setColor(colorChange.oldValue, it)
      }
      myCompletionPercentageChange.ifChanged {completion ->
        taskImpl.completionPercentage = completion
        taskUpdateBuilder?.setCompletionPercentage(myCompletionPercentageChange.oldValue, completion)
      }
      costChange.ifChanged { cost ->
        taskImpl.cost.setValue(cost)
        taskUpdateBuilder?.setCost(costChange.oldValue, cost)
      }
      criticalFlagChange.ifChanged {
        taskImpl.isCritical = it
        taskUpdateBuilder?.setCritical(criticalFlagChange.oldValue, it)
      }
      customPropertiesChange.ifChanged {
        taskImpl.customValues.importFrom(it)
        taskUpdateBuilder?.setCustomProperties(customPropertiesChange.oldValue, it)
      }
      expansionChange.ifChanged {
        taskImpl.expand = it
        // no call to the taskUpdateBuilder because we do not keep the expansion state in the DB
      }
      myNameChange.ifChanged {
        taskImpl.name = it
        taskUpdateBuilder?.setName(myNameChange.oldValue, it)
      }
      notesChange.ifChanged {
        taskImpl.notes = it
        taskUpdateBuilder?.setNotes(notesChange.oldValue, it)
      }
      priorityChange.ifChanged {
        taskImpl.priority = it
        taskUpdateBuilder?.setPriority(priorityChange.oldValue, it)
      }
      projectTaskChange.ifChanged {
        taskImpl.isProjectTask = it
        taskUpdateBuilder?.setProjectTask(projectTaskChange.oldValue, it)
      }
      shapeChange.ifChanged {
        taskImpl.shape = it
        taskUpdateBuilder?.setShape(shapeChange.oldValue, it)
      }
      webLinkChange.ifChanged {
        taskImpl.webLink = it
        taskUpdateBuilder?.setWebLink(webLinkChange.oldValue, it)
      }

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

  override fun getThird(): GanttCalendar? = myThirdChange.newValueOrElse { taskImpl.myThird }

  override fun getActivities(): List<TaskActivity>? {
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

  override fun setCost(cost: Task.Cost) { costChange.setValue(cost) }

  override fun setShape(shape: ShapePaint) { shapeChange.setValue(shape) }

  override fun setColor(color: Color) { colorChange.setValue(color) }

  override fun setCustomProperties(customProperties: CustomPropertyHolder) {
    customPropertiesChange.setValue(customProperties)
  }

  override fun setWebLink(webLink: String) { webLinkChange.setValue(webLink) }

  override fun setNotes(notes: String) { notesChange.setValue(notes) }

  override fun getCompletionPercentage() = myCompletionPercentageChange.newValueOrElse { taskImpl.myCompletionPercentage }

  override fun getStart(): GanttCalendar = myStartChange.newValueOrElse { taskImpl.myStart }

  override fun getEnd(): GanttCalendar? = if (myEndChange.hasChange()) myEndChange.myFieldValue else null

  override fun getDuration(): TimeDuration = myDurationChange.newValueOrElse { taskImpl.myLength }

  override fun reentrance(): MutatorBase = MutatorReentered(this.taskImpl, this)
}

internal class ShiftMutatorImpl(taskImpl: TaskImpl): ShiftMutator {
  override val task: Task = taskImpl
  private val shiftAlgorithm = ShiftTaskTreeAlgorithm(taskImpl.myManager, listOf(taskImpl), true)

  override fun shift(interval: TimeDuration) {
    shiftAlgorithm.run(interval)
  }

  override fun commit() {
    shiftAlgorithm.commit()
  }
}