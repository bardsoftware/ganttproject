package biz.ganttproject.ganttview

import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.lib.fx.BuiltinColumns
import biz.ganttproject.lib.fx.ColumnListImpl
import biz.ganttproject.lib.fx.copyOf
import net.sourceforge.ganttproject.task.CustomColumnsManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @author dbarashev@bardsoftware.com
 */
class ColumnListTest {
  private val builtinColumns = BuiltinColumns(
  isZeroWidth = {
    when (TaskDefaultColumn.find(it)) {
      TaskDefaultColumn.COLOR, TaskDefaultColumn.INFO -> true
      else -> false
    }
  },
  allColumns = {
    ColumnList.Immutable.fromList(TaskDefaultColumn.getColumnStubs()).copyOf()
  }
  )

  @Test
  fun `import - imported subset of current`() {
    val storage = mutableListOf<ColumnList.Column>(
      ColumnList.ColumnStub("tpd1", "id", true, 1, 30),
      ColumnList.ColumnStub("tpd2", "name", true, 2, 30),
      ColumnList.ColumnStub("tpd3", "begin", true, 3, 30),
      ColumnList.ColumnStub("tpd4", "duration", true, 4, 30),
    )
    val currentList = ColumnListImpl(columnList = storage, customPropertyManager = CustomColumnsManager(), tableColumns = { emptyList() }, builtinColumns = builtinColumns)

    val storage1 = mutableListOf<ColumnList.Column>(
      ColumnList.ColumnStub("tpd2", "name", true, 1, 30),
      ColumnList.ColumnStub("tpd4", "duration", true, 2, 30),
    )
    currentList.importData(ColumnListImpl(
      columnList = storage1, customPropertyManager = CustomColumnsManager(), tableColumns = { emptyList() },
      builtinColumns = builtinColumns),
      false
    )
    assertEquals(storage, storage1)
  }

  @Test
  fun `import - imported superset of current`() {
    val storage = mutableListOf<ColumnList.Column>(
      ColumnList.ColumnStub("tpd2", "name", true, 1, 30),
      ColumnList.ColumnStub("tpd4", "duration", true, 2, 30),
    )
    val currentList = ColumnListImpl(columnList = storage, customPropertyManager = CustomColumnsManager(),
      tableColumns = { emptyList() },
      builtinColumns = builtinColumns
    )
    val storage1 = mutableListOf<ColumnList.Column>(
      ColumnList.ColumnStub("tpd1", "id", true, 1, 30),
      ColumnList.ColumnStub("tpd2", "name", true, 2, 30),
      ColumnList.ColumnStub("tpd3", "begin", true, 3, 30),
      ColumnList.ColumnStub("tpd4", "duration", true, 4, 30),
    )
    currentList.importData(ColumnListImpl(columnList = storage1, customPropertyManager = CustomColumnsManager(),
      tableColumns =  { emptyList() }, builtinColumns = builtinColumns), false)
    assertEquals(storage, storage1)
  }

}
