/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
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
package biz.ganttproject.core.chart.scene.gantt

import biz.ganttproject.core.option.EnumerationOption


interface TaskLabelSceneInput {
  val topLabelOption: EnumerationOption

  val bottomLabelOption: EnumerationOption

  val leftLabelOption: EnumerationOption

  val rightLabelOption: EnumerationOption

  val fontSize: Int

  fun hasBaseline(): Boolean
}

class TaskLabelSceneInputImpl(override val fontSize: Int, private val _hasBaseline: Boolean) : TaskLabelSceneInput {
  override val topLabelOption: EnumerationOption
    get() = TODO("Not yet implemented")
  override val bottomLabelOption: EnumerationOption
    get() = TODO("Not yet implemented")
  override val leftLabelOption: EnumerationOption
    get() = TODO("Not yet implemented")
  override val rightLabelOption: EnumerationOption
    get() = TODO("Not yet implemented")

  override fun hasBaseline() = _hasBaseline

}