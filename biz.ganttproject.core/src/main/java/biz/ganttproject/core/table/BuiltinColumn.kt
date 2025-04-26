package biz.ganttproject.core.table

interface BuiltinColumn {
  val valueClass: Class<*>
  fun getName(): String
  fun <NodeType> isEditable(node: NodeType): Boolean
  val isIconified: Boolean
}
