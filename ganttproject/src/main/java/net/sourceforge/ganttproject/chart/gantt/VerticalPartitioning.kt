/*
Copyright 2020 Dmitry Kazakov, BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject.chart.gantt

import com.google.common.collect.Lists

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
class VerticalPartitioning @JvmOverloads constructor(
  private val insideViewport: List<ITaskSceneTask>,
  private val areUnrelated: (ITaskSceneTask, ITaskSceneTask) -> Boolean,
  /**
   * Tasks that the task table HIDES -- through a filter or through a named view. They belong in no
   * partition at all, exactly like the children of a collapsed task.
   *
   * WHY THIS EXISTS. A hidden task that lies between the first and the last visible row falls into
   * the skipped middle part and is correct without any help. A hidden task at the HEAD or the TAIL
   * of the document order, however, lands in [aboveViewport] or [belowViewport], where
   * GanttChartSceneBuilder.renderTasksAboveAndBelowViewport gives it an INVISIBLE rectangle at row
   * -1 or row n+1. DependencySceneBuilder only drops a dependency line when BOTH ends are invisible
   * (DependencySceneBuilder.java:153) -- so a line runs from a visible task to the edge of the
   * chart, pointing at something that is not there.
   *
   * The default says "nothing is hidden", which is exactly what every caller that does not know
   * about hiding means, so the behaviour of the class is unchanged for them.
   */
  private val isHidden: (ITaskSceneTask) -> Boolean = { false }
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
    var hiddenRoot: ITaskSceneTask? = null
    for (nextTask in tasksInDocumentOrder) {
      // A hidden task takes its whole subtree with it, the same way the task table does: the tree
      // node is never created, so nothing below it can appear either.
      if (hiddenRoot != null) {
        if (!areUnrelated(nextTask, hiddenRoot)) {
          continue
        }
        hiddenRoot = null
      }
      if (isHidden(nextTask)) {
        hiddenRoot = nextTask
        continue
      }
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
