/*
 * Copyright 2026 BarD Software s.r.o., Dmitry Barashev.
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
package net.sourceforge.ganttproject.storage

import biz.ganttproject.customproperty.CustomPropertyHolder
import biz.ganttproject.customproperty.CustomPropertyManager
import org.jooq.Field


/**
 * Builds a map of table fields to field value for the custom properties in the holder.
 */
internal fun mapCustomPropertiesToJooq(customPropertyManager: CustomPropertyManager, customProperties: CustomPropertyHolder): Map<Field<*>, Any?> {
  val id2value = customProperties.customProperties.associate { it.definition.id to it.value }
  return customPropertyManager.definitions.filter { !it.isCalculated() }.associate { it.asField() to it.asSqlValue(id2value[it.id] ?: it.defaultValue) }
}
