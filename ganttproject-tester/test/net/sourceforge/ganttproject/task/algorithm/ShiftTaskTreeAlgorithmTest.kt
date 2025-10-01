/*
 * Copyright 2025 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.task.algorithm

import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.test.task.TaskTestCase

/**
 * Tests for the algorithm that shifts a group of tasks
 */
class ShiftTaskTreeAlgorithmTest: TaskTestCase() {
  /**
   * Tests that if we move the whole project, the start dates of all tasks
   * are shifted accordingly.
   */
  fun `test move all tasks`() {
    val task1 = createTask(TestSetupHelper.newMonday())
    val task2 = createTask(TestSetupHelper.newTuesday()).also {
      it.isMilestone = true
    }
    val task3 = createTask(TestSetupHelper.newTuesday())

    val shiftAlgorithm = ShiftTaskTreeAlgorithm(taskManager, listOf(taskManager.rootTask), true)
    shiftAlgorithm.run(taskManager.createLength(2))
    shiftAlgorithm.commit()

    assertEquals(TestSetupHelper.newWendesday(), task1.start)
    assertEquals(TestSetupHelper.newThursday(), task2.start)
    assertEquals(TestSetupHelper.newThursday(), task3.start)
  }

  /**
   * Tests that if we explicitly specify the tasks to shift, only those tasks and their dependencies are moved.
   */
  fun `test move only the start tasks`() {
    val task1 = createTask(TestSetupHelper.newMonday())
    val task2 = createTask().also {
      it.isMilestone = true
    }
    val task3 = createTask()
    val task4 = createTask(TestSetupHelper.newWendesday())

    createDependency(task2, task1)
    createDependency(task3, task2)

    val shiftAlgorithm = ShiftTaskTreeAlgorithm(taskManager, listOf(task1), false)
    shiftAlgorithm.run(taskManager.createLength(2))
    shiftAlgorithm.commit()

    assertEquals(TestSetupHelper.newWendesday(), task1.start)
    assertEquals(TestSetupHelper.newThursday(), task2.start)
    assertEquals(TestSetupHelper.newThursday(), task3.start)
    assertEquals(TestSetupHelper.newWendesday(), task4.start)
  }
}