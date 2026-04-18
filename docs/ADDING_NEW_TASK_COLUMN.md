# How to Add a New Column to the Task Model

This guide was produced by Claude AI. It documents the steps required to add a new column to the task model in GanttProject, 
based on the implementation of the `IS_CRITICAL` column.

## Overview

Adding a new task column requires changes across multiple layers:
1. Core model definition
2. Database schema and functions
3. UI table rendering
4. CSV import/export
5. Calculated columns support

## Step-by-Step Implementation

### 1. Define the Column in TaskDefaultColumn Enum

**File:** `biz.ganttproject.core/src/main/java/biz/ganttproject/core/model/task/TaskDefaultColumn.java`

Add a new enum value with:
- Column stub ID (e.g., `tpd18`)
- Visibility default
- Width
- Value class (e.g., `Boolean.class`)
- Localization key
- Editability function
- Iconified flag

```java
IS_CRITICAL(new ColumnList.ColumnStub("tpd18", null, false, -1, 20),
            Boolean.class,
            "tableColIsCritical",
            Functions.NOT_EDITABLE,
            false);
```

### 2. Update Database Schema

#### 2.1 Add Column to Task Table

**File:** `ganttproject/src/main/resources/resources/sql/init-project-database.sql`

Add the column definition to the `Task` table:

```sql
is_critical boolean not null DEFAULT false,
```

#### 2.2 Add Column to View

Update the `TaskViewForComputedColumns` view:

```sql
CREATE VIEW TaskViewForComputedColumns AS
SELECT
    -- other columns...
    false AS is_critical,
    -- more columns...
```

Note: The view uses a placeholder value (`false`) since the actual value comes from calculated functions.

#### 2.3 Create H2 Function

This and subsequent steps in this section assume that the column is _calculated_. They are not necessary if the column
is a _stored_ one.

**File:** `ganttproject/src/main/java/net/sourceforge/ganttproject/storage/H2Functions.kt`

Add a Kotlin function that retrieves the value from the task object:

```kotlin
fun taskIsCritical(taskId: Int): Boolean {
  return H2Functions.taskManager.get()?.getTask(taskId)?.isCritical ?: false
}
```

#### 2.4 Register the Function Alias

**File:** `ganttproject/src/main/resources/resources/sql/init-project-database-step2.sql`

Create an SQL alias for the function:

```sql
CREATE ALIAS IF NOT EXISTS TASK_IS_CRITICAL FOR "net.sourceforge.ganttproject.storage.H2FunctionsKt.taskIsCritical";
```

#### 2.5 Update Calculated Columns

**File:** `ganttproject/src/main/resources/resources/sql/update-builtin-calculated-columns.sql`

Add the column to the UPDATE statement:

```sql
UPDATE Task SET end_date=TASK_END_DATE(num), cost=TASK_COST(num), is_critical=TASK_IS_CRITICAL(num);
```

### 3. Enable Column in Filters and Calculated Columns

**File:** `ganttproject/src/main/java/biz/ganttproject/customproperty/CalculationMethod.kt`

Add the database column name to `ourTaskTableFields`:

```kotlin
private val ourTaskTableFields: List<String> = Tables.TASKVIEWFORCOMPUTEDCOLUMNS.run {
  listOf(
    COLOR.name, COST_MANUAL_VALUE.name, COMPLETION.name, DURATION.name,
    EARLIEST_START_DATE.name, IS_CRITICAL.name, IS_COST_CALCULATED.name,
    // ... more fields
  )
}
```

### 4. Implement Table Model Support

**File:** `ganttproject/src/main/java/biz/ganttproject/ganttview/TaskTableModel.kt`

#### 4.1 Add getValue Case

Implement value retrieval in `getValueAt`:

```kotlin
TaskDefaultColumn.IS_CRITICAL -> t.isCritical
```

#### 4.2 Add setValue Case (if editable)

If the column is editable, implement value setting in `setValue`:

```kotlin
TaskDefaultColumn.IS_CRITICAL -> {
  // Implementation for setting the value
}
```

### 5. Add UI Column Builder Support

This section makes sense if the column value class is not yet supported in the task table UI.

**File:** `ganttproject/src/main/java/biz/ganttproject/core/table/ColumnBuilder.kt`

Add type-specific column creation logic. For Boolean columns:

```kotlin
modelColumn.valueClass == java.lang.Boolean::class.java -> {
  createBooleanColumn<NodeType>(
    modelColumn.getName(),
    getValue = { tableModel.getValueAt(it, modelColumn) as? Boolean? },
    setValue = { node, value ->
      undoManager.undoableEdit("Edit properties") {
        tableModel.setValue(value, node, modelColumn)
      }
    }
  )
}
```

### 6. Add CSV Export Support

**File:** `ganttproject/src/main/java/biz/ganttproject/impex/csv/GanttCSVExport.java`

Add a case in the export switch statement:

```java
case IS_CRITICAL:
  writer.print(task.isCritical());
  break;
```

### 7. Add Column Formatter Support

**File:** `ganttproject/src/main/java/biz/ganttproject/task/TaskColumnFormatter.kt`

Add formatting logic for the column:

```kotlin
TaskDefaultColumn.IS_CRITICAL -> null  // or custom formatting
```

### 8. Update CSV Options (Optional)

**File:** `ganttproject/src/main/java/net/sourceforge/ganttproject/io/CSVOptions.kt`

The column will be automatically included in CSV export options through the enum iteration.

## Summary Checklist

When adding a new task column, ensure you:

- [ ] Add enum value to `TaskDefaultColumn`
- [ ] Add database column to `Task` table schema
- [ ] Add column to `TaskViewForComputedColumns` view
- [ ] Create H2 function in `H2Functions.kt`
- [ ] Register function alias in `init-project-database-step2.sql`
- [ ] Update calculated columns SQL
- [ ] Add field to `ourTaskTableFields` for filters
- [ ] Implement `getValueAt` in `TaskTableModel`
- [ ] Implement `setValue` in `TaskTableModel` (if editable)
- [ ] Add column builder support in `ColumnBuilder.kt`
- [ ] Add CSV export case in `GanttCSVExport.java`
- [ ] Add formatter in `TaskColumnFormatter.kt`
- [ ] Add localization key to resource bundles

## Files Modified

Based on the `IS_CRITICAL` column implementation:

1. `biz.ganttproject.core/src/main/java/biz/ganttproject/core/model/task/TaskDefaultColumn.java`
2. `ganttproject/src/main/java/biz/ganttproject/core/table/ColumnBuilder.kt`
3. `ganttproject/src/main/java/biz/ganttproject/customproperty/CalculationMethod.kt`
4. `ganttproject/src/main/java/biz/ganttproject/ganttview/TaskTableModel.kt`
5. `ganttproject/src/main/java/biz/ganttproject/impex/csv/GanttCSVExport.java`
6. `ganttproject/src/main/java/biz/ganttproject/task/TaskColumnFormatter.kt`
7. `ganttproject/src/main/java/net/sourceforge/ganttproject/io/CSVOptions.kt`
8. `ganttproject/src/main/java/net/sourceforge/ganttproject/storage/H2Functions.kt`
9. `ganttproject/src/main/resources/resources/sql/init-project-database-step2.sql`
10. `ganttproject/src/main/resources/resources/sql/init-project-database.sql`
11. `ganttproject/src/main/resources/resources/sql/update-builtin-calculated-columns.sql`

## Notes

- Column IDs follow the pattern `tpdXX` where XX is an incrementing number
- Boolean columns automatically get checkbox renderers in the UI
- Date columns should handle null values appropriately
- Always provide appropriate default values in SQL schema
- Localization keys should be added to resource bundles (not shown in this diff)
