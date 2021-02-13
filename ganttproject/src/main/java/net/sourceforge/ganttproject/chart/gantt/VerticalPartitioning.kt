package net.sourceforge.ganttproject.chart.gantt

import com.google.common.collect.Lists
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade

/**
 * This class splits all tasks into 4 groups. One group is pure virtual: it contains
 * tasks which are hidden under some collapsed parent and hence are just filtered out.
 * The remaining groups are: tasks which are shown in the chart viewport, tasks above the viewport
 * and tasks below the viewport. We need tasks outside the viewport because we want to show
 * dependency lines which may connect them with tasks inside the viewport.
 * 
 * @param tasksInsideViewport partition with tasks inside viewport, with hidden tasks already filtered.
 * Tasks must be ordered in their document order.
 */
internal class VerticalPartitioning(
  private val insideViewport: List<ITaskSceneTask>,
  private val areUnrelated: (ITaskSceneTask, ITaskSceneTask) -> Boolean
) {
  val aboveViewport: MutableList<ITaskSceneTask> = Lists.newArrayList()
  val belowViewport: MutableList<ITaskSceneTask> = Lists.newArrayList()

  /**
   * Builds the remaining partitions.
   *
   * In this method we iterate through *all* the tasks in their document order. If we find some
   * collapsed task then we filter out its children. Until we reach the first task in the vieport
   * partition,  we're above the viewport, then we skip the viewport partition and proceed to
   * below viewport
   */
  fun build(tasksInDocumentOrder: List<ITaskSceneTask>) {
    val firstVisible = if (insideViewport.isEmpty()) null else insideViewport[0]
    val lastVisible = if (insideViewport.isEmpty()) null else insideViewport[insideViewport.size - 1]
    var addTo: MutableList<ITaskSceneTask>? = aboveViewport
    var collapsedRoot: ITaskSceneTask? = null
    for (nextTask in tasksInDocumentOrder) {
      if (addTo == null) {
        if (nextTask == lastVisible) {
          addTo = belowViewport
        }
        continue
      }
      if (nextTask == firstVisible) {
        addTo = null
        continue
      }
      if (collapsedRoot != null) {
        collapsedRoot = if (areUnrelated(nextTask, collapsedRoot)) {
          null
        } else {
          continue
        }
      }
      addTo.add(nextTask)
      if (!nextTask.expand) {
        assert(collapsedRoot == null) { "All tasks processed prior to this one must be expanded" }
        collapsedRoot = nextTask
      }
    }
  }
}
