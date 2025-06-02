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
package biz.ganttproject.core.table

import biz.ganttproject.core.table.ColumnList.Column
import biz.ganttproject.customproperty.CustomPropertyClass

/**
 * This interface represents meta-information of a built-in property of a model element (tasks or resources) for the
 * purposes of representing the property in a table.
 *
 * Built-in properties are those that are not user-defined, such as task name, dates, resource email, etc.
 */
interface BuiltinColumn {
  /**
   * Property class, represented as Java class. Must be one of the supported classes:
   * java.lang.String, java.util.GregorianCalendar, java.lang.Double, java.lang.Integer, java.math.BigDecimal
   */
  val valueClass: Class<*>

  val columnClass: CustomPropertyClass
  /**
   * Localized property name to be shown in the UI, e.g. in the table header.
   */
  fun getName(): String

  /**
   * Whether this property values are shown as icons in the table next to the model element name.
   */
  val isIconified: Boolean

  /**
   * Returns true if this property is editable for the given table node.
   * @param <NodeType> Type of the table node.
   */
  fun <NodeType> isEditable(node: NodeType): Boolean

  val stub: Column
}
