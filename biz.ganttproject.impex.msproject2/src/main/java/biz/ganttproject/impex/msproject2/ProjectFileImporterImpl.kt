/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
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
package biz.ganttproject.impex.msproject2

import biz.ganttproject.core.calendar.CalendarEvent
import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.calendar.GPCalendarCalc
import biz.ganttproject.core.calendar.GanttDaysOff
import biz.ganttproject.core.calendar.walker.WorkingUnitCounter
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import biz.ganttproject.core.time.impl.GregorianTimeUnitStack
import biz.ganttproject.customproperty.CustomColumnsException
import biz.ganttproject.customproperty.CustomPropertyClass
import biz.ganttproject.customproperty.CustomPropertyDefinition
import biz.ganttproject.impex.msproject2.ProjectFileImporter.HolidayAdder
import com.google.common.base.Function
import com.google.common.base.Preconditions
import com.google.common.collect.Lists
import net.sf.mpxj.*
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.GanttTask
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.CostStub
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskManager.TaskBuilder
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException
import net.sourceforge.ganttproject.task.dependency.constraint.FinishFinishConstraintImpl
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl
import net.sourceforge.ganttproject.task.dependency.constraint.StartFinishConstraintImpl
import net.sourceforge.ganttproject.task.dependency.constraint.StartStartConstraintImpl
import net.sourceforge.ganttproject.util.collect.Pair
import java.math.BigDecimal
import java.text.MessageFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*
import java.util.logging.Level
import java.util.regex.Pattern

/**
 * Some bits of data that are used for the analysis if import was fine and for hiding some trash from user.
 */
data class ImportResult(
  // MS Project (or is it MPXJ?) creates custom properties for each possible custom field name. We filter out those
  // that contain just a single value.
  val singleValueCustomPropertyNames: Set<String>,

  // A map of the imported task into its original start date. Used to show a warning that some tasks changed their dates.
  val originalStartDates: Map<GanttTask, Date>
)

class ProjectFileImporterImpl(private val myProjectFile: ProjectFile, private val myNativeProject: IGanttProject) {
  private val myNativeTask2foreignStart = mutableMapOf<GanttTask, Date>()
  private val myErrors = mutableListOf<Pair<Level, String>>()
  private var myResourceCustomPropertyMapping: MutableMap<ResourceField, CustomPropertyDefinition> = mutableMapOf()
  private var myTaskCustomPropertyMapping: MutableMap<MpxjTaskField, CustomPropertyDefinition> = mutableMapOf()
  private val singleValueCustomProperties = mutableMapOf<String, Any?>()

  private fun getTaskManager(): TaskManager {
    return myNativeProject.taskManager
  }

  fun run(): ImportResult {
    val pf = myProjectFile
    val foreignId2nativeTask = mutableMapOf<Int, GanttTask>()
    val foreignId2nativeResource = mutableMapOf<Int, HumanResource>()
    importCalendar(pf)
    importResources(pf, foreignId2nativeResource)

    importTasks(pf, foreignId2nativeTask, myNativeTask2foreignStart)
    importDependencies(pf, foreignId2nativeTask)
    val leafTasks: MutableList<Task> = Lists.newArrayList()
    for (task in foreignId2nativeTask.values) {
      if (!getTaskManager().getTaskHierarchy().hasNestedTasks(task)) {
        leafTasks.add(task)
      }
    }
    myNativeProject.taskManager.getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().run(leafTasks)
    importResourceAssignments(pf, foreignId2nativeTask, foreignId2nativeResource)
    return ImportResult(
      singleValueCustomPropertyNames = singleValueCustomProperties.filter { it.value == null }.keys,
      originalStartDates = myNativeTask2foreignStart
    )

  }

  class HolidayAdderImpl(private val e: ProjectCalendarException, private val holidays: MutableList<CalendarEvent>): HolidayAdder {
    override fun addHoliday(date: Date, title: Optional<String>) {
      holidays.add(
        CalendarEvent.newEvent(
          date, false,
          if (e.working) CalendarEvent.Type.WORKING_DAY else CalendarEvent.Type.HOLIDAY, title.orElse(null), null
        )
      )
    }

    override fun addYearlyHoliday(date: Date, title: Optional<String>) {
      holidays.add(
        CalendarEvent.newEvent(
          date,
          true,
          if (e.working) CalendarEvent.Type.WORKING_DAY else CalendarEvent.Type.HOLIDAY,
          title.orElse(null),
          null
        )
      ) }

  }

  private fun importCalendar(pf: ProjectFile) {
    val defaultCalendar = pf.defaultCalendar ?: return
    importWeekends(defaultCalendar)
    val exceptions = defaultCalendar.calendarExceptions
    val holidays = mutableListOf<CalendarEvent>()
    for (e in exceptions) {
      importHolidays(e, HolidayAdderImpl(e, holidays))
    }
    getNativeCalendar().setPublicHolidays(holidays)
  }

  private fun importWeekends(calendar: ProjectCalendar) {
    importDayType(calendar, DayOfWeek.MONDAY, Calendar.MONDAY)
    importDayType(calendar, DayOfWeek.TUESDAY, Calendar.TUESDAY)
    importDayType(calendar, DayOfWeek.WEDNESDAY, Calendar.WEDNESDAY)
    importDayType(calendar, DayOfWeek.THURSDAY, Calendar.THURSDAY)
    importDayType(calendar, DayOfWeek.FRIDAY, Calendar.FRIDAY)
    importDayType(calendar, DayOfWeek.SATURDAY, Calendar.SATURDAY)
    importDayType(calendar, DayOfWeek.SUNDAY, Calendar.SUNDAY)
  }

  private fun importDayType(foreignCalendar: ProjectCalendar, foreignDay: DayOfWeek, nativeDay: Int) {
    getNativeCalendar().setWeekDayType(
      nativeDay,
      if (foreignCalendar.isWorkingDay(foreignDay)) GPCalendar.DayType.WORKING else GPCalendar.DayType.WEEKEND
    )
  }

  private fun getNativeCalendar(): GPCalendarCalc {
    return myNativeProject.activeCalendar
  }

  private fun importHolidays(e: ProjectCalendarException, adder: HolidayAdder) {
    val recurringData = e.recurring
    if (recurringData != null) {
      when (recurringData.recurrenceType) {
        RecurrenceType.DAILY -> importDailyHoliday(e, adder)
        RecurrenceType.YEARLY -> importYearlyHoliday(e, adder)
        else -> myErrors.add(
          Pair.create<Level, String>(
            Level.WARNING,
            String.format("Skipped calendar exception:\n%s", e)
          )
        )
      }
    } else {
      importHolidays(
        e.fromDate, e.toDate,
        Optional.ofNullable(e.name), adder
      )
    }
  }

  private fun importYearlyHoliday(e: ProjectCalendarException, adder: HolidayAdder) {
    val recurringData = e.recurring
    val date = CalendarFactory.createGanttCalendar(1, recurringData.monthNumber - 1, recurringData.dayNumber).time
    adder.addYearlyHoliday(date, Optional.ofNullable(e.name))
  }

  private fun importDailyHoliday(e: ProjectCalendarException, adder: HolidayAdder) {
    val recurringData = e.recurring
    if (recurringData.useEndDate) {
      importHolidays(
        recurringData.startDate, recurringData.finishDate,
        Optional.ofNullable(e.name), adder
      )
    } else {
      importHolidays(
        recurringData.startDate, recurringData.occurrences,
        Optional.ofNullable(e.name), adder
      )
    }
  }

  private fun importHolidays(
    start: LocalDate, occurrences: Int, title: Optional<String>, adder: HolidayAdder
  ) {
    var occurrences = occurrences
    val oneDay: TimeDuration = getTaskManager().createLength(GregorianTimeUnitStack.DAY, 1.0f)
    var dayStart = ProjectFileImporter.toJavaDate(start)
    while (occurrences > 0) {
      adder.addHoliday(dayStart, title)
      dayStart = GPCalendarCalc.PLAIN.shiftDate(dayStart, oneDay)
      occurrences--
    }
  }

  private fun importHolidays(
    start: LocalDate, end: LocalDate, title: Optional<String>, adder: HolidayAdder
  ) {
    val oneDay: TimeDuration = getTaskManager().createLength(GregorianTimeUnitStack.DAY, 1.0f)
    val javaEndDate = ProjectFileImporter.toJavaDate(end)
    var dayStart = ProjectFileImporter.toJavaDate(start)
    while (!dayStart.after(javaEndDate)) {
      adder.addHoliday(dayStart, title)
      dayStart = GPCalendarCalc.PLAIN.shiftDate(dayStart, oneDay)
    }
  }

  private fun importResources(pf: ProjectFile, foreignId2humanResource: MutableMap<Int, HumanResource>) {
    myResourceCustomPropertyMapping = HashMap<ResourceField, CustomPropertyDefinition>()
    for (r: Resource in pf.resources) {
      val nativeResource = myNativeProject.humanResourceManager.newHumanResource()
      nativeResource.id = r.uniqueID
      nativeResource.name = r.name
      nativeResource.mail = r.emailAddress
      val standardRate = r.standardRate
      if (((standardRate != null) && standardRate.amount != 0.0) && standardRate.units == TimeUnit.DAYS) {
        nativeResource.standardPayRate = BigDecimal.valueOf(standardRate.amount)
      }
      myNativeProject.humanResourceManager.add(nativeResource)
      importDaysOff(r, nativeResource)
      importCustomProperties(r, nativeResource)
      foreignId2humanResource[r.uniqueID] = nativeResource
    }
  }

  private fun importCustomProperties(r: Resource, nativeResource: HumanResource) {
    for (rf: ResourceField in ResourceField.entries) {
      if (r[rf] == null || isNotCustomField(rf)) {
        continue
      }
      var def = myResourceCustomPropertyMapping.get(rf) ?: run {
        convertDataType(rf)?.let { propertyClass ->
          var name = r.parentFile.customFields[rf].alias
          if (name == null) {
            name = rf.name
          }
          myNativeProject.resourceCustomPropertyManager.createDefinition(propertyClass, name, null).also {
            it.attributes.put(CustomPropertyMapping.MSPROJECT_TYPE, rf.name)
            myResourceCustomPropertyMapping.put(rf, it)
          }
        }
      }
      if (def != null) {
        try {
          nativeResource.setValue(def, convertDataValue(rf, r[rf]))
        } catch (e: CustomColumnsException) {
          throw RuntimeException(e)
        }
      }
    }
  }

  class HolidayAdderResource(private val nativeResource: HumanResource): HolidayAdder {
    override fun addHoliday(date: Date, title: Optional<String>) {
      nativeResource.addDaysOff(GanttDaysOff(date, GregorianTimeUnitStack.DAY.adjustRight(date)))
    }

    override fun addYearlyHoliday(date: Date?, title: Optional<String>?) {
      TODO("Not yet implemented")
    }

  }
  private fun importDaysOff(r: Resource, nativeResource: HumanResource) {
    val c = r.calendar ?: return
    val holidayAdder = HolidayAdderResource(nativeResource)
    for (e: ProjectCalendarException in c.calendarExceptions) {
      importHolidays(e, holidayAdder)
    }
  }

  private fun importTasks(
    foreignProject: ProjectFile,
    foreignId2nativeTask: MutableMap<Int, GanttTask>,
    nativeTask2foreignStart: MutableMap<GanttTask, Date>
  ) {
    myTaskCustomPropertyMapping = mutableMapOf()
    for (t: net.sf.mpxj.Task in foreignProject.childTasks) {
      importTask(t, getTaskManager().getRootTask(), foreignId2nativeTask, nativeTask2foreignStart)
    }
  }

  private fun findDurationFunction(
    t: net.sf.mpxj.Task,
    reportBuilder: StringBuilder
  ): java.util.function.Function<net.sf.mpxj.Task, Pair<TimeDuration, TimeDuration>>? {
    if (t.start != null && t.finish != null) {
      return DURATION_FROM_START_FINISH
    }
    reportBuilder.append("start+finish not found")
    if (t.start != null && t.duration != null) {
      return DURATION_FROM_START_AND_DURATION
    }
    reportBuilder.append(", start+duration not found")
    return null
  }


  private fun importTask(
    t: net.sf.mpxj.Task, supertask: Task,
    foreignId2nativeTask: MutableMap<Int, GanttTask>, nativeTask2foreignStart: MutableMap<GanttTask, Date>
  ) {
    if (t.getNull()) {
      myErrors.add(
        Pair.create<Level, String>(
          Level.INFO,
          MessageFormat.format("Task with id={0} is blank task. Skipped", foreignId(t))
        )
      )
      return
    }
    if (t.uniqueID == 0) {
      val isRealTask = t.name != null && !t.childTasks.isEmpty()
      if (!isRealTask) {
        for (child: net.sf.mpxj.Task in t.childTasks) {
          importTask(child, getTaskManager().getRootTask(), foreignId2nativeTask, nativeTask2foreignStart)
        }
        return
      }
    }

    val report = StringBuilder()
    val getDuration = findDurationFunction(t, report)
    if (getDuration == null) {
      myErrors.add(
        Pair.create<Level, String>(
          Level.SEVERE,
          String.format("Can't determine the duration  of task %s (%s). Skipped", t, report)
        )
      )
      return
    }

    var taskBuilder: TaskBuilder = getTaskManager().newTaskBuilder()
      .withParent(supertask)
      .withName(t.name)
      .withNotes(t.notes)
      .withWebLink(t.hyperlink)
    if (t.priority != null) {
      taskBuilder = taskBuilder.withPriority(convertPriority(t.priority))
    }
    val foreignStartDate = convertStartTime(ProjectFileImporter.toJavaDate(t.start.toLocalDate()))
    if (t.childTasks.isEmpty()) {
      taskBuilder.withStartDate(foreignStartDate)
      if (t.percentageComplete != null) {
        taskBuilder.withCompletion(t.percentageComplete.toInt())
      }
      if (t.milestone) {
        taskBuilder.withLegacyMilestone()
      }
      val durations = getDuration.apply(t)

      val workingDuration = durations.first()
      val nonWorkingDuration = durations.second()
      val defaultDuration = myNativeProject.taskManager.createLength(
        myNativeProject.timeUnitStack.defaultTimeUnit, 1.0f
      )

      if (!t.milestone) {
        if (workingDuration.length > 0) {
          taskBuilder.withDuration(workingDuration)
        } else if (nonWorkingDuration.length > 0) {
          myErrors.add(
            Pair.create<Level, String>(
              Level.INFO, MessageFormat.format(
                "[FYI] Task with id={0}, name={1}, start date={2}, end date={3}, milestone={4} has working time={5} and non working time={6}.\n"
                  + "We set its duration to {6}", foreignId(t), t.name, t.start, t.finish,
                t.milestone, workingDuration, nonWorkingDuration
              )
            )
          )
          taskBuilder.withDuration(nonWorkingDuration)
        } else {
          myErrors.add(
            Pair.create<Level, String>(
              Level.INFO, MessageFormat.format(
                ("[FYI] Task with id={0}, name={1}, start date={2}, end date={3}, milestone={4} has working time={5} and non working time={6}.\n"
                  + "We set its duration to default={7}"), foreignId(t), t.name, t.start, t.finish,
                t.milestone, workingDuration, nonWorkingDuration, defaultDuration
              )
            )
          )
          taskBuilder.withDuration(defaultDuration)
        }
      } else {
        taskBuilder.withDuration(defaultDuration)
      }
    }
    val nativeTask = taskBuilder.build() as GanttTask
    if (t.cost != null) {
      nativeTask.cost = CostStub(BigDecimal.valueOf(t.cost.toDouble()), false)
    }
    if (!t.childTasks.isEmpty()) {
      for (child: net.sf.mpxj.Task in t.childTasks) {
        importTask(child, nativeTask, foreignId2nativeTask, nativeTask2foreignStart)
      }
    }
    importCustomFields(t, nativeTask)
    foreignId2nativeTask[foreignId(t)] = nativeTask
    nativeTask2foreignStart[nativeTask] = foreignStartDate
  }

  private fun convertStartTime(start: Date): Date {
    return myNativeProject.timeUnitStack.defaultTimeUnit.adjustLeft(start)
  }

  private fun importCustomFields(t: net.sf.mpxj.Task, nativeTask: GanttTask) {
    for (tf in TaskField.entries) {
      if (isNotCustomField(tf) || t[tf] == null) {
        continue
      }

      val mpxjTaskField = ProjectFileImporter.convert(tf)
      val def = myTaskCustomPropertyMapping.getOrElse(mpxjTaskField) {
        convertDataType(tf)?.let { propertyClass ->
          var name = t.parentFile.customFields[tf].alias
          if (name == null) {
            name = mpxjTaskField.name
          }

          val def = myNativeProject.taskCustomColumnManager.createDefinition(propertyClass, name, null)
          def.attributes[CustomPropertyMapping.MSPROJECT_TYPE] = mpxjTaskField.name
          myTaskCustomPropertyMapping[mpxjTaskField] = def
          def
        }
      }
      if (def != null) {
        try {
          val value = convertDataValue(tf, t[tf])
          if (value != null) {
            if (!singleValueCustomProperties.containsKey(def.name)) {
              singleValueCustomProperties[def.name] = value
            } else {
              if (value != singleValueCustomProperties[def.name]) {
                singleValueCustomProperties[def.name] = null
              }
            }
          }
          nativeTask.customValues.setValue((def), value)
        } catch (e: CustomColumnsException) {
          // TODO Auto-generated catch block
          e.printStackTrace()
        }
      }
    }
  }

  private val CUSTOM_FIELD_NAME: Pattern = Pattern.compile("^\\p{Lower}+\\p{Digit}+$")

  private fun isNotCustomField(tf: FieldType): Boolean {
    val name = ProjectFileImporter.getName(tf);
    return (name == null) || !CUSTOM_FIELD_NAME.matcher(name.lowercase(Locale.getDefault())).matches()
  }

  private fun convertDataType(tf: FieldType): CustomPropertyClass? {
    return when (tf.dataType) {
      DataType.ACCRUE, DataType.CONSTRAINT, DataType.DURATION, DataType.PRIORITY, DataType.RELATION_LIST, DataType.RESOURCE_TYPE, DataType.STRING, DataType.TASK_TYPE, DataType.UNITS -> CustomPropertyClass.TEXT

      DataType.BOOLEAN -> CustomPropertyClass.BOOLEAN
      DataType.DATE -> CustomPropertyClass.DATE
      DataType.CURRENCY, DataType.NUMERIC, DataType.PERCENTAGE, DataType.RATE -> CustomPropertyClass.DOUBLE

      else -> null
    }
  }

  private fun convertDataValue(tf: FieldType, value: Any): Any? {
    return when (tf.dataType) {
      DataType.ACCRUE, DataType.CONSTRAINT, DataType.DURATION, DataType.PRIORITY, DataType.RELATION_LIST, DataType.RESOURCE_TYPE, DataType.STRING, DataType.TASK_TYPE, DataType.UNITS -> value.toString()
      DataType.BOOLEAN -> {
        assert(value is Boolean)
        value
      }

      DataType.DATE -> {
        assert(value is Date)
        CalendarFactory.createGanttCalendar(value as Date)
      }

      DataType.CURRENCY, DataType.NUMERIC, DataType.PERCENTAGE -> {
        assert(value is Number)
        (value as Number).toDouble()
      }

      DataType.RATE -> {
        assert(value is Rate)
        (value as Rate).amount
      }

      else -> null
    }
  }

  private fun convertPriority(priority: Priority): Task.Priority {
    return when (priority.value) {
      Priority.HIGHEST, Priority.VERY_HIGH -> Task.Priority.HIGHEST
      Priority.HIGHER, Priority.HIGH -> Task.Priority.HIGH
      Priority.MEDIUM -> Task.Priority.NORMAL
      Priority.LOWER, Priority.LOW -> Task.Priority.LOW
      Priority.VERY_LOW, Priority.LOWEST -> Task.Priority.LOWEST
      else -> Task.Priority.NORMAL
    }
  }

  private fun getDurations(start: Date, end: Date): Pair<TimeDuration, TimeDuration> {
    val unitCounter = WorkingUnitCounter(
      getNativeCalendar(),
      myNativeProject.timeUnitStack.defaultTimeUnit
    )
    val workingDuration = unitCounter.run(start, end)
    val nonWorkingDuration = unitCounter.nonWorkingTime
    return Pair.create(workingDuration, nonWorkingDuration)
  }

  private val DURATION_FROM_START_FINISH: java.util.function.Function<net.sf.mpxj.Task, Pair<TimeDuration, TimeDuration>> =
    Function<net.sf.mpxj.Task, Pair<TimeDuration, TimeDuration>> { t ->
      if (t.milestone) {
        return@Function Pair.create<TimeDuration, TimeDuration>(getTaskManager().createLength(1), null)
      }
      getDurations(
        ProjectFileImporter.toJavaDate(t.start.toLocalDate()),
        myNativeProject.timeUnitStack.defaultTimeUnit.adjustRight(ProjectFileImporter.toJavaDate(t.finish.toLocalDate()))
      )
    }

  private val DURATION_FROM_START_AND_DURATION: java.util.function.Function<net.sf.mpxj.Task, Pair<TimeDuration, TimeDuration>> =
    Function<net.sf.mpxj.Task, Pair<TimeDuration, TimeDuration>> { t ->
      if (t.milestone) {
        return@Function Pair.create<TimeDuration, TimeDuration?>(getTaskManager().createLength(1), null)
      }
      val dayUnits = t.duration.convertUnits(TimeUnit.DAYS, myProjectFile.projectProperties)
      val gpDuration: TimeDuration = getTaskManager().createLength(GPTimeUnitStack.DAY, dayUnits.duration.toFloat())
      val endDate: Date = getTaskManager().shift(ProjectFileImporter.toJavaDate(t.start.toLocalDate()), gpDuration)
      getDurations(ProjectFileImporter.toJavaDate(t.start.toLocalDate()), endDate)
    }

  private fun foreignId(mpxjTask: net.sf.mpxj.Task): Int {
    var result = mpxjTask.id
    if (result != null) {
      return result
    }
    result = mpxjTask.uniqueID
    if (result != null) {
      return result
    }
    throw IllegalStateException("No ID found in task=$mpxjTask")
  }

  private fun importDependencies(pf: ProjectFile, foreignId2nativeTask: Map<Int, GanttTask>) {
    getTaskManager().getAlgorithmCollection().getScheduler().setEnabled(false)
    try {
      for (t: net.sf.mpxj.Task in pf.tasks) {
        if (t.predecessors == null) {
          continue
        }
        for (r: Relation in t.predecessors) {
          val dependant = foreignId2nativeTask[foreignId(r.sourceTask)]
          val dependee = foreignId2nativeTask[foreignId(r.targetTask)]
          if (dependant == null) {
            myErrors.add(
              Pair.create<Level, String>(
                Level.SEVERE, String.format(
                  "Failed to import relation=%s because source task=%s was not found", r, foreignId(r.sourceTask)
                )
              )
            )
            continue
          }
          if (dependee == null) {
            myErrors.add(
              Pair.create<Level, String>(
                Level.SEVERE, String.format(
                  "Failed to import relation=%s because target task=%s", t, foreignId(r.targetTask)
                )
              )
            )
            continue
          }
          try {
            val dependency: TaskDependency =
              getTaskManager().getDependencyCollection().createDependency(dependant, dependee)
            dependency.constraint = convertConstraint(r)
            if (r.lag.duration != 0.0) {
              // TODO(dbarashev): get rid of days
              dependency.difference = r.lag.convertUnits(TimeUnit.DAYS, pf.projectProperties).duration.toInt()
            }
            dependency.hardness =
              TaskDependency.Hardness.parse(getTaskManager().getDependencyHardnessOption().getValue())
          } catch (e: TaskDependencyException) {
            GPLogger.getLogger("MSProject").log(
              Level.SEVERE,
              "Failed to import relation=$r", e
            )
            myErrors.add(
              Pair.create<Level, String>(
                Level.SEVERE,
                String.format("Failed to import relation=%s: %s", r, e.message)
              )
            )
          }
        }
      }
    } finally {
      getTaskManager().getAlgorithmCollection().getScheduler().setEnabled(true)
    }
  }

  private fun convertConstraint(r: Relation): TaskDependencyConstraint {
    return when (r.type) {
      RelationType.FINISH_FINISH -> FinishFinishConstraintImpl()
      RelationType.FINISH_START -> FinishStartConstraintImpl()
      RelationType.START_FINISH -> StartFinishConstraintImpl()
      RelationType.START_START -> StartStartConstraintImpl()
    }
  }

  private fun importResourceAssignments(
    pf: ProjectFile, foreignId2nativeTask: Map<Int, GanttTask>,
    foreignId2nativeResource: Map<Int, HumanResource>
  ) {
    for (ra: ResourceAssignment in pf.resourceAssignments) {
      val nativeTask = foreignId2nativeTask[foreignId(ra.task)]
      if (nativeTask == null) {
        myErrors.add(
          Pair.create<Level, String>(
            Level.SEVERE, String.format(
              "Failed to import resource assignment=%s because its task with ID=%d  and name=%s was not found or not imported",
              ra,
              foreignId(ra.task),
              ra.task.name
            )
          )
        )
        continue
      }
      val resource = ra.resource ?: continue
      val nativeResource = foreignId2nativeResource[resource.uniqueID]
      if (nativeResource == null) {
        myErrors.add(
          Pair.create<Level, String>(
            Level.SEVERE, String.format(
              "Failed to import resource assignment=%s because its resource with ID=%d and name=%s was not found or not imported",
              ra,
              resource.uniqueID,
              resource.name
            )
          )
        )
        continue
      }
      val nativeAssignment = nativeTask.assignmentCollection.addAssignment(
        nativeResource
      )
      Preconditions.checkNotNull(nativeAssignment)
      if (ra.units == null) {
        myErrors.add(
          Pair.create<Level, String>(
            Level.INFO, String.format(
              "Units not found in resource assignment=%s. Replaced with 100", ra
            )
          )
        )
        nativeAssignment.load = 100f
      } else {
        nativeAssignment.load = ra.units.toFloat()
      }
    }
  }
}

internal data class MpxjTaskField(val name: String, val dataType: DataType)