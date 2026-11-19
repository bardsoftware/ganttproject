/*
Copyright 2026 BarD Software s.r.o., GanttProject Cloud OU

This file is part of GanttProject, an opensource project management tool.

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
package biz.ganttproject.core.model.task

import java.time.LocalDate

/*
 * Resource levelling: moves tasks later so that no resource is booked over its capacity on any working day.
 *
 * The scheduler places every task as early as its dependencies allow and does not look at resources at all. This
 * calculation does, and it knows no project types: it works with LocalDate and plain values, and the calendar comes
 * in as a function, so that it can be tested without a project.
 *
 * Rules:
 *  - tasks are placed in dependency order; of the tasks which are ready, the one with higher priority goes first,
 *    and on equal priority the one which is higher in the plan;
 *  - several tasks may run on the same day as long as each resource stays within its capacity;
 *  - a fixed date stays, and so does frozen work; if they overload a resource, the overload is reported, not resolved;
 *  - a deadline is not enforced either, a task which ends after it is reported;
 *  - a task is never split, it runs on consecutive working days;
 *  - a task is placed only where all of its blocking resources are present; if there is no such window, or no window
 *    with room, it stays on its earliest day and this is reported;
 *  - on a day of a task on which a blocking resource is away, no resource of the task books capacity, so other tasks may use it.
 */

/** A task in the form levelling needs it. */
data class LevelTask(
  val id: String,

  /** Position in the plan, from top to bottom. Decides between tasks of equal priority. */
  val orderInPlan: Int,

  /** Greater means more important. */
  val priority: Int,

  /** Duration in working days. */
  val durationDays: Int,

  /** Finish-to-start predecessors. */
  val predecessors: List<String> = emptyList(),

  /** The task stays on this start date. */
  val fixedStart: LocalDate? = null,

  /** The task starts on this date or later. */
  val earliestStart: LocalDate? = null,

  /**
   * Percentage of the working day of each resource which the task takes, by resource id. Every resource books its
   * own load, not the sum of the loads of the task. Empty means no resource is assigned: such a task takes
   * [SHARED_POOL_LOAD] percent of the [SHARED_POOL].
   */
  val loads: Map<String, Int> = emptyMap(),

  /** Work which has started stays where it is and keeps its capacity. Its date is [fixedStart]. */
  val frozen: Boolean = false,

  /** Latest end. Not enforced, a task which ends later is reported. */
  val deadline: LocalDate? = null,

  /**
   * Resources which have to be present on every day of the task, whatever their load. Several of them leave only the
   * days on which all of them are present. Fixed dates and frozen work do not ask.
   */
  val blocking: Set<String> = emptySet(),

  /** The task needs its [blocking] resources on site, so it keeps off the days at home of them. */
  val requiresPresence: Boolean = false
)

sealed interface LevelConflict {
  /** A fixed date is earlier than the dependencies allow. The date is kept. */
  data class FixedDateNotReachable(
    val id: String, val fixedStart: LocalDate, val earliestPossible: LocalDate) : LevelConflict

  /** On this day the tasks need more than the capacity of a resource. Only fixed dates and frozen work cause it. */
  data class Overload(
    val day: LocalDate, val percent: Int, val ids: List<String>,
    /** The overloaded resource, or [SHARED_POOL]. */
    val resourceId: String = SHARED_POOL
  ) : LevelConflict

  /** These tasks depend on each other in a cycle. Nothing is levelled. */
  data class Cycle(val ids: List<String>) : LevelConflict

  /** The task ends [missingDays] working days after its deadline. */
  data class DeadlineMissed(
    val id: String, val deadline: LocalDate, val actualEnd: LocalDate, val missingDays: Int
  ) : LevelConflict

  /**
   * The search found no window for the task, which is placed on its earliest day instead. At least one list is not
   * empty: [blocking] are all blocking resources of the task if one of them was absent on a candidate day, so that no
   * day has them all present; [atHome] are all blocking resources of a task which needs them on site, if one of them
   * was at home on a candidate day; [fullFor] are the pools which had no room on a candidate day.
   */
  data class NoPossibleDate(
    val id: String, val blocking: List<String>, val fullFor: List<String>, val atHome: List<String> = emptyList()
  ) : LevelConflict
}

data class LevelResult(
  val starts: Map<String, LocalDate>,
  val conflicts: List<LevelConflict>,
  /**
   * The duration each task was placed with. It may differ from [LevelTask.durationDays] when the working hours of
   * the resources change over time, and the dates which are written back must use it.
   */
  val durations: Map<String, Int> = emptyMap()
)

/** The pool of the tasks which have no resource assigned. */
const val SHARED_POOL = ""

/** What a task without resources takes of the [SHARED_POOL]. */
const val SHARED_POOL_LOAD = 100

/** Bound of every day by day search, about 200 years of working days. Reaching it means there are no working days. */
private const val MAX_SEARCH_DAYS = 50_000

/** The capacity pools which this task takes. */
val LevelTask.pools: List<String>
  get() = if (loads.isEmpty()) listOf(SHARED_POOL) else loads.keys.toList()

/** The percentage of [pool] which this task takes. */
fun LevelTask.loadIn(pool: String): Int = loads[pool] ?: SHARED_POOL_LOAD

/**
 * Places [tasks] from [projectStart] on.
 *
 * @param durationAt the duration of a task if it starts on the given day; it depends on the day when working hours
 * change over time.
 * @param capacityOf how many percent of a working day of a pool may be booked.
 * @param canStartOn whether a task may start on the given day, such as not on a day off of its resources. A task
 * with a fixed start is not asked.
 * @param isPresent whether a resource is present on the given day; asked only for the [LevelTask.blocking] resources.
 * @param isAtWorkplace whether a resource is on site, not at home, on the given day; asked only for the
 * [LevelTask.blocking] resources of a task which [LevelTask.requiresPresence].
 * @param workingDaysOf the working days of a task, when they differ from task to task, such as by the working weeks
 * of its resources. The duration and the days of a task are counted with its own working days.
 */
fun levelTasks(
  tasks: List<LevelTask>,
  projectStart: LocalDate,
  isWorkingDay: (LocalDate) -> Boolean,
  durationAt: (LevelTask, LocalDate) -> Int = { task, _ -> task.durationDays },
  capacityOf: (String) -> Int = { 100 },
  canStartOn: (LevelTask, LocalDate) -> Boolean = { _, _ -> true },
  isPresent: (String, LocalDate) -> Boolean = { _, _ -> true },
  workingDaysOf: (LevelTask) -> (LocalDate) -> Boolean = { isWorkingDay },
  isAtWorkplace: (String, LocalDate) -> Boolean = { _, _ -> true }
): LevelResult {
  val byId = tasks.associateBy { it.id }
  val order = topologicalOrder(tasks) ?: return LevelResult(emptyMap(), listOf(LevelConflict.Cycle(tasks.map { it.id })))

  // Booked percentage by pool and day.
  val used = mutableMapOf<String, MutableMap<LocalDate, Int>>()
  val starts = mutableMapOf<String, LocalDate>()
  val durations = mutableMapOf<String, Int>()
  // The day after the last day of each task. A successor starts on its own next working day from there.
  val ends = mutableMapOf<String, LocalDate>()
  val conflicts = mutableListOf<LevelConflict>()

  fun place(task: LevelTask, days: List<LocalDate>) {
    // On a day on which a blocking resource is away nobody works on the task, so every resource keeps its capacity.
    val blockedDays = days.filter { day ->
      task.blocking.any { !isPresent(it, day) || (task.requiresPresence && !isAtWorkplace(it, day)) }
    }.toSet()
    task.pools.forEach { pool ->
      val booked = used.getOrPut(pool) { mutableMapOf() }
      days.filter { it !in blockedDays }.forEach { day ->
        booked[day] = (booked[day] ?: 0) + task.loadIn(pool)
      }
    }
    starts[task.id] = days.first()
    durations[task.id] = days.size
    ends[task.id] = days.last().plusDays(1)
  }

  // Frozen work is booked first, so that no movable task can take its days.
  tasks.filter { it.frozen }.forEach { task ->
    val worksOn = workingDaysOf(task)
    val start = nextWorkingDay(task.fixedStart ?: projectStart, worksOn)
    place(task, workingDays(start, durationAt(task, start), worksOn))
  }

  for (id in order) {
    val task = byId.getValue(id)
    if (task.frozen) {
      continue
    }
    var earliest = maxOf(projectStart, task.earliestStart ?: projectStart)
    task.predecessors.forEach { p -> ends[p]?.let { earliest = maxOf(earliest, it) } }
    val worksOn = workingDaysOf(task)
    earliest = nextWorkingDay(earliest, worksOn)

    val days = if (task.fixedStart != null) {
      val start = nextWorkingDay(task.fixedStart, worksOn)
      if (start < earliest) {
        conflicts.add(LevelConflict.FixedDateNotReachable(id, start, earliest))
      }
      workingDays(start, durationAt(task, start), worksOn)
    } else {
      val search = searchWindow(earliest, task, durationAt, used, worksOn, capacityOf, canStartOn, isPresent, isAtWorkplace)
      if (search.exhausted) {
        // A blocking resource counts as a reason only if its absence actually turned a day down.
        val blocking = if (search.absenceBlocked) task.blocking.sorted() else emptyList()
        val atHome = if (search.homeBlocked) task.blocking.sorted() else emptyList()
        if (blocking.isNotEmpty() || atHome.isNotEmpty() || search.fullFor.isNotEmpty()) {
          conflicts.add(LevelConflict.NoPossibleDate(id, blocking, search.fullFor.sorted(), atHome))
        }
      }
      search.window
    }
    place(task, days)
  }

  tasks.forEach { task ->
    val deadline = task.deadline ?: return@forEach
    val lastDay = ends[task.id]?.minusDays(1) ?: return@forEach
    if (lastDay.isAfter(deadline)) {
      val missingDays = workingDaysBetween(deadline.plusDays(1), lastDay.plusDays(1), workingDaysOf(task))
      conflicts.add(LevelConflict.DeadlineMissed(task.id, deadline, lastDay, missingDays))
    }
  }

  used.toSortedMap().forEach { (pool, booked) ->
    booked.filterValues { it > capacityOf(pool) }.toSortedMap().forEach { (day, percent) ->
      val ids = tasks.filter { t ->
        val start = starts[t.id]
        t.pools.contains(pool) && start != null &&
          workingDays(start, durations[t.id] ?: t.durationDays, workingDaysOf(t)).contains(day)
      }.map { it.id }
      conflicts.add(LevelConflict.Overload(day, percent, ids, pool))
    }
  }
  return LevelResult(starts, conflicts, durations)
}

/** Tasks in the order of placing, or null if the dependencies run in a cycle. */
private fun topologicalOrder(tasks: List<LevelTask>): List<String>? {
  val open = tasks.associateBy { it.id }.toMutableMap()
  val result = mutableListOf<String>()
  while (open.isNotEmpty()) {
    val next = open.values
      .filter { t -> t.predecessors.none { it in open } }
      .minWithOrNull(compareByDescending<LevelTask> { it.priority }.thenBy { it.orderInPlan })
      ?: return null
    result.add(next.id)
    open.remove(next.id)
  }
  return result
}

/** The first working day from [from] on, or [from] itself if the calendar has no working day at all. */
fun nextWorkingDay(from: LocalDate, isWorkingDay: (LocalDate) -> Boolean): LocalDate {
  var day = from
  repeat(MAX_SEARCH_DAYS) {
    if (isWorkingDay(day)) {
      return day
    }
    day = day.plusDays(1)
  }
  return from
}

/** [count] working days from [start] on, at least one. */
fun workingDays(start: LocalDate, count: Int, isWorkingDay: (LocalDate) -> Boolean): List<LocalDate> {
  val days = mutableListOf<LocalDate>()
  var day = nextWorkingDay(start, isWorkingDay)
  var steps = 0
  while (days.size < maxOf(count, 1)) {
    if (steps++ > MAX_SEARCH_DAYS) {
      return days.ifEmpty { listOf(start) }
    }
    if (isWorkingDay(day)) {
      days.add(day)
    }
    day = day.plusDays(1)
  }
  return days
}

/** Number of working days from [from] inclusive to [to] exclusive. */
fun workingDaysBetween(from: LocalDate, to: LocalDate, isWorkingDay: (LocalDate) -> Boolean): Int {
  var count = 0
  var day = from
  var steps = 0
  while (day < to && steps++ < MAX_SEARCH_DAYS) {
    if (isWorkingDay(day)) count++
    day = day.plusDays(1)
  }
  return count
}

/**
 * The day after the last of [days] working days from [start] on. This is the end date of the task model: a task which
 * ends on a Friday ends on Saturday there, not on the next Monday, which would show Sunday as its last day.
 */
fun endAfterWorkingDays(start: LocalDate, days: Int, isWorkingDay: (LocalDate) -> Boolean): LocalDate =
  workingDays(start, days, isWorkingDay).last().plusDays(1)

/**
 * The earliest window from [earliest] on in which every resource of the task has room on every day.
 *
 * The limit of a pool is at least the load of the task itself: otherwise a task which needs more than the capacity
 * on its own would fit nowhere and the search would never end. The capacity limits what runs alongside a task.
 */
fun findEarliestWindow(
  earliest: LocalDate,
  task: LevelTask,
  durationAt: (LevelTask, LocalDate) -> Int,
  used: Map<String, Map<LocalDate, Int>>,
  isWorkingDay: (LocalDate) -> Boolean,
  capacityOf: (String) -> Int,
  canStartOn: (LevelTask, LocalDate) -> Boolean = { _, _ -> true },
  isPresent: (String, LocalDate) -> Boolean = { _, _ -> true },
  isAtWorkplace: (String, LocalDate) -> Boolean = { _, _ -> true }
): List<LocalDate> =
  searchWindow(earliest, task, durationAt, used, isWorkingDay, capacityOf, canStartOn, isPresent, isAtWorkplace).window

/**
 * The outcome of a window search. When the search is [exhausted], [window] starts on the earliest day, and
 * [absenceBlocked] and [fullFor] tell why the candidate days were turned down.
 */
private class WindowSearch(
  val window: List<LocalDate>, val exhausted: Boolean, val absenceBlocked: Boolean, val fullFor: Set<String>,
  val homeBlocked: Boolean = false
)

private fun searchWindow(
  earliest: LocalDate,
  task: LevelTask,
  durationAt: (LevelTask, LocalDate) -> Int,
  used: Map<String, Map<LocalDate, Int>>,
  isWorkingDay: (LocalDate) -> Boolean,
  capacityOf: (String) -> Int,
  canStartOn: (LevelTask, LocalDate) -> Boolean,
  isPresent: (String, LocalDate) -> Boolean,
  isAtWorkplace: (String, LocalDate) -> Boolean
): WindowSearch {
  var absenceBlocked = false
  var homeBlocked = false
  val fullFor = mutableSetOf<String>()
  var candidate = nextWorkingDay(earliest, isWorkingDay)
  if (!isWorkingDay(candidate)) {
    // The task has no working day at all, such as when a resource works on no day. Every candidate would search up to
    // the limit again, which takes hours, so the task stays on its earliest day.
    return WindowSearch(listOf(candidate), true, false, emptySet())
  }
  repeat(MAX_SEARCH_DAYS) {
    if (task.requiresPresence && task.blocking.any { !isAtWorkplace(it, candidate) }) {
      // The task does not start on a day at home, whatever its duration.
      homeBlocked = true
      candidate = nextWorkingDay(candidate.plusDays(1), isWorkingDay)
      return@repeat
    }
    if (!canStartOn(task, candidate)) {
      // A start turned down on a day off of a blocking resource is its absence as well.
      if (task.blocking.any { !isPresent(it, candidate) }) {
        absenceBlocked = true
      }
      candidate = nextWorkingDay(candidate.plusDays(1), isWorkingDay)
      return@repeat
    }
    // The duration depends on the start day when working hours change, so it is asked for every candidate.
    val window = workingDays(candidate, durationAt(task, candidate), isWorkingDay)
    val blockedAt = window.firstOrNull { day ->
      // Absence is asked first: on a day without a blocking resource the capacity does not matter.
      val absent = task.blocking.any { !isPresent(it, day) }
      if (absent) {
        absenceBlocked = true
      }
      // A day at home is no absence: it only turns the task down which needs somebody on site.
      val atHome = !absent && task.requiresPresence && task.blocking.any { !isAtWorkplace(it, day) }
      if (atHome) {
        homeBlocked = true
      }
      absent || atHome || task.pools.any { pool ->
        val load = task.loadIn(pool)
        // A load of zero fits even on a day which fixed dates have overloaded.
        (load > 0 && (used[pool]?.get(day) ?: 0) + load > maxOf(capacityOf(pool), load)).also {
          if (it) fullFor.add(pool)
        }
      }
    } ?: return WindowSearch(window, false, absenceBlocked, fullFor, homeBlocked)
    candidate = nextWorkingDay(blockedAt.plusDays(1), isWorkingDay)
  }
  return WindowSearch(
    workingDays(nextWorkingDay(earliest, isWorkingDay), durationAt(task, earliest), isWorkingDay),
    true, absenceBlocked, fullFor, homeBlocked
  )
}
