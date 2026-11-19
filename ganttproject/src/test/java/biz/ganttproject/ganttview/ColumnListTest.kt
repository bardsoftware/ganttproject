package biz.ganttproject.ganttview

import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.lib.fx.BuiltinColumns
import biz.ganttproject.lib.fx.ColumnListImpl
import biz.ganttproject.lib.fx.copyOf
import biz.ganttproject.customproperty.CustomColumnsManager
import biz.ganttproject.customproperty.CustomPropertyClass
import javafx.scene.control.TreeTableColumn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
    TaskDefaultColumn.entries
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
    val col2 = currentList.columns()
    assertEquals(storage1, col2)
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
    val col2 = currentList.columns()
    assertEquals(storage1, col2)
  }

  /**
   * The measured gap (`2026-10-08-import-spalten-sichtbarkeit.md`): both existing cases above call
   * `importData` with `keepVisibleColumns = false` and only built-in `tpd*` columns. The call the
   * `.gan` import actually makes -- `keepVisibleColumns = true` together with a custom column -- was
   * untested, and that is exactly the combination that drops a previously visible custom column the
   * imported list does not mention at all.
   */
  @Test
  fun `import with keepVisibleColumns=true does not drop a previously visible custom column`() {
    val customPropertyManager = CustomColumnsManager()
    val targetDef = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "Zielspalte", null)
    val sourceDef = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "Quellspalte", null)

    val targetColumn = ColumnList.ColumnStub(targetDef.id, "Zielspalte", true, 1, 75)
    val storage = mutableListOf<ColumnList.Column>(targetColumn)
    val fxColumn = TreeTableColumn<Any, Any>().apply {
      isVisible = true
      userData = targetColumn
    }
    val currentList = ColumnListImpl(
      columnList = storage,
      customPropertyManager = customPropertyManager,
      tableColumns = { listOf(fxColumn) },
      builtinColumns = builtinColumns
    )

    val importedSource = mutableListOf<ColumnList.Column>(
      ColumnList.ColumnStub(sourceDef.id, "Quellspalte", true, 1, 75),
    )
    currentList.importData(
      ColumnListImpl(
        columnList = importedSource, customPropertyManager = customPropertyManager,
        tableColumns = { emptyList() }, builtinColumns = builtinColumns
      ),
      true
    )

    val visibleIds = currentList.columns().filter { it.isVisible }.map { it.id }.toSet()
    assertTrue(visibleIds.contains(targetDef.id), "Zielspalte wurde abgeschnitten statt angehaengt: $visibleIds")
    assertEquals(setOf(targetDef.id, sourceDef.id), visibleIds)
  }

}
