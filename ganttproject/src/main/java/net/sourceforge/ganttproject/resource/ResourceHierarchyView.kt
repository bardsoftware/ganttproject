/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

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
package net.sourceforge.ganttproject.resource

interface ResourceHierarchyView {
  fun canMoveUp(resources: List<HumanResource>): Boolean
  fun moveUp(resources: List<HumanResource>)

  fun canMoveDown(resources: List<HumanResource>): Boolean
  fun moveDown(resources: List<HumanResource>)
}

class ResourceHierarchyViewImpl(
  private val resourceList: MutableList<HumanResource>, private val onChange: ()->Unit) : ResourceHierarchyView {
  override fun canMoveUp(resources: List<HumanResource>): Boolean {
    return resources.all { resourceList.indexOf(it) > 0 }
  }

  override fun moveUp(resources: List<HumanResource>) {
    resources.map {it to resourceList.indexOf(it)}.sortedBy { it.second }.forEach { hr2idx ->
      val prev = resourceList[hr2idx.second - 1]
      resourceList[hr2idx.second] = prev
      resourceList[hr2idx.second - 1] = hr2idx.first
    }
    onChange()
  }

  override fun canMoveDown(resources: List<HumanResource>): Boolean {
    return resources.all { resourceList.indexOf(it) < resourceList.size - 1 }
  }

  override fun moveDown(resources: List<HumanResource>) {
    resources.map {it to resourceList.indexOf(it)}.sortedBy { it.second }.reversed().forEach { hr2idx ->
      val next = resourceList[hr2idx.second + 1]
      resourceList[hr2idx.second] = next
      resourceList[hr2idx.second + 1] = hr2idx.first
    }
    onChange()
  }

}