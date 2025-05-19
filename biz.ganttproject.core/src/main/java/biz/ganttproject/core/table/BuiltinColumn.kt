package biz.ganttproject.core.table

import biz.ganttproject.core.table.ColumnList.Column

interface BuiltinColumn {
  val valueClass: Class<*>
  fun getName(): String
  fun <NodeType> isEditable(node: NodeType): Boolean
  val isIconified: Boolean
  val stub: Column
}
