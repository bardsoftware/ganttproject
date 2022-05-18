package biz.ganttproject.ganttview

import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.option.ValidationException
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import biz.ganttproject.customproperty.CustomPropertyDefinition
import com.google.common.base.Joiner
import com.google.common.base.Predicate
import com.google.common.base.Supplier
import com.google.common.collect.Lists
import biz.ganttproject.customproperty.CustomPropertyManager
import net.sourceforge.ganttproject.GPLogger
import biz.ganttproject.customproperty.CustomColumnsException
import net.sourceforge.ganttproject.task.CostStub
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskProperties
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException
import java.math.BigDecimal
import java.text.MessageFormat
import java.util.*
import javax.swing.SwingUtilities

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskTableModel(private val taskManager: TaskManager, private val customColumnsManager: CustomPropertyManager) {
  fun getValueAt(t: Task, defaultColumn: TaskDefaultColumn): Any? {
    var res: Any? = null
    when (defaultColumn) {
      TaskDefaultColumn.PRIORITY -> {
        res = t.priority
      }
      TaskDefaultColumn.INFO -> {
        res = t.getProgressStatus()
      }
      TaskDefaultColumn.NAME -> res = t.name
      TaskDefaultColumn.BEGIN_DATE -> res = t.start
      TaskDefaultColumn.END_DATE -> res = t.displayEnd
      TaskDefaultColumn.DURATION -> res = t.duration
      TaskDefaultColumn.COMPLETION -> res = t.completionPercentage
      TaskDefaultColumn.COORDINATOR -> {
        val tAssign = t.assignments
        val sb = StringBuffer()
        var nb = 0
        var i = 0
        while (i < tAssign.size) {
          val resAss = tAssign[i]
          if (resAss.isCoordinator) {
            sb.append(if (nb++ == 0) "" else ", ").append(resAss.resource.name)
          }
          i++
        }
        res = sb.toString()
      }
      TaskDefaultColumn.PREDECESSORS -> res = TaskProperties.formatPredecessors(t, ",", true)
      TaskDefaultColumn.ID -> res = t.taskID
      TaskDefaultColumn.OUTLINE_NUMBER -> {
        val outlinePath = t.manager.taskHierarchy.getOutlinePath(t)
        res = Joiner.on('.').join(outlinePath)
      }
      TaskDefaultColumn.COST -> res = t.cost.value
      TaskDefaultColumn.COLOR -> res = t.color
      TaskDefaultColumn.RESOURCES -> {
        val resources = Lists.transform(Arrays.asList(*t.assignments)) { ra ->
          ra?.resource?.name ?: ""
        }
        res = Joiner.on(',').join(resources)
      }
      else -> {
      }
    }
    // if(tn.getParent()!=null){
    return res
  }

  fun getValue(t: Task, customProperty: CustomPropertyDefinition): Any? {
    return t.customValues.getValue(customProperty)
  }

  fun setValue(value: Any, task: Task, property: TaskDefaultColumn) {
    when (property) {
      TaskDefaultColumn.NAME -> task.createMutator().also {
        it.setName(value.toString())
        it.commit()
      }
      TaskDefaultColumn.BEGIN_DATE -> {
        val startDate = value as GanttCalendar
        val earliestStart = if (task.thirdDateConstraint == 1) task.third else null

        SwingUtilities.invokeLater {
          task.createMutatorFixingDuration().let {
            it.setStart(minOf(startDate, earliestStart ?: startDate))
            it.commit()
          }
        }
      }
      TaskDefaultColumn.END_DATE -> {
        SwingUtilities.invokeLater {
          task.createMutatorFixingDuration().let {
            it.setEnd(CalendarFactory.createGanttCalendar(
              GPTimeUnitStack.DAY.adjustRight((value as GanttCalendar).time)
            ))
            it.commit()
          }
        }
      }
      TaskDefaultColumn.DURATION -> {
        val tl = task.duration
        SwingUtilities.invokeLater {
          task.createMutator().let {
            it.setDuration(task.manager.createLength(tl.timeUnit, (value as Number).toInt().toFloat()))
            it.commit()
          }
        }
      }
      TaskDefaultColumn.COMPLETION -> task.createMutator().let {
        it.completionPercentage = (value as Number).toInt()
        it.commit()
      }
      TaskDefaultColumn.PREDECESSORS -> {
        //List<Integer> newIds = Lists.newArrayList();
        val specs: MutableList<String> = Lists.newArrayList()
        for (s in value.toString().split(",".toRegex()).toTypedArray()) {
          if (!s.trim { it <= ' ' }.isEmpty()) {
            specs.add(s.trim { it <= ' ' })
          }
        }
        val promises: Map<Int, Supplier<TaskDependency>>
        try {
          promises = TaskProperties.parseDependencies(
            specs, task
          ) { id -> task.manager.getTask(id!!) }
          SwingUtilities.invokeLater {
            try {
              val taskManager = task.manager
              taskManager.algorithmCollection.scheduler.setEnabled(false)
              task.dependenciesAsDependant.clear()
              for (promise in promises.values) {
                promise.get()
              }
              taskManager.algorithmCollection.scheduler.setEnabled(true)
            } catch (e: TaskDependencyException) {
              // TODO: propagate this exception to the UI
              throw ValidationException(e)
            }
          }
        } catch (e: IllegalArgumentException) {
          throw ValidationException(e)
        }
      }
      TaskDefaultColumn.COST -> try {
        val cost = BigDecimal(value.toString())
        task.createMutator().also {
          it.setCost(CostStub(cost, false))
          it.commit()
        }
      } catch (e: NumberFormatException) {
        throw ValidationException(MessageFormat.format("Can't parse {0} as number", value))
      }
      else -> {
      }
    }
  }

  fun setValue(value: Any, task: Task, column: CustomPropertyDefinition) {
    try {
      task.customValues.setValue(column, value)
    } catch (e: CustomColumnsException) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err)
      }
    }
  }

  private fun getCustomProperty(columnIndex: Int): CustomPropertyDefinition? {
    var columnIndex = columnIndex
    assert(columnIndex >= STANDARD_COLUMN_COUNT) {
      ("We have " + STANDARD_COLUMN_COUNT + " default properties, and custom property index starts at " + STANDARD_COLUMN_COUNT + ". I've got index #"
          + columnIndex + ". Something must be wrong here")
    }
    val definitions: List<CustomPropertyDefinition> = customColumnsManager.getDefinitions()
    columnIndex -= STANDARD_COLUMN_COUNT
    return if (columnIndex < definitions.size) definitions[columnIndex] else null
  }

}

fun Task.getProgressStatus(): Task.ProgressStatus =
  if (completionPercentage < 100) {
    val c = GanttCalendar.getInstance()
    if (end.before(c)) {
      Task.ProgressStatus.DEADLINE_MISS
    } else {
      if (start.before(c)) {
        Task.ProgressStatus.INPROGRESS
      } else null
    }
  } else { null } ?: Task.ProgressStatus.NOT_YET

private val STANDARD_COLUMN_COUNT = TaskDefaultColumn.values().size

val NOT_SUPERTASK: Predicate<Task> = Predicate<Task> { task ->
  task?.isSupertask?.not() ?: false
}

val NOT_MILESTONE: Predicate<Task> = Predicate<Task> { task ->
  task?.isMilestone?.not() ?: false
}

//fun getEnumValues(enumClass: KClass<out Enum<*>>): Array<out Enum<*>> = enumClass.java.enumConstants
