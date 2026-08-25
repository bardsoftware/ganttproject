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

import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.customproperty.CustomPropertyClass
import biz.ganttproject.customproperty.CustomPropertyDefinition
import biz.ganttproject.customproperty.CustomPropertyHolder
import biz.ganttproject.storage.db.Tables.TASK
import net.sourceforge.ganttproject.storage.ProjectDatabase.TaskUpdateBuilder
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.util.ColorConvertion
import org.jooq.Field
import org.jooq.SQLDialect
import org.jooq.conf.ParamType
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import java.awt.Color
import java.time.LocalDate
import java.util.Calendar

/**
 * Builds a single UPDATE statement that writes the changed task properties into the in-memory H2 database.
 *
 * Unlike [SqlTaskUpdateBuilder], this class is not concerned with the Colloboque updates at all. Consequently,
 * it writes the values of the stored custom properties into the columns of the Task table (this is where Colloboque
 * agnostic code, such as calculated columns, expects to find them), rather than into the TaskCustomColumn table.
 *
 * All the property values which change in a single mutator session are written with a single UPDATE statement.
 * This is essential for the calculated columns: H2 re-evaluates the generated columns of a row when the row is
 * updated, so all the columns which a calculation depends on shall be up-to-date by that moment.
 */
class H2TaskUpdateBuilder(
  private val task: Task,
  // Code that runs the generated statements against the database.
  private val onCommit: (List<String>) -> Unit,
  private val dialect: SQLDialect
): TaskUpdateBuilder {

  // The new values of the Task table columns, in the order of the setter calls.
  private val newValues = LinkedHashMap<Field<*>, Any?>()
  // The new values of the task custom properties, if they have been changed in this session.
  private var newCustomProperties: CustomPropertyHolder? = null

  private fun <T> append(field: Field<T>, newValue: Any?) {
    newValues[field] = newValue
  }

  /**
   * Appends the values of the stored (that is, not calculated) custom properties to the update statement.
   * The properties which are missing in the holder are reset to NULL, because the holder keeps all the values
   * of the task after the change.
   */
  private fun appendCustomProperties(customProperties: CustomPropertyHolder) {
    val id2value = customProperties.customProperties.associate { it.definition.id to it.value }
    task.manager.customPropertyManager.definitions.filter { !it.isCalculated() }.forEach { def ->
      append(def.asField(), def.asSqlValue(id2value[def.id] ?: def.defaultValue))
    }
  }

  @Throws(ProjectDatabaseException::class)
  override fun commit() {
    newCustomProperties?.let { appendCustomProperties(it) }
    if (newValues.isEmpty()) {
      return
    }
    val statement = DSL.using(dialect)
      .update(TASK)
      .set(newValues)
      .where(TASK.UID.eq(task.uid))
      .getSQL(ParamType.INLINED)
    onCommit(listOf(statement))
  }

  override fun setColor(oldValue: Color?, newValue: Color?) =
    append(TASK.COLOR, newValue?.let(ColorConvertion::getColor))

  override fun setCompletionPercentage(oldValue: Int, newValue: Int) = append(TASK.COMPLETION, newValue)

  override fun setCost(oldValue: Task.Cost, newValue: Task.Cost) {
    append(TASK.IS_COST_CALCULATED, newValue.isCalculated)
    append(TASK.COST_MANUAL_VALUE, newValue.manualValue)
    append(TASK.COST, newValue.value)
  }

  override fun setCritical(oldValue: Boolean, newValue: Boolean) {
    // The value of this property is calculated by the scheduler and is written into the database by
    // ProjectDatabase::updateBuiltInCalculatedColumns
  }

  override fun setCustomProperties(oldCustomProperties: CustomPropertyHolder, newCustomProperties: CustomPropertyHolder) {
    this.newCustomProperties = newCustomProperties
  }

  override fun setDuration(oldValue: TimeDuration, newValue: TimeDuration) = append(TASK.DURATION, newValue.length)

  override fun setEarliestStart(oldValue: GanttCalendar?, newValue: GanttCalendar?) =
    append(TASK.EARLIEST_START_DATE, newValue?.toLocalDate())

  override fun setMilestone(oldValue: Boolean, newValue: Boolean) = append(TASK.IS_MILESTONE, newValue)

  override fun setName(oldName: String?, newName: String?) = append(TASK.NAME, newName)

  override fun setNotes(oldValue: String?, newValue: String?) = append(TASK.NOTES, newValue)

  override fun setPriority(oldValue: Task.Priority?, newValue: Task.Priority?) =
    append(TASK.PRIORITY, newValue?.persistentValue)

  override fun setProjectTask(oldValue: Boolean, newValue: Boolean) = append(TASK.IS_PROJECT_TASK, newValue)

  override fun setShape(oldValue: ShapePaint?, newValue: ShapePaint?) = append(TASK.SHAPE, newValue?.array)

  override fun setStart(oldValue: GanttCalendar, newValue: GanttCalendar) =
    append(TASK.START_DATE, newValue.toLocalDate())

  override fun setEnd(oldValue: GanttCalendar?, newValue: GanttCalendar) = append(TASK.END_DATE, newValue.toLocalDate())

  override fun setWebLink(oldValue: String?, newValue: String?) = append(TASK.WEB_LINK, newValue)
}

/**
 * The column of the Task table where the values of this custom property are stored.
 */
internal fun CustomPropertyDefinition.asField(): Field<*> = DSL.field(DSL.name(this.id), when (this.propertyClass) {
  CustomPropertyClass.TEXT -> SQLDataType.VARCHAR
  CustomPropertyClass.INTEGER -> SQLDataType.INTEGER
  CustomPropertyClass.DOUBLE -> SQLDataType.DOUBLE
  CustomPropertyClass.DATE -> SQLDataType.LOCALDATE
  CustomPropertyClass.BOOLEAN -> SQLDataType.BOOLEAN
})

/**
 * Converts a custom property value, as it is kept in the task custom values, into a value which can be written
 * into the corresponding column of the Task table.
 */
internal fun CustomPropertyDefinition.asSqlValue(value: Any?): Any? = value?.let {
  when (this.propertyClass) {
    CustomPropertyClass.DOUBLE -> (value as? Number)?.toDouble() ?: value
    CustomPropertyClass.DATE -> when (value) {
      is GanttCalendar -> value.toLocalDate()
      is Calendar -> LocalDate.of(
        value.get(Calendar.YEAR), value.get(Calendar.MONTH) + 1, value.get(Calendar.DAY_OF_MONTH)
      )
      else -> value
    }
    else -> value
  }
}
