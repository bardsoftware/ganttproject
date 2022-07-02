/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package biz.ganttproject.customproperty

import net.sourceforge.ganttproject.task.Task

class CustomPropertyEvent {
  val type: Int
  var definition: CustomPropertyDefinition
    private set
  var oldValue: CustomPropertyDefinition? = null
    private set

  constructor(type: Int, definition: CustomPropertyDefinition) {
    this.type = type
    this.definition = definition
  }

  constructor(type: Int, def: CustomPropertyDefinition, oldDef: CustomPropertyDefinition?) {
    this.type = type
    definition = def
    oldValue = oldDef
  }

  companion object {
    const val EVENT_ADD = 0
    const val EVENT_REMOVE = 1
    const val EVENT_REBUILD = 2
    const val EVENT_NAME_CHANGE = 3
    const val EVENT_TYPE_CHANGE = 4
  }
}

sealed class CustomPropertyValueEvent(val def: CustomPropertyDefinition)
class CustomPropertyValueEventStub(def: CustomPropertyDefinition): CustomPropertyValueEvent(def)
class TaskCustomPropertyValueEvent(def: CustomPropertyDefinition, val task: Task): CustomPropertyValueEvent(def)