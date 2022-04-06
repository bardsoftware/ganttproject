package net.sourceforge.ganttproject.parser

import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.io.XmlTasks.XmlTask
import biz.ganttproject.core.model.task.ConstraintType
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.lib.fx.TreeCollapseView
import com.google.common.base.Charsets
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.task.CustomColumnsException
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskManager.TaskBuilder
import net.sourceforge.ganttproject.task.dependency.TaskDependency.Hardness
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl
import org.w3c.util.DateParser
import org.w3c.util.InvalidDateException
import java.awt.Color
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*

class TaskLoader(private val taskManager: TaskManager, private val treeCollapseView: TreeCollapseView<Task>) {
  val legacyFixedStartTasks = mutableListOf<Task>()
  val dependencies = mutableListOf<GanttDependStructure>()
  private val mapXmlGantt = mutableMapOf<XmlTask, Task>()

  fun loadTask(parent: XmlTask?, child: XmlTask) {
    var builder: TaskBuilder = taskManager.newTaskBuilder().withId(child.id)
    builder = builder.withName(child.name)
    val start = child.startDate
    if (start.isNotBlank()) {
      builder = builder.withStartDate(GanttCalendar.parseXMLDate(start).time)
    }
    builder = builder.withDuration(taskManager.createLength(child.duration.toLong()))
    builder =
      if (parent != null) {
        mapXmlGantt[parent]?.let { parentTask -> builder.withParent(parentTask) } ?: run {
          LOG.error("Can't find parent task for xmlTask={}, xmlParent={}", child, parent)
          builder
        }
      } else {
        builder
      }
    builder = builder.withExpansionState(child.isExpanded)
    if (child.isMilestone) {
      builder = builder.withLegacyMilestone()
    }
    val task = builder.build()

    treeCollapseView.setExpanded(task, child.isExpanded)
    task.isProjectTask = child.isProjectTask
    task.completionPercentage = child.completion
    task.priority = Task.Priority.fromPersistentValue(child.priority)
    if (child.color != null) {
      task.color = ColorValueParser.parseString(child.color)
    }

//
//    String fixedStart = attrs.getValue("fixed-start");
//    if ("true".equals(fixedStart)) {
//      myContext.addTaskWithLegacyFixedStart(task);
//    }
    val earliestStart = child.earliestStartDate
    if (earliestStart != null) {
      task.setThirdDate(GanttCalendar.parseXMLDate(earliestStart))
    }
    //    String thirdConstraint = attrs.getValue("thirdDate-constraint");
//    if (thirdConstraint != null) {
//      try {
//        task.setThirdDateConstraint(Integer.parseInt(thirdConstraint));
//      } catch (NumberFormatException e) {
//        throw new RuntimeException("Failed to parse the value '" + thirdConstraint
//            + "' of attribute 'thirdDate-constraint' of tag <task>", e);
//      }
//    }
    child.webLink?.let { it ->
      try {
        task.webLink = URLDecoder.decode(it, Charsets.UTF_8.name())
      } catch (e: UnsupportedEncodingException) {
        LOG.error("Can't decode URL value={} from XML task={}", it, child)
      }
    }
    child.shape?.let { it ->
      val st1 = StringTokenizer(it, ",")
      val array = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
      var token = ""
      var count = 0
      while (st1.hasMoreTokens()) {
        token = st1.nextToken()
        array[count] = token.toInt()
        count++
      }
      task.shape = ShapePaint(4, 4, array, Color.white, task.color)
    }
    val costValue = child.costManualValue
    val isCostCalculated = child.isCostCalculated
    if (isCostCalculated != null) {
      task.cost.isCalculated = isCostCalculated
      task.cost.value = costValue
    } else {
      task.cost.isCalculated = true
    }
    // myContext.pushTask(task);

    loadDependencies(child)
    loadCustomProperties(task, child)
  }

  private fun loadDependencies(xmlTask: XmlTask) {
    xmlTask.dependencies?.forEach { xmlDependency ->
      dependencies.add(GanttDependStructure(
        taskID = xmlTask.id,
        successorTaskID = xmlDependency.id,
        difference = xmlDependency.lag,
        dependType =
          if (xmlDependency.type.isBlank()) { ConstraintType.finishstart }
          else { ConstraintType.fromPersistentValue(xmlDependency.type) },
        hardness =
          if (xmlDependency.type.isBlank()) { Hardness.STRONG }
          else { Hardness.parse(xmlDependency.hardness) }
      ))
    }
  }

  private fun loadCustomProperties(task: Task, xmlTask: XmlTask) {
    xmlTask.customPropertyValues.forEach { xmlCustomProperty ->
      val cc = taskManager.customPropertyManager.getCustomPropertyDefinition(xmlCustomProperty.propId) ?: run {
        LOG.error("Can't find custom property definition for XML custom property {}", xmlCustomProperty)
        return@forEach
      }
      xmlCustomProperty.value?.let { valueStr ->
        when {
          cc.type == String::class.java -> valueStr
          cc.type == Boolean::class.java -> java.lang.Boolean.valueOf(valueStr)
          cc.type == Int::class.java -> Integer.valueOf(valueStr)
          cc.type == Double::class.java -> java.lang.Double.valueOf(valueStr)
          GregorianCalendar::class.java.isAssignableFrom(cc.type) ->
            try {
              CalendarFactory.createGanttCalendar(DateParser.parse(valueStr))
            } catch (e: InvalidDateException) {
              LOG.error("Can't parse date {} from XML custom property {}", valueStr, xmlCustomProperty)
              null
//              if (!GPLogger.log(e)) {
//                e.printStackTrace(System.err)
//              }
            }
          else -> null
        }
      }?.also { value ->
        try {
          task.customValues.setValue(cc, value)
        } catch (e: CustomColumnsException) {
          LOG.error("Can't set the value={} of a custom property={} for task={}", value, cc, task)
        }
      }
    }
  }
}

//private ParsingContext myContext;
data class GanttDependStructure(
  var taskID: Int = 0,
  var successorTaskID: Int = 0,
  var difference: Int = 0,
  var dependType: ConstraintType = ConstraintType.finishstart,
  var hardness: Hardness = Hardness.STRONG
)

fun loadDependencyGraph(deps: List<GanttDependStructure>, taskManager: TaskManager, legacyFixedStartTasks: List<Task>) {
  deps.forEach { ds ->
    val dependee: Task = taskManager.getTask(ds.taskID) ?: return@forEach
    val dependant: Task = taskManager.getTask(ds.successorTaskID) ?: return@forEach
    try {
      val dep = taskManager.dependencyCollection.createDependency(dependant, dependee, FinishStartConstraintImpl())
      dep.constraint = taskManager.createConstraint(ds.dependType)
      dep.difference = ds.difference
      if (legacyFixedStartTasks.contains(dependant)) {
        dep.hardness = Hardness.RUBBER
      } else {
        dep.hardness = ds.hardness
      }
    } catch (e: TaskDependencyException) {
      GPLogger.log(e)
    }
  }
}

private val LOG = GPLogger.create("Project.IO.Load.Task")