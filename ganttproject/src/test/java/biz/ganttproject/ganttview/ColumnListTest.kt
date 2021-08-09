package biz.ganttproject.ganttview

import biz.ganttproject.core.table.ColumnList
import net.sourceforge.ganttproject.task.CustomColumnsManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @author dbarashev@bardsoftware.com
 */
class ColumnListTest {
  @Test
  fun `import - imported subset of current`() {
    val storage = mutableListOf(
      ColumnList.ColumnStub("tpd1", "id", true, 1, 30),
      ColumnList.ColumnStub("tpd2", "name", true, 2, 30),
      ColumnList.ColumnStub("tpd3", "begin", true, 3, 30),
      ColumnList.ColumnStub("tpd4", "duration", true, 4, 30),
    )
    val currentList = ColumnListImpl(storage, CustomColumnsManager()) { emptyList() }

    val storage1 = mutableListOf(
      ColumnList.ColumnStub("tpd2", "name", true, 1, 30),
      ColumnList.ColumnStub("tpd4", "duration", true, 2, 30),
    )
    currentList.importData(ColumnListImpl(storage1, CustomColumnsManager()) { emptyList() }, false)
    assertEquals(storage, storage1)
  }

  @Test
  fun `import - imported superset of current`() {
    val storage = mutableListOf(
      ColumnList.ColumnStub("tpd2", "name", true, 1, 30),
      ColumnList.ColumnStub("tpd4", "duration", true, 2, 30),
    )
    val currentList = ColumnListImpl(storage, CustomColumnsManager()) { emptyList() }

    val storage1 = mutableListOf(
      ColumnList.ColumnStub("tpd1", "id", true, 1, 30),
      ColumnList.ColumnStub("tpd2", "name", true, 2, 30),
      ColumnList.ColumnStub("tpd3", "begin", true, 3, 30),
      ColumnList.ColumnStub("tpd4", "duration", true, 4, 30),
    )
    currentList.importData(ColumnListImpl(storage1, CustomColumnsManager()) { emptyList() }, false)
    assertEquals(storage, storage1)
  }

}
