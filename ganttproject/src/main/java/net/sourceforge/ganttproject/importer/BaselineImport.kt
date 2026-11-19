/*
Copyright 2026 BarD Software s.r.o

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
package net.sourceforge.ganttproject.importer

import net.sourceforge.ganttproject.GanttPreviousState
import net.sourceforge.ganttproject.GanttPreviousStateTask

/**
 * Adds the baselines of an imported project to the baselines of the target project.
 *
 * A baseline records task ids, and importing tasks gives them new ids, so the ids of the entries
 * are translated with [taskMapping]. An entry whose task is not in the mapping is dropped, because
 * its id may belong to a different task of the target project.
 *
 * If the target project already has a baseline with the same name, [onNameCollision] decides what
 * happens, see [BaselineNameCollision].
 *
 * @return the baselines which were added to [targetBaselines]
 */
fun importBaselines(
  sourceBaselines: List<GanttPreviousState>,
  targetBaselines: MutableList<GanttPreviousState>,
  taskMapping: TaskMapping,
  onNameCollision: BaselineNameCollision = BaselineNameCollision.RENAME
): List<GanttPreviousState> {
  val id2id = taskMapping.entries.associate { (original, imported) -> original.taskID to imported.taskID }
  return sourceBaselines.map { source ->
    val tasks = source.tasks.mapNotNull { entry ->
      id2id[entry.id]?.let { newId ->
        GanttPreviousStateTask(newId, entry.start, entry.duration, entry.isMilestone, entry.hasNested())
      }
    }
    val existing = targetBaselines.indexOfFirst { it.name == source.name }
    when {
      existing < 0 || onNameCollision == BaselineNameCollision.KEEP_BOTH ->
        GanttPreviousState(source.name, tasks).also { targetBaselines.add(it) }
      onNameCollision == BaselineNameCollision.REPLACE ->
        GanttPreviousState(source.name, tasks).also { targetBaselines[existing] = it }
      else ->
        GanttPreviousState(freeName(source.name, targetBaselines), tasks).also { targetBaselines.add(it) }
    }
  }
}

/**
 * What to do with an imported baseline whose name the target project already has.
 */
enum class BaselineNameCollision {
  /** The imported baseline gets a number appended, e.g. "Baseline (2)". */
  RENAME,

  /** The imported baseline replaces the baseline of the target project. */
  REPLACE,

  /** The imported baseline is added with the same name, so the name appears twice. */
  KEEP_BOTH
}

/**
 * Names of the baselines of [sourceProjects] which are already taken, either by [targetBaselines]
 * or by a baseline of an earlier source project. Each name is listed once.
 */
fun baselineNameCollisions(
  sourceProjects: List<List<GanttPreviousState>>,
  targetBaselines: List<GanttPreviousState>
): List<String> {
  val taken = targetBaselines.map { it.name }.toMutableSet()
  val collisions = linkedSetOf<String>()
  sourceProjects.forEach { baselines ->
    val names = baselines.map { it.name }
    names.filterTo(collisions) { it in taken }
    taken.addAll(names)
  }
  return collisions.toList()
}

private fun freeName(name: String, baselines: List<GanttPreviousState>): String {
  val taken = baselines.map { it.name }.toSet()
  if (name !in taken) {
    return name
  }
  var n = 2
  while ("$name ($n)" in taken) {
    n++
  }
  return "$name ($n)"
}
