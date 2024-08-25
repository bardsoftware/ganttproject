package net.sourceforge.ganttproject.chart.gantt

import biz.ganttproject.core.chart.scene.IdentifiableRow
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.core.time.TimeUnit
import com.google.common.base.Preconditions
import java.util.*

/**
 * Some parts of the renderer, e.g. progress bar rendering, don't like activities which cross
 * the viewport borders. The reason is that we build shapes (specifically, rectangles) only for
 * visible parts of activities. When activity crosses the viewport border, the invisible parts
 * are no more than ~20px wide. However, progress bar needs to know pixel size of all shapes from
 * the task beginning up to the point where progress bar should be terminated OR needs activities
 * to be split exactly at the viewport border.
 *
 * @param activities
 * @return
 */

/**
 * This method scans the list of activities and splits activities crossing the borders
 * of the given frame into parts "before" and "after" the border date. Activities which
 * do not cross frame borders are left as is, and the relative order of activities is preserved.
 *
 * Normally no more than two activities from the input list are partitioned.
 *
 * @return input activities with those crossing frame borders partitioned into left and right parts
 */
internal class TaskActivitySplitter<T : IdentifiableRow>(
    private val frameStartDate: () -> Date,
    private val frameEndDate: () -> Date,
    private val durationCalculator: (TimeUnit, Date, Date) -> TimeDuration) : ITaskActivitySplitter<T> {
  override fun split(activities: List<ITaskActivity<T>>, limit: Int): List<ITaskActivity<T>> {
    val frameStartDate = frameStartDate()
    val frameEndDate = frameEndDate()

    assert(frameEndDate >= frameStartDate) {
      "Invalid frame: start=$frameStartDate end=$frameEndDate"
    }
    val result = mutableListOf<ITaskActivity<T>>()
    val queue: Deque<ITaskActivity<T>> = LinkedList(activities)
    while (!queue.isEmpty() && result.size < limit) {
      val head = queue.pollFirst()
      if (head.start < frameStartDate && head.end > frameStartDate) {

        // Okay, this activity crosses frame start. Lets add its left part to the result
        // and push back its right part
        val beforeViewport = TaskActivityPart(head, head.start, frameStartDate,
          durationCalculator(head.duration.timeUnit, head.start, frameStartDate))
        val remaining = TaskActivityPart(head, frameStartDate, head.end, durationCalculator(head.duration.timeUnit, frameStartDate, head.end))
        result.add(beforeViewport)
        queue.addFirst(remaining)
        continue
      }
      if (head.start < frameEndDate && head.end > frameEndDate) {
        // This activity crosses frame end date. Again, lets add its left part to the result
        // and push back the remainder.
        val insideViewport = TaskActivityPart(head, head.start, frameEndDate, durationCalculator(head.duration.timeUnit, head.start, frameEndDate))
        val remaining = TaskActivityPart(head, frameEndDate, head.end, durationCalculator(head.duration.timeUnit, frameEndDate, head.end))
        result.add(insideViewport)
        queue.addFirst(remaining)
        continue
      }
      result.add(head)
    }
    return result
  }
}
