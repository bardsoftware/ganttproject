package net.sourceforge.ganttproject.task

import com.google.common.base.Predicate
import com.google.common.collect.Lists
import com.google.common.collect.Queues
import net.sourceforge.ganttproject.util.collect.Pair
import java.util.*

/**
 * @author dbarashev@bardsoftware.com
 */
class FacadeImpl(private val taskManager: TaskManagerImpl, private val root: Task) : TaskContainmentHierarchyFacade {
  override fun getNestedTasks(container: Task): Array<Task> {
    return container.nestedTasks
  }

  override fun getDeepNestedTasks(container: Task): Array<Task> {
    val result = ArrayList<Task>()
    addDeepNestedTasks(container, result)
    return result.toTypedArray()
  }

  private fun addDeepNestedTasks(container: Task, result: ArrayList<Task>) {
    val nested = container.nestedTasks
    result.addAll(Arrays.asList(*nested))
    for (i in nested.indices) {
      addDeepNestedTasks(nested[i], result)
    }
  }

  override fun hasNestedTasks(container: Task): Boolean {
    return container.nestedTasks.size > 0
  }

  override fun getRootTask(): Task {
    return root
  }

  override fun getContainer(nestedTask: Task): Task? {
    return nestedTask.supertask
  }

  override fun sort(comparator: Comparator<Task?>?) {
    throw UnsupportedOperationException("Sort is not available int this implementation. It is stateless!")
  }

  override fun getPreviousSibling(nestedTask: Task): Task? {
    val pos = getTaskIndex(nestedTask)
    return if (pos <= 0) null else nestedTask.supertask.nestedTasks[pos - 1]
  }

  override fun getNextSibling(nestedTask: Task): Task? {
    val pos = getTaskIndex(nestedTask)
    val allSiblings = nestedTask.supertask.nestedTasks
    return if (pos < allSiblings.size - 1) allSiblings[pos + 1] else null
  }

  override fun getTaskIndex(nestedTask: Task): Int {
    val container = nestedTask.supertask ?: return 0
    return Arrays.asList(*container.nestedTasks).indexOf(nestedTask)
  }

  override fun areUnrelated(first: Task, second: Task): Boolean {
    if (first == second) {
      return false
    }
    return !(first.ancestors(includeSelf = false).contains(second) || second.ancestors(includeSelf = false).contains(first))
  }

  override fun move(whatMove: Task, whereMove: Task?) {
    val oldParent = getContainer(whatMove)
    whatMove.move(whereMove)
    this.taskManager.fireTaskMoved(whatMove, oldParent, whereMove)
  }

  override fun move(whatMove: Task, whereMove: Task?, index: Int) {
    val oldParent = getContainer(whatMove)
    whatMove.move(whereMove, index)
    this.taskManager.fireTaskMoved(whatMove, oldParent, whereMove)
  }

  override fun getDepth(task: Task): Int {
    var task = task
    var depth = 0
    while (task !== root) {
      task = task.supertask
      depth++
    }
    return depth
  }

  override fun compareDocumentOrder(task1: Task, task2: Task): Int {
    if (task1 === task2) {
      return 0
    }
    val buffer1 = task1.ancestors().asReversed()
    val buffer2 = task2.ancestors().asReversed()
    if (buffer1[0] !== rootTask && buffer2[0] === rootTask) {
      return -1
    }
    if (buffer1[0] === rootTask && buffer2[0] !== rootTask) {
      return 1
    }
    var i = 0
    var commonRoot: Task? = null
    while (true) {
      if (i == buffer1.size) {
        return -1
      }
      if (i == buffer2.size) {
        return 1
      }
      val root1 = buffer1[i]
      val root2 = buffer2[i]
      if (root1 !== root2) {
        assert(commonRoot != null) {
          """Failure comparing task=$task1 and task=$task2
. Path1=$buffer1
Path2=$buffer2"""
        }
        val nestedTasks = commonRoot!!.nestedTasks
        for (j in nestedTasks.indices) {
          if (nestedTasks[j] === root1) {
            return -1
          }
          if (nestedTasks[j] === root2) {
            return 1
          }
        }
        throw IllegalStateException("We should not be here")
      }
      i++
      commonRoot = root1
    }
  }

  override fun contains(task: Task): Boolean {
    return task.supertask != null
  }

  override fun getTasksInDocumentOrder(): List<Task> {
    val result: MutableList<Task> = Lists.newArrayList()
    val deque = LinkedList<Task>()
    deque.addFirst(rootTask)
    while (!deque.isEmpty()) {
      val head = deque.poll()
      result.add(head)
      deque.addAll(0, Arrays.asList(*head.nestedTasks))
    }
    result.removeAt(0)
    return result
  }

  override fun breadthFirstSearch(root: Task, predicate: Predicate<Pair<Task?, Task>>) {
    val queue: Queue<Task> = Queues.newArrayDeque()
    if (predicate.apply(Pair.create(null as Task?, root))) {
      queue.add(root)
    }
    while (!queue.isEmpty()) {
      val head = queue.poll()
      for (child in head.nestedTasks) {
        if (predicate.apply(Pair.create(head, child))) {
          queue.add(child)
        }
      }
    }
  }

  override fun breadthFirstSearch(root: Task?, includeRoot: Boolean): List<Task> {
    val _root = root ?: rootTask
    val result: MutableList<Task> = Lists.newArrayList()
    breadthFirstSearch(_root) {
      it?.let {
        if (includeRoot || it.first() != null) {
          result.add(it.second())
        }
        true
      } ?: false
    }
    return result
  }

  override fun getOutlinePath(task: Task): List<Int> {
    return task.ancestors().asReversed().zipWithNext().map { (parent, child) ->
      parent.nestedTasks.indexOf(child) + 1
    }.toList()
  }

}

private fun Task.ancestors(includeSelf: Boolean = true) : List<Task> {
  val path = mutableListOf<Task>()
  if (includeSelf) {
    path.add(this)
  }
  var t = this
  while (t.supertask != null) {
    path.add(t.supertask)
    t = t.supertask
  }
  return path
}
