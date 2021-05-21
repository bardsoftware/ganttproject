package biz.ganttproject.ganttview

import biz.ganttproject.core.model.task.TaskDefaultColumn
import com.google.common.base.Joiner
import com.google.common.base.Predicate
import com.google.common.base.Predicates
import com.google.common.collect.Lists
import net.sourceforge.ganttproject.task.*
import java.util.*
import javax.swing.ImageIcon

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskTableModel(private val taskManager: TaskManager) {
  init {
    TaskDefaultColumn.BEGIN_DATE.setIsEditablePredicate(NOT_SUPERTASK)
    TaskDefaultColumn.END_DATE.setIsEditablePredicate(
      Predicates.and(NOT_SUPERTASK, NOT_MILESTONE
      )
    )
    TaskDefaultColumn.DURATION.setIsEditablePredicate(
      Predicates.and(NOT_SUPERTASK, NOT_MILESTONE
      )
    )
  }

  fun getValueAt(t: Task, column: Int): Any? {
    if (column < 0) {
      return ""
    }
    var res: Any? = null
    if (column < STANDARD_COLUMN_COUNT) {
      val defaultColumn = TaskDefaultColumn.values()[column]
      when (defaultColumn) {
        TaskDefaultColumn.PRIORITY -> {
          res = ImageIcon(javaClass.getResource(t.priority.iconPath))
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
    }
    // if(tn.getParent()!=null){
    return res
  }
}

private val STANDARD_COLUMN_COUNT = TaskDefaultColumn.values().size

val NOT_SUPERTASK: Predicate<Task> = Predicate<Task> { task ->
  task?.isSupertask?.not() ?: false
}

val NOT_MILESTONE: Predicate<Task> = Predicate<Task> { task ->
   task?.isMilestone?.not() ?: false
}
