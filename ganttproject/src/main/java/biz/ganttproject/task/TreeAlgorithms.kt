package biz.ganttproject.task

import com.google.common.base.Function
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade
import net.sourceforge.ganttproject.task.TaskDocumentOrderComparator
import net.sourceforge.ganttproject.task.algorithm.RetainRootsAlgorithm

/**
 * @author dbarashev@bardsoftware.com
 */
private val getParentTask = Function<Task, Task> { task ->
  task?.let { it.manager.taskHierarchy.getContainer(task) }
}
private val RETAIN_ROOTS = RetainRootsAlgorithm<Task>()

fun retainRoots(tasks: List<Task>) =
  mutableListOf<Task>().also { RETAIN_ROOTS.run(tasks, getParentTask, it) }

fun documentOrdered(tasks: List<Task>, treeFacade: TaskContainmentHierarchyFacade) =
  tasks.toMutableList().also { it.sortWith(TaskDocumentOrderComparator(treeFacade)) }
